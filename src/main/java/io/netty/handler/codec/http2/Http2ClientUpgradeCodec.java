package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.base64.Base64Dialect;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientUpgradeHandler;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.collection.IntObjectMap;
import io.netty.util.internal.ObjectUtil;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Http2ClientUpgradeCodec implements HttpClientUpgradeHandler.UpgradeCodec {
   private static final List<String> UPGRADE_HEADERS = Collections.singletonList("HTTP2-Settings");
   private final String handlerName;
   private final Http2ConnectionHandler connectionHandler;

   public Http2ClientUpgradeCodec(Http2ConnectionHandler connectionHandler) {
      this("http2ConnectionHandler", connectionHandler);
   }

   public Http2ClientUpgradeCodec(String handlerName, Http2ConnectionHandler connectionHandler) {
      this.handlerName = (String)ObjectUtil.checkNotNull(handlerName, "handlerName");
      this.connectionHandler = (Http2ConnectionHandler)ObjectUtil.checkNotNull(connectionHandler, "connectionHandler");
   }

   public String protocol() {
      return "h2c-16";
   }

   public Collection<String> setUpgradeHeaders(ChannelHandlerContext ctx, HttpRequest upgradeRequest) {
      String settingsValue = this.getSettingsHeaderValue(ctx);
      upgradeRequest.headers().set("HTTP2-Settings", (CharSequence)settingsValue);
      return UPGRADE_HEADERS;
   }

   public void upgradeTo(ChannelHandlerContext ctx, FullHttpResponse upgradeResponse) throws Exception {
      this.connectionHandler.onHttpClientUpgrade();
      ctx.pipeline().addAfter(ctx.name(), this.handlerName, this.connectionHandler);
   }

   private String getSettingsHeaderValue(ChannelHandlerContext ctx) {
      ByteBuf buf = null;
      ByteBuf encodedBuf = null;

      try {
         Http2Settings settings = this.connectionHandler.decoder().localSettings();
         int payloadLength = 6 * settings.size();
         buf = ctx.alloc().buffer(payloadLength);
         Iterator i$ = settings.entries().iterator();

         while(i$.hasNext()) {
            IntObjectMap.Entry<Long> entry = (IntObjectMap.Entry)i$.next();
            Http2CodecUtil.writeUnsignedShort(entry.key(), buf);
            Http2CodecUtil.writeUnsignedInt((Long)entry.value(), buf);
         }

         encodedBuf = Base64.encode(buf, Base64Dialect.URL_SAFE);
         String var11 = encodedBuf.toString(CharsetUtil.UTF_8);
         return var11;
      } finally {
         ReferenceCountUtil.release(buf);
         ReferenceCountUtil.release(encodedBuf);
      }
   }
}
