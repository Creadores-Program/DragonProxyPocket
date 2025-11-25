package io.netty.handler.codec.http;

import io.netty.channel.ChannelHandlerAppender;
import io.netty.channel.ChannelHandlerContext;

public final class HttpServerCodec extends ChannelHandlerAppender implements HttpServerUpgradeHandler.SourceCodec {
   public HttpServerCodec() {
      this(4096, 8192, 8192);
   }

   public HttpServerCodec(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize) {
      super(new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize), new HttpResponseEncoder());
   }

   public HttpServerCodec(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders) {
      super(new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize, validateHeaders), new HttpResponseEncoder());
   }

   public void upgradeFrom(ChannelHandlerContext ctx) {
      ctx.pipeline().remove(HttpRequestDecoder.class);
      ctx.pipeline().remove(HttpResponseEncoder.class);
   }

   public HttpResponseEncoder encoder() {
      return (HttpResponseEncoder)this.handlerAt(1);
   }

   public HttpRequestDecoder decoder() {
      return (HttpRequestDecoder)this.handlerAt(0);
   }
}
