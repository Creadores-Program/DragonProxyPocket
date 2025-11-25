package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.j2objc.annotations.Weak;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.Map.Entry;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@GwtCompatible(
   emulated = true
)
public final class Multimaps {
   private Multimaps() {
   }

   public static <K, V> Multimap<K, V> newMultimap(Map<K, Collection<V>> map, Supplier<? extends Collection<V>> factory) {
      return new Multimaps.CustomMultimap(map, factory);
   }

   public static <K, V> ListMultimap<K, V> newListMultimap(Map<K, Collection<V>> map, Supplier<? extends List<V>> factory) {
      return new Multimaps.CustomListMultimap(map, factory);
   }

   public static <K, V> SetMultimap<K, V> newSetMultimap(Map<K, Collection<V>> map, Supplier<? extends Set<V>> factory) {
      return new Multimaps.CustomSetMultimap(map, factory);
   }

   public static <K, V> SortedSetMultimap<K, V> newSortedSetMultimap(Map<K, Collection<V>> map, Supplier<? extends SortedSet<V>> factory) {
      return new Multimaps.CustomSortedSetMultimap(map, factory);
   }

   public static <K, V, M extends Multimap<K, V>> M invertFrom(Multimap<? extends V, ? extends K> source, M dest) {
      Preconditions.checkNotNull(dest);
      Iterator i$ = source.entries().iterator();

      while(i$.hasNext()) {
         Entry<? extends V, ? extends K> entry = (Entry)i$.next();
         dest.put(entry.getValue(), entry.getKey());
      }

      return dest;
   }

   public static <K, V> Multimap<K, V> synchronizedMultimap(Multimap<K, V> multimap) {
      return Synchronized.multimap(multimap, (Object)null);
   }

   public static <K, V> Multimap<K, V> unmodifiableMultimap(Multimap<K, V> delegate) {
      return (Multimap)(!(delegate instanceof Multimaps.UnmodifiableMultimap) && !(delegate instanceof ImmutableMultimap) ? new Multimaps.UnmodifiableMultimap(delegate) : delegate);
   }

   /** @deprecated */
   @Deprecated
   public static <K, V> Multimap<K, V> unmodifiableMultimap(ImmutableMultimap<K, V> delegate) {
      return (Multimap)Preconditions.checkNotNull(delegate);
   }

   public static <K, V> SetMultimap<K, V> synchronizedSetMultimap(SetMultimap<K, V> multimap) {
      return Synchronized.setMultimap(multimap, (Object)null);
   }

   public static <K, V> SetMultimap<K, V> unmodifiableSetMultimap(SetMultimap<K, V> delegate) {
      return (SetMultimap)(!(delegate instanceof Multimaps.UnmodifiableSetMultimap) && !(delegate instanceof ImmutableSetMultimap) ? new Multimaps.UnmodifiableSetMultimap(delegate) : delegate);
   }

   /** @deprecated */
   @Deprecated
   public static <K, V> SetMultimap<K, V> unmodifiableSetMultimap(ImmutableSetMultimap<K, V> delegate) {
      return (SetMultimap)Preconditions.checkNotNull(delegate);
   }

   public static <K, V> SortedSetMultimap<K, V> synchronizedSortedSetMultimap(SortedSetMultimap<K, V> multimap) {
      return Synchronized.sortedSetMultimap(multimap, (Object)null);
   }

   public static <K, V> SortedSetMultimap<K, V> unmodifiableSortedSetMultimap(SortedSetMultimap<K, V> delegate) {
      return (SortedSetMultimap)(delegate instanceof Multimaps.UnmodifiableSortedSetMultimap ? delegate : new Multimaps.UnmodifiableSortedSetMultimap(delegate));
   }

   public static <K, V> ListMultimap<K, V> synchronizedListMultimap(ListMultimap<K, V> multimap) {
      return Synchronized.listMultimap(multimap, (Object)null);
   }

   public static <K, V> ListMultimap<K, V> unmodifiableListMultimap(ListMultimap<K, V> delegate) {
      return (ListMultimap)(!(delegate instanceof Multimaps.UnmodifiableListMultimap) && !(delegate instanceof ImmutableListMultimap) ? new Multimaps.UnmodifiableListMultimap(delegate) : delegate);
   }

