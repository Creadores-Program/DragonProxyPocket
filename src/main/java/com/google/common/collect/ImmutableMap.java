package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible(
   serializable = true,
   emulated = true
)
public abstract class ImmutableMap<K, V> implements Map<K, V>, Serializable {
   static final Entry<?, ?>[] EMPTY_ENTRY_ARRAY = new Entry[0];
   private transient ImmutableSet<Entry<K, V>> entrySet;
   private transient ImmutableSet<K> keySet;
   private transient ImmutableCollection<V> values;
   private transient ImmutableSetMultimap<K, V> multimapView;

   public static <K, V> ImmutableMap<K, V> of() {
      return ImmutableBiMap.of();
   }

   public static <K, V> ImmutableMap<K, V> of(K k1, V v1) {
      return ImmutableBiMap.of(k1, v1);
   }

   public static <K, V> ImmutableMap<K, V> of(K k1, V v1, K k2, V v2) {
      return RegularImmutableMap.fromEntries(entryOf(k1, v1), entryOf(k2, v2));
   }

   public static <K, V> ImmutableMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
      return RegularImmutableMap.fromEntries(entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3));
   }

   public static <K, V> ImmutableMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
      return RegularImmutableMap.fromEntries(entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3), entryOf(k4, v4));
   }

   public static <K, V> ImmutableMap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
      return RegularImmutableMap.fromEntries(entryOf(k1, v1), entryOf(k2, v2), entryOf(k3, v3), entryOf(k4, v4), entryOf(k5, v5));
   }

   static <K, V> ImmutableMapEntry<K, V> entryOf(K key, V value) {
      return new ImmutableMapEntry(key, value);
   }

   public static <K, V> ImmutableMap.Builder<K, V> builder() {
      return new ImmutableMap.Builder();
   }

   static void checkNoConflict(boolean safe, String conflictDescription, Entry<?, ?> entry1, Entry<?, ?> entry2) {
      if (!safe) {
         throw new IllegalArgumentException("Multiple entries with same " + conflictDescription + ": " + entry1 + " and " + entry2);
      }
   }

   public static <K, V> ImmutableMap<K, V> copyOf(Map<? extends K, ? extends V> map) {
      ImmutableMap kvMap;
      if (map instanceof ImmutableMap && !(map instanceof ImmutableSortedMap)) {
         kvMap = (ImmutableMap)map;
         if (!kvMap.isPartialView()) {
            return kvMap;
         }
      } else if (map instanceof EnumMap) {
         kvMap = copyOfEnumMap((EnumMap)map);
         return kvMap;
      }

      return copyOf((Iterable)map.entrySet());
   }

   @Beta
   public static <K, V> ImmutableMap<K, V> copyOf(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
      Entry<K, V>[] entryArray = (Entry[])((Entry[])Iterables.toArray(entries, (Object[])EMPTY_ENTRY_ARRAY));
      switch(entryArray.length) {
      case 0:
         return of();
      case 1:
         Entry<K, V> onlyEntry = entryArray[0];
         return of(onlyEntry.getKey(), onlyEntry.getValue());
      default:
         return RegularImmutableMap.fromEntries(entryArray);
      }
   }

   private static <K extends Enum<K>, V> ImmutableMap<K, V> copyOfEnumMap(EnumMap<K, ? extends V> original) {
      EnumMap<K, V> copy = new EnumMap(original);
      Iterator i$ = copy.entrySet().iterator();

      while(i$.hasNext()) {
         Entry<?, ?> entry = (Entry)i$.next();
         CollectPreconditions.checkEntryNotNull(entry.getKey(), entry.getValue());
      }

      return ImmutableEnumMap.asImmutable(copy);
   }

   ImmutableMap() {
   }

   /** @deprecated */
   @Deprecated
   public final V put(K k, V v) {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   public final V remove(Object o) {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   public final void putAll(Map<? extends K, ? extends V> map) {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   public final void clear() {
      throw new UnsupportedOperationException();
   }

   public boolean isEmpty() {
      return this.size() == 0;
   }

   public boolean containsKey(@Nullable Object key) {
      return this.get(key) != null;
   }

   public boolean containsValue(@Nullable Object value) {
      return this.values().contains(value);
   }

   public abstract V get(@Nullable Object var1);

   public ImmutableSet<Entry<K, V>> entrySet() {
      ImmutableSet<Entry<K, V>> result = this.entrySet;
      return result == null ? (this.entrySet = this.createEntrySet()) : result;
   }

   abstract ImmutableSet<Entry<K, V>> createEntrySet();

   public ImmutableSet<K> keySet() {
      ImmutableSet<K> result = this.keySet;
      return result == null ? (this.keySet = this.createKeySet()) : result;
   }

   ImmutableSet<K> createKeySet() {
      return (ImmutableSet)(this.isEmpty() ? ImmutableSet.of() : new ImmutableMapKeySet(this));
   }

   UnmodifiableIterator<K> keyIterator() {
      final UnmodifiableIterator<Entry<K, V>> entryIterator = this.entrySet().iterator();
      return new UnmodifiableIterator<K>() {
         public boolean hasNext() {
            return entryIterator.hasNext();
         }

         public K next() {
            return ((Entry)entryIterator.next()).getKey();
         }
      };
   }

   public ImmutableCollection<V> values() {
      ImmutableCollection<V> result = this.values;
      return result == null ? (this.values = new ImmutableMapValues(this)) : result;
   }

   @Beta
   public ImmutableSetMultimap<K, V> asMultimap() {
      if (this.isEmpty()) {
         return ImmutableSetMultimap.of();
      } else {
         ImmutableSetMultimap<K, V> result = this.multimapView;
         return result == null ? (this.multimapView = new ImmutableSetMultimap(new ImmutableMap.MapViewOfValuesAsSingletonSets(), this.size(), (Comparator)null)) : result;
      }
   }

   public boolean equals(@Nullable Object object) {
      return Maps.equalsImpl(this, object);
   }

   abstract boolean isPartialView();

   public int hashCode() {
      return Sets.hashCodeImpl(this.entrySet());
   }

   boolean isHashCodeFast() {
      return false;
   }

   public String toString() {
      return Maps.toStringImpl(this);
   }

   Object writeReplace() {
      return new ImmutableMap.SerializedForm(this);
   }

   static class SerializedForm implements Serializable {
      private final Object[] keys;
      private final Object[] values;
      private static final long serialVersionUID = 0L;

      SerializedForm(ImmutableMap<?, ?> map) {
         this.keys = new Object[map.size()];
         this.values = new Object[map.size()];
         int i = 0;

         for(Iterator i$ = map.entrySet().iterator(); i$.hasNext(); ++i) {
            Entry<?, ?> entry = (Entry)i$.next();
            this.keys[i] = entry.getKey();
            this.values[i] = entry.getValue();
         }

      }

      Object readResolve() {
         ImmutableMap.Builder<Object, Object> builder = new ImmutableMap.Builder(this.keys.length);
         return this.createMap(builder);
      }

      Object createMap(ImmutableMap.Builder<Object, Object> builder) {
         for(int i = 0; i < this.keys.length; ++i) {
            builder.put(this.keys[i], this.values[i]);
         }

         return builder.build();
      }
   }

   private final class MapViewOfValuesAsSingletonSets extends ImmutableMap.IteratorBasedImmutableMap<K, ImmutableSet<V>> {
      private MapViewOfValuesAsSingletonSets() {
      }

      public int size() {
         return ImmutableMap.this.size();
      }

      public ImmutableSet<K> keySet() {
         return ImmutableMap.this.keySet();
      }

      public boolean containsKey(@Nullable Object key) {
         return ImmutableMap.this.containsKey(key);
      }

      public ImmutableSet<V> get(@Nullable Object key) {
         V outerValue = ImmutableMap.this.get(key);
         return outerValue == null ? null : ImmutableSet.of(outerValue);
      }

      boolean isPartialView() {
         return ImmutableMap.this.isPartialView();
      }

      public int hashCode() {
         return ImmutableMap.this.hashCode();
      }

      boolean isHashCodeFast() {
         return ImmutableMap.this.isHashCodeFast();
      }

      UnmodifiableIterator<Entry<K, ImmutableSet<V>>> entryIterator() {
         final Iterator<Entry<K, V>> backingIterator = ImmutableMap.this.entrySet().iterator();
         return new UnmodifiableIterator<Entry<K, ImmutableSet<V>>>() {
            public boolean hasNext() {
               return backingIterator.hasNext();
            }

            public Entry<K, ImmutableSet<V>> next() {
               final Entry<K, V> backingEntry = (Entry)backingIterator.next();
               return new AbstractMapEntry<K, ImmutableSet<V>>() {
                  public K getKey() {
                     return backingEntry.getKey();
                  }

                  public ImmutableSet<V> getValue() {
                     return ImmutableSet.of(backingEntry.getValue());
                  }
               };
            }
         };
      }

      // $FF: synthetic method
      MapViewOfValuesAsSingletonSets(Object x1) {
         this();
      }
   }

   abstract static class IteratorBasedImmutableMap<K, V> extends ImmutableMap<K, V> {
      abstract UnmodifiableIterator<Entry<K, V>> entryIterator();

      ImmutableSet<Entry<K, V>> createEntrySet() {
         class EntrySetImpl extends ImmutableMapEntrySet<K, V> {
            ImmutableMap<K, V> map() {
               return IteratorBasedImmutableMap.this;
            }

            public UnmodifiableIterator<Entry<K, V>> iterator() {
               return IteratorBasedImmutableMap.this.entryIterator();
            }
         }

         return new EntrySetImpl();
      }
   }

   public static class Builder<K, V> {
      Comparator<? super V> valueComparator;
      ImmutableMapEntry<K, V>[] entries;
      int size;
      boolean entriesUsed;

      public Builder() {
         this(4);
      }

      Builder(int initialCapacity) {
         this.entries = new ImmutableMapEntry[initialCapacity];
         this.size = 0;
         this.entriesUsed = false;
      }

      private void ensureCapacity(int minCapacity) {
         if (minCapacity > this.entries.length) {
            this.entries = (ImmutableMapEntry[])ObjectArrays.arraysCopyOf(this.entries, ImmutableCollection.Builder.expandedCapacity(this.entries.length, minCapacity));
            this.entriesUsed = false;
         }

      }

      public ImmutableMap.Builder<K, V> put(K key, V value) {
         this.ensureCapacity(this.size + 1);
         ImmutableMapEntry<K, V> entry = ImmutableMap.entryOf(key, value);
         this.entries[this.size++] = entry;
         return this;
      }

      public ImmutableMap.Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
         return this.put(entry.getKey(), entry.getValue());
      }

      public ImmutableMap.Builder<K, V> putAll(Map<? extends K, ? extends V> map) {
         return this.putAll((Iterable)map.entrySet());
      }

      @Beta
      public ImmutableMap.Builder<K, V> putAll(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
         if (entries instanceof Collection) {
            this.ensureCapacity(this.size + ((Collection)entries).size());
         }

         Iterator i$ = entries.iterator();

         while(i$.hasNext()) {
            Entry<? extends K, ? extends V> entry = (Entry)i$.next();
            this.put(entry);
         }

         return this;
      }

      @Beta
      public ImmutableMap.Builder<K, V> orderEntriesByValue(Comparator<? super V> valueComparator) {
         Preconditions.checkState(this.valueComparator == null, "valueComparator was already set");
         this.valueComparator = (Comparator)Preconditions.checkNotNull(valueComparator, "valueComparator");
         return this;
      }

      public ImmutableMap<K, V> build() {
         switch(this.size) {
         case 0:
            return ImmutableMap.of();
         case 1:
            return ImmutableMap.of(this.entries[0].getKey(), this.entries[0].getValue());
         default:
            if (this.valueComparator != null) {
               if (this.entriesUsed) {
                  this.entries = (ImmutableMapEntry[])ObjectArrays.arraysCopyOf(this.entries, this.size);
               }

               Arrays.sort(this.entries, 0, this.size, Ordering.from(this.valueComparator).onResultOf(Maps.valueFunction()));
            }

            this.entriesUsed = this.size == this.entries.length;
            return RegularImmutableMap.fromEntryArray(this.size, this.entries);
         }
      }
   }
}
