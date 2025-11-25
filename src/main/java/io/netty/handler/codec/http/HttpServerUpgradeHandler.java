package io.netty.handler.codec.http;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.AsciiString;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ReferenceCounted;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class HttpServerUpgradeHandler extends HttpObjectAggregator {
   private final Map<String, HttpServerUpgradeHandler.UpgradeCodec> upgradeCodecMap;
   private final HttpServerUpgradeHandler.SourceCodec sourceCodec;
   private boolean handlingUpgrade;

   public HttpServerUpgradeHandler(HttpServerUpgradeHandler.SourceCodec sourceCodec, Collection<HttpServerUpgradeHandler.UpgradeCodec> upgradeCodecs, int maxContentLength) {
      super(maxContentLength);
      if (sourceCodec == null) {
         throw new NullPointerException("sourceCodec");
      } else if (upgradeCodecs == null) {
         throw new NullPointerException("upgradeCodecs");
      } else {
         this.sourceCodec = sourceCodec;
         this.upgradeCodecMap = new LinkedHashMap(upgradeCodecs.size());
         Iterator i$ = upgradeCodecs.iterator();

         while(i$.hasNext()) {
            HttpServerUpgradeHandler.UpgradeCodec upgradeCodec = (HttpServerUpgradeHandler.UpgradeCodec)i$.next();
            String name = upgradeCodec.protocol().toUpperCase(Locale.US);
            this.upgradeCodecMap.put(name, upgradeCodec);
         }

      }
   }

   protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) throws Exception {
      this.handlingUpgrade |= isUpgradeRequest(msg);
      if (!this.handlingUpgrade) {
         ReferenceCountUtil.retain(msg);
         out.add(msg);
      } else {
         FullHttpRequest fullRequest;
         if (msg instanceof FullHttpRequest) {
            fullRequest = (FullHttpRequest)msg;
            ReferenceCountUtil.retain(msg);
            out.add(msg);
         } else {
            super.decode(ctx, msg, out);
            if (out.isEmpty()) {
               return;
            }

            assert out.size() == 1;

            this.handlingUpgrade = false;
            fullRequest = (FullHttpRequest)out.get(0);
         }

         if (this.upgrade(ctx, fullRequest)) {
            out.clear();
         }

      }
   }

   private static boolean isUpgradeRequest(HttpObject msg) {
      return msg instanceof HttpRequest && ((HttpRequest)msg).headers().get(HttpHeaderNames.UPGRADE) != null;
   }

   private boolean upgrade(final ChannelHandlerContext ctx, final FullHttpRequest request) {
      CharSequence upgradeHeader = (CharSequence)request.headers().get(HttpHeaderNames.UPGRADE);
      final HttpServerUpgradeHandler.UpgradeCodec upgradeCodec = this.selectUpgradeCodec(upgradeHeader);
      if (upgradeCodec == null) {
         return false;
      } else {
         CharSequence connectionHeader = (CharSequence)request.headers().get(HttpHeaderNames.CONNECTION);
         if (connectionHeader == null) {
            return false;
         } else {
            Collection<String> requiredHeaders = upgradeCodec.requiredUpgradeHeaders();
            Set<CharSequence> values = splitHeader(connectionHeader);
            if (values.contains(HttpHeaderNames.UPGRADE) && values.containsAll(requiredHeaders)) {
               Iterator i$ = requiredHeaders.iterator();

               String requiredHeader;
               do {
                  if (!i$.hasNext()) {
                     final HttpServerUpgradeHandler.UpgradeEvent event = new HttpServerUpgradeHandler.UpgradeEvent(upgradeCodec.protocol(), request);
                     final FullHttpResponse upgradeResponse = createUpgradeResponse(upgradeCodec);
                     upgradeCodec.prepareUpgradeResponse(ctx, request, upgradeResponse);
                     ctx.writeAndFlush(upgradeResponse).addListener(new ChannelFutureListener() {
                        public void operationComplete(ChannelFuture future) throws Exception {
                           try {
                              if (future.isSuccess()) {
                                 HttpServerUpgradeHandler.this.sourceCodec.upgradeFrom(ctx);
                                 upgradeCodec.upgradeTo(ctx, request, upgradeResponse);
                                 ctx.fireUserEventTriggered(event.retain());
                                 ctx.pipeline().remove((ChannelHandler)HttpServerUpgradeHandler.this);
                              } else {
                                 future.channel().close();
                              }
                           } finally {
                              event.release();
                           }

                        }
                     });
                     return true;
                  }

                  requiredHeader = (String)i$.next();
               } while(request.headers().contains(requiredHeader));

               return false;
            } else {
               return false;
            }
         }
      }
   }

   private HttpServerUpgradeHandler.UpgradeCodec selectUpgradeCodec(CharSequence upgradeHeader) {
      Set<CharSequence> requestedProtocols = splitHeader(upgradeHeader);
      Set<String> supportedProtocols = new LinkedHashSet(this.upgradeCodecMap.keySet());
      supportedProtocols.retainAll(requestedProtocols);
      if (!supportedProtocols.isEmpty()) {
         String protocol = ((String)supportedProtocols.iterator().next()).toUpperCase(Locale.US);
         return (HttpServerUpgradeHandler.UpgradeCodec)this.upgradeCodecMap.get(protocol);
      } else {
         return null;
      }
   }

   private static FullHttpResponse createUpgradeResponse(HttpServerUpgradeHandler.UpgradeCodec upgradeCodec) {
      DefaultFullHttpResponse res = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.SWITCHING_PROTOCOLS);
      res.headers().add(HttpHeaderNames.CONNECTION, (CharSequence)HttpHeaderValues.UPGRADE);
      res.headers().add(HttpHeaderNames.UPGRADE, (CharSequence)upgradeCodec.protocol());
      res.headers().add(HttpHeaderNames.CONTENT_LENGTH, (CharSequence)"0");
      return res;
   }

   private static Set<CharSequence> splitHeader(CharSequence header) {
      StringBuilder builder = new StringBuilder(header.length());
      Set<CharSequence> protocols = new TreeSet(AsciiString.CHARSEQUENCE_CASE_INSENSITIVE_ORDER);

      for(int i = 0; i < header.length(); ++i) {
         char c = header.charAt(i);
         if (!Character.isWhitespace(c)) {
            if (c == ',') {
               protocols.add(builder.toString());
               builder.setLength(0);
            } else {
               builder.append(c);
            }
         }
      }

      if (builder.length() > 0) {
         protocols.add(builder.toString());
      }

      return protocols;
   }

   public static final class UpgradeEvent implements ReferenceCounted {
      private final String protocol;
      private final FullHttpRequest upgradeRequest;

      private UpgradeEvent(String protocol, FullHttpRequest upgradeRequest) {
         this.protocol = protocol;
         this.upgradeRequest = upgradeRequest;
      }

      public String protocol() {
         return this.protocol;
      }

      public FullHttpRequest upgradeRequest() {
         return this.upgradeRequest;
      }

      public int refCnt() {
         return this.upgradeRequest.refCnt();
      }

      public HttpServerUpgradeHandler.UpgradeEvent retain() {
         this.upgradeRequest.retain();
         return this;
      }

      public HttpServerUpgradeHandler.UpgradeEvent retain(int increment) {
         this.upgradeRequest.retain(increment);
         return this;
      }

      public HttpServerUpgradeHandler.UpgradeEvent touch() {
         this.upgradeRequest.touch();
         return this;
      }

      public HttpServerUpgradeHandler.UpgradeEvent touch(Object hint) {
         this.upgradeRequest.touch(hint);
         return this;
      }

      public boolean release() {
         return this.upgradeRequest.release();
      }

      public boolean release(int decrement) {
         return this.upgradeRequest.release();
      }

      public String toString() {
         return "UpgradeEvent [protocol=" + this.protocol + ", upgradeRequest=" + this.upgradeRequest + ']';
      }

      // $FF: synthetic method
      UpgradeEvent(String x0, FullHttpRequest x1, Object x2) {
         this(x0, x1);
      }
   }

   public interface UpgradeCodec {
      String protocol();

      Collection<String> requiredUpgradeHeaders();

      void prepareUpgradeResponse(ChannelHandlerContext var1, FullHttpRequest var2, FullHttpResponse var3);

      void upgradeTo(ChannelHandlerContext var1, FullHttpRequest var2, FullHttpResponse var3);
   }

   public interface SourceCodec {
      void upgradeFrom(ChannelHandlerContext var1);
   }
}