   /** @deprecated */
   @Deprecated
   public static <K, V> ListMultimap<K, V> unmodifiableListMultimap(ImmutableListMultimap<K, V> delegate) {
      return (ListMultimap)Preconditions.checkNotNull(delegate);
   }

   private static <V> Collection<V> unmodifiableValueCollection(Collection<V> collection) {
      if (collection instanceof SortedSet) {
         return Collections.unmodifiableSortedSet((SortedSet)collection);
      } else if (collection instanceof Set) {
         return Collections.unmodifiableSet((Set)collection);
      } else {
         return (Collection)(collection instanceof List ? Collections.unmodifiableList((List)collection) : Collections.unmodifiableCollection(collection));
      }
   }

   private static <K, V> Collection<Entry<K, V>> unmodifiableEntries(Collection<Entry<K, V>> entries) {
      return (Collection)(entries instanceof Set ? Maps.unmodifiableEntrySet((Set)entries) : new Maps.UnmodifiableEntries(Collections.unmodifiableCollection(entries)));
   }

   @Beta
   public static <K, V> Map<K, List<V>> asMap(ListMultimap<K, V> multimap) {
      return multimap.asMap();
   }

   @Beta
   public static <K, V> Map<K, Set<V>> asMap(SetMultimap<K, V> multimap) {
      return multimap.asMap();
   }

   @Beta
   public static <K, V> Map<K, SortedSet<V>> asMap(SortedSetMultimap<K, V> multimap) {
      return multimap.asMap();
   }

   @Beta
   public static <K, V> Map<K, Collection<V>> asMap(Multimap<K, V> multimap) {
      return multimap.asMap();
   }

   public static <K, V> SetMultimap<K, V> forMap(Map<K, V> map) {
      return new Multimaps.MapMultimap(map);
   }

   public static <K, V1, V2> Multimap<K, V2> transformValues(Multimap<K, V1> fromMultimap, Function<? super V1, V2> function) {
      Preconditions.checkNotNull(function);
      Maps.EntryTransformer<K, V1, V2> transformer = Maps.asEntryTransformer(function);
      return transformEntries(fromMultimap, transformer);
   }

   public static <K, V1, V2> Multimap<K, V2> transformEntries(Multimap<K, V1> fromMap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer) {
      return new Multimaps.TransformedEntriesMultimap(fromMap, transformer);
   }

   public static <K, V1, V2> ListMultimap<K, V2> transformValues(ListMultimap<K, V1> fromMultimap, Function<? super V1, V2> function) {
      Preconditions.checkNotNull(function);
      Maps.EntryTransformer<K, V1, V2> transformer = Maps.asEntryTransformer(function);
      return transformEntries(fromMultimap, transformer);
   }

   public static <K, V1, V2> ListMultimap<K, V2> transformEntries(ListMultimap<K, V1> fromMap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer) {
      return new Multimaps.TransformedEntriesListMultimap(fromMap, transformer);
   }

   public static <K, V> ImmutableListMultimap<K, V> index(Iterable<V> values, Function<? super V, K> keyFunction) {
      return index(values.iterator(), keyFunction);
   }

   public static <K, V> ImmutableListMultimap<K, V> index(Iterator<V> values, Function<? super V, K> keyFunction) {
      Preconditions.checkNotNull(keyFunction);
      ImmutableListMultimap.Builder builder = ImmutableListMultimap.builder();

      while(values.hasNext()) {
         V value = values.next();
         Preconditions.checkNotNull(value, values);
         builder.put(keyFunction.apply(value), value);
      }

      return builder.build();
   }

   @CheckReturnValue
   public static <K, V> Multimap<K, V> filterKeys(Multimap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
      if (unfiltered instanceof SetMultimap) {
         return filterKeys((SetMultimap)unfiltered, keyPredicate);
      } else if (unfiltered instanceof ListMultimap) {
         return filterKeys((ListMultimap)unfiltered, keyPredicate);
      } else if (unfiltered instanceof FilteredKeyMultimap) {
         FilteredKeyMultimap<K, V> prev = (FilteredKeyMultimap)unfiltered;
         return new FilteredKeyMultimap(prev.unfiltered, Predicates.and(prev.keyPredicate, keyPredicate));
      } else if (unfiltered instanceof FilteredMultimap) {
         FilteredMultimap<K, V> prev = (FilteredMultimap)unfiltered;
         return filterFiltered(prev, Maps.keyPredicateOnEntries(keyPredicate));
      } else {
         return new FilteredKeyMultimap(unfiltered, keyPredicate);
      }
   }

