package io.netty.handler.codec.http2;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.TextHeaders;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.netty.util.internal.PlatformDependent;
import java.util.Map.Entry;

public final class InboundHttp2ToHttpPriorityAdapter extends InboundHttp2ToHttpAdapter {
   private static final AsciiString OUT_OF_MESSAGE_SEQUENCE_METHOD;
   private static final AsciiString OUT_OF_MESSAGE_SEQUENCE_PATH;
   private static final AsciiString OUT_OF_MESSAGE_SEQUENCE_RETURN_CODE;
   private final IntObjectMap<HttpHeaders> outOfMessageFlowHeaders = new IntObjectHashMap();

   InboundHttp2ToHttpPriorityAdapter(InboundHttp2ToHttpPriorityAdapter.Builder builder) {
      super(builder);
   }

   protected void removeMessage(int streamId) {
      super.removeMessage(streamId);
      this.outOfMessageFlowHeaders.remove(streamId);
   }

   private static HttpHeaders getActiveHeaders(FullHttpMessage msg) {
      return msg.content().isReadable() ? msg.trailingHeaders() : msg.headers();
   }

   private void importOutOfMessageFlowHeaders(int streamId, HttpHeaders headers) {
      HttpHeaders outOfMessageFlowHeader = (HttpHeaders)this.outOfMessageFlowHeaders.get(streamId);
      if (outOfMessageFlowHeader == null) {
         this.outOfMessageFlowHeaders.put(streamId, headers);
      } else {
         outOfMessageFlowHeader.setAll(headers);
      }

   }

   private void exportOutOfMessageFlowHeaders(int streamId, HttpHeaders headers) {
      HttpHeaders outOfMessageFlowHeader = (HttpHeaders)this.outOfMessageFlowHeaders.get(streamId);
      if (outOfMessageFlowHeader != null) {
         headers.setAll(outOfMessageFlowHeader);
      }

   }

   private static void removePriorityRelatedHeaders(HttpHeaders headers) {
      headers.remove(HttpUtil.ExtensionHeaderNames.STREAM_DEPENDENCY_ID.text());
      headers.remove(HttpUtil.ExtensionHeaderNames.STREAM_WEIGHT.text());
   }

   private void initializePseudoHeaders(Http2Headers headers) {
      if (this.connection.isServer()) {
         headers.method(OUT_OF_MESSAGE_SEQUENCE_METHOD).path(OUT_OF_MESSAGE_SEQUENCE_PATH);
      } else {
         headers.status(OUT_OF_MESSAGE_SEQUENCE_RETURN_CODE);
      }

   }

   private static void addHttpHeadersToHttp2Headers(HttpHeaders httpHeaders, final Http2Headers http2Headers) {
      try {
         httpHeaders.forEachEntry(new TextHeaders.EntryVisitor() {
            public boolean visit(Entry<CharSequence, CharSequence> entry) throws Exception {
               http2Headers.add(AsciiString.of((CharSequence)entry.getKey()), AsciiString.of((CharSequence)entry.getValue()));
               return true;
            }
         });
      } catch (Exception var3) {
         PlatformDependent.throwException(var3);
      }

   }

   protected void fireChannelRead(ChannelHandlerContext ctx, FullHttpMessage msg, int streamId) {
      this.exportOutOfMessageFlowHeaders(streamId, getActiveHeaders(msg));
      super.fireChannelRead(ctx, msg, streamId);
   }

   protected FullHttpMessage processHeadersBegin(ChannelHandlerContext ctx, int streamId, Http2Headers headers, boolean endOfStream, boolean allowAppend, boolean appendToTrailer) throws Http2Exception {
      FullHttpMessage msg = super.processHeadersBegin(ctx, streamId, headers, endOfStream, allowAppend, appendToTrailer);
      if (msg != null) {
         this.exportOutOfMessageFlowHeaders(streamId, getActiveHeaders(msg));
      }

      return msg;
   }

   public void priorityTreeParentChanged(Http2Stream stream, Http2Stream oldParent) {
      Http2Stream parent = stream.parent();
      FullHttpMessage msg = (FullHttpMessage)this.messageMap.get(stream.id());
      if (msg == null) {
         if (parent != null && !parent.equals(this.connection.connectionStream())) {
            HttpHeaders headers = new DefaultHttpHeaders();
            headers.setInt(HttpUtil.ExtensionHeaderNames.STREAM_DEPENDENCY_ID.text(), parent.id());
            this.importOutOfMessageFlowHeaders(stream.id(), headers);
         }
      } else if (parent == null) {
         removePriorityRelatedHeaders(msg.headers());
         removePriorityRelatedHeaders(msg.trailingHeaders());
      } else if (!parent.equals(this.connection.connectionStream())) {
         HttpHeaders headers = getActiveHeaders(msg);
         headers.setInt(HttpUtil.ExtensionHeaderNames.STREAM_DEPENDENCY_ID.text(), parent.id());
      }

   }

   public void onWeightChanged(Http2Stream stream, short oldWeight) {
      FullHttpMessage msg = (FullHttpMessage)this.messageMap.get(stream.id());
      Object headers;
      if (msg == null) {
         headers = new DefaultHttpHeaders();
         this.importOutOfMessageFlowHeaders(stream.id(), (HttpHeaders)headers);
      } else {
         headers = getActiveHeaders(msg);
      }

      ((HttpHeaders)headers).setShort(HttpUtil.ExtensionHeaderNames.STREAM_WEIGHT.text(), stream.weight());
   }

   public void onPriorityRead(ChannelHandlerContext ctx, int streamId, int streamDependency, short weight, boolean exclusive) throws Http2Exception {
      FullHttpMessage msg = (FullHttpMessage)this.messageMap.get(streamId);
      if (msg == null) {
         HttpHeaders httpHeaders = (HttpHeaders)this.outOfMessageFlowHeaders.remove(streamId);
         if (httpHeaders == null) {
            throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Priority Frame recieved for unknown stream id %d", streamId);
         }

         Http2Headers http2Headers = new DefaultHttp2Headers();
         this.initializePseudoHeaders(http2Headers);
         addHttpHeadersToHttp2Headers(httpHeaders, http2Headers);
         msg = this.newMessage(streamId, http2Headers, this.validateHttpHeaders);
         this.fireChannelRead(ctx, msg, streamId);
      }

   }

   static {
      OUT_OF_MESSAGE_SEQUENCE_METHOD = new AsciiString(HttpUtil.OUT_OF_MESSAGE_SEQUENCE_METHOD.toString());
      OUT_OF_MESSAGE_SEQUENCE_PATH = new AsciiString("");
      OUT_OF_MESSAGE_SEQUENCE_RETURN_CODE = new AsciiString(HttpUtil.OUT_OF_MESSAGE_SEQUENCE_RETURN_CODE.toString());
   }

   public static final class Builder extends InboundHttp2ToHttpAdapter.Builder {
      public Builder(Http2Connection connection) {
         super(connection);
      }

      public InboundHttp2ToHttpPriorityAdapter build() {
         InboundHttp2ToHttpPriorityAdapter instance = new InboundHttp2ToHttpPriorityAdapter(this);
         instance.connection.addListener(instance);
         return instance;
      }
   }
}
