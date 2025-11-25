package io.netty.handler.codec.http;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public final class ServerCookieDecoder {
   public static Set<Cookie> decode(String header) {
      if (header == null) {
         throw new NullPointerException("header");
      } else {
         int headerLen = header.length();
         if (headerLen == 0) {
            return Collections.emptySet();
         } else {
            Set<Cookie> cookies = new TreeSet();
            int i = 0;
            boolean rfc2965Style = false;
            if (header.regionMatches(true, 0, "$Version", 0, 8)) {
               i = header.indexOf(59) + 1;
               rfc2965Style = true;
            }

            while(true) {
               int newNameEnd;
               String value;
               label115:
               do {
                  while(i != headerLen) {
                     char c = header.charAt(i);
                     if (c != '\t' && c != '\n' && c != 11 && c != '\f' && c != '\r' && c != ' ' && c != ',' && c != ';') {
                        newNameEnd = i;
                        if (i == headerLen) {
                           value = null;
                        } else {
                           do {
                              char curChar = header.charAt(i);
                              if (curChar == ';') {
                                 newNameEnd = i;
                                 value = null;
                                 continue label115;
                              }

                              if (curChar == '=') {
                                 newNameEnd = i++;
                                 if (i == headerLen) {
                                    value = "";
                                    continue label115;
                                 }

                                 char c = header.charAt(i);
                                 if (c != '"') {
                                    int semiPos = header.indexOf(59, i);
                                    if (semiPos > 0) {
                                       value = header.substring(i, semiPos);
                                       i = semiPos;
                                    } else {
                                       value = header.substring(i);
                                       i = headerLen;
                                    }
                                    continue label115;
                                 }

                                 StringBuilder newValueBuf = CookieEncoderUtil.stringBuilder();
                                 char q = c;
                                 boolean hadBackslash = false;
                                 ++i;

                                 while(true) {
                                    while(true) {
                                       while(i != headerLen) {
                                          if (hadBackslash) {
                                             hadBackslash = false;
                                             c = header.charAt(i++);
                                             if (c != '\\' && c != '"') {
                                                newValueBuf.append(c);
                                             } else {
                                                newValueBuf.setCharAt(newValueBuf.length() - 1, c);
                                             }
                                          } else {
                                             c = header.charAt(i++);
                                             if (c == q) {
                                                value = newValueBuf.toString();
                                                continue label115;
                                             }

                                             newValueBuf.append(c);
                                             if (c == '\\') {
                                                hadBackslash = true;
                                             }
                                          }
                                       }

                                       value = newValueBuf.toString();
                                       continue label115;
                                    }
                                 }
                              }

                              ++i;
                           } while(i != headerLen);

                           newNameEnd = headerLen;
                           value = null;
                        }
                        continue label115;
                     }

                     ++i;
                  }

                  return cookies;
               } while(rfc2965Style && (header.regionMatches(i, "$Path", 0, "$Path".length()) || header.regionMatches(i, "$Domain", 0, "$Domain".length()) || header.regionMatches(i, "$Port", 0, "$Port".length())));

               String name = header.substring(i, newNameEnd);
               cookies.add(new DefaultCookie(name, value));
            }
         }
      }
   }

   private ServerCookieDecoder() {
   }
}
