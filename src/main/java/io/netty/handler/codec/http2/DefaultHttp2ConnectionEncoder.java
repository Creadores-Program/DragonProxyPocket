package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ObjectUtil;
import java.util.ArrayDeque;

public class DefaultHttp2ConnectionEncoder implements Http2ConnectionEncoder {
   private final Http2FrameWriter frameWriter;
   private final Http2Connection connection;
   private final Http2LifecycleManager lifecycleManager;
   private final ArrayDeque<Http2Settings> outstandingLocalSettingsQueue = new ArrayDeque(4);

   public static DefaultHttp2ConnectionEncoder.Builder newBuilder() {
      return new DefaultHttp2ConnectionEncoder.Builder();
   }

   protected DefaultHttp2ConnectionEncoder(DefaultHttp2ConnectionEncoder.Builder builder) {
      this.connection = (Http2Connection)ObjectUtil.checkNotNull(builder.connection, "connection");
      this.frameWriter = (Http2FrameWriter)ObjectUtil.checkNotNull(builder.frameWriter, "frameWriter");
      this.lifecycleManager = (Http2LifecycleManager)ObjectUtil.checkNotNull(builder.lifecycleManager, "lifecycleManager");
      if (this.connection.remote().flowController() == null) {
         this.connection.remote().flowController(new DefaultHttp2RemoteFlowController(this.connection));
      }

   }

   public Http2FrameWriter frameWriter() {
      return this.frameWriter;
   }

   public Http2Connection connection() {
      return this.connection;
   }

   public final Http2RemoteFlowController flowController() {
      return (Http2RemoteFlowController)this.connection().remote().flowController();
   }

   public void remoteSettings(Http2Settings settings) throws Http2Exception {
      Boolean pushEnabled = settings.pushEnabled();
      Http2FrameWriter.Configuration config = this.configuration();
      Http2HeaderTable outboundHeaderTable = config.headerTable();
      Http2FrameSizePolicy outboundFrameSizePolicy = config.frameSizePolicy();
      if (pushEnabled != null) {
         if (!this.connection.isServer()) {
            throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Client received SETTINGS frame with ENABLE_PUSH specified");
         }

         this.connection.remote().allowPushTo(pushEnabled);
      }

      Long maxConcurrentStreams = settings.maxConcurrentStreams();
      if (maxConcurrentStreams != null) {
         this.connection.local().maxStreams((int)Math.min(maxConcurrentStreams, 2147483647L));
      }

      Long headerTableSize = settings.headerTableSize();
      if (headerTableSize != null) {
         outboundHeaderTable.maxHeaderTableSize((int)Math.min(headerTableSize, 2147483647L));
      }

      Integer maxHeaderListSize = settings.maxHeaderListSize();
      if (maxHeaderListSize != null) {
         outboundHeaderTable.maxHeaderListSize(maxHeaderListSize);
      }

      Integer maxFrameSize = settings.maxFrameSize();
      if (maxFrameSize != null) {
         outboundFrameSizePolicy.maxFrameSize(maxFrameSize);
      }

      Integer initialWindowSize = settings.initialWindowSize();
      if (initialWindowSize != null) {
         this.flowController().initialWindowSize(initialWindowSize);
      }

   }

   public ChannelFuture writeData(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream, ChannelPromise promise) {
      Http2Stream stream;
      try {
         if (this.connection.isGoAway()) {
            throw new IllegalStateException("Sending data after connection going away.");
         }

         stream = this.connection.requireStream(streamId);
         switch(stream.state()) {
         case OPEN:
         case HALF_CLOSED_REMOTE:
            if (endOfStream) {
               this.lifecycleManager.closeLocalSide(stream, promise);
            }
            break;
         default:
            throw new IllegalStateException(String.format("Stream %d in unexpected state: %s", stream.id(), stream.state()));
         }
      } catch (Throwable var9) {
         data.release();
         return promise.setFailure(var9);
      }

      this.flowController().sendFlowControlled(ctx, stream, new DefaultHttp2ConnectionEncoder.FlowControlledData(ctx, stream, data, padding, endOfStream, promise));
      return promise;
   }