   @CheckReturnValue
   public static <K, V> SetMultimap<K, V> filterKeys(SetMultimap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
      if (unfiltered instanceof FilteredKeySetMultimap) {
         FilteredKeySetMultimap<K, V> prev = (FilteredKeySetMultimap)unfiltered;
         return new FilteredKeySetMultimap(prev.unfiltered(), Predicates.and(prev.keyPredicate, keyPredicate));
      } else if (unfiltered instanceof FilteredSetMultimap) {
         FilteredSetMultimap<K, V> prev = (FilteredSetMultimap)unfiltered;
         return filterFiltered(prev, Maps.keyPredicateOnEntries(keyPredicate));
      } else {
         return new FilteredKeySetMultimap(unfiltered, keyPredicate);
      }
   }

   @CheckReturnValue
   public static <K, V> ListMultimap<K, V> filterKeys(ListMultimap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
      if (unfiltered instanceof FilteredKeyListMultimap) {
         FilteredKeyListMultimap<K, V> prev = (FilteredKeyListMultimap)unfiltered;
         return new FilteredKeyListMultimap(prev.unfiltered(), Predicates.and(prev.keyPredicate, keyPredicate));
      } else {
         return new FilteredKeyListMultimap(unfiltered, keyPredicate);
      }
   }

   @CheckReturnValue
   public static <K, V> Multimap<K, V> filterValues(Multimap<K, V> unfiltered, Predicate<? super V> valuePredicate) {
      return filterEntries(unfiltered, Maps.valuePredicateOnEntries(valuePredicate));
   }

   @CheckReturnValue
   public static <K, V> SetMultimap<K, V> filterValues(SetMultimap<K, V> unfiltered, Predicate<? super V> valuePredicate) {
      return filterEntries(unfiltered, Maps.valuePredicateOnEntries(valuePredicate));
   }

   @CheckReturnValue
   public static <K, V> Multimap<K, V> filterEntries(Multimap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
      Preconditions.checkNotNull(entryPredicate);
      if (unfiltered instanceof SetMultimap) {
         return filterEntries((SetMultimap)unfiltered, entryPredicate);
      } else {
         return (Multimap)(unfiltered instanceof FilteredMultimap ? filterFiltered((FilteredMultimap)unfiltered, entryPredicate) : new FilteredEntryMultimap((Multimap)Preconditions.checkNotNull(unfiltered), entryPredicate));
      }
   }

   @CheckReturnValue
   public static <K, V> SetMultimap<K, V> filterEntries(SetMultimap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
      Preconditions.checkNotNull(entryPredicate);
      return (SetMultimap)(unfiltered instanceof FilteredSetMultimap ? filterFiltered((FilteredSetMultimap)unfiltered, entryPredicate) : new FilteredEntrySetMultimap((SetMultimap)Preconditions.checkNotNull(unfiltered), entryPredicate));
   }

   private static <K, V> Multimap<K, V> filterFiltered(FilteredMultimap<K, V> multimap, Predicate<? super Entry<K, V>> entryPredicate) {
      Predicate<Entry<K, V>> predicate = Predicates.and(multimap.entryPredicate(), entryPredicate);
      return new FilteredEntryMultimap(multimap.unfiltered(), predicate);
   }

   private static <K, V> SetMultimap<K, V> filterFiltered(FilteredSetMultimap<K, V> multimap, Predicate<? super Entry<K, V>> entryPredicate) {
      Predicate<Entry<K, V>> predicate = Predicates.and(multimap.entryPredicate(), entryPredicate);
      return new FilteredEntrySetMultimap(multimap.unfiltered(), predicate);
   }

   static boolean equalsImpl(Multimap<?, ?> multimap, @Nullable Object object) {
      if (object == multimap) {
         return true;
      } else if (object instanceof Multimap) {
         Multimap<?, ?> that = (Multimap)object;
         return multimap.asMap().equals(that.asMap());
      } else {
         return false;
      }
   }

   static final class AsMap<K, V> extends Maps.ViewCachingAbstractMap<K, Collection<V>> {
      @Weak
      private final Multimap<K, V> multimap;

      AsMap(Multimap<K, V> multimap) {
         this.multimap = (Multimap)Preconditions.checkNotNull(multimap);
      }

