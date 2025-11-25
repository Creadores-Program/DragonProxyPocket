package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpStatusClass;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.netty.util.internal.ObjectUtil;

public class InboundHttp2ToHttpAdapter extends Http2EventAdapter {
   private static final InboundHttp2ToHttpAdapter.ImmediateSendDetector DEFAULT_SEND_DETECTOR = new InboundHttp2ToHttpAdapter.ImmediateSendDetector() {
      public boolean mustSendImmediately(FullHttpMessage msg) {
         if (msg instanceof FullHttpResponse) {
            return ((FullHttpResponse)msg).status().codeClass() == HttpStatusClass.INFORMATIONAL;
         } else {
            return msg instanceof FullHttpRequest ? msg.headers().contains(HttpHeaderNames.EXPECT) : false;
         }
      }

      public FullHttpMessage copyIfNeeded(FullHttpMessage msg) {
         if (msg instanceof FullHttpRequest) {
            FullHttpRequest copy = ((FullHttpRequest)msg).copy((ByteBuf)null);
            copy.headers().remove(HttpHeaderNames.EXPECT);
            return copy;
         } else {
            return null;
         }
      }
   };
   private final int maxContentLength;
   protected final Http2Connection connection;
   protected final boolean validateHttpHeaders;
   private final InboundHttp2ToHttpAdapter.ImmediateSendDetector sendDetector;
   protected final IntObjectMap<FullHttpMessage> messageMap;
   private final boolean propagateSettings;

   protected InboundHttp2ToHttpAdapter(InboundHttp2ToHttpAdapter.Builder builder) {
      ObjectUtil.checkNotNull(builder.connection, "connection");
      if (builder.maxContentLength <= 0) {
         throw new IllegalArgumentException("maxContentLength must be a positive integer: " + builder.maxContentLength);
      } else {
         this.connection = builder.connection;
         this.maxContentLength = builder.maxContentLength;
         this.validateHttpHeaders = builder.validateHttpHeaders;
         this.propagateSettings = builder.propagateSettings;
         this.sendDetector = DEFAULT_SEND_DETECTOR;
         this.messageMap = new IntObjectHashMap();
      }
   }

   protected void removeMessage(int streamId) {
      this.messageMap.remove(streamId);
   }

   public void streamRemoved(Http2Stream stream) {
      this.removeMessage(stream.id());
   }

   protected void fireChannelRead(ChannelHandlerContext ctx, FullHttpMessage msg, int streamId) {
      this.removeMessage(streamId);
      HttpHeaderUtil.setContentLength(msg, (long)msg.content().readableBytes());
      ctx.fireChannelRead(msg);
   }

   protected FullHttpMessage newMessage(int streamId, Http2Headers headers, boolean validateHttpHeaders) throws Http2Exception {
      return (FullHttpMessage)(this.connection.isServer() ? HttpUtil.toHttpRequest(streamId, headers, validateHttpHeaders) : HttpUtil.toHttpResponse(streamId, headers, validateHttpHeaders));
   }

   protected FullHttpMessage processHeadersBegin(ChannelHandlerContext ctx, int streamId, Http2Headers headers, boolean endOfStream, boolean allowAppend, boolean appendToTrailer) throws Http2Exception {
      FullHttpMessage msg = (FullHttpMessage)this.messageMap.get(streamId);
      if (msg == null) {
         msg = this.newMessage(streamId, headers, this.validateHttpHeaders);
      } else if (allowAppend) {
         try {
            HttpUtil.addHttp2ToHttpHeaders(streamId, headers, msg, appendToTrailer);
         } catch (Http2Exception var9) {
            this.removeMessage(streamId);
            throw var9;
         }
      } else {
         msg = null;
      }

      if (this.sendDetector.mustSendImmediately(msg)) {
         FullHttpMessage copy = endOfStream ? null : this.sendDetector.copyIfNeeded(msg);
         this.fireChannelRead(ctx, msg, streamId);
         return copy;
      } else {
         return msg;
      }
   }

