package io.netty.util.collection;

import java.util.Collection;

public interface IntObjectMap<V> {
   V get(int var1);

   V put(int var1, V var2);

   void putAll(IntObjectMap<V> var1);

   V remove(int var1);

   int size();

   boolean isEmpty();

   void clear();

   boolean containsKey(int var1);

   boolean containsValue(V var1);

   Iterable<IntObjectMap.Entry<V>> entries();

   int[] keys();

   V[] values(Class<V> var1);

   Collection<V> values();

   public interface Entry<V> {
      int key();

      V value();

      void setValue(V var1);
   }
}
