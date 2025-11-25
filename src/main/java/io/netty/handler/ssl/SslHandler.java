package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.PendingWriteQueue;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.OneTimeTask;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

public class SslHandler extends ByteToMessageDecoder {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(SslHandler.class);
   private static final Pattern IGNORABLE_CLASS_IN_STACK = Pattern.compile("^.*(?:Socket|Datagram|Sctp|Udt)Channel.*$");
   private static final Pattern IGNORABLE_ERROR_MESSAGE = Pattern.compile("^.*(?:connection.*(?:reset|closed|abort|broken)|broken.*pipe).*$", 2);
   private static final SSLException SSLENGINE_CLOSED = new SSLException("SSLEngine closed already");
   private static final SSLException HANDSHAKE_TIMED_OUT = new SSLException("handshake timed out");
   private static final ClosedChannelException CHANNEL_CLOSED = new ClosedChannelException();
   private volatile ChannelHandlerContext ctx;
   private final SSLEngine engine;
   private final int maxPacketBufferSize;
   private final ByteBuffer[] singleBuffer;
   private final boolean wantsDirectBuffer;
   private final boolean wantsLargeOutboundNetworkBuffer;
   private boolean wantsInboundHeapBuffer;
   private final boolean startTls;
   private boolean sentFirstMessage;
   private boolean flushedBeforeHandshake;
   private boolean readDuringHandshake;
   private PendingWriteQueue pendingUnencryptedWrites;
   private Promise<Channel> handshakePromise;
   private final SslHandler.LazyChannelPromise sslCloseFuture;
   private boolean needsFlush;
   private int packetLength;
   private volatile long handshakeTimeoutMillis;
   private volatile long closeNotifyTimeoutMillis;

   public SslHandler(SSLEngine engine) {
      this(engine, false);
   }

   public SslHandler(SSLEngine engine, boolean startTls) {
      this.singleBuffer = new ByteBuffer[1];
      this.handshakePromise = new SslHandler.LazyChannelPromise();
      this.sslCloseFuture = new SslHandler.LazyChannelPromise();
      this.handshakeTimeoutMillis = 10000L;
      this.closeNotifyTimeoutMillis = 3000L;
      if (engine == null) {
         throw new NullPointerException("engine");
      } else {
         this.engine = engine;
         this.startTls = startTls;
         this.maxPacketBufferSize = engine.getSession().getPacketBufferSize();
         boolean opensslEngine = engine instanceof OpenSslEngine;
         this.wantsDirectBuffer = opensslEngine;
         this.wantsLargeOutboundNetworkBuffer = !opensslEngine;
         this.setCumulator(opensslEngine ? COMPOSITE_CUMULATOR : MERGE_CUMULATOR);
      }
   }

   public long getHandshakeTimeoutMillis() {
      return this.handshakeTimeoutMillis;
   }

   public void setHandshakeTimeout(long handshakeTimeout, TimeUnit unit) {
      if (unit == null) {
         throw new NullPointerException("unit");
      } else {
         this.setHandshakeTimeoutMillis(unit.toMillis(handshakeTimeout));
      }
   }

   public void setHandshakeTimeoutMillis(long handshakeTimeoutMillis) {
      if (handshakeTimeoutMillis < 0L) {
         throw new IllegalArgumentException("handshakeTimeoutMillis: " + handshakeTimeoutMillis + " (expected: >= 0)");
      } else {
         this.handshakeTimeoutMillis = handshakeTimeoutMillis;
      }
   }

   public long getCloseNotifyTimeoutMillis() {
      return this.closeNotifyTimeoutMillis;
   }

   public void setCloseNotifyTimeout(long closeNotifyTimeout, TimeUnit unit) {
      if (unit == null) {
         throw new NullPointerException("unit");
      } else {
         this.setCloseNotifyTimeoutMillis(unit.toMillis(closeNotifyTimeout));
      }
   }

   public void setCloseNotifyTimeoutMillis(long closeNotifyTimeoutMillis) {
      if (closeNotifyTimeoutMillis < 0L) {
         throw new IllegalArgumentException("closeNotifyTimeoutMillis: " + closeNotifyTimeoutMillis + " (expected: >= 0)");
      } else {
         this.closeNotifyTimeoutMillis = closeNotifyTimeoutMillis;
      }
   }