      public int size() {
         return this.multimap.keySet().size();
      }

      protected Set<Entry<K, Collection<V>>> createEntrySet() {
         return new Multimaps.AsMap.EntrySet();
      }

      void removeValuesForKey(Object key) {
         this.multimap.keySet().remove(key);
      }

      public Collection<V> get(Object key) {
         return this.containsKey(key) ? this.multimap.get(key) : null;
      }

      public Collection<V> remove(Object key) {
         return this.containsKey(key) ? this.multimap.removeAll(key) : null;
      }

      public Set<K> keySet() {
         return this.multimap.keySet();
      }

      public boolean isEmpty() {
         return this.multimap.isEmpty();
      }

      public boolean containsKey(Object key) {
         return this.multimap.containsKey(key);
      }

      public void clear() {
         this.multimap.clear();
      }

      class EntrySet extends Maps.EntrySet<K, Collection<V>> {
         Map<K, Collection<V>> map() {
            return AsMap.this;
         }

         public Iterator<Entry<K, Collection<V>>> iterator() {
            return Maps.asMapEntryIterator(AsMap.this.multimap.keySet(), new Function<K, Collection<V>>() {
               public Collection<V> apply(K key) {
                  return AsMap.this.multimap.get(key);
               }
            });
         }

         public boolean remove(Object o) {
            if (!this.contains(o)) {
               return false;
            } else {
               Entry<?, ?> entry = (Entry)o;
               AsMap.this.removeValuesForKey(entry.getKey());
               return true;
            }
         }
      }
   }

   abstract static class Entries<K, V> extends AbstractCollection<Entry<K, V>> {
      abstract Multimap<K, V> multimap();

      public int size() {
         return this.multimap().size();
      }

      public boolean contains(@Nullable Object o) {
         if (o instanceof Entry) {
            Entry<?, ?> entry = (Entry)o;
            return this.multimap().containsEntry(entry.getKey(), entry.getValue());
         } else {
            return false;
         }
      }

      public boolean remove(@Nullable Object o) {
         if (o instanceof Entry) {
            Entry<?, ?> entry = (Entry)o;
            return this.multimap().remove(entry.getKey(), entry.getValue());
         } else {
            return false;
         }
      }

      public void clear() {
         this.multimap().clear();
      }
   }

   static class Keys<K, V> extends AbstractMultiset<K> {
      @Weak
      final Multimap<K, V> multimap;

      Keys(Multimap<K, V> multimap) {
         this.multimap = multimap;
      }

      Iterator<Multiset.Entry<K>> entryIterator() {
         return new TransformedIterator<Entry<K, Collection<V>>, Multiset.Entry<K>>(this.multimap.asMap().entrySet().iterator()) {
            Multiset.Entry<K> transform(final Entry<K, Collection<V>> backingEntry) {
               return new Multisets.AbstractEntry<K>() {
                  public K getElement() {
                     return backingEntry.getKey();
                  }

                  public int getCount() {
                     return ((Collection)backingEntry.getValue()).size();
                  }
               };
            }
         };
      }

      int distinctElements() {
         return this.multimap.asMap().size();
      }

      Set<Multiset.Entry<K>> createEntrySet() {
         return new Multimaps.Keys.KeysEntrySet();
      }

      public boolean contains(@Nullable Object element) {
         return this.multimap.containsKey(element);
      }

      public Iterator<K> iterator() {
         return Maps.keyIterator(this.multimap.entries().iterator());
      }

      public int count(@Nullable Object element) {
         Collection<V> values = (Collection)Maps.safeGet(this.multimap.asMap(), element);
         return values == null ? 0 : values.size();
      }

      public int remove(@Nullable Object element, int occurrences) {
         CollectPreconditions.checkNonnegative(occurrences, "occurrences");
         if (occurrences == 0) {
            return this.count(element);
         } else {
            Collection<V> values = (Collection)Maps.safeGet(this.multimap.asMap(), element);
            if (values == null) {
               return 0;
            } else {
               int oldCount = values.size();
               if (occurrences >= oldCount) {
                  values.clear();
               } else {
                  Iterator<V> iterator = values.iterator();

                  for(int i = 0; i < occurrences; ++i) {
                     iterator.next();
                     iterator.remove();
                  }
               }

               return oldCount;
            }
         }
      }

