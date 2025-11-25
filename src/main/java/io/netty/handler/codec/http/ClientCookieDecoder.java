package io.netty.handler.codec.http;

import java.text.ParsePosition;
import java.util.Date;

public final class ClientCookieDecoder {
   public static Cookie decode(String header) {
      if (header == null) {
         throw new NullPointerException("header");
      } else {
         int headerLen = header.length();
         if (headerLen == 0) {
            return null;
         } else {
            ClientCookieDecoder.CookieBuilder cookieBuilder = null;
            int i = 0;

            while(i != headerLen) {
               char c = header.charAt(i);
               if (c == ',') {
                  break;
               }

               if (c != '\t' && c != '\n' && c != 11 && c != '\f' && c != '\r' && c != ' ' && c != ';') {
                  int newNameEnd = i;
                  String value;
                  String rawValue;
                  if (i == headerLen) {
                     rawValue = null;
                     value = null;
                  } else {
                     label92:
                     while(true) {
                        char curChar = header.charAt(i);
                        if (curChar == ';') {
                           newNameEnd = i;
                           rawValue = null;
                           value = null;
                           break;
                        }

                        if (curChar == '=') {
                           newNameEnd = i++;
                           if (i == headerLen) {
                              rawValue = "";
                              value = "";
                           } else {
                              char c = header.charAt(i);
                              if (c == '"') {
                                 StringBuilder newValueBuf = CookieEncoderUtil.stringBuilder();
                                 int rawValueStart = i;
                                 int rawValueEnd = i;
                                 char q = c;
                                 boolean hadBackslash = false;
                                 ++i;

                                 while(true) {
                                    while(true) {
                                       while(i != headerLen) {
                                          if (hadBackslash) {
                                             hadBackslash = false;
                                             c = header.charAt(i++);
                                             rawValueEnd = i;
                                             if (c != '\\' && c != '"') {
                                                newValueBuf.append(c);
                                             } else {
                                                newValueBuf.setCharAt(newValueBuf.length() - 1, c);
                                             }
                                          } else {
                                             c = header.charAt(i++);
                                             rawValueEnd = i;
                                             if (c == q) {
                                                value = newValueBuf.toString();
                                                rawValue = header.substring(rawValueStart, i);
                                                break label92;
                                             }

                                             newValueBuf.append(c);
                                             if (c == '\\') {
                                                hadBackslash = true;
                                             }
                                          }
                                       }

                                       value = newValueBuf.toString();
                                       rawValue = header.substring(rawValueStart, rawValueEnd);
                                       break label92;
                                    }
                                 }
                              } else {
                                 int semiPos = header.indexOf(59, i);
                                 if (semiPos > 0) {
                                    value = rawValue = header.substring(i, semiPos);
                                    i = semiPos;
                                 } else {
                                    value = rawValue = header.substring(i);
                                    i = headerLen;
                                 }
                              }
                           }
                           break;
                        }

                        ++i;
                        if (i == headerLen) {
                           newNameEnd = i;
                           rawValue = null;
                           value = null;
                           break;
                        }
                     }
                  }

                  if (cookieBuilder == null) {
                     cookieBuilder = new ClientCookieDecoder.CookieBuilder(header, i, newNameEnd, value, rawValue);
                  } else {
                     cookieBuilder.appendAttribute(header, i, newNameEnd, value);
                  }
               } else {
                  ++i;
               }
            }

            return cookieBuilder.cookie();
         }
      }
   }

   private ClientCookieDecoder() {
   }

   private static class CookieBuilder {
      private final String name;
      private final String value;
      private final String rawValue;
      private String domain;
      private String path;
      private long maxAge = Long.MIN_VALUE;
      private String expires;
      private boolean secure;
      private boolean httpOnly;

      public CookieBuilder(String header, int keyStart, int keyEnd, String value, String rawValue) {
         this.name = header.substring(keyStart, keyEnd);
         this.value = value;
         this.rawValue = rawValue;
      }

      private long mergeMaxAgeAndExpire(long maxAge, String expires) {
         if (maxAge != Long.MIN_VALUE) {
            return maxAge;
         } else {
            if (expires != null) {
               Date expiresDate = HttpHeaderDateFormat.get().parse(expires, new ParsePosition(0));
               if (expiresDate != null) {
                  long maxAgeMillis = expiresDate.getTime() - System.currentTimeMillis();
                  return maxAgeMillis / 1000L + (long)(maxAgeMillis % 1000L != 0L ? 1 : 0);
               }
            }

            return Long.MIN_VALUE;
         }
      }

      public Cookie cookie() {
         if (this.name == null) {
            return null;
         } else {
            DefaultCookie cookie = new DefaultCookie(this.name, this.value);
            cookie.setValue(this.value);
            cookie.setRawValue(this.rawValue);
            cookie.setDomain(this.domain);
            cookie.setPath(this.path);
            cookie.setMaxAge(this.mergeMaxAgeAndExpire(this.maxAge, this.expires));
            cookie.setSecure(this.secure);
            cookie.setHttpOnly(this.httpOnly);
            return cookie;
         }
      }

      public void appendAttribute(String header, int keyStart, int keyEnd, String value) {
         this.setCookieAttribute(header, keyStart, keyEnd, value);
      }

      private void setCookieAttribute(String header, int keyStart, int keyEnd, String value) {
         int length = keyEnd - keyStart;
         if (length == 4) {
            this.parse4(header, keyStart, value);
         } else if (length == 6) {
            this.parse6(header, keyStart, value);
         } else if (length == 7) {
            this.parse7(header, keyStart, value);
         } else if (length == 8) {
            this.parse8(header, keyStart, value);
         }

      }

      private void parse4(String header, int nameStart, String value) {
         if (header.regionMatches(true, nameStart, "Path", 0, 4)) {
            this.path = value;
         }

      }

      private void parse6(String header, int nameStart, String value) {
         if (header.regionMatches(true, nameStart, "Domain", 0, 5)) {
            this.domain = value.isEmpty() ? null : value;
         } else if (header.regionMatches(true, nameStart, "Secure", 0, 5)) {
            this.secure = true;
         }

      }

      private void setExpire(String value) {
         this.expires = value;
      }

      private void setMaxAge(String value) {
         try {
            this.maxAge = Math.max(Long.valueOf(value), 0L);
         } catch (NumberFormatException var3) {
         }

      }

      private void parse7(String header, int nameStart, String value) {
         if (header.regionMatches(true, nameStart, "Expires", 0, 7)) {
            this.setExpire(value);
         } else if (header.regionMatches(true, nameStart, "Max-Age", 0, 7)) {
            this.setMaxAge(value);
         }

      }

      private void parse8(String header, int nameStart, String value) {
         if (header.regionMatches(true, nameStart, "HttpOnly", 0, 8)) {
            this.httpOnly = true;
         }

      }
   }
}
