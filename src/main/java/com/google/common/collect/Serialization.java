package com.google.common.collect;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

final class Serialization {
   private Serialization() {
   }

   static int readCount(ObjectInputStream stream) throws IOException {
      return stream.readInt();
   }

   static <K, V> void writeMap(Map<K, V> map, ObjectOutputStream stream) throws IOException {
      stream.writeInt(map.size());
      Iterator i$ = map.entrySet().iterator();

      while(i$.hasNext()) {
         Entry<K, V> entry = (Entry)i$.next();
         stream.writeObject(entry.getKey());
         stream.writeObject(entry.getValue());
      }

   }

   static <K, V> void populateMap(Map<K, V> map, ObjectInputStream stream) throws IOException, ClassNotFoundException {
      int size = stream.readInt();
      populateMap(map, stream, size);
   }

   static <K, V> void populateMap(Map<K, V> map, ObjectInputStream stream, int size) throws IOException, ClassNotFoundException {
      for(int i = 0; i < size; ++i) {
         K key = stream.readObject();
         V value = stream.readObject();
         map.put(key, value);
      }

   }

   static <E> void writeMultiset(Multiset<E> multiset, ObjectOutputStream stream) throws IOException {
      int entryCount = multiset.entrySet().size();
      stream.writeInt(entryCount);
      Iterator i$ = multiset.entrySet().iterator();

      while(i$.hasNext()) {
         Multiset.Entry<E> entry = (Multiset.Entry)i$.next();
         stream.writeObject(entry.getElement());
         stream.writeInt(entry.getCount());
      }

   }

   static <E> void populateMultiset(Multiset<E> multiset, ObjectInputStream stream) throws IOException, ClassNotFoundException {
      int distinctElements = stream.readInt();
      populateMultiset(multiset, stream, distinctElements);
   }

   static <E> void populateMultiset(Multiset<E> multiset, ObjectInputStream stream, int distinctElements) throws IOException, ClassNotFoundException {
      for(int i = 0; i < distinctElements; ++i) {
         E element = stream.readObject();
         int count = stream.readInt();
         multiset.add(element, count);
      }

   }

   static <K, V> void writeMultimap(Multimap<K, V> multimap, ObjectOutputStream stream) throws IOException {
      stream.writeInt(multimap.asMap().size());
      Iterator i$ = multimap.asMap().entrySet().iterator();

      while(i$.hasNext()) {
         Entry<K, Collection<V>> entry = (Entry)i$.next();
         stream.writeObject(entry.getKey());
         stream.writeInt(((Collection)entry.getValue()).size());
         Iterator i$ = ((Collection)entry.getValue()).iterator();

         while(i$.hasNext()) {
            V value = i$.next();
            stream.writeObject(value);
         }
      }

   }

   static <K, V> void populateMultimap(Multimap<K, V> multimap, ObjectInputStream stream) throws IOException, ClassNotFoundException {
      int distinctKeys = stream.readInt();
      populateMultimap(multimap, stream, distinctKeys);
   }

   static <K, V> void populateMultimap(Multimap<K, V> multimap, ObjectInputStream stream, int distinctKeys) throws IOException, ClassNotFoundException {
      for(int i = 0; i < distinctKeys; ++i) {
         K key = stream.readObject();
         Collection<V> values = multimap.get(key);
         int valueCount = stream.readInt();

         for(int j = 0; j < valueCount; ++j) {
            V value = stream.readObject();
            values.add(value);
         }
      }

   }

   static <T> Serialization.FieldSetter<T> getFieldSetter(Class<T> clazz, String fieldName) {
      try {
         Field field = clazz.getDeclaredField(fieldName);
         return new Serialization.FieldSetter(field);
      } catch (NoSuchFieldException var3) {
         throw new AssertionError(var3);
      }
   }

   static final class FieldSetter<T> {
      private final Field field;

      private FieldSetter(Field field) {
         this.field = field;
         field.setAccessible(true);
      }

      void set(T instance, Object value) {
         try {
            this.field.set(instance, value);
         } catch (IllegalAccessException var4) {
            throw new AssertionError(var4);
         }
      }

      void set(T instance, int value) {
         try {
            this.field.set(instance, value);
         } catch (IllegalAccessException var4) {
            throw new AssertionError(var4);
         }
      }

      // $FF: synthetic method
      FieldSetter(Field x0, Object x1) {
         this(x0);
      }
   }
}
