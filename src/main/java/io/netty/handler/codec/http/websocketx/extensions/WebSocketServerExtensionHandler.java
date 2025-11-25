package io.netty.handler.codec.http.websocketx.extensions;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class WebSocketServerExtensionHandler extends ChannelHandlerAdapter {
   private final List<WebSocketServerExtensionHandshaker> extensionHandshakers;
   private List<WebSocketServerExtension> validExtensions;

   public WebSocketServerExtensionHandler(WebSocketServerExtensionHandshaker... extensionHandshakers) {
      if (extensionHandshakers == null) {
         throw new NullPointerException("extensionHandshakers");
      } else if (extensionHandshakers.length == 0) {
         throw new IllegalArgumentException("extensionHandshakers must contains at least one handshaker");
      } else {
         this.extensionHandshakers = Arrays.asList(extensionHandshakers);
      }
   }

   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof HttpRequest) {
         HttpRequest request = (HttpRequest)msg;
         if (WebSocketExtensionUtil.isWebsocketUpgrade(request)) {
            String extensionsHeader = (String)request.headers().getAndConvert(HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS);
            if (extensionsHeader != null) {
               List<WebSocketExtensionData> extensions = WebSocketExtensionUtil.extractExtensions(extensionsHeader);
               int rsv = 0;
               Iterator i$ = extensions.iterator();

               while(i$.hasNext()) {
                  WebSocketExtensionData extensionData = (WebSocketExtensionData)i$.next();
                  Iterator<WebSocketServerExtensionHandshaker> extensionHandshakersIterator = this.extensionHandshakers.iterator();

                  WebSocketServerExtension validExtension;
                  WebSocketServerExtensionHandshaker extensionHandshaker;
                  for(validExtension = null; validExtension == null && extensionHandshakersIterator.hasNext(); validExtension = extensionHandshaker.handshakeExtension(extensionData)) {
                     extensionHandshaker = (WebSocketServerExtensionHandshaker)extensionHandshakersIterator.next();
                  }

                  if (validExtension != null && (validExtension.rsv() & rsv) == 0) {
                     if (this.validExtensions == null) {
                        this.validExtensions = new ArrayList(1);
                     }

                     rsv |= validExtension.rsv();
                     this.validExtensions.add(validExtension);
                  }
               }
            }
         }
      }

      super.channelRead(ctx, msg);
   }

   public void write(final ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      if (msg instanceof HttpResponse && WebSocketExtensionUtil.isWebsocketUpgrade((HttpResponse)msg) && this.validExtensions != null) {
         HttpResponse response = (HttpResponse)msg;
         String headerValue = (String)response.headers().getAndConvert(HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS);

         WebSocketExtensionData extensionData;
         for(Iterator i$ = this.validExtensions.iterator(); i$.hasNext(); headerValue = WebSocketExtensionUtil.appendExtension(headerValue, extensionData.name(), extensionData.parameters())) {
            WebSocketServerExtension extension = (WebSocketServerExtension)i$.next();
            extensionData = extension.newReponseData();
         }

         promise.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
               if (future.isSuccess()) {
                  Iterator i$ = WebSocketServerExtensionHandler.this.validExtensions.iterator();

                  while(i$.hasNext()) {
                     WebSocketServerExtension extension = (WebSocketServerExtension)i$.next();
                     WebSocketExtensionDecoder decoder = extension.newExtensionDecoder();
                     WebSocketExtensionEncoder encoder = extension.newExtensionEncoder();
                     ctx.pipeline().addAfter(ctx.name(), decoder.getClass().getName(), decoder);
                     ctx.pipeline().addAfter(ctx.name(), encoder.getClass().getName(), encoder);
                  }
               }

               ctx.pipeline().remove(ctx.name());
            }
         });
         if (headerValue != null) {
            response.headers().set(HttpHeaderNames.SEC_WEBSOCKET_EXTENSIONS, (CharSequence)headerValue);
         }
      }

      super.write(ctx, msg, promise);
   }
}
