package io.netty.handler.codec;

public interface BinaryHeaders extends Headers<AsciiString> {
   BinaryHeaders add(AsciiString var1, AsciiString var2);

   BinaryHeaders add(AsciiString var1, Iterable<? extends AsciiString> var2);

   BinaryHeaders add(AsciiString var1, AsciiString... var2);

   BinaryHeaders addObject(AsciiString var1, Object var2);

   BinaryHeaders addObject(AsciiString var1, Iterable<?> var2);

   BinaryHeaders addObject(AsciiString var1, Object... var2);

   BinaryHeaders addBoolean(AsciiString var1, boolean var2);

   BinaryHeaders addByte(AsciiString var1, byte var2);

   BinaryHeaders addChar(AsciiString var1, char var2);

   BinaryHeaders addShort(AsciiString var1, short var2);

   BinaryHeaders addInt(AsciiString var1, int var2);

   BinaryHeaders addLong(AsciiString var1, long var2);

   BinaryHeaders addFloat(AsciiString var1, float var2);

   BinaryHeaders addDouble(AsciiString var1, double var2);

   BinaryHeaders addTimeMillis(AsciiString var1, long var2);

   BinaryHeaders add(BinaryHeaders var1);

   BinaryHeaders set(AsciiString var1, AsciiString var2);

   BinaryHeaders set(AsciiString var1, Iterable<? extends AsciiString> var2);

   BinaryHeaders set(AsciiString var1, AsciiString... var2);

   BinaryHeaders setObject(AsciiString var1, Object var2);

   BinaryHeaders setObject(AsciiString var1, Iterable<?> var2);

   BinaryHeaders setObject(AsciiString var1, Object... var2);

   BinaryHeaders setBoolean(AsciiString var1, boolean var2);

   BinaryHeaders setByte(AsciiString var1, byte var2);

   BinaryHeaders setChar(AsciiString var1, char var2);

   BinaryHeaders setShort(AsciiString var1, short var2);

   BinaryHeaders setInt(AsciiString var1, int var2);

   BinaryHeaders setLong(AsciiString var1, long var2);

   BinaryHeaders setFloat(AsciiString var1, float var2);

   BinaryHeaders setDouble(AsciiString var1, double var2);

   BinaryHeaders setTimeMillis(AsciiString var1, long var2);

   BinaryHeaders set(BinaryHeaders var1);

   BinaryHeaders setAll(BinaryHeaders var1);

   BinaryHeaders clear();

   public interface NameVisitor extends Headers.NameVisitor<AsciiString> {
   }

   public interface EntryVisitor extends Headers.EntryVisitor<AsciiString> {
   }
}