      public void clear() {
         this.multimap.clear();
      }

      public Set<K> elementSet() {
         return this.multimap.keySet();
      }

      class KeysEntrySet extends Multisets.EntrySet<K> {
         Multiset<K> multiset() {
            return Keys.this;
         }

         public Iterator<Multiset.Entry<K>> iterator() {
            return Keys.this.entryIterator();
         }

         public int size() {
            return Keys.this.distinctElements();
         }

         public boolean isEmpty() {
            return Keys.this.multimap.isEmpty();
         }

         public boolean contains(@Nullable Object o) {
            if (!(o instanceof Multiset.Entry)) {
               return false;
            } else {
               Multiset.Entry<?> entry = (Multiset.Entry)o;
               Collection<V> collection = (Collection)Keys.this.multimap.asMap().get(entry.getElement());
               return collection != null && collection.size() == entry.getCount();
            }
         }

         public boolean remove(@Nullable Object o) {
            if (o instanceof Multiset.Entry) {
               Multiset.Entry<?> entry = (Multiset.Entry)o;
               Collection<V> collection = (Collection)Keys.this.multimap.asMap().get(entry.getElement());
               if (collection != null && collection.size() == entry.getCount()) {
                  collection.clear();
                  return true;
               }
            }

            return false;
         }
      }
   }

   private static final class TransformedEntriesListMultimap<K, V1, V2> extends Multimaps.TransformedEntriesMultimap<K, V1, V2> implements ListMultimap<K, V2> {
      TransformedEntriesListMultimap(ListMultimap<K, V1> fromMultimap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer) {
         super(fromMultimap, transformer);
      }

      List<V2> transform(K key, Collection<V1> values) {
         return Lists.transform((List)values, Maps.asValueToValueFunction(this.transformer, key));
      }

      public List<V2> get(K key) {
         return this.transform(key, this.fromMultimap.get(key));
      }

      public List<V2> removeAll(Object key) {
         return this.transform(key, this.fromMultimap.removeAll(key));
      }

      public List<V2> replaceValues(K key, Iterable<? extends V2> values) {
         throw new UnsupportedOperationException();
      }
   }

   private static class TransformedEntriesMultimap<K, V1, V2> extends AbstractMultimap<K, V2> {
      final Multimap<K, V1> fromMultimap;
      final Maps.EntryTransformer<? super K, ? super V1, V2> transformer;

      TransformedEntriesMultimap(Multimap<K, V1> fromMultimap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer) {
         this.fromMultimap = (Multimap)Preconditions.checkNotNull(fromMultimap);
         this.transformer = (Maps.EntryTransformer)Preconditions.checkNotNull(transformer);
      }

      Collection<V2> transform(K key, Collection<V1> values) {
         Function<? super V1, V2> function = Maps.asValueToValueFunction(this.transformer, key);
         return (Collection)(values instanceof List ? Lists.transform((List)values, function) : Collections2.transform(values, function));
      }

      Map<K, Collection<V2>> createAsMap() {
         return Maps.transformEntries(this.fromMultimap.asMap(), new Maps.EntryTransformer<K, Collection<V1>, Collection<V2>>() {
            public Collection<V2> transformEntry(K key, Collection<V1> value) {
               return TransformedEntriesMultimap.this.transform(key, value);
            }
         });
      }

      public void clear() {
         this.fromMultimap.clear();
      }

      public boolean containsKey(Object key) {
         return this.fromMultimap.containsKey(key);
      }

      Iterator<Entry<K, V2>> entryIterator() {
         return Iterators.transform(this.fromMultimap.entries().iterator(), Maps.asEntryToEntryFunction(this.transformer));
      }

      public Collection<V2> get(K key) {
         return this.transform(key, this.fromMultimap.get(key));
      }

      public boolean isEmpty() {
         return this.fromMultimap.isEmpty();
      }

      public Set<K> keySet() {
         return this.fromMultimap.keySet();
      }

      public Multiset<K> keys() {
         return this.fromMultimap.keys();
      }

      public boolean put(K key, V2 value) {
         throw new UnsupportedOperationException();
      }

      public boolean putAll(K key, Iterable<? extends V2> values) {
         throw new UnsupportedOperationException();
      }

      public boolean putAll(Multimap<? extends K, ? extends V2> multimap) {
         throw new UnsupportedOperationException();
      }

