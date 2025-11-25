package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;
import java.util.List;
import javax.net.ssl.SSLEngine;

public abstract class Http2OrHttpChooser extends ByteToMessageDecoder {
   private final int maxHttpContentLength;

   protected Http2OrHttpChooser(int maxHttpContentLength) {
      this.maxHttpContentLength = maxHttpContentLength;
   }

   protected abstract Http2OrHttpChooser.SelectedProtocol getProtocol(SSLEngine var1);

   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      if (this.initPipeline(ctx)) {
         ctx.pipeline().remove((ChannelHandler)this);
      }

   }

   private boolean initPipeline(ChannelHandlerContext ctx) {
      SslHandler handler = (SslHandler)ctx.pipeline().get(SslHandler.class);
      if (handler == null) {
         throw new IllegalStateException("SslHandler is needed for HTTP2");
      } else {
         Http2OrHttpChooser.SelectedProtocol protocol = this.getProtocol(handler.engine());
         switch(protocol) {
         case UNKNOWN:
            return false;
         case HTTP_2:
            this.addHttp2Handlers(ctx);
            break;
         case HTTP_1_0:
         case HTTP_1_1:
            this.addHttpHandlers(ctx);
            break;
         default:
            throw new IllegalStateException("Unknown SelectedProtocol");
         }

         return true;
      }
   }

   protected void addHttp2Handlers(ChannelHandlerContext ctx) {
      ChannelPipeline pipeline = ctx.pipeline();
      pipeline.addLast((String)"http2ConnectionHandler", (ChannelHandler)this.createHttp2RequestHandler());
   }

   protected void addHttpHandlers(ChannelHandlerContext ctx) {
      ChannelPipeline pipeline = ctx.pipeline();
      pipeline.addLast((String)"httpRequestDecoder", (ChannelHandler)(new HttpRequestDecoder()));
      pipeline.addLast((String)"httpResponseEncoder", (ChannelHandler)(new HttpResponseEncoder()));
      pipeline.addLast((String)"httpChunkAggregator", (ChannelHandler)(new HttpObjectAggregator(this.maxHttpContentLength)));
      pipeline.addLast("httpRequestHandler", this.createHttp1RequestHandler());
   }

   protected abstract ChannelHandler createHttp1RequestHandler();

   protected abstract Http2ConnectionHandler createHttp2RequestHandler();

   public static enum SelectedProtocol {
      HTTP_2("h2-16"),
      HTTP_1_1("http/1.1"),
      HTTP_1_0("http/1.0"),
      UNKNOWN("Unknown");

      private final String name;

      private SelectedProtocol(String defaultName) {
         this.name = defaultName;
      }

      public String protocolName() {
         return this.name;
      }

      public static Http2OrHttpChooser.SelectedProtocol protocol(String name) {
         Http2OrHttpChooser.SelectedProtocol[] arr$ = values();
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            Http2OrHttpChooser.SelectedProtocol protocol = arr$[i$];
            if (protocol.protocolName().equals(name)) {
               return protocol;
            }
         }

         return UNKNOWN;
      }
   }
}
