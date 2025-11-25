package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import com.google.j2objc.annotations.Weak;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible(
   emulated = true
)
public abstract class ImmutableMultimap<K, V> extends AbstractMultimap<K, V> implements Serializable {
   final transient ImmutableMap<K, ? extends ImmutableCollection<V>> map;
   final transient int size;
   private static final long serialVersionUID = 0L;

   public static <K, V> ImmutableMultimap<K, V> of() {
      return ImmutableListMultimap.of();
   }

   public static <K, V> ImmutableMultimap<K, V> of(K k1, V v1) {
      return ImmutableListMultimap.of(k1, v1);
   }

   public static <K, V> ImmutableMultimap<K, V> of(K k1, V v1, K k2, V v2) {
      return ImmutableListMultimap.of(k1, v1, k2, v2);
   }

   public static <K, V> ImmutableMultimap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
      return ImmutableListMultimap.of(k1, v1, k2, v2, k3, v3);
   }

   public static <K, V> ImmutableMultimap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
      return ImmutableListMultimap.of(k1, v1, k2, v2, k3, v3, k4, v4);
   }

   public static <K, V> ImmutableMultimap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
      return ImmutableListMultimap.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
   }

   public static <K, V> ImmutableMultimap.Builder<K, V> builder() {
      return new ImmutableMultimap.Builder();
   }

   public static <K, V> ImmutableMultimap<K, V> copyOf(Multimap<? extends K, ? extends V> multimap) {
      if (multimap instanceof ImmutableMultimap) {
         ImmutableMultimap<K, V> kvMultimap = (ImmutableMultimap)multimap;
         if (!kvMultimap.isPartialView()) {
            return kvMultimap;
         }
      }

      return ImmutableListMultimap.copyOf(multimap);
   }

   @Beta
   public static <K, V> ImmutableMultimap<K, V> copyOf(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
      return ImmutableListMultimap.copyOf(entries);
   }

   ImmutableMultimap(ImmutableMap<K, ? extends ImmutableCollection<V>> map, int size) {
      this.map = map;
      this.size = size;
   }

   /** @deprecated */
   @Deprecated
   public ImmutableCollection<V> removeAll(Object key) {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   public ImmutableCollection<V> replaceValues(K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   public void clear() {
      throw new UnsupportedOperationException();
   }

   public abstract ImmutableCollection<V> get(K var1);

   public abstract ImmutableMultimap<V, K> inverse();

   /** @deprecated */
   @Deprecated
   public boolean put(K key, V value) {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   public boolean putAll(K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   public boolean remove(Object key, Object value) {
      throw new UnsupportedOperationException();
   }

   boolean isPartialView() {
      return this.map.isPartialView();
   }

   public boolean containsKey(@Nullable Object key) {
      return this.map.containsKey(key);
   }

   public boolean containsValue(@Nullable Object value) {
      return value != null && super.containsValue(value);
   }

   public int size() {
      return this.size;
   }

   public ImmutableSet<K> keySet() {
      return this.map.keySet();
   }

   public ImmutableMap<K, Collection<V>> asMap() {
      return this.map;
   }

   Map<K, Collection<V>> createAsMap() {
      throw new AssertionError("should never be called");
   }

   public ImmutableCollection<Entry<K, V>> entries() {
      return (ImmutableCollection)super.entries();
   }

   ImmutableCollection<Entry<K, V>> createEntries() {
      return new ImmutableMultimap.EntryCollection(this);
   }

   UnmodifiableIterator<Entry<K, V>> entryIterator() {
      return new ImmutableMultimap<K, V>.Itr<Entry<K, V>>() {
         Entry<K, V> output(K key, V value) {
            return Maps.immutableEntry(key, value);
         }
      };
   }

   public ImmutableMultiset<K> keys() {
      return (ImmutableMultiset)super.keys();
   }

   ImmutableMultiset<K> createKeys() {
      return new ImmutableMultimap.Keys();
   }

   public ImmutableCollection<V> values() {
      return (ImmutableCollection)super.values();
   }

   ImmutableCollection<V> createValues() {
      return new ImmutableMultimap.Values(this);
   }

   UnmodifiableIterator<V> valueIterator() {
      return new ImmutableMultimap<K, V>.Itr<V>() {
         V output(K key, V value) {
            return value;
         }
      };
   }

   private static final class Values<K, V> extends ImmutableCollection<V> {
      @Weak
      private final transient ImmutableMultimap<K, V> multimap;
      private static final long serialVersionUID = 0L;

      Values(ImmutableMultimap<K, V> multimap) {
         this.multimap = multimap;
      }

      public boolean contains(@Nullable Object object) {
         return this.multimap.containsValue(object);
      }

      public UnmodifiableIterator<V> iterator() {
         return this.multimap.valueIterator();
      }

      @GwtIncompatible("not present in emulated superclass")
      int copyIntoArray(Object[] dst, int offset) {
         ImmutableCollection valueCollection;
         for(Iterator i$ = this.multimap.map.values().iterator(); i$.hasNext(); offset = valueCollection.copyIntoArray(dst, offset)) {
            valueCollection = (ImmutableCollection)i$.next();
         }

         return offset;
      }

      public int size() {
         return this.multimap.size();
      }

      boolean isPartialView() {
         return true;
      }
   }

   class Keys extends ImmutableMultiset<K> {
      public boolean contains(@Nullable Object object) {
         return ImmutableMultimap.this.containsKey(object);
      }

      public int count(@Nullable Object element) {
         Collection<V> values = (Collection)ImmutableMultimap.this.map.get(element);
         return values == null ? 0 : values.size();
      }

      public Set<K> elementSet() {
         return ImmutableMultimap.this.keySet();
      }

      public int size() {
         return ImmutableMultimap.this.size();
      }

      Multiset.Entry<K> getEntry(int index) {
         Entry<K, ? extends Collection<V>> entry = (Entry)ImmutableMultimap.this.map.entrySet().asList().get(index);
         return Multisets.immutableEntry(entry.getKey(), ((Collection)entry.getValue()).size());
      }

      boolean isPartialView() {
         return true;
      }
   }

   private abstract class Itr<T> extends UnmodifiableIterator<T> {
      final Iterator<Entry<K, Collection<V>>> mapIterator;
      K key;
      Iterator<V> valueIterator;

      private Itr() {
         this.mapIterator = ImmutableMultimap.this.asMap().entrySet().iterator();
         this.key = null;
         this.valueIterator = Iterators.emptyIterator();
      }

      abstract T output(K var1, V var2);

      public boolean hasNext() {
         return this.mapIterator.hasNext() || this.valueIterator.hasNext();
      }

      public T next() {
         if (!this.valueIterator.hasNext()) {
            Entry<K, Collection<V>> mapEntry = (Entry)this.mapIterator.next();
            this.key = mapEntry.getKey();
            this.valueIterator = ((Collection)mapEntry.getValue()).iterator();
         }

         return this.output(this.key, this.valueIterator.next());
      }

      // $FF: synthetic method
      Itr(Object x1) {
         this();
      }
   }

   private static class EntryCollection<K, V> extends ImmutableCollection<Entry<K, V>> {
      @Weak
      final ImmutableMultimap<K, V> multimap;
      private static final long serialVersionUID = 0L;

      EntryCollection(ImmutableMultimap<K, V> multimap) {
         this.multimap = multimap;
      }

      public UnmodifiableIterator<Entry<K, V>> iterator() {
         return this.multimap.entryIterator();
      }

      boolean isPartialView() {
         return this.multimap.isPartialView();
      }

      public int size() {
         return this.multimap.size();
      }

      public boolean contains(Object object) {
         if (object instanceof Entry) {
            Entry<?, ?> entry = (Entry)object;
            return this.multimap.containsEntry(entry.getKey(), entry.getValue());
         } else {
            return false;
         }
      }
   }

   @GwtIncompatible("java serialization is not supported")
   static class FieldSettersHolder {
      static final Serialization.FieldSetter<ImmutableMultimap> MAP_FIELD_SETTER = Serialization.getFieldSetter(ImmutableMultimap.class, "map");
      static final Serialization.FieldSetter<ImmutableMultimap> SIZE_FIELD_SETTER = Serialization.getFieldSetter(ImmutableMultimap.class, "size");
      static final Serialization.FieldSetter<ImmutableSetMultimap> EMPTY_SET_FIELD_SETTER = Serialization.getFieldSetter(ImmutableSetMultimap.class, "emptySet");
   }

   public static class Builder<K, V> {
      Multimap<K, V> builderMultimap;
      Comparator<? super K> keyComparator;
      Comparator<? super V> valueComparator;

      public Builder() {
         this(MultimapBuilder.linkedHashKeys().arrayListValues().build());
      }

      Builder(Multimap<K, V> builderMultimap) {
         this.builderMultimap = builderMultimap;
      }

      public ImmutableMultimap.Builder<K, V> put(K key, V value) {
         CollectPreconditions.checkEntryNotNull(key, value);
         this.builderMultimap.put(key, value);
         return this;
      }

      public ImmutableMultimap.Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
         return this.put(entry.getKey(), entry.getValue());
      }

      @Beta
      public ImmutableMultimap.Builder<K, V> putAll(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
         Iterator i$ = entries.iterator();

         while(i$.hasNext()) {
            Entry<? extends K, ? extends V> entry = (Entry)i$.next();
            this.put(entry);
         }

         return this;
      }

      public ImmutableMultimap.Builder<K, V> putAll(K key, Iterable<? extends V> values) {
         if (key == null) {
            throw new NullPointerException("null key in entry: null=" + Iterables.toString(values));
         } else {
            Collection<V> valueList = this.builderMultimap.get(key);
            Iterator i$ = values.iterator();

            while(i$.hasNext()) {
               V value = i$.next();
               CollectPreconditions.checkEntryNotNull(key, value);
               valueList.add(value);
            }

            return this;
         }
      }

      public ImmutableMultimap.Builder<K, V> putAll(K key, V... values) {
         return this.putAll(key, (Iterable)Arrays.asList(values));
      }

      public ImmutableMultimap.Builder<K, V> putAll(Multimap<? extends K, ? extends V> multimap) {
         Iterator i$ = multimap.asMap().entrySet().iterator();

         while(i$.hasNext()) {
            Entry<? extends K, ? extends Collection<? extends V>> entry = (Entry)i$.next();
            this.putAll(entry.getKey(), (Iterable)entry.getValue());
         }

         return this;
      }

      public ImmutableMultimap.Builder<K, V> orderKeysBy(Comparator<? super K> keyComparator) {
         this.keyComparator = (Comparator)Preconditions.checkNotNull(keyComparator);
         return this;
      }

      public ImmutableMultimap.Builder<K, V> orderValuesBy(Comparator<? super V> valueComparator) {
         this.valueComparator = (Comparator)Preconditions.checkNotNull(valueComparator);
         return this;
      }

      public ImmutableMultimap<K, V> build() {
         if (this.valueComparator != null) {
            Iterator i$ = this.builderMultimap.asMap().values().iterator();

            while(i$.hasNext()) {
               Collection<V> values = (Collection)i$.next();
               List<V> list = (List)values;
               Collections.sort(list, this.valueComparator);
            }
         }

         if (this.keyComparator != null) {
            Multimap<K, V> sortedCopy = MultimapBuilder.linkedHashKeys().arrayListValues().build();
            List<Entry<K, Collection<V>>> entries = Ordering.from(this.keyComparator).onKeys().immutableSortedCopy(this.builderMultimap.asMap().entrySet());
            Iterator i$ = entries.iterator();

            while(i$.hasNext()) {
               Entry<K, Collection<V>> entry = (Entry)i$.next();
               sortedCopy.putAll(entry.getKey(), (Iterable)entry.getValue());
            }

            this.builderMultimap = sortedCopy;
         }

         return ImmutableMultimap.copyOf(this.builderMultimap);
      }
   }
}