   public ChannelFuture writeHeaders(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream, ChannelPromise promise) {
      return this.writeHeaders(ctx, streamId, headers, 0, (short)16, false, padding, endStream, promise);
   }

   public ChannelFuture writeHeaders(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream, ChannelPromise promise) {
      try {
         if (this.connection.isGoAway()) {
            throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Sending headers after connection going away.");
         } else {
            Http2Stream stream = this.connection.stream(streamId);
            if (stream == null) {
               stream = this.connection.createLocalStream(streamId);
            }

            switch(stream.state()) {
            case RESERVED_LOCAL:
            case IDLE:
               stream.open(endOfStream);
            case OPEN:
            case HALF_CLOSED_REMOTE:
               this.flowController().sendFlowControlled(ctx, stream, new DefaultHttp2ConnectionEncoder.FlowControlledHeaders(ctx, stream, headers, streamDependency, weight, exclusive, padding, endOfStream, promise));
               if (endOfStream) {
                  this.lifecycleManager.closeLocalSide(stream, promise);
               }

               return promise;
            default:
               throw new IllegalStateException(String.format("Stream %d in unexpected state: %s", stream.id(), stream.state()));
            }
         }
      } catch (Http2NoMoreStreamIdsException var11) {
         this.lifecycleManager.onException(ctx, var11);
         return promise.setFailure(var11);
      } catch (Throwable var12) {
         return promise.setFailure(var12);
      }
   }

   public ChannelFuture writePriority(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight, boolean exclusive, ChannelPromise promise) {
      try {
         if (this.connection.isGoAway()) {
            throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Sending priority after connection going away.");
         }

         Http2Stream stream = this.connection.stream(streamId);
         if (stream == null) {
            stream = this.connection.createLocalStream(streamId);
         }

         stream.setPriority(streamDependency, weight, exclusive);
      } catch (Throwable var8) {
         return promise.setFailure(var8);
      }

      ChannelFuture future = this.frameWriter.writePriority(ctx, streamId, streamDependency, weight, exclusive, promise);
      ctx.flush();
      return future;
   }

   public ChannelFuture writeRstStream(ChannelHandlerContext ctx, int streamId, long errorCode, ChannelPromise promise) {
      return this.lifecycleManager.writeRstStream(ctx, streamId, errorCode, promise);
   }

   public ChannelFuture writeRstStream(ChannelHandlerContext ctx, int streamId, long errorCode, ChannelPromise promise, boolean writeIfNoStream) {
      Http2Stream stream = this.connection.stream(streamId);
      if (stream == null && !writeIfNoStream) {
         promise.setSuccess();
         return promise;
      } else {
         ChannelFuture future = this.frameWriter.writeRstStream(ctx, streamId, errorCode, promise);
         ctx.flush();
         if (stream != null) {
            stream.resetSent();
            this.lifecycleManager.closeStream(stream, promise);
         }

         return future;
      }
   }

   public ChannelFuture writeSettings(ChannelHandlerContext ctx, Http2Settings settings, ChannelPromise promise) {
      this.outstandingLocalSettingsQueue.add(settings);

      try {
         if (this.connection.isGoAway()) {
            throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Sending settings after connection going away.");
         }

         Boolean pushEnabled = settings.pushEnabled();
         if (pushEnabled != null && this.connection.isServer()) {
            throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Server sending SETTINGS frame with ENABLE_PUSH specified");
         }
      } catch (Throwable var5) {
         return promise.setFailure(var5);
      }

      ChannelFuture future = this.frameWriter.writeSettings(ctx, settings, promise);
      ctx.flush();
      return future;
   }

   public ChannelFuture writeSettingsAck(ChannelHandlerContext ctx, ChannelPromise promise) {
      ChannelFuture future = this.frameWriter.writeSettingsAck(ctx, promise);
      ctx.flush();
      return future;
   }

