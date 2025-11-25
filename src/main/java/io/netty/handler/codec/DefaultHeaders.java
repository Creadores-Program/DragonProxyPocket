package io.netty.handler.codec;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Map.Entry;

public class DefaultHeaders<T> implements Headers<T> {
   private static final int HASH_CODE_PRIME = 31;
   private static final int DEFAULT_BUCKET_SIZE = 17;
   private static final int DEFAULT_MAP_SIZE = 4;
   private static final DefaultHeaders.NameConverter<Object> DEFAULT_NAME_CONVERTER = new DefaultHeaders.IdentityNameConverter();
   private final IntObjectMap<DefaultHeaders<T>.HeaderEntry> entries;
   private final IntObjectMap<DefaultHeaders<T>.HeaderEntry> tailEntries;
   private final DefaultHeaders<T>.HeaderEntry head;
   private final Comparator<? super T> keyComparator;
   private final Comparator<? super T> valueComparator;
   private final DefaultHeaders.HashCodeGenerator<T> hashCodeGenerator;
   private final Headers.ValueConverter<T> valueConverter;
   private final DefaultHeaders.NameConverter<T> nameConverter;
   private final int bucketSize;
   int size;

   public DefaultHeaders(Comparator<? super T> keyComparator, Comparator<? super T> valueComparator, DefaultHeaders.HashCodeGenerator<T> hashCodeGenerator, Headers.ValueConverter<T> typeConverter) {
      this(keyComparator, valueComparator, hashCodeGenerator, typeConverter, DEFAULT_NAME_CONVERTER);
   }

   public DefaultHeaders(Comparator<? super T> keyComparator, Comparator<? super T> valueComparator, DefaultHeaders.HashCodeGenerator<T> hashCodeGenerator, Headers.ValueConverter<T> typeConverter, DefaultHeaders.NameConverter<T> nameConverter) {
      this(keyComparator, valueComparator, hashCodeGenerator, typeConverter, nameConverter, 17, 4);
   }

   public DefaultHeaders(Comparator<? super T> keyComparator, Comparator<? super T> valueComparator, DefaultHeaders.HashCodeGenerator<T> hashCodeGenerator, Headers.ValueConverter<T> valueConverter, DefaultHeaders.NameConverter<T> nameConverter, int bucketSize, int initialMapSize) {
      if (keyComparator == null) {
         throw new NullPointerException("keyComparator");
      } else if (valueComparator == null) {
         throw new NullPointerException("valueComparator");
      } else if (hashCodeGenerator == null) {
         throw new NullPointerException("hashCodeGenerator");
      } else if (valueConverter == null) {
         throw new NullPointerException("valueConverter");
      } else if (nameConverter == null) {
         throw new NullPointerException("nameConverter");
      } else if (bucketSize < 1) {
         throw new IllegalArgumentException("bucketSize must be a positive integer");
      } else {
         this.head = new DefaultHeaders.HeaderEntry();
         this.head.before = this.head.after = this.head;
         this.keyComparator = keyComparator;
         this.valueComparator = valueComparator;
         this.hashCodeGenerator = hashCodeGenerator;
         this.valueConverter = valueConverter;
         this.nameConverter = nameConverter;
         this.bucketSize = bucketSize;
         this.entries = new IntObjectHashMap(initialMapSize);
         this.tailEntries = new IntObjectHashMap(initialMapSize);
      }
   }

   public T get(T name) {
      ObjectUtil.checkNotNull(name, "name");
      int h = this.hashCodeGenerator.generateHashCode(name);
      int i = this.index(h);

      for(DefaultHeaders.HeaderEntry e = (DefaultHeaders.HeaderEntry)this.entries.get(i); e != null; e = e.next) {
         if (e.hash == h && this.keyComparator.compare(e.name, name) == 0) {
            return e.value;
         }
      }

      return null;
   }

   public T get(T name, T defaultValue) {
      T value = this.get(name);
      return value == null ? defaultValue : value;
   }

