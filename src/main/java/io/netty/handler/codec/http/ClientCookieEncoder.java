package io.netty.handler.codec.http;

import java.util.Iterator;

public final class ClientCookieEncoder {
   public static String encode(String name, String value) {
      return encode((Cookie)(new DefaultCookie(name, value)));
   }

   public static String encode(Cookie cookie) {
      if (cookie == null) {
         throw new NullPointerException("cookie");
      } else {
         StringBuilder buf = CookieEncoderUtil.stringBuilder();
         encode(buf, cookie);
         return CookieEncoderUtil.stripTrailingSeparator(buf);
      }
   }

   public static String encode(Cookie... cookies) {
      if (cookies == null) {
         throw new NullPointerException("cookies");
      } else if (cookies.length == 0) {
         return null;
      } else {
         StringBuilder buf = CookieEncoderUtil.stringBuilder();
         Cookie[] arr$ = cookies;
         int len$ = cookies.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            Cookie c = arr$[i$];
            if (c == null) {
               break;
            }

            encode(buf, c);
         }

         return CookieEncoderUtil.stripTrailingSeparatorOrNull(buf);
      }
   }

   public static String encode(Iterable<Cookie> cookies) {
      if (cookies == null) {
         throw new NullPointerException("cookies");
      } else if (!cookies.iterator().hasNext()) {
         return null;
      } else {
         StringBuilder buf = CookieEncoderUtil.stringBuilder();
         Iterator i$ = cookies.iterator();

         while(i$.hasNext()) {
            Cookie c = (Cookie)i$.next();
            if (c == null) {
               break;
            }

            encode(buf, c);
         }

         return CookieEncoderUtil.stripTrailingSeparatorOrNull(buf);
      }
   }

   private static void encode(StringBuilder buf, Cookie c) {
      String value = c.rawValue() != null ? c.rawValue() : (c.value() != null ? c.value() : "");
      CookieEncoderUtil.addUnquoted(buf, c.name(), value);
   }

   private ClientCookieEncoder() {
   }
}