   public ChannelFuture writePing(ChannelHandlerContext ctx, boolean ack, ByteBuf data, ChannelPromise promise) {
      if (this.connection.isGoAway()) {
         data.release();
         return promise.setFailure(Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Sending ping after connection going away."));
      } else {
         ChannelFuture future = this.frameWriter.writePing(ctx, ack, data, promise);
         ctx.flush();
         return future;
      }
   }

   public ChannelFuture writePushPromise(ChannelHandlerContext ctx, int streamId, int promisedStreamId, Http2Headers headers, int padding, ChannelPromise promise) {
      try {
         if (this.connection.isGoAway()) {
            throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Sending push promise after connection going away.");
         }

         Http2Stream stream = this.connection.requireStream(streamId);
         this.connection.local().reservePushStream(promisedStreamId, stream);
      } catch (Throwable var8) {
         return promise.setFailure(var8);
      }

      ChannelFuture future = this.frameWriter.writePushPromise(ctx, streamId, promisedStreamId, headers, padding, promise);
      ctx.flush();
      return future;
   }

   public ChannelFuture writeGoAway(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData, ChannelPromise promise) {
      return this.lifecycleManager.writeGoAway(ctx, lastStreamId, errorCode, debugData, promise);
   }

   public ChannelFuture writeWindowUpdate(ChannelHandlerContext ctx, int streamId, int windowSizeIncrement, ChannelPromise promise) {
      return promise.setFailure(new UnsupportedOperationException("Use the Http2[Inbound|Outbound]FlowController objects to control window sizes"));
   }

   public ChannelFuture writeFrame(ChannelHandlerContext ctx, byte frameType, int streamId, Http2Flags flags, ByteBuf payload, ChannelPromise promise) {
      return this.frameWriter.writeFrame(ctx, frameType, streamId, flags, payload, promise);
   }

   public void close() {
      this.frameWriter.close();
   }

   public Http2Settings pollSentSettings() {
      return (Http2Settings)this.outstandingLocalSettingsQueue.poll();
   }

   public Http2FrameWriter.Configuration configuration() {
      return this.frameWriter.configuration();
   }

   public abstract class FlowControlledBase implements Http2RemoteFlowController.FlowControlled, ChannelFutureListener {
      protected final ChannelHandlerContext ctx;
      protected final Http2Stream stream;
      protected final ChannelPromise promise;
      protected final boolean endOfStream;
      protected int padding;

      public FlowControlledBase(ChannelHandlerContext ctx, Http2Stream stream, int padding, boolean endOfStream, ChannelPromise promise) {
         this.ctx = ctx;
         if (padding < 0) {
            throw new IllegalArgumentException("padding must be >= 0");
         } else {
            this.padding = padding;
            this.endOfStream = endOfStream;
            this.stream = stream;
            this.promise = promise;
            promise.addListener(this);
         }
      }

      public void operationComplete(ChannelFuture future) throws Exception {
         if (!future.isSuccess()) {
            this.error(future.cause());
         }

      }
   }

   private final class FlowControlledHeaders extends DefaultHttp2ConnectionEncoder.FlowControlledBase {
      private final Http2Headers headers;
      private final int streamDependency;
      private final short weight;
      private final boolean exclusive;

      private FlowControlledHeaders(ChannelHandlerContext ctx, Http2Stream stream, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream, ChannelPromise promise) {
         super(ctx, stream, padding, endOfStream, promise);
         this.headers = headers;
         this.streamDependency = streamDependency;
         this.weight = weight;
         this.exclusive = exclusive;
      }

      public int size() {
         return 0;
      }

      public void error(Throwable cause) {
         DefaultHttp2ConnectionEncoder.this.lifecycleManager.onException(this.ctx, cause);
         this.promise.tryFailure(cause);
      }

      public boolean write(int allowedBytes) {
         DefaultHttp2ConnectionEncoder.this.frameWriter().writeHeaders(this.ctx, this.stream.id(), this.headers, this.streamDependency, this.weight, this.exclusive, this.padding, this.endOfStream, this.promise);
         return true;
      }

      // $FF: synthetic method
      FlowControlledHeaders(ChannelHandlerContext x1, Http2Stream x2, Http2Headers x3, int x4, short x5, boolean x6, int x7, boolean x8, ChannelPromise x9, Object x10) {
         this(x1, x2, x3, x4, x5, x6, x7, x8, x9);
      }
   }

   private final class FlowControlledData extends DefaultHttp2ConnectionEncoder.FlowControlledBase {
      private ByteBuf data;
      private int size;

      private FlowControlledData(ChannelHandlerContext ctx, Http2Stream stream, ByteBuf data, int padding, boolean endOfStream, ChannelPromise promise) {
         super(ctx, stream, padding, endOfStream, promise);
         this.data = data;
         this.size = data.readableBytes() + padding;
      }

      public int size() {
         return this.size;
      }

      public void error(Throwable cause) {
         ReferenceCountUtil.safeRelease(this.data);
         DefaultHttp2ConnectionEncoder.this.lifecycleManager.onException(this.ctx, cause);
         this.data = null;
         this.size = 0;
         this.promise.tryFailure(cause);
      }

      public boolean write(int allowedBytes) {
         if (this.data == null) {
            return false;
         } else if (allowedBytes == 0 && this.size() != 0) {
            return false;
         } else {
            int maxFrameSize = DefaultHttp2ConnectionEncoder.this.frameWriter().configuration().frameSizePolicy().maxFrameSize();

            try {
               int bytesWritten = 0;

               do {
                  int allowedFrameSize = Math.min(maxFrameSize, allowedBytes - bytesWritten);
                  int writeableData = this.data.readableBytes();
                  ByteBuf toWrite;
                  if (writeableData > allowedFrameSize) {
                     writeableData = allowedFrameSize;
                     toWrite = this.data.readSlice(allowedFrameSize).retain();
                  } else {
                     toWrite = this.data;
                     this.data = Unpooled.EMPTY_BUFFER;
                  }

                  int writeablePadding = Math.min(allowedFrameSize - writeableData, this.padding);
                  this.padding -= writeablePadding;
                  bytesWritten += writeableData + writeablePadding;
                  ChannelPromise writePromise;
                  if (this.size == bytesWritten) {
                     writePromise = this.promise;
                  } else {
                     writePromise = this.ctx.newPromise();
                     writePromise.addListener(this);
                  }

                  DefaultHttp2ConnectionEncoder.this.frameWriter().writeData(this.ctx, this.stream.id(), toWrite, writeablePadding, this.size == bytesWritten && this.endOfStream, writePromise);
               } while(this.size != bytesWritten && allowedBytes > bytesWritten);

               this.size -= bytesWritten;
               return true;
            } catch (Throwable var9) {
               this.error(var9);
               return false;
            }
         }
      }

      // $FF: synthetic method
      FlowControlledData(ChannelHandlerContext x1, Http2Stream x2, ByteBuf x3, int x4, boolean x5, ChannelPromise x6, Object x7) {
         this(x1, x2, x3, x4, x5, x6);
      }
   }

   public static class Builder implements Http2ConnectionEncoder.Builder {
      protected Http2FrameWriter frameWriter;
      protected Http2Connection connection;
      protected Http2LifecycleManager lifecycleManager;

      public DefaultHttp2ConnectionEncoder.Builder connection(Http2Connection connection) {
         this.connection = connection;
         return this;
      }

      public DefaultHttp2ConnectionEncoder.Builder lifecycleManager(Http2LifecycleManager lifecycleManager) {
         this.lifecycleManager = lifecycleManager;
         return this;
      }

      public Http2LifecycleManager lifecycleManager() {
         return this.lifecycleManager;
      }

      public DefaultHttp2ConnectionEncoder.Builder frameWriter(Http2FrameWriter frameWriter) {
         this.frameWriter = frameWriter;
         return this;
      }

      public Http2ConnectionEncoder build() {
         return new DefaultHttp2ConnectionEncoder(this);
      }
   }
}