      public boolean remove(Object key, Object value) {
         return this.get(key).remove(value);
      }

      public Collection<V2> removeAll(Object key) {
         return this.transform(key, this.fromMultimap.removeAll(key));
      }

      public Collection<V2> replaceValues(K key, Iterable<? extends V2> values) {
         throw new UnsupportedOperationException();
      }

      public int size() {
         return this.fromMultimap.size();
      }

      Collection<V2> createValues() {
         return Collections2.transform(this.fromMultimap.entries(), Maps.asEntryToValueFunction(this.transformer));
      }
   }

   private static class MapMultimap<K, V> extends AbstractMultimap<K, V> implements SetMultimap<K, V>, Serializable {
      final Map<K, V> map;
      private static final long serialVersionUID = 7845222491160860175L;

      MapMultimap(Map<K, V> map) {
         this.map = (Map)Preconditions.checkNotNull(map);
      }

      public int size() {
         return this.map.size();
      }

      public boolean containsKey(Object key) {
         return this.map.containsKey(key);
      }

      public boolean containsValue(Object value) {
         return this.map.containsValue(value);
      }

      public boolean containsEntry(Object key, Object value) {
         return this.map.entrySet().contains(Maps.immutableEntry(key, value));
      }

      public Set<V> get(final K key) {
         return new Sets.ImprovedAbstractSet<V>() {
            public Iterator<V> iterator() {
               return new Iterator<V>() {
                  int i;

                  public boolean hasNext() {
                     return this.i == 0 && MapMultimap.this.map.containsKey(key);
                  }

                  public V next() {
                     if (!this.hasNext()) {
                        throw new NoSuchElementException();
                     } else {
                        ++this.i;
                        return MapMultimap.this.map.get(key);
                     }
                  }

                  public void remove() {
                     CollectPreconditions.checkRemove(this.i == 1);
                     this.i = -1;
                     MapMultimap.this.map.remove(key);
                  }
               };
            }

            public int size() {
               return MapMultimap.this.map.containsKey(key) ? 1 : 0;
            }
         };
      }

      public boolean put(K key, V value) {
         throw new UnsupportedOperationException();
      }

      public boolean putAll(K key, Iterable<? extends V> values) {
         throw new UnsupportedOperationException();
      }

      public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
         throw new UnsupportedOperationException();
      }

      public Set<V> replaceValues(K key, Iterable<? extends V> values) {
         throw new UnsupportedOperationException();
      }

      public boolean remove(Object key, Object value) {
         return this.map.entrySet().remove(Maps.immutableEntry(key, value));
      }

      public Set<V> removeAll(Object key) {
         Set<V> values = new HashSet(2);
         if (!this.map.containsKey(key)) {
            return values;
         } else {
            values.add(this.map.remove(key));
            return values;
         }
      }

      public void clear() {
         this.map.clear();
      }

      public Set<K> keySet() {
         return this.map.keySet();
      }

      public Collection<V> values() {
         return this.map.values();
      }

      public Set<Entry<K, V>> entries() {
         return this.map.entrySet();
      }

      Iterator<Entry<K, V>> entryIterator() {
         return this.map.entrySet().iterator();
      }

      Map<K, Collection<V>> createAsMap() {
         return new Multimaps.AsMap(this);
      }

