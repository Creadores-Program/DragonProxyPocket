package io.netty.handler.codec.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.AsciiString;
import io.netty.util.ReferenceCountUtil;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class HttpClientUpgradeHandler extends HttpObjectAggregator {
   private final HttpClientUpgradeHandler.SourceCodec sourceCodec;
   private final HttpClientUpgradeHandler.UpgradeCodec upgradeCodec;
   private boolean upgradeRequested;

   public HttpClientUpgradeHandler(HttpClientUpgradeHandler.SourceCodec sourceCodec, HttpClientUpgradeHandler.UpgradeCodec upgradeCodec, int maxContentLength) {
      super(maxContentLength);
      if (sourceCodec == null) {
         throw new NullPointerException("sourceCodec");
      } else if (upgradeCodec == null) {
         throw new NullPointerException("upgradeCodec");
      } else {
         this.sourceCodec = sourceCodec;
         this.upgradeCodec = upgradeCodec;
      }
   }

   public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      if (!(msg instanceof HttpRequest)) {
         super.write(ctx, msg, promise);
      } else if (this.upgradeRequested) {
         promise.setFailure(new IllegalStateException("Attempting to write HTTP request with upgrade in progress"));
      } else {
         this.upgradeRequested = true;
         this.setUpgradeRequestHeaders(ctx, (HttpRequest)msg);
         super.write(ctx, msg, promise);
         ctx.fireUserEventTriggered(HttpClientUpgradeHandler.UpgradeEvent.UPGRADE_ISSUED);
      }
   }

   protected void decode(ChannelHandlerContext ctx, HttpObject msg, List<Object> out) throws Exception {
      FullHttpResponse response = null;

      try {
         if (!this.upgradeRequested) {
            throw new IllegalStateException("Read HTTP response without requesting protocol switch");
         }

         if (msg instanceof FullHttpResponse) {
            response = (FullHttpResponse)msg;
            response.retain();
            out.add(response);
         } else {
            super.decode(ctx, msg, out);
            if (out.isEmpty()) {
               return;
            }

            assert out.size() == 1;

            response = (FullHttpResponse)out.get(0);
         }

         if (!HttpResponseStatus.SWITCHING_PROTOCOLS.equals(response.status())) {
            ctx.fireUserEventTriggered(HttpClientUpgradeHandler.UpgradeEvent.UPGRADE_REJECTED);
            removeThisHandler(ctx);
            return;
         }

         CharSequence upgradeHeader = (CharSequence)response.headers().get(HttpHeaderNames.UPGRADE);
         if (upgradeHeader == null) {
            throw new IllegalStateException("Switching Protocols response missing UPGRADE header");
         }

         if (!AsciiString.equalsIgnoreCase(this.upgradeCodec.protocol(), upgradeHeader)) {
            throw new IllegalStateException("Switching Protocols response with unexpected UPGRADE protocol: " + upgradeHeader);
         }

         this.sourceCodec.upgradeFrom(ctx);
         this.upgradeCodec.upgradeTo(ctx, response);
         ctx.fireUserEventTriggered(HttpClientUpgradeHandler.UpgradeEvent.UPGRADE_SUCCESSFUL);
         response.release();
         out.clear();
         removeThisHandler(ctx);
      } catch (Throwable var6) {
         ReferenceCountUtil.release(response);
         ctx.fireExceptionCaught(var6);
         removeThisHandler(ctx);
      }

   }

   private static void removeThisHandler(ChannelHandlerContext ctx) {
      ctx.pipeline().remove(ctx.name());
   }

   private void setUpgradeRequestHeaders(ChannelHandlerContext ctx, HttpRequest request) {
      request.headers().set(HttpHeaderNames.UPGRADE, (CharSequence)this.upgradeCodec.protocol());
      Set<String> connectionParts = new LinkedHashSet(2);
      connectionParts.addAll(this.upgradeCodec.setUpgradeHeaders(ctx, request));
      StringBuilder builder = new StringBuilder();
      Iterator i$ = connectionParts.iterator();

      while(i$.hasNext()) {
         String part = (String)i$.next();
         builder.append(part);
         builder.append(',');
      }

      builder.append(HttpHeaderNames.UPGRADE);
      request.headers().set(HttpHeaderNames.CONNECTION, (CharSequence)builder.toString());
   }

   public interface UpgradeCodec {
      String protocol();

      Collection<String> setUpgradeHeaders(ChannelHandlerContext var1, HttpRequest var2);

      void upgradeTo(ChannelHandlerContext var1, FullHttpResponse var2) throws Exception;
   }

   public interface SourceCodec {
      void upgradeFrom(ChannelHandlerContext var1);
   }

   public static enum UpgradeEvent {
      UPGRADE_ISSUED,
      UPGRADE_SUCCESSFUL,
      UPGRADE_REJECTED;
   }
}