   public T getAndRemove(T name) {
      ObjectUtil.checkNotNull(name, "name");
      int h = this.hashCodeGenerator.generateHashCode(name);
      int i = this.index(h);
      DefaultHeaders<T>.HeaderEntry e = (DefaultHeaders.HeaderEntry)this.entries.get(i);
      if (e == null) {
         return null;
      } else {
         Object value;
         DefaultHeaders.HeaderEntry next;
         for(value = null; e.hash == h && this.keyComparator.compare(e.name, name) == 0; e = next) {
            if (value == null) {
               value = e.value;
            }

            e.remove();
            next = e.next;
            if (next == null) {
               this.entries.remove(i);
               this.tailEntries.remove(i);
               return value;
            }

            this.entries.put(i, next);
         }

         while(true) {
            while(true) {
               next = e.next;
               if (next == null) {
                  return value;
               }

               if (next.hash == h && this.keyComparator.compare(e.name, name) == 0) {
                  if (value == null) {
                     value = next.value;
                  }

                  e.next = next.next;
                  if (e.next == null) {
                     this.tailEntries.put(i, e);
                  }

                  next.remove();
               } else {
                  e = next;
               }
            }
         }
      }
   }

   public T getAndRemove(T name, T defaultValue) {
      T value = this.getAndRemove(name);
      return value == null ? defaultValue : value;
   }

   public List<T> getAll(T name) {
      ObjectUtil.checkNotNull(name, "name");
      List<T> values = new ArrayList(4);
      int h = this.hashCodeGenerator.generateHashCode(name);
      int i = this.index(h);

      for(DefaultHeaders.HeaderEntry e = (DefaultHeaders.HeaderEntry)this.entries.get(i); e != null; e = e.next) {
         if (e.hash == h && this.keyComparator.compare(e.name, name) == 0) {
            values.add(e.value);
         }
      }

      return values;
   }

   public List<T> getAllAndRemove(T name) {
      ObjectUtil.checkNotNull(name, "name");
      int h = this.hashCodeGenerator.generateHashCode(name);
      int i = this.index(h);
      DefaultHeaders<T>.HeaderEntry e = (DefaultHeaders.HeaderEntry)this.entries.get(i);
      if (e == null) {
         return null;
      } else {
         ArrayList values;
         DefaultHeaders.HeaderEntry next;
         for(values = new ArrayList(4); e.hash == h && this.keyComparator.compare(e.name, name) == 0; e = next) {
            values.add(e.value);
            e.remove();
            next = e.next;
            if (next == null) {
               this.entries.remove(i);
               this.tailEntries.remove(i);
               return values;
            }

            this.entries.put(i, next);
         }

         while(true) {
            while(true) {
               next = e.next;
               if (next == null) {
                  return values;
               }

               if (next.hash == h && this.keyComparator.compare(next.name, name) == 0) {
                  values.add(next.value);
                  e.next = next.next;
                  if (e.next == null) {
                     this.tailEntries.put(i, e);
                  }

                  next.remove();
               } else {
                  e = next;
               }
            }
         }
      }
   }

   public List<Entry<T, T>> entries() {
      int size = this.size();
      List<Entry<T, T>> localEntries = new ArrayList(size);

      for(DefaultHeaders.HeaderEntry e = this.head.after; e != this.head; e = e.after) {
         localEntries.add(e);
      }

      assert size == localEntries.size();

      return localEntries;
   }

   public boolean contains(T name) {
      return this.get(name) != null;
   }

   public boolean contains(T name, T value) {
      return this.contains(name, value, this.keyComparator, this.valueComparator);
   }

   public boolean containsObject(T name, Object value) {
      return this.contains(name, this.valueConverter.convertObject(ObjectUtil.checkNotNull(value, "value")));
   }

   public boolean containsBoolean(T name, boolean value) {
      return this.contains(name, this.valueConverter.convertBoolean((Boolean)ObjectUtil.checkNotNull(value, "value")));
   }

   public boolean containsByte(T name, byte value) {
      return this.contains(name, this.valueConverter.convertByte((Byte)ObjectUtil.checkNotNull(value, "value")));
   }

   public boolean containsChar(T name, char value) {
      return this.contains(name, this.valueConverter.convertChar((Character)ObjectUtil.checkNotNull(value, "value")));
   }

   public boolean containsShort(T name, short value) {
      return this.contains(name, this.valueConverter.convertShort((Short)ObjectUtil.checkNotNull(value, "value")));
   }

   public boolean containsInt(T name, int value) {
      return this.contains(name, this.valueConverter.convertInt((Integer)ObjectUtil.checkNotNull(value, "value")));
   }