   private void processHeadersEnd(ChannelHandlerContext ctx, int streamId, FullHttpMessage msg, boolean endOfStream) {
      if (endOfStream) {
         this.fireChannelRead(ctx, msg, streamId);
      } else {
         this.messageMap.put(streamId, msg);
      }

   }

   public int onDataRead(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream) throws Http2Exception {
      FullHttpMessage msg = (FullHttpMessage)this.messageMap.get(streamId);
      if (msg == null) {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Data Frame recieved for unknown stream id %d", streamId);
      } else {
         ByteBuf content = msg.content();
         int dataReadableBytes = data.readableBytes();
         if (content.readableBytes() > this.maxContentLength - dataReadableBytes) {
            throw Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, "Content length exceeded max of %d for stream id %d", this.maxContentLength, streamId);
         } else {
            content.writeBytes(data, data.readerIndex(), dataReadableBytes);
            if (endOfStream) {
               this.fireChannelRead(ctx, msg, streamId);
            }

            return dataReadableBytes + padding;
         }
      }
   }

   public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endOfStream) throws Http2Exception {
      FullHttpMessage msg = this.processHeadersBegin(ctx, streamId, headers, endOfStream, true, true);
      if (msg != null) {
         this.processHeadersEnd(ctx, streamId, msg, endOfStream);
      }

   }

   public void onHeadersRead(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream) throws Http2Exception {
      FullHttpMessage msg = this.processHeadersBegin(ctx, streamId, headers, endOfStream, true, true);
      if (msg != null) {
         this.processHeadersEnd(ctx, streamId, msg, endOfStream);
      }

   }

   public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) throws Http2Exception {
      FullHttpMessage msg = (FullHttpMessage)this.messageMap.get(streamId);
      if (msg != null) {
         this.fireChannelRead(ctx, msg, streamId);
      }

   }

   public void onPushPromiseRead(ChannelHandlerContext ctx, int streamId, int promisedStreamId, Http2Headers headers, int padding) throws Http2Exception {
      FullHttpMessage msg = this.processHeadersBegin(ctx, promisedStreamId, headers, false, false, false);
      if (msg == null) {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Push Promise Frame recieved for pre-existing stream id %d", promisedStreamId);
      } else {
         msg.headers().setInt(HttpUtil.ExtensionHeaderNames.STREAM_PROMISE_ID.text(), streamId);
         this.processHeadersEnd(ctx, promisedStreamId, msg, false);
      }
   }

   public void onSettingsRead(ChannelHandlerContext ctx, Http2Settings settings) throws Http2Exception {
      if (this.propagateSettings) {
         ctx.fireChannelRead(settings);
      }

   }

   private interface ImmediateSendDetector {
      boolean mustSendImmediately(FullHttpMessage var1);

      FullHttpMessage copyIfNeeded(FullHttpMessage var1);
   }

   public static class Builder {
      private final Http2Connection connection;
      private int maxContentLength;
      private boolean validateHttpHeaders;
      private boolean propagateSettings;

      public Builder(Http2Connection connection) {
         this.connection = connection;
      }

      public InboundHttp2ToHttpAdapter.Builder maxContentLength(int maxContentLength) {
         this.maxContentLength = maxContentLength;
         return this;
      }

      public InboundHttp2ToHttpAdapter.Builder validateHttpHeaders(boolean validate) {
         this.validateHttpHeaders = validate;
         return this;
      }

      public InboundHttp2ToHttpAdapter.Builder propagateSettings(boolean propagate) {
         this.propagateSettings = propagate;
         return this;
      }

      public InboundHttp2ToHttpAdapter build() {
         InboundHttp2ToHttpAdapter instance = new InboundHttp2ToHttpAdapter(this);
         this.connection.addListener(instance);
         return instance;
      }
   }
}