      public int hashCode() {
         return this.map.hashCode();
      }
   }

   private static class UnmodifiableSortedSetMultimap<K, V> extends Multimaps.UnmodifiableSetMultimap<K, V> implements SortedSetMultimap<K, V> {
      private static final long serialVersionUID = 0L;

      UnmodifiableSortedSetMultimap(SortedSetMultimap<K, V> delegate) {
         super(delegate);
      }

      public SortedSetMultimap<K, V> delegate() {
         return (SortedSetMultimap)super.delegate();
      }

      public SortedSet<V> get(K key) {
         return Collections.unmodifiableSortedSet(this.delegate().get(key));
      }

      public SortedSet<V> removeAll(Object key) {
         throw new UnsupportedOperationException();
      }

      public SortedSet<V> replaceValues(K key, Iterable<? extends V> values) {
         throw new UnsupportedOperationException();
      }

      public Comparator<? super V> valueComparator() {
         return this.delegate().valueComparator();
      }
   }

   private static class UnmodifiableSetMultimap<K, V> extends Multimaps.UnmodifiableMultimap<K, V> implements SetMultimap<K, V> {
      private static final long serialVersionUID = 0L;

      UnmodifiableSetMultimap(SetMultimap<K, V> delegate) {
         super(delegate);
      }

      public SetMultimap<K, V> delegate() {
         return (SetMultimap)super.delegate();
      }

      public Set<V> get(K key) {
         return Collections.unmodifiableSet(this.delegate().get(key));
      }

      public Set<Entry<K, V>> entries() {
         return Maps.unmodifiableEntrySet(this.delegate().entries());
      }

      public Set<V> removeAll(Object key) {
         throw new UnsupportedOperationException();
      }

      public Set<V> replaceValues(K key, Iterable<? extends V> values) {
         throw new UnsupportedOperationException();
      }
   }

   private static class UnmodifiableListMultimap<K, V> extends Multimaps.UnmodifiableMultimap<K, V> implements ListMultimap<K, V> {
      private static final long serialVersionUID = 0L;

      UnmodifiableListMultimap(ListMultimap<K, V> delegate) {
         super(delegate);
      }

      public ListMultimap<K, V> delegate() {
         return (ListMultimap)super.delegate();
      }

      public List<V> get(K key) {
         return Collections.unmodifiableList(this.delegate().get(key));
      }

      public List<V> removeAll(Object key) {
         throw new UnsupportedOperationException();
      }

      public List<V> replaceValues(K key, Iterable<? extends V> values) {
         throw new UnsupportedOperationException();
      }
   }

   private static class UnmodifiableMultimap<K, V> extends ForwardingMultimap<K, V> implements Serializable {
      final Multimap<K, V> delegate;
      transient Collection<Entry<K, V>> entries;
      transient Multiset<K> keys;
      transient Set<K> keySet;
      transient Collection<V> values;
      transient Map<K, Collection<V>> map;
      private static final long serialVersionUID = 0L;

      UnmodifiableMultimap(Multimap<K, V> delegate) {
         this.delegate = (Multimap)Preconditions.checkNotNull(delegate);
      }

      protected Multimap<K, V> delegate() {
         return this.delegate;
      }

      public void clear() {
         throw new UnsupportedOperationException();
      }

      public Map<K, Collection<V>> asMap() {
         Map<K, Collection<V>> result = this.map;
         if (result == null) {
            result = this.map = Collections.unmodifiableMap(Maps.transformValues(this.delegate.asMap(), new Function<Collection<V>, Collection<V>>() {
               public Collection<V> apply(Collection<V> collection) {
                  return Multimaps.unmodifiableValueCollection(collection);
               }
            }));
         }

         return result;
      }

      public Collection<Entry<K, V>> entries() {
         Collection<Entry<K, V>> result = this.entries;
         if (result == null) {
            this.entries = result = Multimaps.unmodifiableEntries(this.delegate.entries());
         }

         return result;
      }

      public Collection<V> get(K key) {
         return Multimaps.unmodifiableValueCollection(this.delegate.get(key));
      }

      public Multiset<K> keys() {
         Multiset<K> result = this.keys;
         if (result == null) {
            this.keys = result = Multisets.unmodifiableMultiset(this.delegate.keys());
         }

         return result;
      }

      public Set<K> keySet() {
         Set<K> result = this.keySet;
         if (result == null) {
            this.keySet = result = Collections.unmodifiableSet(this.delegate.keySet());
         }

         return result;
      }

      public boolean put(K key, V value) {
         throw new UnsupportedOperationException();
      }

      public boolean putAll(K key, Iterable<? extends V> values) {
         throw new UnsupportedOperationException();
      }

      public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
         throw new UnsupportedOperationException();
      }

      public boolean remove(Object key, Object value) {
         throw new UnsupportedOperationException();
      }

      public Collection<V> removeAll(Object key) {
         throw new UnsupportedOperationException();
      }

      public Collection<V> replaceValues(K key, Iterable<? extends V> values) {
         throw new UnsupportedOperationException();
      }

      public Collection<V> values() {
         Collection<V> result = this.values;
         if (result == null) {
            this.values = result = Collections.unmodifiableCollection(this.delegate.values());
         }

         return result;
      }
   }

   private static class CustomSortedSetMultimap<K, V> extends AbstractSortedSetMultimap<K, V> {
      transient Supplier<? extends SortedSet<V>> factory;
      transient Comparator<? super V> valueComparator;
      @GwtIncompatible("not needed in emulated source")
      private static final long serialVersionUID = 0L;

      CustomSortedSetMultimap(Map<K, Collection<V>> map, Supplier<? extends SortedSet<V>> factory) {
         super(map);
         this.factory = (Supplier)Preconditions.checkNotNull(factory);
         this.valueComparator = ((SortedSet)factory.get()).comparator();
      }

      protected SortedSet<V> createCollection() {
         return (SortedSet)this.factory.get();
      }

      public Comparator<? super V> valueComparator() {
         return this.valueComparator;
      }

      @GwtIncompatible("java.io.ObjectOutputStream")
      private void writeObject(ObjectOutputStream stream) throws IOException {
         stream.defaultWriteObject();
         stream.writeObject(this.factory);
         stream.writeObject(this.backingMap());
      }

      @GwtIncompatible("java.io.ObjectInputStream")
      private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
         stream.defaultReadObject();
         this.factory = (Supplier)stream.readObject();
         this.valueComparator = ((SortedSet)this.factory.get()).comparator();
         Map<K, Collection<V>> map = (Map)stream.readObject();
         this.setMap(map);
      }
   }

   private static class CustomSetMultimap<K, V> extends AbstractSetMultimap<K, V> {
      transient Supplier<? extends Set<V>> factory;
      @GwtIncompatible("not needed in emulated source")
      private static final long serialVersionUID = 0L;

      CustomSetMultimap(Map<K, Collection<V>> map, Supplier<? extends Set<V>> factory) {
         super(map);
         this.factory = (Supplier)Preconditions.checkNotNull(factory);
      }

      protected Set<V> createCollection() {
         return (Set)this.factory.get();
      }

      @GwtIncompatible("java.io.ObjectOutputStream")
      private void writeObject(ObjectOutputStream stream) throws IOException {
         stream.defaultWriteObject();
         stream.writeObject(this.factory);
         stream.writeObject(this.backingMap());
      }

      @GwtIncompatible("java.io.ObjectInputStream")
      private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
         stream.defaultReadObject();
         this.factory = (Supplier)stream.readObject();
         Map<K, Collection<V>> map = (Map)stream.readObject();
         this.setMap(map);
      }
   }

   private static class CustomListMultimap<K, V> extends AbstractListMultimap<K, V> {
      transient Supplier<? extends List<V>> factory;
      @GwtIncompatible("java serialization not supported")
      private static final long serialVersionUID = 0L;

      CustomListMultimap(Map<K, Collection<V>> map, Supplier<? extends List<V>> factory) {
         super(map);
         this.factory = (Supplier)Preconditions.checkNotNull(factory);
      }

      protected List<V> createCollection() {
         return (List)this.factory.get();
      }

      @GwtIncompatible("java.io.ObjectOutputStream")
      private void writeObject(ObjectOutputStream stream) throws IOException {
         stream.defaultWriteObject();
         stream.writeObject(this.factory);
         stream.writeObject(this.backingMap());
      }

      @GwtIncompatible("java.io.ObjectInputStream")
      private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
         stream.defaultReadObject();
         this.factory = (Supplier)stream.readObject();
         Map<K, Collection<V>> map = (Map)stream.readObject();
         this.setMap(map);
      }
   }

   private static class CustomMultimap<K, V> extends AbstractMapBasedMultimap<K, V> {
      transient Supplier<? extends Collection<V>> factory;
      @GwtIncompatible("java serialization not supported")
      private static final long serialVersionUID = 0L;

      CustomMultimap(Map<K, Collection<V>> map, Supplier<? extends Collection<V>> factory) {
         super(map);
         this.factory = (Supplier)Preconditions.checkNotNull(factory);
      }

      protected Collection<V> createCollection() {
         return (Collection)this.factory.get();
      }

      @GwtIncompatible("java.io.ObjectOutputStream")
      private void writeObject(ObjectOutputStream stream) throws IOException {
         stream.defaultWriteObject();
         stream.writeObject(this.factory);
         stream.writeObject(this.backingMap());
      }

      @GwtIncompatible("java.io.ObjectInputStream")
      private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
         stream.defaultReadObject();
         this.factory = (Supplier)stream.readObject();
         Map<K, Collection<V>> map = (Map)stream.readObject();
         this.setMap(map);
      }
   }
}
