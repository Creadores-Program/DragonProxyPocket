package io.netty.channel.epoll;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.channel.unix.FileDescriptor;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractEpollStreamChannel extends AbstractEpollChannel {
   private static final String EXPECTED_TYPES = " (expected: " + StringUtil.simpleClassName(ByteBuf.class) + ", " + StringUtil.simpleClassName(DefaultFileRegion.class) + ')';
   private volatile boolean inputShutdown;
   private volatile boolean outputShutdown;

   protected AbstractEpollStreamChannel(Channel parent, int fd) {
      super(parent, fd, Native.EPOLLIN, true);
      this.flags |= Native.EPOLLRDHUP;
   }

   protected AbstractEpollStreamChannel(int fd) {
      super(fd, Native.EPOLLIN);
      this.flags |= Native.EPOLLRDHUP;
   }

   protected AbstractEpollStreamChannel(FileDescriptor fd) {
      super((Channel)null, fd, Native.EPOLLIN, Native.getSoError(fd.intValue()) == 0);
   }

   protected AbstractEpollChannel.AbstractEpollUnsafe newUnsafe() {
      return new AbstractEpollStreamChannel.EpollStreamUnsafe();
   }

   private boolean writeBytes(ChannelOutboundBuffer in, ByteBuf buf, int writeSpinCount) throws Exception {
      int readableBytes = buf.readableBytes();
      if (readableBytes == 0) {
         in.remove();
         return true;
      } else if (!buf.hasMemoryAddress() && buf.nioBufferCount() != 1) {
         ByteBuffer[] nioBuffers = buf.nioBuffers();
         return this.writeBytesMultiple(in, nioBuffers, nioBuffers.length, (long)readableBytes, writeSpinCount);
      } else {
         int writtenBytes = this.doWriteBytes(buf, writeSpinCount);
         in.removeBytes((long)writtenBytes);
         return writtenBytes == readableBytes;
      }
   }

   private boolean writeBytesMultiple(ChannelOutboundBuffer in, IovArray array, int writeSpinCount) throws IOException {
      long expectedWrittenBytes = array.size();
      int cnt = array.count();

      assert expectedWrittenBytes != 0L;

      assert cnt != 0;

      boolean done = false;
      int offset = 0;
      int end = offset + cnt;

      for(int i = writeSpinCount - 1; i >= 0; --i) {
         long localWrittenBytes = Native.writevAddresses(this.fd().intValue(), array.memoryAddress(offset), cnt);
         if (localWrittenBytes == 0L) {
            break;
         }

         expectedWrittenBytes -= localWrittenBytes;
         if (expectedWrittenBytes == 0L) {
            done = true;
            break;
         }

         do {
            long bytes = array.processWritten(offset, localWrittenBytes);
            if (bytes == -1L) {
               break;
            }

            ++offset;
            --cnt;
            localWrittenBytes -= bytes;
         } while(offset < end && localWrittenBytes > 0L);
      }

      if (!done) {
         this.setFlag(Native.EPOLLOUT);
      }

      in.removeBytes(expectedWrittenBytes - expectedWrittenBytes);
      return done;
   }

   private boolean writeBytesMultiple(ChannelOutboundBuffer in, ByteBuffer[] nioBuffers, int nioBufferCnt, long expectedWrittenBytes, int writeSpinCount) throws IOException {
      assert expectedWrittenBytes != 0L;

      boolean done = false;
      int offset = 0;
      int end = offset + nioBufferCnt;

      for(int i = writeSpinCount - 1; i >= 0; --i) {
         long localWrittenBytes = Native.writev(this.fd().intValue(), nioBuffers, offset, nioBufferCnt);
         if (localWrittenBytes == 0L) {
            break;
         }

         expectedWrittenBytes -= localWrittenBytes;
         if (expectedWrittenBytes == 0L) {
            done = true;
            break;
         }

         do {
            ByteBuffer buffer = nioBuffers[offset];
            int pos = buffer.position();
            int bytes = buffer.limit() - pos;
            if ((long)bytes > localWrittenBytes) {
               buffer.position(pos + (int)localWrittenBytes);
               break;
            }

            ++offset;
            --nioBufferCnt;
            localWrittenBytes -= (long)bytes;
         } while(offset < end && localWrittenBytes > 0L);
      }

      in.removeBytes(expectedWrittenBytes - expectedWrittenBytes);
      if (!done) {
         this.setFlag(Native.EPOLLOUT);
      }

      return done;
   }

   private boolean writeFileRegion(ChannelOutboundBuffer in, DefaultFileRegion region, int writeSpinCount) throws Exception {
      long regionCount = region.count();
      if (region.transfered() >= regionCount) {
         in.remove();
         return true;
      } else {
         long baseOffset = region.position();
         boolean done = false;
         long flushedAmount = 0L;

         for(int i = writeSpinCount - 1; i >= 0; --i) {
            long offset = region.transfered();
            long localFlushedAmount = Native.sendfile(this.fd().intValue(), region, baseOffset, offset, regionCount - offset);
            if (localFlushedAmount == 0L) {
               break;
            }

            flushedAmount += localFlushedAmount;
            if (region.transfered() >= regionCount) {
               done = true;
               break;
            }
         }

         if (flushedAmount > 0L) {
            in.progress(flushedAmount);
         }

         if (done) {
            in.remove();
         } else {
            this.setFlag(Native.EPOLLOUT);
         }

         return done;
      }
   }

   protected void doWrite(ChannelOutboundBuffer in) throws Exception {
      int writeSpinCount = this.config().getWriteSpinCount();

      while(true) {
         int msgCount = in.size();
         if (msgCount == 0) {
            this.clearFlag(Native.EPOLLOUT);
            break;
         }

         if (msgCount > 1 && in.current() instanceof ByteBuf) {
            if (!this.doWriteMultiple(in, writeSpinCount)) {
               break;
            }
         } else if (!this.doWriteSingle(in, writeSpinCount)) {
            break;
         }
      }

   }

   protected boolean doWriteSingle(ChannelOutboundBuffer in, int writeSpinCount) throws Exception {
      Object msg = in.current();
      if (msg instanceof ByteBuf) {
         ByteBuf buf = (ByteBuf)msg;
         if (!this.writeBytes(in, buf, writeSpinCount)) {
            return false;
         }
      } else {
         if (!(msg instanceof DefaultFileRegion)) {
            throw new Error();
         }

         DefaultFileRegion region = (DefaultFileRegion)msg;
         if (!this.writeFileRegion(in, region, writeSpinCount)) {
            return false;
         }
      }

      return true;
   }

   private boolean doWriteMultiple(ChannelOutboundBuffer in, int writeSpinCount) throws Exception {
      int cnt;
      if (PlatformDependent.hasUnsafe()) {
         IovArray array = IovArrayThreadLocal.get(in);
         cnt = array.count();
         if (cnt >= 1) {
            if (!this.writeBytesMultiple(in, array, writeSpinCount)) {
               return false;
            }
         } else {
            in.removeBytes(0L);
         }
      } else {
         ByteBuffer[] buffers = in.nioBuffers();
         cnt = in.nioBufferCount();
         if (cnt >= 1) {
            if (!this.writeBytesMultiple(in, buffers, cnt, in.nioBufferSize(), writeSpinCount)) {
               return false;
            }
         } else {
            in.removeBytes(0L);
         }
      }

      return true;
   }

   protected Object filterOutboundMessage(Object msg) {
      if (!(msg instanceof ByteBuf)) {
         if (msg instanceof DefaultFileRegion) {
            return msg;
         } else {
            throw new UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
         }
      } else {
         ByteBuf buf = (ByteBuf)msg;
         if (!buf.hasMemoryAddress() && (PlatformDependent.hasUnsafe() || !buf.isDirect())) {
            if (buf instanceof CompositeByteBuf) {
               CompositeByteBuf comp = (CompositeByteBuf)buf;
               if (!comp.isDirect() || comp.nioBufferCount() > Native.IOV_MAX) {
                  buf = this.newDirectBuffer(buf);

                  assert buf.hasMemoryAddress();
               }
            } else {
               buf = this.newDirectBuffer(buf);

               assert buf.hasMemoryAddress();
            }
         }

         return buf;
      }
   }

   protected boolean isInputShutdown0() {
      return this.inputShutdown;
   }

   protected boolean isOutputShutdown0() {
      return this.outputShutdown || !this.isActive();
   }

   protected void shutdownOutput0(ChannelPromise promise) {
      try {
         Native.shutdown(this.fd().intValue(), false, true);
         this.outputShutdown = true;
         promise.setSuccess();
      } catch (Throwable var3) {
         promise.setFailure(var3);
      }

   }

   protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
      if (localAddress != null) {
         Native.bind(this.fd().intValue(), localAddress);
      }

      boolean success = false;

      boolean var5;
      try {
         boolean connected = Native.connect(this.fd().intValue(), remoteAddress);
         if (!connected) {
            this.setFlag(Native.EPOLLOUT);
         }

         success = true;
         var5 = connected;
      } finally {
         if (!success) {
            this.doClose();
         }

      }

      return var5;
   }

   class EpollStreamUnsafe extends AbstractEpollChannel.AbstractEpollUnsafe {
      private ChannelPromise connectPromise;
      private ScheduledFuture<?> connectTimeoutFuture;
      private SocketAddress requestedRemoteAddress;
      private RecvByteBufAllocator.Handle allocHandle;

      EpollStreamUnsafe() {
         super();
      }

      private void closeOnRead(ChannelPipeline pipeline) {
         AbstractEpollStreamChannel.this.inputShutdown = true;
         if (AbstractEpollStreamChannel.this.isOpen()) {
            if (Boolean.TRUE.equals(AbstractEpollStreamChannel.this.config().getOption(ChannelOption.ALLOW_HALF_CLOSURE))) {
               this.clearEpollIn0();
               pipeline.fireUserEventTriggered(ChannelInputShutdownEvent.INSTANCE);
            } else {
               this.close(this.voidPromise());
            }
         }

      }

      private boolean handleReadException(ChannelPipeline pipeline, ByteBuf byteBuf, Throwable cause, boolean close) {
         if (byteBuf != null) {
            if (byteBuf.isReadable()) {
               this.readPending = false;
               pipeline.fireChannelRead(byteBuf);
            } else {
               byteBuf.release();
            }
         }

         pipeline.fireChannelReadComplete();
         pipeline.fireExceptionCaught(cause);
         if (!close && !(cause instanceof IOException)) {
            return false;
         } else {
            this.closeOnRead(pipeline);
            return true;
         }
      }

      public void connect(final SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
         if (promise.setUncancellable() && this.ensureOpen(promise)) {
            try {
               if (this.connectPromise != null) {
                  throw new IllegalStateException("connection attempt already made");
               }

               boolean wasActive = AbstractEpollStreamChannel.this.isActive();
               if (AbstractEpollStreamChannel.this.doConnect(remoteAddress, localAddress)) {
                  this.fulfillConnectPromise(promise, wasActive);
               } else {
                  this.connectPromise = promise;
                  this.requestedRemoteAddress = remoteAddress;
                  int connectTimeoutMillis = AbstractEpollStreamChannel.this.config().getConnectTimeoutMillis();
                  if (connectTimeoutMillis > 0) {
                     this.connectTimeoutFuture = AbstractEpollStreamChannel.this.eventLoop().schedule(new Runnable() {
                        public void run() {
                           ChannelPromise connectPromise = EpollStreamUnsafe.this.connectPromise;
                           ConnectTimeoutException cause = new ConnectTimeoutException("connection timed out: " + remoteAddress);
                           if (connectPromise != null && connectPromise.tryFailure(cause)) {
                              EpollStreamUnsafe.this.close(EpollStreamUnsafe.this.voidPromise());
                           }

                        }
                     }, (long)connectTimeoutMillis, TimeUnit.MILLISECONDS);
                  }

                  promise.addListener(new ChannelFutureListener() {
                     public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isCancelled()) {
                           if (EpollStreamUnsafe.this.connectTimeoutFuture != null) {
                              EpollStreamUnsafe.this.connectTimeoutFuture.cancel(false);
                           }

                           EpollStreamUnsafe.this.connectPromise = null;
                           EpollStreamUnsafe.this.close(EpollStreamUnsafe.this.voidPromise());
                        }

                     }
                  });
               }
            } catch (Throwable var6) {
               this.closeIfClosed();
               promise.tryFailure(this.annotateConnectException(var6, remoteAddress));
            }

         }
      }

      private void fulfillConnectPromise(ChannelPromise promise, boolean wasActive) {
         if (promise != null) {
            AbstractEpollStreamChannel.this.active = true;
            boolean promiseSet = promise.trySuccess();
            if (!wasActive && AbstractEpollStreamChannel.this.isActive()) {
               AbstractEpollStreamChannel.this.pipeline().fireChannelActive();
            }

            if (!promiseSet) {
               this.close(this.voidPromise());
            }

         }
      }

      private void fulfillConnectPromise(ChannelPromise promise, Throwable cause) {
         if (promise != null) {
            promise.tryFailure(cause);
            this.closeIfClosed();
         }
      }

      private void finishConnect() {
         assert AbstractEpollStreamChannel.this.eventLoop().inEventLoop();

         boolean connectStillInProgress = false;

         try {
            boolean wasActive = AbstractEpollStreamChannel.this.isActive();
            if (this.doFinishConnect()) {
               this.fulfillConnectPromise(this.connectPromise, wasActive);
               return;
            }

            connectStillInProgress = true;
         } catch (Throwable var6) {
            this.fulfillConnectPromise(this.connectPromise, this.annotateConnectException(var6, this.requestedRemoteAddress));
            return;
         } finally {
            if (!connectStillInProgress) {
               if (this.connectTimeoutFuture != null) {
                  this.connectTimeoutFuture.cancel(false);
               }

               this.connectPromise = null;
            }

         }

      }

      void epollOutReady() {
         if (this.connectPromise != null) {
            this.finishConnect();
         } else {
            super.epollOutReady();
         }

      }

      private boolean doFinishConnect() throws Exception {
         if (Native.finishConnect(AbstractEpollStreamChannel.this.fd().intValue())) {
            AbstractEpollStreamChannel.this.clearFlag(Native.EPOLLOUT);
            return true;
         } else {
            AbstractEpollStreamChannel.this.setFlag(Native.EPOLLOUT);
            return false;
         }
      }

      void epollRdHupReady() {
         if (AbstractEpollStreamChannel.this.isActive()) {
            this.epollInReady();
         } else {
            this.closeOnRead(AbstractEpollStreamChannel.this.pipeline());
         }

      }

      void epollInReady() {
         ChannelConfig config = AbstractEpollStreamChannel.this.config();
         boolean edgeTriggered = AbstractEpollStreamChannel.this.isFlagSet(Native.EPOLLET);
         if (!this.readPending && !edgeTriggered && !config.isAutoRead()) {
            this.clearEpollIn0();
         } else {
            ChannelPipeline pipeline = AbstractEpollStreamChannel.this.pipeline();
            ByteBufAllocator allocator = config.getAllocator();
            RecvByteBufAllocator.Handle allocHandle = this.allocHandle;
            if (allocHandle == null) {
               this.allocHandle = allocHandle = config.getRecvByteBufAllocator().newHandle();
            }

            ByteBuf byteBuf = null;
            boolean close = false;

            try {
               int maxMessagesPerRead = edgeTriggered ? Integer.MAX_VALUE : config.getMaxMessagesPerRead();
               int messages = 0;
               int totalReadAmount = 0;

               do {
                  byteBuf = allocHandle.allocate(allocator);
                  int writable = byteBuf.writableBytes();
                  int localReadAmount = AbstractEpollStreamChannel.this.doReadBytes(byteBuf);
                  if (localReadAmount <= 0) {
                     byteBuf.release();
                     close = localReadAmount < 0;
                     break;
                  }

                  this.readPending = false;
                  pipeline.fireChannelRead(byteBuf);
                  byteBuf = null;
                  if (totalReadAmount >= Integer.MAX_VALUE - localReadAmount) {
                     allocHandle.record(totalReadAmount);
                     totalReadAmount = localReadAmount;
                  } else {
                     totalReadAmount += localReadAmount;
                  }

                  if (localReadAmount < writable || !edgeTriggered && !config.isAutoRead()) {
                     break;
                  }

                  ++messages;
               } while(messages < maxMessagesPerRead);

               pipeline.fireChannelReadComplete();
               allocHandle.record(totalReadAmount);
               if (close) {
                  this.closeOnRead(pipeline);
                  close = false;
               }
            } catch (Throwable var16) {
               boolean closed = this.handleReadException(pipeline, byteBuf, var16, close);
               if (!closed) {
                  AbstractEpollStreamChannel.this.eventLoop().execute(new Runnable() {
                     public void run() {
                        EpollStreamUnsafe.this.epollInReady();
                     }
                  });
               }
            } finally {
               if (!this.readPending && !config.isAutoRead()) {
                  this.clearEpollIn0();
               }

            }

         }
      }
   }
}
