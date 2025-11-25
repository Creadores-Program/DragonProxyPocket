package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.internal.ObjectUtil;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Http2ConnectionHandler extends ByteToMessageDecoder implements Http2LifecycleManager {
   private final Http2ConnectionDecoder decoder;
   private final Http2ConnectionEncoder encoder;
   private ByteBuf clientPrefaceString;
   private boolean prefaceSent;
   private ChannelFutureListener closeListener;

   public Http2ConnectionHandler(boolean server, Http2FrameListener listener) {
      this((Http2Connection)(new DefaultHttp2Connection(server)), (Http2FrameListener)listener);
   }

   public Http2ConnectionHandler(Http2Connection connection, Http2FrameListener listener) {
      this(connection, new DefaultHttp2FrameReader(), new DefaultHttp2FrameWriter(), listener);
   }

   public Http2ConnectionHandler(Http2Connection connection, Http2FrameReader frameReader, Http2FrameWriter frameWriter, Http2FrameListener listener) {
      this((Http2ConnectionDecoder.Builder)DefaultHttp2ConnectionDecoder.newBuilder().connection(connection).frameReader(frameReader).listener(listener), (Http2ConnectionEncoder.Builder)DefaultHttp2ConnectionEncoder.newBuilder().connection(connection).frameWriter(frameWriter));
   }

   public Http2ConnectionHandler(Http2ConnectionDecoder.Builder decoderBuilder, Http2ConnectionEncoder.Builder encoderBuilder) {
      ObjectUtil.checkNotNull(decoderBuilder, "decoderBuilder");
      ObjectUtil.checkNotNull(encoderBuilder, "encoderBuilder");
      if (encoderBuilder.lifecycleManager() != decoderBuilder.lifecycleManager()) {
         throw new IllegalArgumentException("Encoder and Decoder must share a lifecycle manager");
      } else {
         if (encoderBuilder.lifecycleManager() == null) {
            encoderBuilder.lifecycleManager(this);
            decoderBuilder.lifecycleManager(this);
         }

         this.encoder = (Http2ConnectionEncoder)ObjectUtil.checkNotNull(encoderBuilder.build(), "encoder");
         decoderBuilder.encoder(this.encoder);
         this.decoder = (Http2ConnectionDecoder)ObjectUtil.checkNotNull(decoderBuilder.build(), "decoder");
         ObjectUtil.checkNotNull(this.encoder.connection(), "encoder.connection");
         if (this.encoder.connection() != this.decoder.connection()) {
            throw new IllegalArgumentException("Encoder and Decoder do not share the same connection object");
         } else {
            this.clientPrefaceString = clientPrefaceString(this.encoder.connection());
         }
      }
   }

   public Http2Connection connection() {
      return this.encoder.connection();
   }

   public Http2ConnectionDecoder decoder() {
      return this.decoder;
   }

   public Http2ConnectionEncoder encoder() {
      return this.encoder;
   }

   public void onHttpClientUpgrade() throws Http2Exception {
      if (this.connection().isServer()) {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Client-side HTTP upgrade requested for a server");
      } else if (!this.prefaceSent && !this.decoder.prefaceReceived()) {
         this.connection().createLocalStream(1).open(true);
      } else {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "HTTP upgrade must occur before HTTP/2 preface is sent or received");
      }
   }

   public void onHttpServerUpgrade(Http2Settings settings) throws Http2Exception {
      if (!this.connection().isServer()) {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Server-side HTTP upgrade requested for a client");
      } else if (!this.prefaceSent && !this.decoder.prefaceReceived()) {
         this.encoder.remoteSettings(settings);
         this.connection().createRemoteStream(1).open(true);
      } else {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "HTTP upgrade must occur before HTTP/2 preface is sent or received");
      }
   }

   public void channelActive(ChannelHandlerContext ctx) throws Exception {
      this.sendPreface(ctx);
      super.channelActive(ctx);
   }

   public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      this.sendPreface(ctx);
   }

   protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
      this.dispose();
   }

   public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
      if (!ctx.channel().isActive()) {
         ctx.close(promise);
      } else {
         ChannelFuture future = this.writeGoAway(ctx, (Http2Exception)null);
         if (this.connection().numActiveStreams() == 0) {
            future.addListener(new Http2ConnectionHandler.ClosingChannelFutureListener(ctx, promise));
         } else {
            this.closeListener = new Http2ConnectionHandler.ClosingChannelFutureListener(ctx, promise);
         }

      }
   }

   public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      ChannelFuture future = ctx.newSucceededFuture();
      Collection<Http2Stream> streams = this.connection().activeStreams();
      Http2Stream[] arr$ = (Http2Stream[])streams.toArray(new Http2Stream[streams.size()]);
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         Http2Stream s = arr$[i$];
         this.closeStream(s, future);
      }

      super.channelInactive(ctx);
   }

   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
      if (Http2CodecUtil.getEmbeddedHttp2Exception(cause) != null) {
         this.onException(ctx, cause);
      } else {
         super.exceptionCaught(ctx, cause);
      }

   }

   public void closeLocalSide(Http2Stream stream, ChannelFuture future) {
      switch(stream.state()) {
      case HALF_CLOSED_LOCAL:
      case OPEN:
         stream.closeLocalSide();
         break;
      default:
         this.closeStream(stream, future);
      }

   }

   public void closeRemoteSide(Http2Stream stream, ChannelFuture future) {
      switch(stream.state()) {
      case OPEN:
      case HALF_CLOSED_REMOTE:
         stream.closeRemoteSide();
         break;
      default:
         this.closeStream(stream, future);
      }

   }

   public void closeStream(final Http2Stream stream, ChannelFuture future) {
      stream.close();
      future.addListener(new ChannelFutureListener() {
         public void operationComplete(ChannelFuture future) throws Exception {
            Http2ConnectionHandler.this.connection().deactivate(stream);
            if (Http2ConnectionHandler.this.closeListener != null && Http2ConnectionHandler.this.connection().numActiveStreams() == 0) {
               Http2ConnectionHandler.this.closeListener.operationComplete(future);
            }

         }
      });
   }

   public void onException(ChannelHandlerContext ctx, Throwable cause) {
      Http2Exception embedded = Http2CodecUtil.getEmbeddedHttp2Exception(cause);
      if (Http2Exception.isStreamError(embedded)) {
         this.onStreamError(ctx, cause, (Http2Exception.StreamException)embedded);
      } else if (embedded instanceof Http2Exception.CompositeStreamException) {
         Http2Exception.CompositeStreamException compositException = (Http2Exception.CompositeStreamException)embedded;
         Iterator i$ = compositException.iterator();

         while(i$.hasNext()) {
            Http2Exception.StreamException streamException = (Http2Exception.StreamException)i$.next();
            this.onStreamError(ctx, cause, streamException);
         }
      } else {
         this.onConnectionError(ctx, cause, embedded);
      }

   }

   protected void onConnectionError(ChannelHandlerContext ctx, Throwable cause, Http2Exception http2Ex) {
      if (http2Ex == null) {
         http2Ex = new Http2Exception(Http2Error.INTERNAL_ERROR, cause.getMessage(), cause);
      }

      this.writeGoAway(ctx, http2Ex).addListener(new Http2ConnectionHandler.ClosingChannelFutureListener(ctx, ctx.newPromise()));
   }

   protected void onStreamError(ChannelHandlerContext ctx, Throwable cause, Http2Exception.StreamException http2Ex) {
      this.writeRstStream(ctx, http2Ex.streamId(), http2Ex.error().code(), ctx.newPromise());
   }

   protected Http2FrameWriter frameWriter() {
      return this.encoder().frameWriter();
   }

   public ChannelFuture writeRstStream(ChannelHandlerContext ctx, int streamId, long errorCode, ChannelPromise promise) {
      Http2Stream stream = this.connection().stream(streamId);
      ChannelFuture future = this.frameWriter().writeRstStream(ctx, streamId, errorCode, promise);
      ctx.flush();
      if (stream != null) {
         stream.resetSent();
         this.closeStream(stream, promise);
      }

      return future;
   }

   public ChannelFuture writeGoAway(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData, ChannelPromise promise) {
      Http2Connection connection = this.connection();
      if (connection.isGoAway()) {
         debugData.release();
         return ctx.newSucceededFuture();
      } else {
         ChannelFuture future = this.frameWriter().writeGoAway(ctx, lastStreamId, errorCode, debugData, promise);
         ctx.flush();
         connection.goAwaySent(lastStreamId);
         return future;
      }
   }

   private ChannelFuture writeGoAway(ChannelHandlerContext ctx, Http2Exception cause) {
      Http2Connection connection = this.connection();
      if (connection.isGoAway()) {
         return ctx.newSucceededFuture();
      } else {
         long errorCode = cause != null ? cause.error().code() : Http2Error.NO_ERROR.code();
         ByteBuf debugData = Http2CodecUtil.toByteBuf(ctx, cause);
         int lastKnownStream = connection.remote().lastStreamCreated();
         return this.writeGoAway(ctx, lastKnownStream, errorCode, debugData, ctx.newPromise());
      }
   }

   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      try {
         if (!this.readClientPrefaceString(in)) {
            return;
         }

         this.decoder.decodeFrame(ctx, in, out);
      } catch (Throwable var5) {
         this.onException(ctx, var5);
      }

   }

   private void sendPreface(ChannelHandlerContext ctx) {
      if (!this.prefaceSent && ctx.channel().isActive()) {
         this.prefaceSent = true;
         if (!this.connection().isServer()) {
            ctx.write(Http2CodecUtil.connectionPrefaceBuf()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
         }

         this.encoder.writeSettings(ctx, this.decoder.localSettings(), ctx.newPromise()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
      }
   }

   private void dispose() {
      this.encoder.close();
      this.decoder.close();
      if (this.clientPrefaceString != null) {
         this.clientPrefaceString.release();
         this.clientPrefaceString = null;
      }

   }

   private boolean readClientPrefaceString(ByteBuf in) throws Http2Exception {
      if (this.clientPrefaceString == null) {
         return true;
      } else {
         int prefaceRemaining = this.clientPrefaceString.readableBytes();
         int bytesRead = Math.min(in.readableBytes(), prefaceRemaining);
         ByteBuf sourceSlice = in.readSlice(bytesRead);
         ByteBuf prefaceSlice = this.clientPrefaceString.readSlice(bytesRead);
         if (bytesRead != 0 && prefaceSlice.equals(sourceSlice)) {
            if (!this.clientPrefaceString.isReadable()) {
               this.clientPrefaceString.release();
               this.clientPrefaceString = null;
               return true;
            } else {
               return false;
            }
         } else {
            throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "HTTP/2 client preface string missing or corrupt.");
         }
      }
   }

   private static ByteBuf clientPrefaceString(Http2Connection connection) {
      return connection.isServer() ? Http2CodecUtil.connectionPrefaceBuf() : null;
   }

   private static final class ClosingChannelFutureListener implements ChannelFutureListener {
      private final ChannelHandlerContext ctx;
      private final ChannelPromise promise;

      ClosingChannelFutureListener(ChannelHandlerContext ctx, ChannelPromise promise) {
         this.ctx = ctx;
         this.promise = promise;
      }

      public void operationComplete(ChannelFuture sentGoAwayFuture) throws Exception {
         this.ctx.close(this.promise);
      }
   }
}
