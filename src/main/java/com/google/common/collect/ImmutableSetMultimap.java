package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.j2objc.annotations.Weak;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible(
   serializable = true,
   emulated = true
)
public class ImmutableSetMultimap<K, V> extends ImmutableMultimap<K, V> implements SetMultimap<K, V> {
   private final transient ImmutableSet<V> emptySet;
   private transient ImmutableSetMultimap<V, K> inverse;
   private transient ImmutableSet<Entry<K, V>> entries;
   @GwtIncompatible("not needed in emulated source.")
   private static final long serialVersionUID = 0L;

   public static <K, V> ImmutableSetMultimap<K, V> of() {
      return EmptyImmutableSetMultimap.INSTANCE;
   }

   public static <K, V> ImmutableSetMultimap<K, V> of(K k1, V v1) {
      ImmutableSetMultimap.Builder<K, V> builder = builder();
      builder.put(k1, v1);
      return builder.build();
   }

   public static <K, V> ImmutableSetMultimap<K, V> of(K k1, V v1, K k2, V v2) {
      ImmutableSetMultimap.Builder<K, V> builder = builder();
      builder.put(k1, v1);
      builder.put(k2, v2);
      return builder.build();
   }

   public static <K, V> ImmutableSetMultimap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
      ImmutableSetMultimap.Builder<K, V> builder = builder();
      builder.put(k1, v1);
      builder.put(k2, v2);
      builder.put(k3, v3);
      return builder.build();
   }

   public static <K, V> ImmutableSetMultimap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
      ImmutableSetMultimap.Builder<K, V> builder = builder();
      builder.put(k1, v1);
      builder.put(k2, v2);
      builder.put(k3, v3);
      builder.put(k4, v4);
      return builder.build();
   }

   public static <K, V> ImmutableSetMultimap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
      ImmutableSetMultimap.Builder<K, V> builder = builder();
      builder.put(k1, v1);
      builder.put(k2, v2);
      builder.put(k3, v3);
      builder.put(k4, v4);
      builder.put(k5, v5);
      return builder.build();
   }

   public static <K, V> ImmutableSetMultimap.Builder<K, V> builder() {
      return new ImmutableSetMultimap.Builder();
   }

   public static <K, V> ImmutableSetMultimap<K, V> copyOf(Multimap<? extends K, ? extends V> multimap) {
      return copyOf(multimap, (Comparator)null);
   }

   private static <K, V> ImmutableSetMultimap<K, V> copyOf(Multimap<? extends K, ? extends V> multimap, Comparator<? super V> valueComparator) {
      Preconditions.checkNotNull(multimap);
      if (multimap.isEmpty() && valueComparator == null) {
         return of();
      } else {
         if (multimap instanceof ImmutableSetMultimap) {
            ImmutableSetMultimap<K, V> kvMultimap = (ImmutableSetMultimap)multimap;
            if (!kvMultimap.isPartialView()) {
               return kvMultimap;
            }
         }

         ImmutableMap.Builder<K, ImmutableSet<V>> builder = new ImmutableMap.Builder(multimap.asMap().size());
         int size = 0;
         Iterator i$ = multimap.asMap().entrySet().iterator();

         while(i$.hasNext()) {
            Entry<? extends K, ? extends Collection<? extends V>> entry = (Entry)i$.next();
            K key = entry.getKey();
            Collection<? extends V> values = (Collection)entry.getValue();
            ImmutableSet<V> set = valueSet(valueComparator, values);
            if (!set.isEmpty()) {
               builder.put(key, set);
               size += set.size();
            }
         }

         return new ImmutableSetMultimap(builder.build(), size, valueComparator);
      }
   }

   @Beta
   public static <K, V> ImmutableSetMultimap<K, V> copyOf(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
      return (new ImmutableSetMultimap.Builder()).putAll(entries).build();
   }

   ImmutableSetMultimap(ImmutableMap<K, ImmutableSet<V>> map, int size, @Nullable Comparator<? super V> valueComparator) {
      super(map, size);
      this.emptySet = emptySet(valueComparator);
   }

   public ImmutableSet<V> get(@Nullable K key) {
      ImmutableSet<V> set = (ImmutableSet)this.map.get(key);
      return (ImmutableSet)MoreObjects.firstNonNull(set, this.emptySet);
   }

   public ImmutableSetMultimap<V, K> inverse() {
      ImmutableSetMultimap<V, K> result = this.inverse;
      return result == null ? (this.inverse = this.invert()) : result;
   }

   private ImmutableSetMultimap<V, K> invert() {
      ImmutableSetMultimap.Builder<V, K> builder = builder();
      Iterator i$ = this.entries().iterator();

      while(i$.hasNext()) {
         Entry<K, V> entry = (Entry)i$.next();
         builder.put(entry.getValue(), entry.getKey());
      }

      ImmutableSetMultimap<V, K> invertedMultimap = builder.build();
      invertedMultimap.inverse = this;
      return invertedMultimap;
   }

   /** @deprecated */
   @Deprecated
   public ImmutableSet<V> removeAll(Object key) {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   public ImmutableSet<V> replaceValues(K key, Iterable<? extends V> values) {
      throw new UnsupportedOperationException();
   }

   public ImmutableSet<Entry<K, V>> entries() {
      ImmutableSet<Entry<K, V>> result = this.entries;
      return result == null ? (this.entries = new ImmutableSetMultimap.EntrySet(this)) : result;
   }

   private static <V> ImmutableSet<V> valueSet(@Nullable Comparator<? super V> valueComparator, Collection<? extends V> values) {
      return (ImmutableSet)(valueComparator == null ? ImmutableSet.copyOf(values) : ImmutableSortedSet.copyOf(valueComparator, values));
   }

   private static <V> ImmutableSet<V> emptySet(@Nullable Comparator<? super V> valueComparator) {
      return (ImmutableSet)(valueComparator == null ? ImmutableSet.of() : ImmutableSortedSet.emptySet(valueComparator));
   }

   private static <V> ImmutableSet.Builder<V> valuesBuilder(@Nullable Comparator<? super V> valueComparator) {
      return (ImmutableSet.Builder)(valueComparator == null ? new ImmutableSet.Builder() : new ImmutableSortedSet.Builder(valueComparator));
   }

   @GwtIncompatible("java.io.ObjectOutputStream")
   private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
      stream.writeObject(this.valueComparator());
      Serialization.writeMultimap(this, stream);
   }

   @Nullable
   Comparator<? super V> valueComparator() {
      return this.emptySet instanceof ImmutableSortedSet ? ((ImmutableSortedSet)this.emptySet).comparator() : null;
   }

   @GwtIncompatible("java.io.ObjectInputStream")
   private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      Comparator<Object> valueComparator = (Comparator)stream.readObject();
      int keyCount = stream.readInt();
      if (keyCount < 0) {
         throw new InvalidObjectException("Invalid key count " + keyCount);
      } else {
         ImmutableMap.Builder<Object, ImmutableSet<Object>> builder = ImmutableMap.builder();
         int tmpSize = 0;

         for(int i = 0; i < keyCount; ++i) {
            Object key = stream.readObject();
            int valueCount = stream.readInt();
            if (valueCount <= 0) {
               throw new InvalidObjectException("Invalid value count " + valueCount);
            }

            ImmutableSet.Builder<Object> valuesBuilder = valuesBuilder(valueComparator);

            for(int j = 0; j < valueCount; ++j) {
               valuesBuilder.add(stream.readObject());
            }

            ImmutableSet<Object> valueSet = valuesBuilder.build();
            if (valueSet.size() != valueCount) {
               throw new InvalidObjectException("Duplicate key-value pairs exist for key " + key);
            }

            builder.put(key, valueSet);
            tmpSize += valueCount;
         }

         ImmutableMap tmpMap;
         try {
            tmpMap = builder.build();
         } catch (IllegalArgumentException var11) {
            throw (InvalidObjectException)(new InvalidObjectException(var11.getMessage())).initCause(var11);
         }

         ImmutableMultimap.FieldSettersHolder.MAP_FIELD_SETTER.set(this, tmpMap);
         ImmutableMultimap.FieldSettersHolder.SIZE_FIELD_SETTER.set(this, tmpSize);
         ImmutableMultimap.FieldSettersHolder.EMPTY_SET_FIELD_SETTER.set(this, emptySet(valueComparator));
      }
   }

   private static final class EntrySet<K, V> extends ImmutableSet<Entry<K, V>> {
      @Weak
      private final transient ImmutableSetMultimap<K, V> multimap;

      EntrySet(ImmutableSetMultimap<K, V> multimap) {
         this.multimap = multimap;
      }

      public boolean contains(@Nullable Object object) {
         if (object instanceof Entry) {
            Entry<?, ?> entry = (Entry)object;
            return this.multimap.containsEntry(entry.getKey(), entry.getValue());
         } else {
            return false;
         }
      }

      public int size() {
         return this.multimap.size();
      }

      public UnmodifiableIterator<Entry<K, V>> iterator() {
         return this.multimap.entryIterator();
      }

      boolean isPartialView() {
         return false;
      }
   }

   public static final class Builder<K, V> extends ImmutableMultimap.Builder<K, V> {
      public Builder() {
         super(MultimapBuilder.linkedHashKeys().linkedHashSetValues().build());
      }

      public ImmutableSetMultimap.Builder<K, V> put(K key, V value) {
         this.builderMultimap.put(Preconditions.checkNotNull(key), Preconditions.checkNotNull(value));
         return this;
      }

      public ImmutableSetMultimap.Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
         this.builderMultimap.put(Preconditions.checkNotNull(entry.getKey()), Preconditions.checkNotNull(entry.getValue()));
         return this;
      }

      @Beta
      public ImmutableSetMultimap.Builder<K, V> putAll(Iterable<? extends Entry<? extends K, ? extends V>> entries) {
         super.putAll(entries);
         return this;
      }

      public ImmutableSetMultimap.Builder<K, V> putAll(K key, Iterable<? extends V> values) {
         Collection<V> collection = this.builderMultimap.get(Preconditions.checkNotNull(key));
         Iterator i$ = values.iterator();

         while(i$.hasNext()) {
            V value = i$.next();
            collection.add(Preconditions.checkNotNull(value));
         }

         return this;
      }

      public ImmutableSetMultimap.Builder<K, V> putAll(K key, V... values) {
         return this.putAll(key, (Iterable)Arrays.asList(values));
      }

      public ImmutableSetMultimap.Builder<K, V> putAll(Multimap<? extends K, ? extends V> multimap) {
         Iterator i$ = multimap.asMap().entrySet().iterator();

         while(i$.hasNext()) {
            Entry<? extends K, ? extends Collection<? extends V>> entry = (Entry)i$.next();
            this.putAll(entry.getKey(), (Iterable)entry.getValue());
         }

         return this;
      }

      public ImmutableSetMultimap.Builder<K, V> orderKeysBy(Comparator<? super K> keyComparator) {
         this.keyComparator = (Comparator)Preconditions.checkNotNull(keyComparator);
         return this;
      }

      public ImmutableSetMultimap.Builder<K, V> orderValuesBy(Comparator<? super V> valueComparator) {
         super.orderValuesBy(valueComparator);
         return this;
      }

      public ImmutableSetMultimap<K, V> build() {
         if (this.keyComparator != null) {
            Multimap<K, V> sortedCopy = MultimapBuilder.linkedHashKeys().linkedHashSetValues().build();
            List<Entry<K, Collection<V>>> entries = Ordering.from(this.keyComparator).onKeys().immutableSortedCopy(this.builderMultimap.asMap().entrySet());
            Iterator i$ = entries.iterator();

            while(i$.hasNext()) {
               Entry<K, Collection<V>> entry = (Entry)i$.next();
               sortedCopy.putAll(entry.getKey(), (Iterable)entry.getValue());
            }

            this.builderMultimap = sortedCopy;
         }

         return ImmutableSetMultimap.copyOf(this.builderMultimap, this.valueComparator);
      }
   }
}
