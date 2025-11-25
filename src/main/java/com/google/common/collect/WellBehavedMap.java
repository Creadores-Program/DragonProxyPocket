package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

@GwtCompatible
final class WellBehavedMap<K, V> extends ForwardingMap<K, V> {
   private final Map<K, V> delegate;
   private Set<Entry<K, V>> entrySet;

   private WellBehavedMap(Map<K, V> delegate) {
      this.delegate = delegate;
   }

   static <K, V> WellBehavedMap<K, V> wrap(Map<K, V> delegate) {
      return new WellBehavedMap(delegate);
   }

   protected Map<K, V> delegate() {
      return this.delegate;
   }

   public Set<Entry<K, V>> entrySet() {
      Set<Entry<K, V>> es = this.entrySet;
      return es != null ? es : (this.entrySet = new WellBehavedMap.EntrySet());
   }

   private final class EntrySet extends Maps.EntrySet<K, V> {
      private EntrySet() {
      }

      Map<K, V> map() {
         return WellBehavedMap.this;
      }

      public Iterator<Entry<K, V>> iterator() {
         return new TransformedIterator<K, Entry<K, V>>(WellBehavedMap.this.keySet().iterator()) {
            Entry<K, V> transform(final K key) {
               return new AbstractMapEntry<K, V>() {
                  public K getKey() {
                     return key;
                  }

                  public V getValue() {
                     return WellBehavedMap.this.get(key);
                  }

                  public V setValue(V value) {
                     return WellBehavedMap.this.put(key, value);
                  }
               };
            }
         };
      }

      // $FF: synthetic method
      EntrySet(Object x1) {
         this();
      }
   }
}
