package io.netty.handler.codec.spdy;

import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.TextHeaders;

public interface SpdyHeaders extends TextHeaders {
   SpdyHeaders add(CharSequence var1, CharSequence var2);

   SpdyHeaders add(CharSequence var1, Iterable<? extends CharSequence> var2);

   SpdyHeaders add(CharSequence var1, CharSequence... var2);

   SpdyHeaders addObject(CharSequence var1, Object var2);

   SpdyHeaders addObject(CharSequence var1, Iterable<?> var2);

   SpdyHeaders addObject(CharSequence var1, Object... var2);

   SpdyHeaders addBoolean(CharSequence var1, boolean var2);

   SpdyHeaders addByte(CharSequence var1, byte var2);

   SpdyHeaders addChar(CharSequence var1, char var2);

   SpdyHeaders addShort(CharSequence var1, short var2);

   SpdyHeaders addInt(CharSequence var1, int var2);

   SpdyHeaders addLong(CharSequence var1, long var2);

   SpdyHeaders addFloat(CharSequence var1, float var2);

   SpdyHeaders addDouble(CharSequence var1, double var2);

   SpdyHeaders addTimeMillis(CharSequence var1, long var2);

   SpdyHeaders add(TextHeaders var1);

   SpdyHeaders set(CharSequence var1, CharSequence var2);

   SpdyHeaders set(CharSequence var1, Iterable<? extends CharSequence> var2);

   SpdyHeaders set(CharSequence var1, CharSequence... var2);

   SpdyHeaders setBoolean(CharSequence var1, boolean var2);

   SpdyHeaders setByte(CharSequence var1, byte var2);

   SpdyHeaders setChar(CharSequence var1, char var2);

   SpdyHeaders setShort(CharSequence var1, short var2);

   SpdyHeaders setInt(CharSequence var1, int var2);

   SpdyHeaders setLong(CharSequence var1, long var2);

   SpdyHeaders setFloat(CharSequence var1, float var2);

   SpdyHeaders setDouble(CharSequence var1, double var2);

   SpdyHeaders setTimeMillis(CharSequence var1, long var2);

   SpdyHeaders setObject(CharSequence var1, Object var2);

   SpdyHeaders setObject(CharSequence var1, Iterable<?> var2);

   SpdyHeaders setObject(CharSequence var1, Object... var2);

   SpdyHeaders set(TextHeaders var1);

   SpdyHeaders setAll(TextHeaders var1);

   SpdyHeaders clear();

   public static final class HttpNames {
      public static final AsciiString HOST = new AsciiString(":host");
      public static final AsciiString METHOD = new AsciiString(":method");
      public static final AsciiString PATH = new AsciiString(":path");
      public static final AsciiString SCHEME = new AsciiString(":scheme");
      public static final AsciiString STATUS = new AsciiString(":status");
      public static final AsciiString VERSION = new AsciiString(":version");

      private HttpNames() {
      }
   }
}
