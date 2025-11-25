package io.netty.handler.codec.http.websocketx.extensions;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class WebSocketClientExtensionHandler extends ChannelHandlerAdapter {
   private final List<WebSocketClientExtensionHandshaker> extensionHandshakers;

   public WebSocketClientExtensionHandler(WebSocketClientExtensionHandshaker... extensionHandshakers) {
      if (extensionHandshakers == null) {
         throw new NullPointerException("extensionHandshakers");
      } else if (extensionHandshakers.length == 0) {
         throw new IllegalArgumentException("extensionHandshakers must contains at least one handshaker");
      } else {
         this.extensionHandshakers = Arrays.asList(extensionHandshakers);
      }
   }

   public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      if (msg instanceof HttpRequest && WebSocketExtensionUtil.isWebsocketUpgrade((HttpRequest)msg)) {
         HttpRequest request = (HttpRequest)msg;
         String headerValue = (String)request.headers().getAndConvert(HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS);

         WebSocketExtensionData extensionData;
         for(Iterator i$ = this.extensionHandshakers.iterator(); i$.hasNext(); headerValue = WebSocketExtensionUtil.appendExtension(headerValue, extensionData.name(), extensionData.parameters())) {
            WebSocketClientExtensionHandshaker extentionHandshaker = (WebSocketClientExtensionHandshaker)i$.next();
            extensionData = extentionHandshaker.newRequestData();
         }

         request.headers().set(HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS, (CharSequence)headerValue);
      }

      super.write(ctx, msg, promise);
   }

   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof HttpResponse) {
         HttpResponse response = (HttpResponse)msg;
         if (WebSocketExtensionUtil.isWebsocketUpgrade(response)) {
            String extensionsHeader = (String)response.headers().getAndConvert(HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS);
            if (extensionsHeader != null) {
               List<WebSocketExtensionData> extensions = WebSocketExtensionUtil.extractExtensions(extensionsHeader);
               List<WebSocketClientExtension> validExtensions = new ArrayList(extensions.size());
               int rsv = 0;
               Iterator i$ = extensions.iterator();

               label51:
               while(true) {
                  if (!i$.hasNext()) {
                     i$ = validExtensions.iterator();

                     while(true) {
                        if (!i$.hasNext()) {
                           break label51;
                        }

                        WebSocketClientExtension validExtension = (WebSocketClientExtension)i$.next();
                        WebSocketExtensionDecoder decoder = validExtension.newExtensionDecoder();
                        WebSocketExtensionEncoder encoder = validExtension.newExtensionEncoder();
                        ctx.pipeline().addAfter(ctx.name(), decoder.getClass().getName(), decoder);
                        ctx.pipeline().addAfter(ctx.name(), encoder.getClass().getName(), encoder);
                     }
                  }

                  WebSocketExtensionData extensionData = (WebSocketExtensionData)i$.next();
                  Iterator<WebSocketClientExtensionHandshaker> extensionHandshakersIterator = this.extensionHandshakers.iterator();

                  WebSocketClientExtension validExtension;
                  WebSocketClientExtensionHandshaker extensionHandshaker;
                  for(validExtension = null; validExtension == null && extensionHandshakersIterator.hasNext(); validExtension = extensionHandshaker.handshakeExtension(extensionData)) {
                     extensionHandshaker = (WebSocketClientExtensionHandshaker)extensionHandshakersIterator.next();
                  }

                  if (validExtension == null || (validExtension.rsv() & rsv) != 0) {
                     throw new CodecException("invalid WebSocket Extension handhshake for \"" + extensionsHeader + "\"");
                  }

                  rsv |= validExtension.rsv();
                  validExtensions.add(validExtension);
               }
            }

            ctx.pipeline().remove(ctx.name());
         }
      }

      super.channelRead(ctx, msg);
   }
}
