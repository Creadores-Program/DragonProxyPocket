package io.netty.handler.codec.http;

import io.netty.handler.codec.TextHeaders;

public interface HttpHeaders extends TextHeaders {
   HttpHeaders add(CharSequence var1, CharSequence var2);

   HttpHeaders add(CharSequence var1, Iterable<? extends CharSequence> var2);

   HttpHeaders add(CharSequence var1, CharSequence... var2);

   HttpHeaders addObject(CharSequence var1, Object var2);

   HttpHeaders addObject(CharSequence var1, Iterable<?> var2);

   HttpHeaders addObject(CharSequence var1, Object... var2);

   HttpHeaders addBoolean(CharSequence var1, boolean var2);

   HttpHeaders addByte(CharSequence var1, byte var2);

   HttpHeaders addChar(CharSequence var1, char var2);

   HttpHeaders addShort(CharSequence var1, short var2);

   HttpHeaders addInt(CharSequence var1, int var2);

   HttpHeaders addLong(CharSequence var1, long var2);

   HttpHeaders addFloat(CharSequence var1, float var2);

   HttpHeaders addDouble(CharSequence var1, double var2);

   HttpHeaders addTimeMillis(CharSequence var1, long var2);

   HttpHeaders add(TextHeaders var1);

   HttpHeaders set(CharSequence var1, CharSequence var2);

   HttpHeaders set(CharSequence var1, Iterable<? extends CharSequence> var2);

   HttpHeaders set(CharSequence var1, CharSequence... var2);

   HttpHeaders setObject(CharSequence var1, Object var2);

   HttpHeaders setObject(CharSequence var1, Iterable<?> var2);

   HttpHeaders setObject(CharSequence var1, Object... var2);

   HttpHeaders setBoolean(CharSequence var1, boolean var2);

   HttpHeaders setByte(CharSequence var1, byte var2);

   HttpHeaders setChar(CharSequence var1, char var2);

   HttpHeaders setShort(CharSequence var1, short var2);

   HttpHeaders setInt(CharSequence var1, int var2);

   HttpHeaders setLong(CharSequence var1, long var2);

   HttpHeaders setFloat(CharSequence var1, float var2);

   HttpHeaders setDouble(CharSequence var1, double var2);

   HttpHeaders setTimeMillis(CharSequence var1, long var2);

   HttpHeaders set(TextHeaders var1);

   HttpHeaders setAll(TextHeaders var1);

   HttpHeaders clear();
}