   public boolean containsLong(T name, long value) {
      return this.contains(name, this.valueConverter.convertLong((Long)ObjectUtil.checkNotNull(value, "value")));
   }

   public boolean containsFloat(T name, float value) {
      return this.contains(name, this.valueConverter.convertFloat((Float)ObjectUtil.checkNotNull(value, "value")));
   }

   public boolean containsDouble(T name, double value) {
      return this.contains(name, this.valueConverter.convertDouble((Double)ObjectUtil.checkNotNull(value, "value")));
   }

   public boolean containsTimeMillis(T name, long value) {
      return this.contains(name, this.valueConverter.convertTimeMillis((Long)ObjectUtil.checkNotNull(value, "value")));
   }

   public boolean contains(T name, T value, Comparator<? super T> comparator) {
      return this.contains(name, value, comparator, comparator);
   }

   public boolean contains(T name, T value, Comparator<? super T> keyComparator, Comparator<? super T> valueComparator) {
      ObjectUtil.checkNotNull(name, "name");
      ObjectUtil.checkNotNull(value, "value");
      ObjectUtil.checkNotNull(keyComparator, "keyComparator");
      ObjectUtil.checkNotNull(valueComparator, "valueComparator");
      int h = this.hashCodeGenerator.generateHashCode(name);
      int i = this.index(h);

      for(DefaultHeaders.HeaderEntry e = (DefaultHeaders.HeaderEntry)this.entries.get(i); e != null; e = e.next) {
         if (e.hash == h && keyComparator.compare(e.name, name) == 0 && valueComparator.compare(e.value, value) == 0) {
            return true;
         }
      }

      return false;
   }

   public boolean containsObject(T name, Object value, Comparator<? super T> comparator) {
      return this.containsObject(name, value, comparator, comparator);
   }

   public boolean containsObject(T name, Object value, Comparator<? super T> keyComparator, Comparator<? super T> valueComparator) {
      return this.contains(name, this.valueConverter.convertObject(ObjectUtil.checkNotNull(value, "value")), keyComparator, valueComparator);
   }

   public int size() {
      return this.size;
   }

   public boolean isEmpty() {
      return this.head == this.head.after;
   }

   public Set<T> names() {
      Set<T> names = new TreeSet(this.keyComparator);

      for(DefaultHeaders.HeaderEntry e = this.head.after; e != this.head; e = e.after) {
         names.add(e.name);
      }

      return names;
   }

   public List<T> namesList() {
      List<T> names = new ArrayList(this.size());

      for(DefaultHeaders.HeaderEntry e = this.head.after; e != this.head; e = e.after) {
         names.add(e.name);
      }

      return names;
   }

   public Headers<T> add(T name, T value) {
      name = this.convertName(name);
      ObjectUtil.checkNotNull(value, "value");
      int h = this.hashCodeGenerator.generateHashCode(name);
      int i = this.index(h);
      this.add0(h, i, name, value);
      return this;
   }

   public Headers<T> add(T name, Iterable<? extends T> values) {
      name = this.convertName(name);
      ObjectUtil.checkNotNull(values, "values");
      int h = this.hashCodeGenerator.generateHashCode(name);
      int i = this.index(h);
      Iterator i$ = values.iterator();

      while(i$.hasNext()) {
         T v = i$.next();
         if (v == null) {
            break;
         }

         this.add0(h, i, name, v);
      }

      return this;
   }

   public Headers<T> add(T name, T... values) {
      name = this.convertName(name);
      ObjectUtil.checkNotNull(values, "values");
      int h = this.hashCodeGenerator.generateHashCode(name);
      int i = this.index(h);
      Object[] arr$ = values;
      int len$ = values.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         T v = arr$[i$];
         if (v == null) {
            break;
         }

         this.add0(h, i, name, v);
      }

