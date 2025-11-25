package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible
abstract class AbstractMultimap<K, V> implements Multimap<K, V> {
   private transient Collection<Entry<K, V>> entries;
   private transient Set<K> keySet;
   private transient Multiset<K> keys;
   private transient Collection<V> values;
   private transient Map<K, Collection<V>> asMap;

   public boolean isEmpty() {
      return this.size() == 0;
   }

   public boolean containsValue(@Nullable Object value) {
      Iterator i$ = this.asMap().values().iterator();

      Collection collection;
      do {
         if (!i$.hasNext()) {
            return false;
         }

         collection = (Collection)i$.next();
      } while(!collection.contains(value));

      return true;
   }

   public boolean containsEntry(@Nullable Object key, @Nullable Object value) {
      Collection<V> collection = (Collection)this.asMap().get(key);
      return collection != null && collection.contains(value);
   }

   public boolean remove(@Nullable Object key, @Nullable Object value) {
      Collection<V> collection = (Collection)this.asMap().get(key);
      return collection != null && collection.remove(value);
   }

   public boolean put(@Nullable K key, @Nullable V value) {
      return this.get(key).add(value);
   }

   public boolean putAll(@Nullable K key, Iterable<? extends V> values) {
      Preconditions.checkNotNull(values);
      if (values instanceof Collection) {
         Collection<? extends V> valueCollection = (Collection)values;
         return !valueCollection.isEmpty() && this.get(key).addAll(valueCollection);
      } else {
         Iterator<? extends V> valueItr = values.iterator();
         return valueItr.hasNext() && Iterators.addAll(this.get(key), valueItr);
      }
   }

   public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
      boolean changed = false;

      Entry entry;
      for(Iterator i$ = multimap.entries().iterator(); i$.hasNext(); changed |= this.put(entry.getKey(), entry.getValue())) {
         entry = (Entry)i$.next();
      }

      return changed;
   }

   public Collection<V> replaceValues(@Nullable K key, Iterable<? extends V> values) {
      Preconditions.checkNotNull(values);
      Collection<V> result = this.removeAll(key);
      this.putAll(key, values);
      return result;
   }

   public Collection<Entry<K, V>> entries() {
      Collection<Entry<K, V>> result = this.entries;
      return result == null ? (this.entries = this.createEntries()) : result;
   }

   Collection<Entry<K, V>> createEntries() {
      return (Collection)(this instanceof SetMultimap ? new AbstractMultimap.EntrySet() : new AbstractMultimap.Entries());
   }

   abstract Iterator<Entry<K, V>> entryIterator();

   public Set<K> keySet() {
      Set<K> result = this.keySet;
      return result == null ? (this.keySet = this.createKeySet()) : result;
   }

   Set<K> createKeySet() {
      return new Maps.KeySet(this.asMap());
   }

   public Multiset<K> keys() {
      Multiset<K> result = this.keys;
      return result == null ? (this.keys = this.createKeys()) : result;
   }

   Multiset<K> createKeys() {
      return new Multimaps.Keys(this);
   }

   public Collection<V> values() {
      Collection<V> result = this.values;
      return result == null ? (this.values = this.createValues()) : result;
   }

   Collection<V> createValues() {
      return new AbstractMultimap.Values();
   }

   Iterator<V> valueIterator() {
      return Maps.valueIterator(this.entries().iterator());
   }

   public Map<K, Collection<V>> asMap() {
      Map<K, Collection<V>> result = this.asMap;
      return result == null ? (this.asMap = this.createAsMap()) : result;
   }

   abstract Map<K, Collection<V>> createAsMap();

   public boolean equals(@Nullable Object object) {
      return Multimaps.equalsImpl(this, object);
   }

   public int hashCode() {
      return this.asMap().hashCode();
   }

   public String toString() {
      return this.asMap().toString();
   }

   class Values extends AbstractCollection<V> {
      public Iterator<V> iterator() {
         return AbstractMultimap.this.valueIterator();
      }

      public int size() {
         return AbstractMultimap.this.size();
      }

      public boolean contains(@Nullable Object o) {
         return AbstractMultimap.this.containsValue(o);
      }

      public void clear() {
         AbstractMultimap.this.clear();
      }
   }

   private class EntrySet extends AbstractMultimap<K, V>.Entries implements Set<Entry<K, V>> {
      private EntrySet() {
         super(null);
      }

      public int hashCode() {
         return Sets.hashCodeImpl(this);
      }

      public boolean equals(@Nullable Object obj) {
         return Sets.equalsImpl(this, obj);
      }

      // $FF: synthetic method
      EntrySet(Object x1) {
         this();
      }
   }

   private class Entries extends Multimaps.Entries<K, V> {
      private Entries() {
      }

      Multimap<K, V> multimap() {
         return AbstractMultimap.this;
      }

      public Iterator<Entry<K, V>> iterator() {
         return AbstractMultimap.this.entryIterator();
      }

      // $FF: synthetic method
      Entries(Object x1) {
         this();
      }
   }
}