   public SSLEngine engine() {
      return this.engine;
   }

   public Future<Channel> handshakeFuture() {
      return this.handshakePromise;
   }

   public ChannelFuture close() {
      return this.close(this.ctx.newPromise());
   }

   public ChannelFuture close(final ChannelPromise future) {
      final ChannelHandlerContext ctx = this.ctx;
      ctx.executor().execute(new Runnable() {
         public void run() {
            SslHandler.this.engine.closeOutbound();

            try {
               SslHandler.this.write(ctx, Unpooled.EMPTY_BUFFER, future);
               SslHandler.this.flush(ctx);
            } catch (Exception var2) {
               if (!future.tryFailure(var2)) {
                  SslHandler.logger.warn("{} flush() raised a masked exception.", ctx.channel(), var2);
               }
            }

         }
      });
      return future;
   }

   public Future<Channel> sslCloseFuture() {
      return this.sslCloseFuture;
   }

   public void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
      if (!this.pendingUnencryptedWrites.isEmpty()) {
         this.pendingUnencryptedWrites.removeAndFailAll(new ChannelException("Pending write on removal of SslHandler"));
      }

   }

   public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
      this.closeOutboundAndChannel(ctx, promise, true);
   }

   public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
      this.closeOutboundAndChannel(ctx, promise, false);
   }

   public void read(ChannelHandlerContext ctx) throws Exception {
      if (!this.handshakePromise.isDone()) {
         this.readDuringHandshake = true;
      }

      ctx.read();
   }

   public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      this.pendingUnencryptedWrites.add(msg, promise);
   }

   public void flush(ChannelHandlerContext ctx) throws Exception {
      if (this.startTls && !this.sentFirstMessage) {
         this.sentFirstMessage = true;
         this.pendingUnencryptedWrites.removeAndWriteAll();
         ctx.flush();
      } else {
         if (this.pendingUnencryptedWrites.isEmpty()) {
            this.pendingUnencryptedWrites.add(Unpooled.EMPTY_BUFFER, ctx.newPromise());
         }

         if (!this.handshakePromise.isDone()) {
            this.flushedBeforeHandshake = true;
         }

         this.wrap(ctx, false);
         ctx.flush();
      }
   }

   private void wrap(ChannelHandlerContext ctx, boolean inUnwrap) throws SSLException {
      ByteBuf out = null;
      ChannelPromise promise = null;
      ByteBufAllocator alloc = ctx.alloc();

      try {
         while(true) {
            Object msg = this.pendingUnencryptedWrites.current();
            if (msg == null) {
               return;
            }

            if (!(msg instanceof ByteBuf)) {
               this.pendingUnencryptedWrites.removeAndWrite();
            } else {
               ByteBuf buf = (ByteBuf)msg;
               if (out == null) {
                  out = this.allocateOutNetBuf(ctx, buf.readableBytes());
               }

               SSLEngineResult result = this.wrap(alloc, this.engine, buf, out);
               if (!buf.isReadable()) {
                  promise = this.pendingUnencryptedWrites.remove();
               } else {
                  promise = null;
               }

               if (result.getStatus() == Status.CLOSED) {
                  this.pendingUnencryptedWrites.removeAndFailAll(SSLENGINE_CLOSED);
                  return;
               }

               switch(result.getHandshakeStatus()) {
               case NEED_TASK:
                  this.runDelegatedTasks();
                  break;
               case FINISHED:
                  this.setHandshakeSuccess();
               case NOT_HANDSHAKING:
                  this.setHandshakeSuccessIfStillHandshaking();
               case NEED_WRAP:
                  this.finishWrap(ctx, out, promise, inUnwrap);
                  promise = null;
                  out = null;
                  break;
               case NEED_UNWRAP:
                  return;
               default:
                  throw new IllegalStateException("Unknown handshake status: " + result.getHandshakeStatus());
               }
            }
         }
      } catch (SSLException var12) {
         this.setHandshakeFailure(ctx, var12);
         throw var12;
      } finally {
         this.finishWrap(ctx, out, promise, inUnwrap);
      }
   }

   private void finishWrap(ChannelHandlerContext ctx, ByteBuf out, ChannelPromise promise, boolean inUnwrap) {
      if (out == null) {
         out = Unpooled.EMPTY_BUFFER;
      } else if (!out.isReadable()) {
         out.release();
         out = Unpooled.EMPTY_BUFFER;
      }

      if (promise != null) {
         ctx.write(out, promise);
      } else {
         ctx.write(out);
      }

      if (inUnwrap) {
         this.needsFlush = true;
      }

   }

   private void wrapNonAppData(ChannelHandlerContext ctx, boolean inUnwrap) throws SSLException {
      ByteBuf out = null;
      ByteBufAllocator alloc = ctx.alloc();

      try {
         SSLEngineResult result;
         try {
            do {
               if (out == null) {
                  out = this.allocateOutNetBuf(ctx, 0);
               }

               result = this.wrap(alloc, this.engine, Unpooled.EMPTY_BUFFER, out);
               if (result.bytesProduced() > 0) {
                  ctx.write(out);
                  if (inUnwrap) {
                     this.needsFlush = true;
                  }

                  out = null;
               }

               switch(result.getHandshakeStatus()) {
               case NEED_TASK:
                  this.runDelegatedTasks();
                  break;
               case FINISHED:
                  this.setHandshakeSuccess();
                  break;
               case NOT_HANDSHAKING:
                  this.setHandshakeSuccessIfStillHandshaking();
                  if (!inUnwrap) {
                     this.unwrapNonAppData(ctx);
                  }
               case NEED_WRAP:
                  break;
               case NEED_UNWRAP:
                  if (!inUnwrap) {
                     this.unwrapNonAppData(ctx);
                  }
                  break;
               default:
                  throw new IllegalStateException("Unknown handshake status: " + result.getHandshakeStatus());
               }
            } while(result.bytesProduced() != 0);
         } catch (SSLException var9) {
            this.setHandshakeFailure(ctx, var9);
            throw var9;
         }
      } finally {
         if (out != null) {
            out.release();
         }

      }

   }

   private SSLEngineResult wrap(ByteBufAllocator alloc, SSLEngine engine, ByteBuf in, ByteBuf out) throws SSLException {
      ByteBuf newDirectIn = null;

      try {
         int readerIndex = in.readerIndex();
         int readableBytes = in.readableBytes();
         ByteBuffer[] in0;
         if (!in.isDirect() && this.wantsDirectBuffer) {
            newDirectIn = alloc.directBuffer(readableBytes);
            newDirectIn.writeBytes(in, readerIndex, readableBytes);
            in0 = this.singleBuffer;
            in0[0] = newDirectIn.internalNioBuffer(0, readableBytes);
         } else if (!(in instanceof CompositeByteBuf) && in.nioBufferCount() == 1) {
            in0 = this.singleBuffer;
            in0[0] = in.internalNioBuffer(readerIndex, readableBytes);
         } else {
            in0 = in.nioBuffers();
         }

         while(true) {
            ByteBuffer out0 = out.nioBuffer(out.writerIndex(), out.writableBytes());
            SSLEngineResult result = engine.wrap(in0, out0);
            in.skipBytes(result.bytesConsumed());
            out.writerIndex(out.writerIndex() + result.bytesProduced());
            switch(result.getStatus()) {
            case BUFFER_OVERFLOW:
               out.ensureWritable(this.maxPacketBufferSize);
               break;
            default:
               SSLEngineResult var11 = result;
               return var11;
            }
         }
      } finally {
         this.singleBuffer[0] = null;
         if (newDirectIn != null) {
            newDirectIn.release();
         }

      }
   }

   public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      this.setHandshakeFailure(ctx, CHANNEL_CLOSED);
      super.channelInactive(ctx);
   }

   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      if (this.ignoreException(cause)) {
         if (logger.isDebugEnabled()) {
            logger.debug("{} Swallowing a harmless 'connection reset by peer / broken pipe' error that occurred while writing close_notify in response to the peer's close_notify", ctx.channel(), cause);
         }

         if (ctx.channel().isActive()) {
            ctx.close();
         }
      } else {
         ctx.fireExceptionCaught(cause);
      }

   }

   private boolean ignoreException(Throwable t) {
      if (!(t instanceof SSLException) && t instanceof IOException && this.sslCloseFuture.isDone()) {
         String message = String.valueOf(t.getMessage()).toLowerCase();
         if (IGNORABLE_ERROR_MESSAGE.matcher(message).matches()) {
            return true;
         }

         StackTraceElement[] elements = t.getStackTrace();
         StackTraceElement[] arr$ = elements;
         int len$ = elements.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            StackTraceElement element = arr$[i$];
            String classname = element.getClassName();
            String methodname = element.getMethodName();
            if (!classname.startsWith("io.netty.") && "read".equals(methodname)) {
               if (IGNORABLE_CLASS_IN_STACK.matcher(classname).matches()) {
                  return true;
               }

               try {
                  Class<?> clazz = PlatformDependent.getClassLoader(this.getClass()).loadClass(classname);
                  if (SocketChannel.class.isAssignableFrom(clazz) || DatagramChannel.class.isAssignableFrom(clazz)) {
                     return true;
                  }

                  if (PlatformDependent.javaVersion() >= 7 && "com.sun.nio.sctp.SctpChannel".equals(clazz.getSuperclass().getName())) {
                     return true;
                  }
               } catch (ClassNotFoundException var11) {
               }
            }
         }
      }

      return false;
   }

   public static boolean isEncrypted(ByteBuf buffer) {
      if (buffer.readableBytes() < 5) {
         throw new IllegalArgumentException("buffer must have at least 5 readable bytes");
      } else {
         return getEncryptedPacketLength(buffer, buffer.readerIndex()) != -1;
      }
   }

   private static int getEncryptedPacketLength(ByteBuf buffer, int offset) {
      int packetLength = 0;
      boolean tls;
      switch(buffer.getUnsignedByte(offset)) {
      case 20:
      case 21:
      case 22:
      case 23:
         tls = true;
         break;
      default:
         tls = false;
      }

      if (tls) {
         int majorVersion = buffer.getUnsignedByte(offset + 1);
         if (majorVersion == 3) {
            packetLength = buffer.getUnsignedShort(offset + 3) + 5;
            if (packetLength <= 5) {
               tls = false;
            }
         } else {
            tls = false;
         }
      }

      if (!tls) {
         boolean sslv2 = true;
         int headerLength = (buffer.getUnsignedByte(offset) & 128) != 0 ? 2 : 3;
         int majorVersion = buffer.getUnsignedByte(offset + headerLength + 1);
         if (majorVersion != 2 && majorVersion != 3) {
            sslv2 = false;
         } else {
            if (headerLength == 2) {
               packetLength = (buffer.getShort(offset) & 32767) + 2;
            } else {
               packetLength = (buffer.getShort(offset) & 16383) + 3;
            }

            if (packetLength <= headerLength) {
               sslv2 = false;
            }
         }

         if (!sslv2) {
            return -1;
         }
      }

      return packetLength;
   }

   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws SSLException {
      int startOffset = in.readerIndex();
      int endOffset = in.writerIndex();
      int offset = startOffset;
      int totalLength = 0;
      if (this.packetLength > 0) {
         if (endOffset - startOffset < this.packetLength) {
            return;
         }

         offset = startOffset + this.packetLength;
         totalLength = this.packetLength;
         this.packetLength = 0;
      }

      boolean nonSslRecord;
      int newTotalLength;
      for(nonSslRecord = false; totalLength < 18713; totalLength = newTotalLength) {
         int readableBytes = endOffset - offset;
         if (readableBytes < 5) {
            break;
         }

         int packetLength = getEncryptedPacketLength(in, offset);
         if (packetLength == -1) {
            nonSslRecord = true;
            break;
         }

         assert packetLength > 0;

         if (packetLength > readableBytes) {
            this.packetLength = packetLength;
            break;
         }

         newTotalLength = totalLength + packetLength;
         if (newTotalLength > 18713) {
            break;
         }

         offset += packetLength;
      }

      if (totalLength > 0) {
         in.skipBytes(totalLength);
         if (in.isDirect() && this.wantsInboundHeapBuffer) {
            ByteBuf copy = ctx.alloc().heapBuffer(totalLength);

            try {
               copy.writeBytes(in, startOffset, totalLength);
               this.unwrap(ctx, copy, 0, totalLength);
            } finally {
               copy.release();
            }
         } else {
            this.unwrap(ctx, in, startOffset, totalLength);
         }
      }

      if (nonSslRecord) {
         NotSslRecordException e = new NotSslRecordException("not an SSL/TLS record: " + ByteBufUtil.hexDump(in));
         in.skipBytes(in.readableBytes());
         ctx.fireExceptionCaught(e);
         this.setHandshakeFailure(ctx, e);
      }

   }

   public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
      if (this.needsFlush) {
         this.needsFlush = false;
         ctx.flush();
      }

      if (!this.handshakePromise.isDone() && !ctx.channel().config().isAutoRead()) {
         ctx.read();
      }

      ctx.fireChannelReadComplete();
   }

   private void unwrapNonAppData(ChannelHandlerContext ctx) throws SSLException {
      this.unwrap(ctx, Unpooled.EMPTY_BUFFER, 0, 0);
   }

   private void unwrap(ChannelHandlerContext ctx, ByteBuf packet, int offset, int length) throws SSLException {
      boolean wrapLater = false;
      boolean notifyClosure = false;
      ByteBuf decodeOut = this.allocate(ctx, length);

      try {
         while(true) {
            SSLEngineResult result = this.unwrap(this.engine, packet, offset, length, decodeOut);
            Status status = result.getStatus();
            HandshakeStatus handshakeStatus = result.getHandshakeStatus();
            int produced = result.bytesProduced();
            int consumed = result.bytesConsumed();
            offset += consumed;
            length -= consumed;
            if (status == Status.CLOSED) {
               notifyClosure = true;
            }

            switch(handshakeStatus) {
            case NEED_TASK:
               this.runDelegatedTasks();
               break;
            case FINISHED:
               this.setHandshakeSuccess();
               wrapLater = true;
               continue;
            case NOT_HANDSHAKING:
               if (this.setHandshakeSuccessIfStillHandshaking()) {
                  wrapLater = true;
                  continue;
               }

               if (this.flushedBeforeHandshake) {
                  this.flushedBeforeHandshake = false;
                  wrapLater = true;
               }
               break;
            case NEED_WRAP:
               this.wrapNonAppData(ctx, true);
            case NEED_UNWRAP:
               break;
            default:
               throw new IllegalStateException("unknown handshake status: " + handshakeStatus);
            }

            if (status == Status.BUFFER_UNDERFLOW || consumed == 0 && produced == 0) {
               if (wrapLater) {
                  this.wrap(ctx, true);
               }

               if (notifyClosure) {
                  this.sslCloseFuture.trySuccess(ctx.channel());
               }
               break;
            }
         }
      } catch (SSLException var16) {
         this.setHandshakeFailure(ctx, var16);
         throw var16;
      } finally {
         if (decodeOut.isReadable()) {
            ctx.fireChannelRead(decodeOut);
         } else {
            decodeOut.release();
         }

      }

   }

   private SSLEngineResult unwrap(SSLEngine engine, ByteBuf in, int readerIndex, int len, ByteBuf out) throws SSLException {
      int nioBufferCount = in.nioBufferCount();
      int writableBytes;
      if (engine instanceof OpenSslEngine && nioBufferCount > 1) {
         OpenSslEngine opensslEngine = (OpenSslEngine)engine;
         int overflows = 0;
         ByteBuffer[] in0 = in.nioBuffers(readerIndex, len);

         try {
            while(true) {
               writableBytes = out.writerIndex();
               int writableBytes = out.writableBytes();
               ByteBuffer out0;
               if (out.nioBufferCount() == 1) {
                  out0 = out.internalNioBuffer(writableBytes, writableBytes);
               } else {
                  out0 = out.nioBuffer(writableBytes, writableBytes);
               }

               this.singleBuffer[0] = out0;
               SSLEngineResult result = opensslEngine.unwrap(in0, this.singleBuffer);
               out.writerIndex(out.writerIndex() + result.bytesProduced());
               switch(result.getStatus()) {
               case BUFFER_OVERFLOW:
                  int max = engine.getSession().getApplicationBufferSize();
                  switch(overflows++) {
                  case 0:
                     out.ensureWritable(Math.min(max, in.readableBytes()));
                     continue;
                  default:
                     out.ensureWritable(max);
                     continue;
                  }
               default:
                  SSLEngineResult var15 = result;
                  return var15;
               }
            }
         } finally {
            this.singleBuffer[0] = null;
         }
      } else {
         int overflows = 0;
         ByteBuffer in0;
         if (nioBufferCount == 1) {
            in0 = in.internalNioBuffer(readerIndex, len);
         } else {
            in0 = in.nioBuffer(readerIndex, len);
         }

         while(true) {
            int writerIndex = out.writerIndex();
            writableBytes = out.writableBytes();
            ByteBuffer out0;
            if (out.nioBufferCount() == 1) {
               out0 = out.internalNioBuffer(writerIndex, writableBytes);
            } else {
               out0 = out.nioBuffer(writerIndex, writableBytes);
            }

            SSLEngineResult result = engine.unwrap(in0, out0);
            out.writerIndex(out.writerIndex() + result.bytesProduced());
            switch(result.getStatus()) {
            case BUFFER_OVERFLOW:
               int max = engine.getSession().getApplicationBufferSize();
               switch(overflows++) {
               case 0:
                  out.ensureWritable(Math.min(max, in.readableBytes()));
                  continue;
               default:
                  out.ensureWritable(max);
                  continue;
               }
            default:
               return result;
            }
         }
      }
   }

   private void runDelegatedTasks() {
      while(true) {
         Runnable task = this.engine.getDelegatedTask();
         if (task == null) {
            return;
         }

         task.run();
      }
   }

   private boolean setHandshakeSuccessIfStillHandshaking() {
      if (!this.handshakePromise.isDone()) {
         this.setHandshakeSuccess();
         return true;
      } else {
         return false;
      }
   }

   private void setHandshakeSuccess() {
      String cipherSuite = String.valueOf(this.engine.getSession().getCipherSuite());
      if (!this.wantsDirectBuffer && (cipherSuite.contains("_GCM_") || cipherSuite.contains("-GCM-"))) {
         this.wantsInboundHeapBuffer = true;
      }

      this.handshakePromise.trySuccess(this.ctx.channel());
      if (logger.isDebugEnabled()) {
         logger.debug("{} HANDSHAKEN: {}", this.ctx.channel(), this.engine.getSession().getCipherSuite());
      }

      this.ctx.fireUserEventTriggered(SslHandshakeCompletionEvent.SUCCESS);
      if (this.readDuringHandshake && !this.ctx.channel().config().isAutoRead()) {
         this.readDuringHandshake = false;
         this.ctx.read();
      }

   }

   private void setHandshakeFailure(ChannelHandlerContext ctx, Throwable cause) {
      this.engine.closeOutbound();

      try {
         this.engine.closeInbound();
      } catch (SSLException var5) {
         String msg = var5.getMessage();
         if (msg == null || !msg.contains("possible truncation attack")) {
            logger.debug("{} SSLEngine.closeInbound() raised an exception.", ctx.channel(), var5);
         }
      }

      this.notifyHandshakeFailure(cause);
      this.pendingUnencryptedWrites.removeAndFailAll(cause);
   }

   private void notifyHandshakeFailure(Throwable cause) {
      if (this.handshakePromise.tryFailure(cause)) {
         this.ctx.fireUserEventTriggered(new SslHandshakeCompletionEvent(cause));
         this.ctx.close();
      }

   }

   private void closeOutboundAndChannel(ChannelHandlerContext ctx, ChannelPromise promise, boolean disconnect) throws Exception {
      if (!ctx.channel().isActive()) {
         if (disconnect) {
            ctx.disconnect(promise);
         } else {
            ctx.close(promise);
         }

      } else {
         this.engine.closeOutbound();
         ChannelPromise closeNotifyFuture = ctx.newPromise();
         this.write(ctx, Unpooled.EMPTY_BUFFER, closeNotifyFuture);
         this.flush(ctx);
         this.safeClose(ctx, closeNotifyFuture, promise);
      }
   }

   public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      this.ctx = ctx;
      this.pendingUnencryptedWrites = new PendingWriteQueue(ctx);
      if (ctx.channel().isActive() && this.engine.getUseClientMode()) {
         this.handshake((Promise)null);
      }

   }

   public Future<Channel> renegotiate() {
      ChannelHandlerContext ctx = this.ctx;
      if (ctx == null) {
         throw new IllegalStateException();
      } else {
         return this.renegotiate(ctx.executor().newPromise());
      }
   }

   public Future<Channel> renegotiate(final Promise<Channel> promise) {
      if (promise == null) {
         throw new NullPointerException("promise");
      } else {
         ChannelHandlerContext ctx = this.ctx;
         if (ctx == null) {
            throw new IllegalStateException();
         } else {
            EventExecutor executor = ctx.executor();
            if (!executor.inEventLoop()) {
               executor.execute(new OneTimeTask() {
                  public void run() {
                     SslHandler.this.handshake(promise);
                  }
               });
               return promise;
            } else {
               this.handshake(promise);
               return promise;
            }
         }
      }
   }

   private void handshake(final Promise<Channel> newHandshakePromise) {
      final Promise p;
      if (newHandshakePromise != null) {
         Promise<Channel> oldHandshakePromise = this.handshakePromise;
         if (!oldHandshakePromise.isDone()) {
            oldHandshakePromise.addListener(new FutureListener<Channel>() {
               public void operationComplete(Future<Channel> future) throws Exception {
                  if (future.isSuccess()) {
                     newHandshakePromise.setSuccess(future.getNow());
                  } else {
                     newHandshakePromise.setFailure(future.cause());
                  }

               }
            });
            return;
         }

         p = newHandshakePromise;
         this.handshakePromise = newHandshakePromise;
      } else {
         p = this.handshakePromise;

         assert !p.isDone();
      }

      ChannelHandlerContext ctx = this.ctx;

      try {
         this.engine.beginHandshake();
         this.wrapNonAppData(ctx, false);
         ctx.flush();
      } catch (Exception var7) {
         this.notifyHandshakeFailure(var7);
      }

      long handshakeTimeoutMillis = this.handshakeTimeoutMillis;
      if (handshakeTimeoutMillis > 0L && !p.isDone()) {
         final ScheduledFuture<?> timeoutFuture = ctx.executor().schedule(new Runnable() {
            public void run() {
               if (!p.isDone()) {
                  SslHandler.this.notifyHandshakeFailure(SslHandler.HANDSHAKE_TIMED_OUT);
               }
            }
         }, handshakeTimeoutMillis, TimeUnit.MILLISECONDS);
         p.addListener(new FutureListener<Channel>() {
            public void operationComplete(Future<Channel> f) throws Exception {
               timeoutFuture.cancel(false);
            }
         });
      }
   }

   public void channelActive(ChannelHandlerContext ctx) throws Exception {
      if (!this.startTls && this.engine.getUseClientMode()) {
         this.handshake((Promise)null);
      }

      ctx.fireChannelActive();
   }

   private void safeClose(final ChannelHandlerContext ctx, ChannelFuture flushFuture, final ChannelPromise promise) {
      if (!ctx.channel().isActive()) {
         ctx.close(promise);
      } else {
         final io.netty.util.concurrent.ScheduledFuture timeoutFuture;
         if (this.closeNotifyTimeoutMillis > 0L) {
            timeoutFuture = ctx.executor().schedule(new Runnable() {
               public void run() {
                  SslHandler.logger.warn("{} Last write attempt timed out; force-closing the connection.", (Object)ctx.channel());
                  ctx.close(promise);
               }
            }, this.closeNotifyTimeoutMillis, TimeUnit.MILLISECONDS);
         } else {
            timeoutFuture = null;
         }

         flushFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture f) throws Exception {
               if (timeoutFuture != null) {
                  timeoutFuture.cancel(false);
               }

               ctx.close(promise);
            }
         });
      }
   }

   private ByteBuf allocate(ChannelHandlerContext ctx, int capacity) {
      ByteBufAllocator alloc = ctx.alloc();
      return this.wantsDirectBuffer ? alloc.directBuffer(capacity) : alloc.buffer(capacity);
   }

   private ByteBuf allocateOutNetBuf(ChannelHandlerContext ctx, int pendingBytes) {
      return this.wantsLargeOutboundNetworkBuffer ? this.allocate(ctx, this.maxPacketBufferSize) : this.allocate(ctx, Math.min(pendingBytes + 2329, this.maxPacketBufferSize));
   }

   static {
      SSLENGINE_CLOSED.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
      HANDSHAKE_TIMED_OUT.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
      CHANNEL_CLOSED.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
   }

   private final class LazyChannelPromise extends DefaultPromise<Channel> {
      private LazyChannelPromise() {
      }

      protected EventExecutor executor() {
         if (SslHandler.this.ctx == null) {
            throw new IllegalStateException();
         } else {
            return SslHandler.this.ctx.executor();
         }
      }

      // $FF: synthetic method
      LazyChannelPromise(Object x1) {
         this();
      }
   }
}