      return this;
   }

   public Headers<T> addObject(T name, Object value) {
      return this.add(name, this.valueConverter.convertObject(ObjectUtil.checkNotNull(value, "value")));
   }

   public Headers<T> addObject(T name, Iterable<?> values) {
      name = this.convertName(name);
      ObjectUtil.checkNotNull(values, "values");
      int h = this.hashCodeGenerator.generateHashCode(name);
      int i = this.index(h);
      Iterator i$ = values.iterator();

      while(i$.hasNext()) {
         Object o = i$.next();
         if (o == null) {
            break;
         }

         T converted = this.valueConverter.convertObject(o);
         ObjectUtil.checkNotNull(converted, "converted");
         this.add0(h, i, name, converted);
      }

      return this;
   }

   public Headers<T> addObject(T name, Object... values) {
      name = this.convertName(name);
      ObjectUtil.checkNotNull(values, "values");
      int h = this.hashCodeGenerator.generateHashCode(name);
      int i = this.index(h);
      Object[] arr$ = values;
      int len$ = values.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         Object o = arr$[i$];
         if (o == null) {
            break;
         }

         T converted = this.valueConverter.convertObject(o);
         ObjectUtil.checkNotNull(converted, "converted");
         this.add0(h, i, name, converted);
      }

      return this;
   }

   public Headers<T> addInt(T name, int value) {
      return this.add(name, this.valueConverter.convertInt(value));
   }

   public Headers<T> addLong(T name, long value) {
      return this.add(name, this.valueConverter.convertLong(value));
   }

   public Headers<T> addDouble(T name, double value) {
      return this.add(name, this.valueConverter.convertDouble(value));
   }

   public Headers<T> addTimeMillis(T name, long value) {
      return this.add(name, this.valueConverter.convertTimeMillis(value));
   }

   public Headers<T> addChar(T name, char value) {
      return this.add(name, this.valueConverter.convertChar(value));
   }

   public Headers<T> addBoolean(T name, boolean value) {
      return this.add(name, this.valueConverter.convertBoolean(value));
   }

   public Headers<T> addFloat(T name, float value) {
      return this.add(name, this.valueConverter.convertFloat(value));
   }

   public Headers<T> addByte(T name, byte value) {
      return this.add(name, this.valueConverter.convertByte(value));
   }

   public Headers<T> addShort(T name, short value) {
      return this.add(name, this.valueConverter.convertShort(value));
   }

   public Headers<T> add(Headers<T> headers) {
      ObjectUtil.checkNotNull(headers, "headers");
      this.add0(headers);
      return this;
   }

   public Headers<T> set(T name, T value) {
      name = this.convertName(name);
      ObjectUtil.checkNotNull(value, "value");
      int h = this.hashCodeGenerator.generateHashCode(name);
      int i = this.index(h);
      this.remove0(h, i, name);
      this.add0(h, i, name, value);
      return this;
   }

   public Headers<T> set(T name, Iterable<? extends T> values) {
      name = this.convertName(name);
      ObjectUtil.checkNotNull(values, "values");
      int h = this.hashCodeGenerator.generateHashCode(name);
      int i = this.index(h);
      this.remove0(h, i, name);
      Iterator i$ = values.iterator();

      while(i$.hasNext()) {
         T v = i$.next();
         if (v == null) {
            break;
         }

         this.add0(h, i, name, v);
      }

      return this;
   }

   public Headers<T> set(T name, T... values) {
      name = this.convertName(name);
      ObjectUtil.checkNotNull(values, "values");
      int h = this.hashCodeGenerator.generateHashCode(name);
      int i = this.index(h);
      this.remove0(h, i, name);
      Object[] arr$ = values;
      int len$ = values.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         T v = arr$[i$];
         if (v == null) {
            break;
         }

         this.add0(h, i, name, v);
      }

      return this;
   }

   public Headers<T> setObject(T name, Object value) {
      return this.set(name, this.valueConverter.convertObject(ObjectUtil.checkNotNull(value, "value")));
   }

   public Headers<T> setObject(T name, Iterable<?> values) {
      name = this.convertName(name);
      ObjectUtil.checkNotNull(values, "values");
      int h = this.hashCodeGenerator.generateHashCode(name);
      int i = this.index(h);
      this.remove0(h, i, name);
      Iterator i$ = values.iterator();

      while(i$.hasNext()) {
         Object o = i$.next();
         if (o == null) {
            break;
         }

         T converted = this.valueConverter.convertObject(o);
         ObjectUtil.checkNotNull(converted, "converted");
         this.add0(h, i, name, converted);
      }

      return this;
   }

   public Headers<T> setObject(T name, Object... values) {
      name = this.convertName(name);
      ObjectUtil.checkNotNull(values, "values");
      int h = this.hashCodeGenerator.generateHashCode(name);
      int i = this.index(h);
      this.remove0(h, i, name);
      Object[] arr$ = values;
      int len$ = values.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         Object o = arr$[i$];
         if (o == null) {
            break;
         }

         T converted = this.valueConverter.convertObject(o);
         ObjectUtil.checkNotNull(converted, "converted");
         this.add0(h, i, name, converted);
      }

      return this;
   }

   public Headers<T> setInt(T name, int value) {
      return this.set(name, this.valueConverter.convertInt(value));
   }

   public Headers<T> setLong(T name, long value) {
      return this.set(name, this.valueConverter.convertLong(value));
   }

   public Headers<T> setDouble(T name, double value) {
      return this.set(name, this.valueConverter.convertDouble(value));
   }

   public Headers<T> setTimeMillis(T name, long value) {
      return this.set(name, this.valueConverter.convertTimeMillis(value));
   }

   public Headers<T> setFloat(T name, float value) {
      return this.set(name, this.valueConverter.convertFloat(value));
   }

   public Headers<T> setChar(T name, char value) {
      return this.set(name, this.valueConverter.convertChar(value));
   }

   public Headers<T> setBoolean(T name, boolean value) {
      return this.set(name, this.valueConverter.convertBoolean(value));
   }

   public Headers<T> setByte(T name, byte value) {
      return this.set(name, this.valueConverter.convertByte(value));
   }

   public Headers<T> setShort(T name, short value) {
      return this.set(name, this.valueConverter.convertShort(value));
   }

   public Headers<T> set(Headers<T> headers) {
      ObjectUtil.checkNotNull(headers, "headers");
      this.clear();
      this.add0(headers);
      return this;
   }

   public Headers<T> setAll(Headers<T> headers) {
      ObjectUtil.checkNotNull(headers, "headers");
      if (headers instanceof DefaultHeaders) {
         DefaultHeaders<T> m = (DefaultHeaders)headers;

         for(DefaultHeaders.HeaderEntry e = m.head.after; e != m.head; e = e.after) {
            this.set(e.name, e.value);
         }
      } else {
         try {
            headers.forEachEntry(this.setAllVisitor());
         } catch (Exception var4) {
            PlatformDependent.throwException(var4);
         }
      }

      return this;
   }

   public boolean remove(T name) {
      ObjectUtil.checkNotNull(name, "name");
      int h = this.hashCodeGenerator.generateHashCode(name);
      int i = this.index(h);
      return this.remove0(h, i, name);
   }

   public Headers<T> clear() {
      this.entries.clear();
      this.tailEntries.clear();
      this.head.before = this.head.after = this.head;
      this.size = 0;
      return this;
   }

   public Iterator<Entry<T, T>> iterator() {
      return new DefaultHeaders.KeyValueHeaderIterator();
   }

   public Entry<T, T> forEachEntry(Headers.EntryVisitor<T> visitor) throws Exception {
      for(DefaultHeaders.HeaderEntry e = this.head.after; e != this.head; e = e.after) {
         if (!visitor.visit(e)) {
            return e;
         }
      }

      return null;
   }

   public T forEachName(Headers.NameVisitor<T> visitor) throws Exception {
      for(DefaultHeaders.HeaderEntry e = this.head.after; e != this.head; e = e.after) {
         if (!visitor.visit(e.name)) {
            return e.name;
         }
      }

      return null;
   }

   public Boolean getBoolean(T name) {
      T v = this.get(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToBoolean(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public boolean getBoolean(T name, boolean defaultValue) {
      Boolean v = this.getBoolean(name);
      return v == null ? defaultValue : v;
   }

   public Byte getByte(T name) {
      T v = this.get(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToByte(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public byte getByte(T name, byte defaultValue) {
      Byte v = this.getByte(name);
      return v == null ? defaultValue : v;
   }

   public Character getChar(T name) {
      T v = this.get(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToChar(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public char getChar(T name, char defaultValue) {
      Character v = this.getChar(name);
      return v == null ? defaultValue : v;
   }

   public Short getShort(T name) {
      T v = this.get(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToShort(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public short getInt(T name, short defaultValue) {
      Short v = this.getShort(name);
      return v == null ? defaultValue : v;
   }

   public Integer getInt(T name) {
      T v = this.get(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToInt(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public int getInt(T name, int defaultValue) {
      Integer v = this.getInt(name);
      return v == null ? defaultValue : v;
   }

   public Long getLong(T name) {
      T v = this.get(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToLong(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public long getLong(T name, long defaultValue) {
      Long v = this.getLong(name);
      return v == null ? defaultValue : v;
   }

   public Float getFloat(T name) {
      T v = this.get(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToFloat(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public float getFloat(T name, float defaultValue) {
      Float v = this.getFloat(name);
      return v == null ? defaultValue : v;
   }

   public Double getDouble(T name) {
      T v = this.get(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToDouble(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public double getDouble(T name, double defaultValue) {
      Double v = this.getDouble(name);
      return v == null ? defaultValue : v;
   }

   public Long getTimeMillis(T name) {
      T v = this.get(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToTimeMillis(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public long getTimeMillis(T name, long defaultValue) {
      Long v = this.getTimeMillis(name);
      return v == null ? defaultValue : v;
   }

   public Boolean getBooleanAndRemove(T name) {
      T v = this.getAndRemove(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToBoolean(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public boolean getBooleanAndRemove(T name, boolean defaultValue) {
      Boolean v = this.getBooleanAndRemove(name);
      return v == null ? defaultValue : v;
   }

   public Byte getByteAndRemove(T name) {
      T v = this.getAndRemove(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToByte(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public byte getByteAndRemove(T name, byte defaultValue) {
      Byte v = this.getByteAndRemove(name);
      return v == null ? defaultValue : v;
   }

   public Character getCharAndRemove(T name) {
      T v = this.getAndRemove(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToChar(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public char getCharAndRemove(T name, char defaultValue) {
      Character v = this.getCharAndRemove(name);
      return v == null ? defaultValue : v;
   }

   public Short getShortAndRemove(T name) {
      T v = this.getAndRemove(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToShort(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public short getShortAndRemove(T name, short defaultValue) {
      Short v = this.getShortAndRemove(name);
      return v == null ? defaultValue : v;
   }

   public Integer getIntAndRemove(T name) {
      T v = this.getAndRemove(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToInt(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public int getIntAndRemove(T name, int defaultValue) {
      Integer v = this.getIntAndRemove(name);
      return v == null ? defaultValue : v;
   }

   public Long getLongAndRemove(T name) {
      T v = this.getAndRemove(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToLong(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public long getLongAndRemove(T name, long defaultValue) {
      Long v = this.getLongAndRemove(name);
      return v == null ? defaultValue : v;
   }

   public Float getFloatAndRemove(T name) {
      T v = this.getAndRemove(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToFloat(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public float getFloatAndRemove(T name, float defaultValue) {
      Float v = this.getFloatAndRemove(name);
      return v == null ? defaultValue : v;
   }

   public Double getDoubleAndRemove(T name) {
      T v = this.getAndRemove(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToDouble(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public double getDoubleAndRemove(T name, double defaultValue) {
      Double v = this.getDoubleAndRemove(name);
      return v == null ? defaultValue : v;
   }

   public Long getTimeMillisAndRemove(T name) {
      T v = this.getAndRemove(name);
      if (v == null) {
         return null;
      } else {
         try {
            return this.valueConverter.convertToTimeMillis(v);
         } catch (Throwable var4) {
            return null;
         }
      }
   }

   public long getTimeMillisAndRemove(T name, long defaultValue) {
      Long v = this.getTimeMillisAndRemove(name);
      return v == null ? defaultValue : v;
   }

   public boolean equals(Object o) {
      if (!(o instanceof DefaultHeaders)) {
         return false;
      } else {
         DefaultHeaders<T> h2 = (DefaultHeaders)o;
         List<T> namesList = this.namesList();
         List<T> otherNamesList = h2.namesList();
         if (!equals(namesList, otherNamesList, this.keyComparator)) {
            return false;
         } else {
            Set<T> names = new TreeSet(this.keyComparator);
            names.addAll(namesList);
            Iterator i$ = names.iterator();

            Object name;
            do {
               if (!i$.hasNext()) {
                  return true;
               }

               name = i$.next();
            } while(equals(this.getAll(name), h2.getAll(name), this.valueComparator));

            return false;
         }
      }
   }

   private static <T> boolean equals(List<T> lhs, List<T> rhs, Comparator<? super T> comparator) {
      int lhsSize = lhs.size();
      if (lhsSize != rhs.size()) {
         return false;
      } else {
         Collections.sort(lhs, comparator);
         Collections.sort(rhs, comparator);

         for(int i = 0; i < lhsSize; ++i) {
            if (comparator.compare(lhs.get(i), rhs.get(i)) != 0) {
               return false;
            }
         }

         return true;
      }
   }

   public int hashCode() {
      int result = 1;
      Iterator i$ = this.names().iterator();

      while(i$.hasNext()) {
         T name = i$.next();
         result = 31 * result + name.hashCode();
         List<T> values = this.getAll(name);
         Collections.sort(values, this.valueComparator);

         for(int i = 0; i < values.size(); ++i) {
            result = 31 * result + this.hashCodeGenerator.generateHashCode(values.get(i));
         }
      }

      return result;
   }

   public String toString() {
      StringBuilder builder = (new StringBuilder(this.getClass().getSimpleName())).append('[');
      Iterator i$ = this.names().iterator();

      while(i$.hasNext()) {
         T name = i$.next();
         List<T> values = this.getAll(name);
         Collections.sort(values, this.valueComparator);

         for(int i = 0; i < values.size(); ++i) {
            builder.append(name).append(": ").append(values.get(i)).append(", ");
         }
      }

      if (builder.length() > 2) {
         builder.setLength(builder.length() - 2);
      }

      return builder.append(']').toString();
   }

   protected Headers.ValueConverter<T> valueConverter() {
      return this.valueConverter;
   }

   private T convertName(T name) {
      return this.nameConverter.convertName(ObjectUtil.checkNotNull(name, "name"));
   }

   private int index(int hash) {
      return Math.abs(hash % this.bucketSize);
   }

   private void add0(Headers<T> headers) {
      if (!headers.isEmpty()) {
         if (headers instanceof DefaultHeaders) {
            DefaultHeaders<T> m = (DefaultHeaders)headers;

            for(DefaultHeaders.HeaderEntry e = m.head.after; e != m.head; e = e.after) {
               this.add(e.name, e.value);
            }
         } else {
            try {
               headers.forEachEntry(this.addAllVisitor());
            } catch (Exception var4) {
               PlatformDependent.throwException(var4);
            }
         }

      }
   }

   private void add0(int h, int i, T name, T value) {
      DefaultHeaders<T>.HeaderEntry newEntry = new DefaultHeaders.HeaderEntry(h, name, value);
      DefaultHeaders<T>.HeaderEntry oldTail = (DefaultHeaders.HeaderEntry)this.tailEntries.get(i);
      if (oldTail == null) {
         this.entries.put(i, newEntry);
      } else {
         oldTail.next = newEntry;
      }

      this.tailEntries.put(i, newEntry);
      newEntry.addBefore(this.head);
   }

   private boolean remove0(int h, int i, T name) {
      DefaultHeaders<T>.HeaderEntry e = (DefaultHeaders.HeaderEntry)this.entries.get(i);
      if (e == null) {
         return false;
      } else {
         boolean removed;
         DefaultHeaders.HeaderEntry next;
         for(removed = false; e.hash == h && this.keyComparator.compare(e.name, name) == 0; removed = true) {
            e.remove();
            next = e.next;
            if (next == null) {
               this.entries.remove(i);
               this.tailEntries.remove(i);
               return true;
            }

            this.entries.put(i, next);
            e = next;
         }

         while(true) {
            while(true) {
               next = e.next;
               if (next == null) {
                  return removed;
               }

               if (next.hash == h && this.keyComparator.compare(next.name, name) == 0) {
                  e.next = next.next;
                  if (e.next == null) {
                     this.tailEntries.put(i, e);
                  }

                  next.remove();
                  removed = true;
               } else {
                  e = next;
               }
            }
         }
      }
   }

   private Headers.EntryVisitor<T> setAllVisitor() {
      return new Headers.EntryVisitor<T>() {
         public boolean visit(Entry<T, T> entry) {
            DefaultHeaders.this.set(entry.getKey(), entry.getValue());
            return true;
         }
      };
   }

   private Headers.EntryVisitor<T> addAllVisitor() {
      return new Headers.EntryVisitor<T>() {
         public boolean visit(Entry<T, T> entry) {
            DefaultHeaders.this.add(entry.getKey(), entry.getValue());
            return true;
         }
      };
   }

   static final class HeaderDateFormat {
      private static final ParsePosition parsePos = new ParsePosition(0);
      private static final FastThreadLocal<DefaultHeaders.HeaderDateFormat> dateFormatThreadLocal = new FastThreadLocal<DefaultHeaders.HeaderDateFormat>() {
         protected DefaultHeaders.HeaderDateFormat initialValue() {
            return new DefaultHeaders.HeaderDateFormat();
         }
      };
      private final DateFormat dateFormat1;
      private final DateFormat dateFormat2;
      private final DateFormat dateFormat3;

      static DefaultHeaders.HeaderDateFormat get() {
         return (DefaultHeaders.HeaderDateFormat)dateFormatThreadLocal.get();
      }

      private HeaderDateFormat() {
         this.dateFormat1 = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
         this.dateFormat2 = new SimpleDateFormat("E, dd-MMM-yy HH:mm:ss z", Locale.ENGLISH);
         this.dateFormat3 = new SimpleDateFormat("E MMM d HH:mm:ss yyyy", Locale.ENGLISH);
         TimeZone tz = TimeZone.getTimeZone("GMT");
         this.dateFormat1.setTimeZone(tz);
         this.dateFormat2.setTimeZone(tz);
         this.dateFormat3.setTimeZone(tz);
      }

      long parse(String text) throws ParseException {
         Date date = this.dateFormat1.parse(text, parsePos);
         if (date == null) {
            date = this.dateFormat2.parse(text, parsePos);
         }

         if (date == null) {
            date = this.dateFormat3.parse(text, parsePos);
         }

         if (date == null) {
            throw new ParseException(text, 0);
         } else {
            return date.getTime();
         }
      }

      long parse(String text, long defaultValue) {
         Date date = this.dateFormat1.parse(text, parsePos);
         if (date == null) {
            date = this.dateFormat2.parse(text, parsePos);
         }

         if (date == null) {
            date = this.dateFormat3.parse(text, parsePos);
         }

         return date == null ? defaultValue : date.getTime();
      }

      // $FF: synthetic method
      HeaderDateFormat(Object x0) {
         this();
      }
   }

   protected final class KeyValueHeaderIterator implements Iterator<Entry<T, T>> {
      private DefaultHeaders<T>.HeaderEntry current;

      protected KeyValueHeaderIterator() {
         this.current = DefaultHeaders.this.head;
      }

      public boolean hasNext() {
         return this.current.after != DefaultHeaders.this.head;
      }

      public Entry<T, T> next() {
         this.current = this.current.after;
         if (this.current == DefaultHeaders.this.head) {
            throw new NoSuchElementException();
         } else {
            return this.current;
         }
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }
   }

   private final class HeaderEntry implements Entry<T, T> {
      final int hash;
      final T name;
      T value;
      DefaultHeaders<T>.HeaderEntry next;
      DefaultHeaders<T>.HeaderEntry before;
      DefaultHeaders<T>.HeaderEntry after;

      HeaderEntry(int hash, T name, T value) {
         this.hash = hash;
         this.name = name;
         this.value = value;
      }

      HeaderEntry() {
         this.hash = -1;
         this.name = null;
         this.value = null;
      }

      void remove() {
         this.before.after = this.after;
         this.after.before = this.before;
         --DefaultHeaders.this.size;
      }

      void addBefore(DefaultHeaders<T>.HeaderEntry e) {
         this.after = e;
         this.before = e.before;
         this.before.after = this;
         this.after.before = this;
         ++DefaultHeaders.this.size;
      }

      public T getKey() {
         return this.name;
      }

      public T getValue() {
         return this.value;
      }

      public T setValue(T value) {
         ObjectUtil.checkNotNull(value, "value");
         T oldValue = this.value;
         this.value = value;
         return oldValue;
      }

      public String toString() {
         return "" + this.name + '=' + this.value;
      }
   }

   public static final class IdentityNameConverter<T> implements DefaultHeaders.NameConverter<T> {
      public T convertName(T name) {
         return name;
      }
   }

   public interface NameConverter<T> {
      T convertName(T var1);
   }

   public interface HashCodeGenerator<T> {
      int generateHashCode(T var1);
   }
}
