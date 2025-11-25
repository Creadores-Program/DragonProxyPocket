package io.netty.handler.codec;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

public interface Headers<T> extends Iterable<Entry<T, T>> {
   T get(T var1);

   T get(T var1, T var2);

   T getAndRemove(T var1);

   T getAndRemove(T var1, T var2);

   List<T> getAll(T var1);

   List<T> getAllAndRemove(T var1);

   Boolean getBoolean(T var1);

   boolean getBoolean(T var1, boolean var2);

   Byte getByte(T var1);

   byte getByte(T var1, byte var2);

   Character getChar(T var1);

   char getChar(T var1, char var2);

   Short getShort(T var1);

   short getInt(T var1, short var2);

   Integer getInt(T var1);

   int getInt(T var1, int var2);

   Long getLong(T var1);

   long getLong(T var1, long var2);

   Float getFloat(T var1);

   float getFloat(T var1, float var2);

   Double getDouble(T var1);

   double getDouble(T var1, double var2);

   Long getTimeMillis(T var1);

   long getTimeMillis(T var1, long var2);

   Boolean getBooleanAndRemove(T var1);

   boolean getBooleanAndRemove(T var1, boolean var2);

   Byte getByteAndRemove(T var1);

   byte getByteAndRemove(T var1, byte var2);

   Character getCharAndRemove(T var1);

   char getCharAndRemove(T var1, char var2);

   Short getShortAndRemove(T var1);

   short getShortAndRemove(T var1, short var2);

   Integer getIntAndRemove(T var1);

   int getIntAndRemove(T var1, int var2);

   Long getLongAndRemove(T var1);

   long getLongAndRemove(T var1, long var2);

   Float getFloatAndRemove(T var1);

   float getFloatAndRemove(T var1, float var2);

   Double getDoubleAndRemove(T var1);

   double getDoubleAndRemove(T var1, double var2);

   Long getTimeMillisAndRemove(T var1);

   long getTimeMillisAndRemove(T var1, long var2);

   List<Entry<T, T>> entries();

   boolean contains(T var1);

   boolean contains(T var1, T var2);

   boolean containsObject(T var1, Object var2);

   boolean containsBoolean(T var1, boolean var2);

   boolean containsByte(T var1, byte var2);

   boolean containsChar(T var1, char var2);

   boolean containsShort(T var1, short var2);

   boolean containsInt(T var1, int var2);

   boolean containsLong(T var1, long var2);

   boolean containsFloat(T var1, float var2);

   boolean containsDouble(T var1, double var2);

   boolean containsTimeMillis(T var1, long var2);

   boolean contains(T var1, T var2, Comparator<? super T> var3);

   boolean contains(T var1, T var2, Comparator<? super T> var3, Comparator<? super T> var4);

   boolean containsObject(T var1, Object var2, Comparator<? super T> var3);

   boolean containsObject(T var1, Object var2, Comparator<? super T> var3, Comparator<? super T> var4);

   int size();

   boolean isEmpty();

   Set<T> names();

   List<T> namesList();

   Headers<T> add(T var1, T var2);

   Headers<T> add(T var1, Iterable<? extends T> var2);

   Headers<T> add(T var1, T... var2);

   Headers<T> addObject(T var1, Object var2);

   Headers<T> addObject(T var1, Iterable<?> var2);

   Headers<T> addObject(T var1, Object... var2);

   Headers<T> addBoolean(T var1, boolean var2);

   Headers<T> addByte(T var1, byte var2);

   Headers<T> addChar(T var1, char var2);

   Headers<T> addShort(T var1, short var2);

   Headers<T> addInt(T var1, int var2);

   Headers<T> addLong(T var1, long var2);

   Headers<T> addFloat(T var1, float var2);

   Headers<T> addDouble(T var1, double var2);

   Headers<T> addTimeMillis(T var1, long var2);

   Headers<T> add(Headers<T> var1);

   Headers<T> set(T var1, T var2);

   Headers<T> set(T var1, Iterable<? extends T> var2);

   Headers<T> set(T var1, T... var2);

   Headers<T> setObject(T var1, Object var2);

   Headers<T> setObject(T var1, Iterable<?> var2);

   Headers<T> setObject(T var1, Object... var2);

   Headers<T> setBoolean(T var1, boolean var2);

   Headers<T> setByte(T var1, byte var2);

   Headers<T> setChar(T var1, char var2);

   Headers<T> setShort(T var1, short var2);

   Headers<T> setInt(T var1, int var2);

   Headers<T> setLong(T var1, long var2);

   Headers<T> setFloat(T var1, float var2);

   Headers<T> setDouble(T var1, double var2);

   Headers<T> setTimeMillis(T var1, long var2);

   Headers<T> set(Headers<T> var1);

   Headers<T> setAll(Headers<T> var1);

   boolean remove(T var1);

   Headers<T> clear();

   Iterator<Entry<T, T>> iterator();

   Entry<T, T> forEachEntry(Headers.EntryVisitor<T> var1) throws Exception;

   T forEachName(Headers.NameVisitor<T> var1) throws Exception;

   public interface ValueConverter<T> {
      T convertObject(Object var1);

      T convertBoolean(boolean var1);

      boolean convertToBoolean(T var1);

      T convertByte(byte var1);

      byte convertToByte(T var1);

      T convertChar(char var1);

      char convertToChar(T var1);

      T convertShort(short var1);

      short convertToShort(T var1);

      T convertInt(int var1);

      int convertToInt(T var1);

      T convertLong(long var1);

      long convertToLong(T var1);

      T convertTimeMillis(long var1);

      long convertToTimeMillis(T var1);

      T convertFloat(float var1);

      float convertToFloat(T var1);

      T convertDouble(double var1);

      double convertToDouble(T var1);
   }

   public interface NameVisitor<T> {
      boolean visit(T var1) throws Exception;
   }

   public interface EntryVisitor<T> {
      boolean visit(Entry<T, T> var1) throws Exception;
   }
}
