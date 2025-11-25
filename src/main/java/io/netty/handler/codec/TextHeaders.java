package io.netty.handler.codec;

public interface TextHeaders extends ConvertibleHeaders<CharSequence, String> {
   boolean contains(CharSequence var1, CharSequence var2, boolean var3);

   boolean containsObject(CharSequence var1, Object var2, boolean var3);

   TextHeaders add(CharSequence var1, CharSequence var2);

   TextHeaders add(CharSequence var1, Iterable<? extends CharSequence> var2);

   TextHeaders add(CharSequence var1, CharSequence... var2);

   TextHeaders addObject(CharSequence var1, Object var2);

   TextHeaders addObject(CharSequence var1, Iterable<?> var2);

   TextHeaders addObject(CharSequence var1, Object... var2);

   TextHeaders addBoolean(CharSequence var1, boolean var2);

   TextHeaders addByte(CharSequence var1, byte var2);

   TextHeaders addChar(CharSequence var1, char var2);

   TextHeaders addShort(CharSequence var1, short var2);

   TextHeaders addInt(CharSequence var1, int var2);

   TextHeaders addLong(CharSequence var1, long var2);

   TextHeaders addFloat(CharSequence var1, float var2);

   TextHeaders addDouble(CharSequence var1, double var2);

   TextHeaders addTimeMillis(CharSequence var1, long var2);

   TextHeaders add(TextHeaders var1);

   TextHeaders set(CharSequence var1, CharSequence var2);

   TextHeaders set(CharSequence var1, Iterable<? extends CharSequence> var2);

   TextHeaders set(CharSequence var1, CharSequence... var2);

   TextHeaders setObject(CharSequence var1, Object var2);

   TextHeaders setObject(CharSequence var1, Iterable<?> var2);

   TextHeaders setObject(CharSequence var1, Object... var2);

   TextHeaders setBoolean(CharSequence var1, boolean var2);

   TextHeaders setByte(CharSequence var1, byte var2);

   TextHeaders setChar(CharSequence var1, char var2);

   TextHeaders setShort(CharSequence var1, short var2);

   TextHeaders setInt(CharSequence var1, int var2);

   TextHeaders setLong(CharSequence var1, long var2);

   TextHeaders setFloat(CharSequence var1, float var2);

   TextHeaders setDouble(CharSequence var1, double var2);

   TextHeaders setTimeMillis(CharSequence var1, long var2);

   TextHeaders set(TextHeaders var1);

   TextHeaders setAll(TextHeaders var1);

   TextHeaders clear();

   public interface NameVisitor extends Headers.NameVisitor<CharSequence> {
   }

   public interface EntryVisitor extends Headers.EntryVisitor<CharSequence> {
   }
}
