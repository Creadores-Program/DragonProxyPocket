package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Converter;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.j2objc.annotations.Weak;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@GwtCompatible(
   emulated = true
)
public final class Maps {
   static final Joiner.MapJoiner STANDARD_JOINER;

   private Maps() {
   }

   static <K> Function<Entry<K, ?>, K> keyFunction() {
      return Maps.EntryFunction.KEY;
   }

   static <V> Function<Entry<?, V>, V> valueFunction() {
      return Maps.EntryFunction.VALUE;
   }

   static <K, V> Iterator<K> keyIterator(Iterator<Entry<K, V>> entryIterator) {
      return Iterators.transform(entryIterator, keyFunction());
   }

   static <K, V> Iterator<V> valueIterator(Iterator<Entry<K, V>> entryIterator) {
      return Iterators.transform(entryIterator, valueFunction());
   }

   @GwtCompatible(
      serializable = true
   )
   @Beta
   public static <K extends Enum<K>, V> ImmutableMap<K, V> immutableEnumMap(Map<K, ? extends V> map) {
      if (map instanceof ImmutableEnumMap) {
         ImmutableEnumMap<K, V> result = (ImmutableEnumMap)map;
         return result;
      } else if (map.isEmpty()) {
         return ImmutableMap.of();
      } else {
         Iterator i$ = map.entrySet().iterator();

         while(i$.hasNext()) {
            Entry<K, ? extends V> entry = (Entry)i$.next();
            Preconditions.checkNotNull(entry.getKey());
            Preconditions.checkNotNull(entry.getValue());
         }

         return ImmutableEnumMap.asImmutable(new EnumMap(map));
      }
   }

   public static <K, V> HashMap<K, V> newHashMap() {
      return new HashMap();
   }

   public static <K, V> HashMap<K, V> newHashMapWithExpectedSize(int expectedSize) {
      return new HashMap(capacity(expectedSize));
   }

   static int capacity(int expectedSize) {
      if (expectedSize < 3) {
         CollectPreconditions.checkNonnegative(expectedSize, "expectedSize");
         return expectedSize + 1;
      } else {
         return expectedSize < 1073741824 ? (int)((float)expectedSize / 0.75F + 1.0F) : Integer.MAX_VALUE;
      }
   }

   public static <K, V> HashMap<K, V> newHashMap(Map<? extends K, ? extends V> map) {
      return new HashMap(map);
   }

   public static <K, V> LinkedHashMap<K, V> newLinkedHashMap() {
      return new LinkedHashMap();
   }

   public static <K, V> LinkedHashMap<K, V> newLinkedHashMapWithExpectedSize(int expectedSize) {
      return new LinkedHashMap(capacity(expectedSize));
   }

   public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(Map<? extends K, ? extends V> map) {
      return new LinkedHashMap(map);
   }

   public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
      return (new MapMaker()).makeMap();
   }

   public static <K extends Comparable, V> TreeMap<K, V> newTreeMap() {
      return new TreeMap();
   }

   public static <K, V> TreeMap<K, V> newTreeMap(SortedMap<K, ? extends V> map) {
      return new TreeMap(map);
   }

   public static <C, K extends C, V> TreeMap<K, V> newTreeMap(@Nullable Comparator<C> comparator) {
      return new TreeMap(comparator);
   }

   public static <K extends Enum<K>, V> EnumMap<K, V> newEnumMap(Class<K> type) {
      return new EnumMap((Class)Preconditions.checkNotNull(type));
   }

   public static <K extends Enum<K>, V> EnumMap<K, V> newEnumMap(Map<K, ? extends V> map) {
      return new EnumMap(map);
   }

   public static <K, V> IdentityHashMap<K, V> newIdentityHashMap() {
      return new IdentityHashMap();
   }

   public static <K, V> MapDifference<K, V> difference(Map<? extends K, ? extends V> left, Map<? extends K, ? extends V> right) {
      if (left instanceof SortedMap) {
         SortedMap<K, ? extends V> sortedLeft = (SortedMap)left;
         SortedMapDifference<K, V> result = difference(sortedLeft, right);
         return result;
      } else {
         return difference(left, right, Equivalence.equals());
      }
   }

   @Beta
   public static <K, V> MapDifference<K, V> difference(Map<? extends K, ? extends V> left, Map<? extends K, ? extends V> right, Equivalence<? super V> valueEquivalence) {
      Preconditions.checkNotNull(valueEquivalence);
      Map<K, V> onlyOnLeft = newLinkedHashMap();
      Map<K, V> onlyOnRight = new LinkedHashMap(right);
      Map<K, V> onBoth = newLinkedHashMap();
      Map<K, MapDifference.ValueDifference<V>> differences = newLinkedHashMap();
      doDifference(left, right, valueEquivalence, onlyOnLeft, onlyOnRight, onBoth, differences);
      return new Maps.MapDifferenceImpl(onlyOnLeft, onlyOnRight, onBoth, differences);
   }

   private static <K, V> void doDifference(Map<? extends K, ? extends V> left, Map<? extends K, ? extends V> right, Equivalence<? super V> valueEquivalence, Map<K, V> onlyOnLeft, Map<K, V> onlyOnRight, Map<K, V> onBoth, Map<K, MapDifference.ValueDifference<V>> differences) {
      Iterator i$ = left.entrySet().iterator();

      while(i$.hasNext()) {
         Entry<? extends K, ? extends V> entry = (Entry)i$.next();
         K leftKey = entry.getKey();
         V leftValue = entry.getValue();
         if (right.containsKey(leftKey)) {
            V rightValue = onlyOnRight.remove(leftKey);
            if (valueEquivalence.equivalent(leftValue, rightValue)) {
               onBoth.put(leftKey, leftValue);
            } else {
               differences.put(leftKey, Maps.ValueDifferenceImpl.create(leftValue, rightValue));
            }
         } else {
            onlyOnLeft.put(leftKey, leftValue);
         }
      }

   }

   private static <K, V> Map<K, V> unmodifiableMap(Map<K, V> map) {
      return (Map)(map instanceof SortedMap ? Collections.unmodifiableSortedMap((SortedMap)map) : Collections.unmodifiableMap(map));
   }

   public static <K, V> SortedMapDifference<K, V> difference(SortedMap<K, ? extends V> left, Map<? extends K, ? extends V> right) {
      Preconditions.checkNotNull(left);
      Preconditions.checkNotNull(right);
      Comparator<? super K> comparator = orNaturalOrder(left.comparator());
      SortedMap<K, V> onlyOnLeft = newTreeMap(comparator);
      SortedMap<K, V> onlyOnRight = newTreeMap(comparator);
      onlyOnRight.putAll(right);
      SortedMap<K, V> onBoth = newTreeMap(comparator);
      SortedMap<K, MapDifference.ValueDifference<V>> differences = newTreeMap(comparator);
      doDifference(left, right, Equivalence.equals(), onlyOnLeft, onlyOnRight, onBoth, differences);
      return new Maps.SortedMapDifferenceImpl(onlyOnLeft, onlyOnRight, onBoth, differences);
   }

   static <E> Comparator<? super E> orNaturalOrder(@Nullable Comparator<? super E> comparator) {
      return (Comparator)(comparator != null ? comparator : Ordering.natural());
   }

   public static <K, V> Map<K, V> asMap(Set<K> set, Function<? super K, V> function) {
      return (Map)(set instanceof SortedSet ? asMap((SortedSet)set, function) : new Maps.AsMapView(set, function));
   }

   public static <K, V> SortedMap<K, V> asMap(SortedSet<K> set, Function<? super K, V> function) {
      return Platform.mapsAsMapSortedSet(set, function);
   }

   static <K, V> SortedMap<K, V> asMapSortedIgnoreNavigable(SortedSet<K> set, Function<? super K, V> function) {
      return new Maps.SortedAsMapView(set, function);
   }

   @GwtIncompatible("NavigableMap")
   public static <K, V> NavigableMap<K, V> asMap(NavigableSet<K> set, Function<? super K, V> function) {
      return new Maps.NavigableAsMapView(set, function);
   }

   static <K, V> Iterator<Entry<K, V>> asMapEntryIterator(Set<K> set, final Function<? super K, V> function) {
      return new TransformedIterator<K, Entry<K, V>>(set.iterator()) {
         Entry<K, V> transform(K key) {
            return Maps.immutableEntry(key, function.apply(key));
         }
      };
   }

   private static <E> Set<E> removeOnlySet(final Set<E> set) {
      return new ForwardingSet<E>() {
         protected Set<E> delegate() {
            return set;
         }

         public boolean add(E element) {
            throw new UnsupportedOperationException();
         }

         public boolean addAll(Collection<? extends E> es) {
            throw new UnsupportedOperationException();
         }
      };
   }

   private static <E> SortedSet<E> removeOnlySortedSet(final SortedSet<E> set) {
      return new ForwardingSortedSet<E>() {
         protected SortedSet<E> delegate() {
            return set;
         }

         public boolean add(E element) {
            throw new UnsupportedOperationException();
         }

         public boolean addAll(Collection<? extends E> es) {
            throw new UnsupportedOperationException();
         }

         public SortedSet<E> headSet(E toElement) {
            return Maps.removeOnlySortedSet(super.headSet(toElement));
         }

         public SortedSet<E> subSet(E fromElement, E toElement) {
            return Maps.removeOnlySortedSet(super.subSet(fromElement, toElement));
         }

         public SortedSet<E> tailSet(E fromElement) {
            return Maps.removeOnlySortedSet(super.tailSet(fromElement));
         }
      };
   }

   @GwtIncompatible("NavigableSet")
   private static <E> NavigableSet<E> removeOnlyNavigableSet(final NavigableSet<E> set) {
      return new ForwardingNavigableSet<E>() {
         protected NavigableSet<E> delegate() {
            return set;
         }

         public boolean add(E element) {
            throw new UnsupportedOperationException();
         }

         public boolean addAll(Collection<? extends E> es) {
            throw new UnsupportedOperationException();
         }

         public SortedSet<E> headSet(E toElement) {
            return Maps.removeOnlySortedSet(super.headSet(toElement));
         }

         public SortedSet<E> subSet(E fromElement, E toElement) {
            return Maps.removeOnlySortedSet(super.subSet(fromElement, toElement));
         }

         public SortedSet<E> tailSet(E fromElement) {
            return Maps.removeOnlySortedSet(super.tailSet(fromElement));
         }

         public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return Maps.removeOnlyNavigableSet(super.headSet(toElement, inclusive));
         }

         public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return Maps.removeOnlyNavigableSet(super.tailSet(fromElement, inclusive));
         }

         public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return Maps.removeOnlyNavigableSet(super.subSet(fromElement, fromInclusive, toElement, toInclusive));
         }

         public NavigableSet<E> descendingSet() {
            return Maps.removeOnlyNavigableSet(super.descendingSet());
         }
      };
   }

   public static <K, V> ImmutableMap<K, V> toMap(Iterable<K> keys, Function<? super K, V> valueFunction) {
      return toMap(keys.iterator(), valueFunction);
   }

   public static <K, V> ImmutableMap<K, V> toMap(Iterator<K> keys, Function<? super K, V> valueFunction) {
      Preconditions.checkNotNull(valueFunction);
      LinkedHashMap builder = newLinkedHashMap();

      while(keys.hasNext()) {
         K key = keys.next();
         builder.put(key, valueFunction.apply(key));
      }

      return ImmutableMap.copyOf((Map)builder);
   }

   public static <K, V> ImmutableMap<K, V> uniqueIndex(Iterable<V> values, Function<? super V, K> keyFunction) {
      return uniqueIndex(values.iterator(), keyFunction);
   }

   public static <K, V> ImmutableMap<K, V> uniqueIndex(Iterator<V> values, Function<? super V, K> keyFunction) {
      Preconditions.checkNotNull(keyFunction);
      ImmutableMap.Builder builder = ImmutableMap.builder();

      while(values.hasNext()) {
         V value = values.next();
         builder.put(keyFunction.apply(value), value);
      }

      try {
         return builder.build();
      } catch (IllegalArgumentException var4) {
         throw new IllegalArgumentException(var4.getMessage() + ". To index multiple values under a key, use Multimaps.index.");
      }
   }

   @GwtIncompatible("java.util.Properties")
   public static ImmutableMap<String, String> fromProperties(Properties properties) {
      ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
      Enumeration e = properties.propertyNames();

      while(e.hasMoreElements()) {
         String key = (String)e.nextElement();
         builder.put(key, properties.getProperty(key));
      }

      return builder.build();
   }

   @GwtCompatible(
      serializable = true
   )
   public static <K, V> Entry<K, V> immutableEntry(@Nullable K key, @Nullable V value) {
      return new ImmutableEntry(key, value);
   }

   static <K, V> Set<Entry<K, V>> unmodifiableEntrySet(Set<Entry<K, V>> entrySet) {
      return new Maps.UnmodifiableEntrySet(Collections.unmodifiableSet(entrySet));
   }

   static <K, V> Entry<K, V> unmodifiableEntry(final Entry<? extends K, ? extends V> entry) {
      Preconditions.checkNotNull(entry);
      return new AbstractMapEntry<K, V>() {
         public K getKey() {
            return entry.getKey();
         }

         public V getValue() {
            return entry.getValue();
         }
      };
   }

   static <K, V> UnmodifiableIterator<Entry<K, V>> unmodifiableEntryIterator(final Iterator<Entry<K, V>> entryIterator) {
      return new UnmodifiableIterator<Entry<K, V>>() {
         public boolean hasNext() {
            return entryIterator.hasNext();
         }

         public Entry<K, V> next() {
            return Maps.unmodifiableEntry((Entry)entryIterator.next());
         }
      };
   }

   @Beta
   public static <A, B> Converter<A, B> asConverter(BiMap<A, B> bimap) {
      return new Maps.BiMapConverter(bimap);
   }

   public static <K, V> BiMap<K, V> synchronizedBiMap(BiMap<K, V> bimap) {
      return Synchronized.biMap(bimap, (Object)null);
   }

   public static <K, V> BiMap<K, V> unmodifiableBiMap(BiMap<? extends K, ? extends V> bimap) {
      return new Maps.UnmodifiableBiMap(bimap, (BiMap)null);
   }

   public static <K, V1, V2> Map<K, V2> transformValues(Map<K, V1> fromMap, Function<? super V1, V2> function) {
      return transformEntries(fromMap, asEntryTransformer(function));
   }

   public static <K, V1, V2> SortedMap<K, V2> transformValues(SortedMap<K, V1> fromMap, Function<? super V1, V2> function) {
      return transformEntries(fromMap, asEntryTransformer(function));
   }

   @GwtIncompatible("NavigableMap")
   public static <K, V1, V2> NavigableMap<K, V2> transformValues(NavigableMap<K, V1> fromMap, Function<? super V1, V2> function) {
      return transformEntries(fromMap, asEntryTransformer(function));
   }

   public static <K, V1, V2> Map<K, V2> transformEntries(Map<K, V1> fromMap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer) {
      return (Map)(fromMap instanceof SortedMap ? transformEntries((SortedMap)fromMap, transformer) : new Maps.TransformedEntriesMap(fromMap, transformer));
   }

   public static <K, V1, V2> SortedMap<K, V2> transformEntries(SortedMap<K, V1> fromMap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer) {
      return Platform.mapsTransformEntriesSortedMap(fromMap, transformer);
   }

   @GwtIncompatible("NavigableMap")
   public static <K, V1, V2> NavigableMap<K, V2> transformEntries(NavigableMap<K, V1> fromMap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer) {
      return new Maps.TransformedEntriesNavigableMap(fromMap, transformer);
   }

   static <K, V1, V2> SortedMap<K, V2> transformEntriesIgnoreNavigable(SortedMap<K, V1> fromMap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer) {
      return new Maps.TransformedEntriesSortedMap(fromMap, transformer);
   }

   static <K, V1, V2> Maps.EntryTransformer<K, V1, V2> asEntryTransformer(final Function<? super V1, V2> function) {
      Preconditions.checkNotNull(function);
      return new Maps.EntryTransformer<K, V1, V2>() {
         public V2 transformEntry(K key, V1 value) {
            return function.apply(value);
         }
      };
   }

   static <K, V1, V2> Function<V1, V2> asValueToValueFunction(final Maps.EntryTransformer<? super K, V1, V2> transformer, final K key) {
      Preconditions.checkNotNull(transformer);
      return new Function<V1, V2>() {
         public V2 apply(@Nullable V1 v1) {
            return transformer.transformEntry(key, v1);
         }
      };
   }

   static <K, V1, V2> Function<Entry<K, V1>, V2> asEntryToValueFunction(final Maps.EntryTransformer<? super K, ? super V1, V2> transformer) {
      Preconditions.checkNotNull(transformer);
      return new Function<Entry<K, V1>, V2>() {
         public V2 apply(Entry<K, V1> entry) {
            return transformer.transformEntry(entry.getKey(), entry.getValue());
         }
      };
   }

   static <V2, K, V1> Entry<K, V2> transformEntry(final Maps.EntryTransformer<? super K, ? super V1, V2> transformer, final Entry<K, V1> entry) {
      Preconditions.checkNotNull(transformer);
      Preconditions.checkNotNull(entry);
      return new AbstractMapEntry<K, V2>() {
         public K getKey() {
            return entry.getKey();
         }

         public V2 getValue() {
            return transformer.transformEntry(entry.getKey(), entry.getValue());
         }
      };
   }

   static <K, V1, V2> Function<Entry<K, V1>, Entry<K, V2>> asEntryToEntryFunction(final Maps.EntryTransformer<? super K, ? super V1, V2> transformer) {
      Preconditions.checkNotNull(transformer);
      return new Function<Entry<K, V1>, Entry<K, V2>>() {
         public Entry<K, V2> apply(Entry<K, V1> entry) {
            return Maps.transformEntry(transformer, entry);
         }
      };
   }

   static <K> Predicate<Entry<K, ?>> keyPredicateOnEntries(Predicate<? super K> keyPredicate) {
      return Predicates.compose(keyPredicate, keyFunction());
   }

   static <V> Predicate<Entry<?, V>> valuePredicateOnEntries(Predicate<? super V> valuePredicate) {
      return Predicates.compose(valuePredicate, valueFunction());
   }

   @CheckReturnValue
   public static <K, V> Map<K, V> filterKeys(Map<K, V> unfiltered, Predicate<? super K> keyPredicate) {
      if (unfiltered instanceof SortedMap) {
         return filterKeys((SortedMap)unfiltered, keyPredicate);
      } else if (unfiltered instanceof BiMap) {
         return filterKeys((BiMap)unfiltered, keyPredicate);
      } else {
         Preconditions.checkNotNull(keyPredicate);
         Predicate<Entry<K, ?>> entryPredicate = keyPredicateOnEntries(keyPredicate);
         return (Map)(unfiltered instanceof Maps.AbstractFilteredMap ? filterFiltered((Maps.AbstractFilteredMap)unfiltered, entryPredicate) : new Maps.FilteredKeyMap((Map)Preconditions.checkNotNull(unfiltered), keyPredicate, entryPredicate));
      }
   }

   @CheckReturnValue
   public static <K, V> SortedMap<K, V> filterKeys(SortedMap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
      return filterEntries(unfiltered, keyPredicateOnEntries(keyPredicate));
   }

   @CheckReturnValue
   @GwtIncompatible("NavigableMap")
   public static <K, V> NavigableMap<K, V> filterKeys(NavigableMap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
      return filterEntries(unfiltered, keyPredicateOnEntries(keyPredicate));
   }

   @CheckReturnValue
   public static <K, V> BiMap<K, V> filterKeys(BiMap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
      Preconditions.checkNotNull(keyPredicate);
      return filterEntries(unfiltered, keyPredicateOnEntries(keyPredicate));
   }

   @CheckReturnValue
   public static <K, V> Map<K, V> filterValues(Map<K, V> unfiltered, Predicate<? super V> valuePredicate) {
      if (unfiltered instanceof SortedMap) {
         return filterValues((SortedMap)unfiltered, valuePredicate);
      } else {
         return (Map)(unfiltered instanceof BiMap ? filterValues((BiMap)unfiltered, valuePredicate) : filterEntries(unfiltered, valuePredicateOnEntries(valuePredicate)));
      }
   }

   @CheckReturnValue
   public static <K, V> SortedMap<K, V> filterValues(SortedMap<K, V> unfiltered, Predicate<? super V> valuePredicate) {
      return filterEntries(unfiltered, valuePredicateOnEntries(valuePredicate));
   }

   @CheckReturnValue
   @GwtIncompatible("NavigableMap")
   public static <K, V> NavigableMap<K, V> filterValues(NavigableMap<K, V> unfiltered, Predicate<? super V> valuePredicate) {
      return filterEntries(unfiltered, valuePredicateOnEntries(valuePredicate));
   }

   @CheckReturnValue
   public static <K, V> BiMap<K, V> filterValues(BiMap<K, V> unfiltered, Predicate<? super V> valuePredicate) {
      return filterEntries(unfiltered, valuePredicateOnEntries(valuePredicate));
   }

   @CheckReturnValue
   public static <K, V> Map<K, V> filterEntries(Map<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
      if (unfiltered instanceof SortedMap) {
         return filterEntries((SortedMap)unfiltered, entryPredicate);
      } else if (unfiltered instanceof BiMap) {
         return filterEntries((BiMap)unfiltered, entryPredicate);
      } else {
         Preconditions.checkNotNull(entryPredicate);
         return (Map)(unfiltered instanceof Maps.AbstractFilteredMap ? filterFiltered((Maps.AbstractFilteredMap)unfiltered, entryPredicate) : new Maps.FilteredEntryMap((Map)Preconditions.checkNotNull(unfiltered), entryPredicate));
      }
   }

   @CheckReturnValue
   public static <K, V> SortedMap<K, V> filterEntries(SortedMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
      return Platform.mapsFilterSortedMap(unfiltered, entryPredicate);
   }

   static <K, V> SortedMap<K, V> filterSortedIgnoreNavigable(SortedMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
      Preconditions.checkNotNull(entryPredicate);
      return (SortedMap)(unfiltered instanceof Maps.FilteredEntrySortedMap ? filterFiltered((Maps.FilteredEntrySortedMap)unfiltered, entryPredicate) : new Maps.FilteredEntrySortedMap((SortedMap)Preconditions.checkNotNull(unfiltered), entryPredicate));
   }

   @CheckReturnValue
   @GwtIncompatible("NavigableMap")
   public static <K, V> NavigableMap<K, V> filterEntries(NavigableMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
      Preconditions.checkNotNull(entryPredicate);
      return (NavigableMap)(unfiltered instanceof Maps.FilteredEntryNavigableMap ? filterFiltered((Maps.FilteredEntryNavigableMap)unfiltered, entryPredicate) : new Maps.FilteredEntryNavigableMap((NavigableMap)Preconditions.checkNotNull(unfiltered), entryPredicate));
   }

   @CheckReturnValue
   public static <K, V> BiMap<K, V> filterEntries(BiMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
      Preconditions.checkNotNull(unfiltered);
      Preconditions.checkNotNull(entryPredicate);
      return (BiMap)(unfiltered instanceof Maps.FilteredEntryBiMap ? filterFiltered((Maps.FilteredEntryBiMap)unfiltered, entryPredicate) : new Maps.FilteredEntryBiMap(unfiltered, entryPredicate));
   }

   private static <K, V> Map<K, V> filterFiltered(Maps.AbstractFilteredMap<K, V> map, Predicate<? super Entry<K, V>> entryPredicate) {
      return new Maps.FilteredEntryMap(map.unfiltered, Predicates.and(map.predicate, entryPredicate));
   }

   private static <K, V> SortedMap<K, V> filterFiltered(Maps.FilteredEntrySortedMap<K, V> map, Predicate<? super Entry<K, V>> entryPredicate) {
      Predicate<Entry<K, V>> predicate = Predicates.and(map.predicate, entryPredicate);
      return new Maps.FilteredEntrySortedMap(map.sortedMap(), predicate);
   }

   @GwtIncompatible("NavigableMap")
   private static <K, V> NavigableMap<K, V> filterFiltered(Maps.FilteredEntryNavigableMap<K, V> map, Predicate<? super Entry<K, V>> entryPredicate) {
      Predicate<Entry<K, V>> predicate = Predicates.and(map.entryPredicate, entryPredicate);
      return new Maps.FilteredEntryNavigableMap(map.unfiltered, predicate);
   }

   private static <K, V> BiMap<K, V> filterFiltered(Maps.FilteredEntryBiMap<K, V> map, Predicate<? super Entry<K, V>> entryPredicate) {
      Predicate<Entry<K, V>> predicate = Predicates.and(map.predicate, entryPredicate);
      return new Maps.FilteredEntryBiMap(map.unfiltered(), predicate);
   }

   @GwtIncompatible("NavigableMap")
   public static <K, V> NavigableMap<K, V> unmodifiableNavigableMap(NavigableMap<K, V> map) {
      Preconditions.checkNotNull(map);
      return (NavigableMap)(map instanceof Maps.UnmodifiableNavigableMap ? map : new Maps.UnmodifiableNavigableMap(map));
   }

   @Nullable
   private static <K, V> Entry<K, V> unmodifiableOrNull(@Nullable Entry<K, V> entry) {
      return entry == null ? null : unmodifiableEntry(entry);
   }

   @GwtIncompatible("NavigableMap")
   public static <K, V> NavigableMap<K, V> synchronizedNavigableMap(NavigableMap<K, V> navigableMap) {
      return Synchronized.navigableMap(navigableMap);
   }

   static <V> V safeGet(Map<?, V> map, @Nullable Object key) {
      Preconditions.checkNotNull(map);

      try {
         return map.get(key);
      } catch (ClassCastException var3) {
         return null;
      } catch (NullPointerException var4) {
         return null;
      }
   }

   static boolean safeContainsKey(Map<?, ?> map, Object key) {
      Preconditions.checkNotNull(map);

      try {
         return map.containsKey(key);
      } catch (ClassCastException var3) {
         return false;
      } catch (NullPointerException var4) {
         return false;
      }
   }

   static <V> V safeRemove(Map<?, V> map, Object key) {
      Preconditions.checkNotNull(map);

      try {
         return map.remove(key);
      } catch (ClassCastException var3) {
         return null;
      } catch (NullPointerException var4) {
         return null;
      }
   }

   static boolean containsKeyImpl(Map<?, ?> map, @Nullable Object key) {
      return Iterators.contains(keyIterator(map.entrySet().iterator()), key);
   }

   static boolean containsValueImpl(Map<?, ?> map, @Nullable Object value) {
      return Iterators.contains(valueIterator(map.entrySet().iterator()), value);
   }

   static <K, V> boolean containsEntryImpl(Collection<Entry<K, V>> c, Object o) {
      return !(o instanceof Entry) ? false : c.contains(unmodifiableEntry((Entry)o));
   }

   static <K, V> boolean removeEntryImpl(Collection<Entry<K, V>> c, Object o) {
      return !(o instanceof Entry) ? false : c.remove(unmodifiableEntry((Entry)o));
   }

   static boolean equalsImpl(Map<?, ?> map, Object object) {
      if (map == object) {
         return true;
      } else if (object instanceof Map) {
         Map<?, ?> o = (Map)object;
         return map.entrySet().equals(o.entrySet());
      } else {
         return false;
      }
   }

   static String toStringImpl(Map<?, ?> map) {
      StringBuilder sb = Collections2.newStringBuilderForCollection(map.size()).append('{');
      STANDARD_JOINER.appendTo(sb, map);
      return sb.append('}').toString();
   }

   static <K, V> void putAllImpl(Map<K, V> self, Map<? extends K, ? extends V> map) {
      Iterator i$ = map.entrySet().iterator();

      while(i$.hasNext()) {
         Entry<? extends K, ? extends V> entry = (Entry)i$.next();
         self.put(entry.getKey(), entry.getValue());
      }

   }

   @Nullable
   static <K> K keyOrNull(@Nullable Entry<K, ?> entry) {
      return entry == null ? null : entry.getKey();
   }

   @Nullable
   static <V> V valueOrNull(@Nullable Entry<?, V> entry) {
      return entry == null ? null : entry.getValue();
   }

   static <E> ImmutableMap<E, Integer> indexMap(Collection<E> list) {
      ImmutableMap.Builder<E, Integer> builder = new ImmutableMap.Builder(list.size());
      int i = 0;
      Iterator i$ = list.iterator();

      while(i$.hasNext()) {
         E e = i$.next();
         builder.put(e, i++);
      }

      return builder.build();
   }

   static {
      STANDARD_JOINER = Collections2.STANDARD_JOINER.withKeyValueSeparator("=");
   }

   @GwtIncompatible("NavigableMap")
   abstract static class DescendingMap<K, V> extends ForwardingMap<K, V> implements NavigableMap<K, V> {
      private transient Comparator<? super K> comparator;
      private transient Set<Entry<K, V>> entrySet;
      private transient NavigableSet<K> navigableKeySet;

      abstract NavigableMap<K, V> forward();

      protected final Map<K, V> delegate() {
         return this.forward();
      }

      public Comparator<? super K> comparator() {
         Comparator<? super K> result = this.comparator;
         if (result == null) {
            Comparator<? super K> forwardCmp = this.forward().comparator();
            if (forwardCmp == null) {
               forwardCmp = Ordering.natural();
            }

            result = this.comparator = reverse((Comparator)forwardCmp);
         }

         return result;
      }

      private static <T> Ordering<T> reverse(Comparator<T> forward) {
         return Ordering.from(forward).reverse();
      }

      public K firstKey() {
         return this.forward().lastKey();
      }

      public K lastKey() {
         return this.forward().firstKey();
      }

      public Entry<K, V> lowerEntry(K key) {
         return this.forward().higherEntry(key);
      }

      public K lowerKey(K key) {
         return this.forward().higherKey(key);
      }

      public Entry<K, V> floorEntry(K key) {
         return this.forward().ceilingEntry(key);
      }

      public K floorKey(K key) {
         return this.forward().ceilingKey(key);
      }

      public Entry<K, V> ceilingEntry(K key) {
         return this.forward().floorEntry(key);
      }

      public K ceilingKey(K key) {
         return this.forward().floorKey(key);
      }

      public Entry<K, V> higherEntry(K key) {
         return this.forward().lowerEntry(key);
      }

      public K higherKey(K key) {
         return this.forward().lowerKey(key);
      }

      public Entry<K, V> firstEntry() {
         return this.forward().lastEntry();
      }

      public Entry<K, V> lastEntry() {
         return this.forward().firstEntry();
      }

      public Entry<K, V> pollFirstEntry() {
         return this.forward().pollLastEntry();
      }

      public Entry<K, V> pollLastEntry() {
         return this.forward().pollFirstEntry();
      }

      public NavigableMap<K, V> descendingMap() {
         return this.forward();
      }

      public Set<Entry<K, V>> entrySet() {
         Set<Entry<K, V>> result = this.entrySet;
         return result == null ? (this.entrySet = this.createEntrySet()) : result;
      }

      abstract Iterator<Entry<K, V>> entryIterator();

      Set<Entry<K, V>> createEntrySet() {
         class EntrySetImpl extends Maps.EntrySet<K, V> {
            Map<K, V> map() {
               return DescendingMap.this;
            }

            public Iterator<Entry<K, V>> iterator() {
               return DescendingMap.this.entryIterator();
            }
         }

         return new EntrySetImpl();
      }

      public Set<K> keySet() {
         return this.navigableKeySet();
      }

      public NavigableSet<K> navigableKeySet() {
         NavigableSet<K> result = this.navigableKeySet;
         return result == null ? (this.navigableKeySet = new Maps.NavigableKeySet(this)) : result;
      }

      public NavigableSet<K> descendingKeySet() {
         return this.forward().navigableKeySet();
      }

      public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
         return this.forward().subMap(toKey, toInclusive, fromKey, fromInclusive).descendingMap();
      }

      public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
         return this.forward().tailMap(toKey, inclusive).descendingMap();
      }

      public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
         return this.forward().headMap(fromKey, inclusive).descendingMap();
      }

      public SortedMap<K, V> subMap(K fromKey, K toKey) {
         return this.subMap(fromKey, true, toKey, false);
      }

      public SortedMap<K, V> headMap(K toKey) {
         return this.headMap(toKey, false);
      }

      public SortedMap<K, V> tailMap(K fromKey) {
         return this.tailMap(fromKey, true);
      }

      public Collection<V> values() {
         return new Maps.Values(this);
      }

      public String toString() {
         return this.standardToString();
      }
   }

   abstract static class EntrySet<K, V> extends Sets.ImprovedAbstractSet<Entry<K, V>> {
      abstract Map<K, V> map();

      public int size() {
         return this.map().size();
      }

      public void clear() {
         this.map().clear();
      }

      public boolean contains(Object o) {
         if (!(o instanceof Entry)) {
            return false;
         } else {
            Entry<?, ?> entry = (Entry)o;
            Object key = entry.getKey();
            V value = Maps.safeGet(this.map(), key);
            return Objects.equal(value, entry.getValue()) && (value != null || this.map().containsKey(key));
         }
      }

      public boolean isEmpty() {
         return this.map().isEmpty();
      }

      public boolean remove(Object o) {
         if (this.contains(o)) {
            Entry<?, ?> entry = (Entry)o;
            return this.map().keySet().remove(entry.getKey());
         } else {
            return false;
         }
      }

      public boolean removeAll(Collection<?> c) {
         try {
            return super.removeAll((Collection)Preconditions.checkNotNull(c));
         } catch (UnsupportedOperationException var3) {
            return Sets.removeAllImpl(this, (Iterator)c.iterator());
         }
      }

      public boolean retainAll(Collection<?> c) {
         try {
            return super.retainAll((Collection)Preconditions.checkNotNull(c));
         } catch (UnsupportedOperationException var7) {
            Set<Object> keys = Sets.newHashSetWithExpectedSize(c.size());
            Iterator i$ = c.iterator();

            while(i$.hasNext()) {
               Object o = i$.next();
               if (this.contains(o)) {
                  Entry<?, ?> entry = (Entry)o;
                  keys.add(entry.getKey());
               }
            }

            return this.map().keySet().retainAll(keys);
         }
      }
   }

   static class Values<K, V> extends AbstractCollection<V> {
      @Weak
      final Map<K, V> map;

      Values(Map<K, V> map) {
         this.map = (Map)Preconditions.checkNotNull(map);
      }

      final Map<K, V> map() {
         return this.map;
      }

      public Iterator<V> iterator() {
         return Maps.valueIterator(this.map().entrySet().iterator());
      }

      public boolean remove(Object o) {
         try {
            return super.remove(o);
         } catch (UnsupportedOperationException var5) {
            Iterator i$ = this.map().entrySet().iterator();

            Entry entry;
            do {
               if (!i$.hasNext()) {
                  return false;
               }

               entry = (Entry)i$.next();
            } while(!Objects.equal(o, entry.getValue()));

            this.map().remove(entry.getKey());
            return true;
         }
      }

      public boolean removeAll(Collection<?> c) {
         try {
            return super.removeAll((Collection)Preconditions.checkNotNull(c));
         } catch (UnsupportedOperationException var6) {
            Set<K> toRemove = Sets.newHashSet();
            Iterator i$ = this.map().entrySet().iterator();

            while(i$.hasNext()) {
               Entry<K, V> entry = (Entry)i$.next();
               if (c.contains(entry.getValue())) {
                  toRemove.add(entry.getKey());
               }
            }

            return this.map().keySet().removeAll(toRemove);
         }
      }

      public boolean retainAll(Collection<?> c) {
         try {
            return super.retainAll((Collection)Preconditions.checkNotNull(c));
         } catch (UnsupportedOperationException var6) {
            Set<K> toRetain = Sets.newHashSet();
            Iterator i$ = this.map().entrySet().iterator();

            while(i$.hasNext()) {
               Entry<K, V> entry = (Entry)i$.next();
               if (c.contains(entry.getValue())) {
                  toRetain.add(entry.getKey());
               }
            }

            return this.map().keySet().retainAll(toRetain);
         }
      }

      public int size() {
         return this.map().size();
      }

      public boolean isEmpty() {
         return this.map().isEmpty();
      }

      public boolean contains(@Nullable Object o) {
         return this.map().containsValue(o);
      }

      public void clear() {
         this.map().clear();
      }
   }

   @GwtIncompatible("NavigableMap")
   static class NavigableKeySet<K, V> extends Maps.SortedKeySet<K, V> implements NavigableSet<K> {
      NavigableKeySet(NavigableMap<K, V> map) {
         super(map);
      }

      NavigableMap<K, V> map() {
         return (NavigableMap)this.map;
      }

      public K lower(K e) {
         return this.map().lowerKey(e);
      }

      public K floor(K e) {
         return this.map().floorKey(e);
      }

      public K ceiling(K e) {
         return this.map().ceilingKey(e);
      }

      public K higher(K e) {
         return this.map().higherKey(e);
      }

      public K pollFirst() {
         return Maps.keyOrNull(this.map().pollFirstEntry());
      }

      public K pollLast() {
         return Maps.keyOrNull(this.map().pollLastEntry());
      }

      public NavigableSet<K> descendingSet() {
         return this.map().descendingKeySet();
      }

      public Iterator<K> descendingIterator() {
         return this.descendingSet().iterator();
      }

      public NavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement, boolean toInclusive) {
         return this.map().subMap(fromElement, fromInclusive, toElement, toInclusive).navigableKeySet();
      }

      public NavigableSet<K> headSet(K toElement, boolean inclusive) {
         return this.map().headMap(toElement, inclusive).navigableKeySet();
      }

      public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
         return this.map().tailMap(fromElement, inclusive).navigableKeySet();
      }

      public SortedSet<K> subSet(K fromElement, K toElement) {
         return this.subSet(fromElement, true, toElement, false);
      }

      public SortedSet<K> headSet(K toElement) {
         return this.headSet(toElement, false);
      }

      public SortedSet<K> tailSet(K fromElement) {
         return this.tailSet(fromElement, true);
      }
   }

   static class SortedKeySet<K, V> extends Maps.KeySet<K, V> implements SortedSet<K> {
      SortedKeySet(SortedMap<K, V> map) {
         super(map);
      }

      SortedMap<K, V> map() {
         return (SortedMap)super.map();
      }

      public Comparator<? super K> comparator() {
         return this.map().comparator();
      }

      public SortedSet<K> subSet(K fromElement, K toElement) {
         return new Maps.SortedKeySet(this.map().subMap(fromElement, toElement));
      }

      public SortedSet<K> headSet(K toElement) {
         return new Maps.SortedKeySet(this.map().headMap(toElement));
      }

      public SortedSet<K> tailSet(K fromElement) {
         return new Maps.SortedKeySet(this.map().tailMap(fromElement));
      }

      public K first() {
         return this.map().firstKey();
      }

      public K last() {
         return this.map().lastKey();
      }
   }

   static class KeySet<K, V> extends Sets.ImprovedAbstractSet<K> {
      @Weak
      final Map<K, V> map;

      KeySet(Map<K, V> map) {
         this.map = (Map)Preconditions.checkNotNull(map);
      }

      Map<K, V> map() {
         return this.map;
      }

      public Iterator<K> iterator() {
         return Maps.keyIterator(this.map().entrySet().iterator());
      }

      public int size() {
         return this.map().size();
      }

      public boolean isEmpty() {
         return this.map().isEmpty();
      }

      public boolean contains(Object o) {
         return this.map().containsKey(o);
      }

      public boolean remove(Object o) {
         if (this.contains(o)) {
            this.map().remove(o);
            return true;
         } else {
            return false;
         }
      }

      public void clear() {
         this.map().clear();
      }
   }

   abstract static class IteratorBasedAbstractMap<K, V> extends AbstractMap<K, V> {
      public abstract int size();

      abstract Iterator<Entry<K, V>> entryIterator();

      public Set<Entry<K, V>> entrySet() {
         return new Maps.EntrySet<K, V>() {
            Map<K, V> map() {
               return IteratorBasedAbstractMap.this;
            }

            public Iterator<Entry<K, V>> iterator() {
               return IteratorBasedAbstractMap.this.entryIterator();
            }
         };
      }

      public void clear() {
         Iterators.clear(this.entryIterator());
      }
   }

   @GwtCompatible
   abstract static class ViewCachingAbstractMap<K, V> extends AbstractMap<K, V> {
      private transient Set<Entry<K, V>> entrySet;
      private transient Set<K> keySet;
      private transient Collection<V> values;

      abstract Set<Entry<K, V>> createEntrySet();

      public Set<Entry<K, V>> entrySet() {
         Set<Entry<K, V>> result = this.entrySet;
         return result == null ? (this.entrySet = this.createEntrySet()) : result;
      }

      public Set<K> keySet() {
         Set<K> result = this.keySet;
         return result == null ? (this.keySet = this.createKeySet()) : result;
      }

      Set<K> createKeySet() {
         return new Maps.KeySet(this);
      }

      public Collection<V> values() {
         Collection<V> result = this.values;
         return result == null ? (this.values = this.createValues()) : result;
      }

      Collection<V> createValues() {
         return new Maps.Values(this);
      }
   }

   @GwtIncompatible("NavigableMap")
   static class UnmodifiableNavigableMap<K, V> extends ForwardingSortedMap<K, V> implements NavigableMap<K, V>, Serializable {
      private final NavigableMap<K, V> delegate;
      private transient Maps.UnmodifiableNavigableMap<K, V> descendingMap;

      UnmodifiableNavigableMap(NavigableMap<K, V> delegate) {
         this.delegate = delegate;
      }

      UnmodifiableNavigableMap(NavigableMap<K, V> delegate, Maps.UnmodifiableNavigableMap<K, V> descendingMap) {
         this.delegate = delegate;
         this.descendingMap = descendingMap;
      }

      protected SortedMap<K, V> delegate() {
         return Collections.unmodifiableSortedMap(this.delegate);
      }

      public Entry<K, V> lowerEntry(K key) {
         return Maps.unmodifiableOrNull(this.delegate.lowerEntry(key));
      }

      public K lowerKey(K key) {
         return this.delegate.lowerKey(key);
      }

      public Entry<K, V> floorEntry(K key) {
         return Maps.unmodifiableOrNull(this.delegate.floorEntry(key));
      }

      public K floorKey(K key) {
         return this.delegate.floorKey(key);
      }

      public Entry<K, V> ceilingEntry(K key) {
         return Maps.unmodifiableOrNull(this.delegate.ceilingEntry(key));
      }

      public K ceilingKey(K key) {
         return this.delegate.ceilingKey(key);
      }

      public Entry<K, V> higherEntry(K key) {
         return Maps.unmodifiableOrNull(this.delegate.higherEntry(key));
      }

      public K higherKey(K key) {
         return this.delegate.higherKey(key);
      }

      public Entry<K, V> firstEntry() {
         return Maps.unmodifiableOrNull(this.delegate.firstEntry());
      }

      public Entry<K, V> lastEntry() {
         return Maps.unmodifiableOrNull(this.delegate.lastEntry());
      }

      public final Entry<K, V> pollFirstEntry() {
         throw new UnsupportedOperationException();
      }

      public final Entry<K, V> pollLastEntry() {
         throw new UnsupportedOperationException();
      }

      public NavigableMap<K, V> descendingMap() {
         Maps.UnmodifiableNavigableMap<K, V> result = this.descendingMap;
         return result == null ? (this.descendingMap = new Maps.UnmodifiableNavigableMap(this.delegate.descendingMap(), this)) : result;
      }

      public Set<K> keySet() {
         return this.navigableKeySet();
      }

      public NavigableSet<K> navigableKeySet() {
         return Sets.unmodifiableNavigableSet(this.delegate.navigableKeySet());
      }

      public NavigableSet<K> descendingKeySet() {
         return Sets.unmodifiableNavigableSet(this.delegate.descendingKeySet());
      }

      public SortedMap<K, V> subMap(K fromKey, K toKey) {
         return this.subMap(fromKey, true, toKey, false);
      }

      public SortedMap<K, V> headMap(K toKey) {
         return this.headMap(toKey, false);
      }

      public SortedMap<K, V> tailMap(K fromKey) {
         return this.tailMap(fromKey, true);
      }

      public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
         return Maps.unmodifiableNavigableMap(this.delegate.subMap(fromKey, fromInclusive, toKey, toInclusive));
      }

      public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
         return Maps.unmodifiableNavigableMap(this.delegate.headMap(toKey, inclusive));
      }

      public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
         return Maps.unmodifiableNavigableMap(this.delegate.tailMap(fromKey, inclusive));
      }
   }

   static final class FilteredEntryBiMap<K, V> extends Maps.FilteredEntryMap<K, V> implements BiMap<K, V> {
      private final BiMap<V, K> inverse;

      private static <K, V> Predicate<Entry<V, K>> inversePredicate(final Predicate<? super Entry<K, V>> forwardPredicate) {
         return new Predicate<Entry<V, K>>() {
            public boolean apply(Entry<V, K> input) {
               return forwardPredicate.apply(Maps.immutableEntry(input.getValue(), input.getKey()));
            }
         };
      }

      FilteredEntryBiMap(BiMap<K, V> delegate, Predicate<? super Entry<K, V>> predicate) {
         super(delegate, predicate);
         this.inverse = new Maps.FilteredEntryBiMap(delegate.inverse(), inversePredicate(predicate), this);
      }

      private FilteredEntryBiMap(BiMap<K, V> delegate, Predicate<? super Entry<K, V>> predicate, BiMap<V, K> inverse) {
         super(delegate, predicate);
         this.inverse = inverse;
      }

      BiMap<K, V> unfiltered() {
         return (BiMap)this.unfiltered;
      }

      public V forcePut(@Nullable K key, @Nullable V value) {
         Preconditions.checkArgument(this.apply(key, value));
         return this.unfiltered().forcePut(key, value);
      }

      public BiMap<V, K> inverse() {
         return this.inverse;
      }

      public Set<V> values() {
         return this.inverse.keySet();
      }
   }

   @GwtIncompatible("NavigableMap")
   private static class FilteredEntryNavigableMap<K, V> extends AbstractNavigableMap<K, V> {
      private final NavigableMap<K, V> unfiltered;
      private final Predicate<? super Entry<K, V>> entryPredicate;
      private final Map<K, V> filteredDelegate;

      FilteredEntryNavigableMap(NavigableMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
         this.unfiltered = (NavigableMap)Preconditions.checkNotNull(unfiltered);
         this.entryPredicate = entryPredicate;
         this.filteredDelegate = new Maps.FilteredEntryMap(unfiltered, entryPredicate);
      }

      public Comparator<? super K> comparator() {
         return this.unfiltered.comparator();
      }

      public NavigableSet<K> navigableKeySet() {
         return new Maps.NavigableKeySet<K, V>(this) {
            public boolean removeAll(Collection<?> c) {
               return Iterators.removeIf(FilteredEntryNavigableMap.this.unfiltered.entrySet().iterator(), Predicates.and(FilteredEntryNavigableMap.this.entryPredicate, Maps.keyPredicateOnEntries(Predicates.in(c))));
            }

            public boolean retainAll(Collection<?> c) {
               return Iterators.removeIf(FilteredEntryNavigableMap.this.unfiltered.entrySet().iterator(), Predicates.and(FilteredEntryNavigableMap.this.entryPredicate, Maps.keyPredicateOnEntries(Predicates.not(Predicates.in(c)))));
            }
         };
      }

      public Collection<V> values() {
         return new Maps.FilteredMapValues(this, this.unfiltered, this.entryPredicate);
      }

      Iterator<Entry<K, V>> entryIterator() {
         return Iterators.filter(this.unfiltered.entrySet().iterator(), this.entryPredicate);
      }

      Iterator<Entry<K, V>> descendingEntryIterator() {
         return Iterators.filter(this.unfiltered.descendingMap().entrySet().iterator(), this.entryPredicate);
      }

      public int size() {
         return this.filteredDelegate.size();
      }

      public boolean isEmpty() {
         return !Iterables.any(this.unfiltered.entrySet(), this.entryPredicate);
      }

      @Nullable
      public V get(@Nullable Object key) {
         return this.filteredDelegate.get(key);
      }

      public boolean containsKey(@Nullable Object key) {
         return this.filteredDelegate.containsKey(key);
      }

      public V put(K key, V value) {
         return this.filteredDelegate.put(key, value);
      }

      public V remove(@Nullable Object key) {
         return this.filteredDelegate.remove(key);
      }

      public void putAll(Map<? extends K, ? extends V> m) {
         this.filteredDelegate.putAll(m);
      }

      public void clear() {
         this.filteredDelegate.clear();
      }

      public Set<Entry<K, V>> entrySet() {
         return this.filteredDelegate.entrySet();
      }

      public Entry<K, V> pollFirstEntry() {
         return (Entry)Iterables.removeFirstMatching(this.unfiltered.entrySet(), this.entryPredicate);
      }

      public Entry<K, V> pollLastEntry() {
         return (Entry)Iterables.removeFirstMatching(this.unfiltered.descendingMap().entrySet(), this.entryPredicate);
      }

      public NavigableMap<K, V> descendingMap() {
         return Maps.filterEntries(this.unfiltered.descendingMap(), this.entryPredicate);
      }

      public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
         return Maps.filterEntries(this.unfiltered.subMap(fromKey, fromInclusive, toKey, toInclusive), this.entryPredicate);
      }

      public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
         return Maps.filterEntries(this.unfiltered.headMap(toKey, inclusive), this.entryPredicate);
      }

      public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
         return Maps.filterEntries(this.unfiltered.tailMap(fromKey, inclusive), this.entryPredicate);
      }
   }

   private static class FilteredEntrySortedMap<K, V> extends Maps.FilteredEntryMap<K, V> implements SortedMap<K, V> {
      FilteredEntrySortedMap(SortedMap<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
         super(unfiltered, entryPredicate);
      }

      SortedMap<K, V> sortedMap() {
         return (SortedMap)this.unfiltered;
      }

      public SortedSet<K> keySet() {
         return (SortedSet)super.keySet();
      }

      SortedSet<K> createKeySet() {
         return new Maps.FilteredEntrySortedMap.SortedKeySet();
      }

      public Comparator<? super K> comparator() {
         return this.sortedMap().comparator();
      }

      public K firstKey() {
         return this.keySet().iterator().next();
      }

      public K lastKey() {
         SortedMap headMap = this.sortedMap();

         while(true) {
            K key = headMap.lastKey();
            if (this.apply(key, this.unfiltered.get(key))) {
               return key;
            }

            headMap = this.sortedMap().headMap(key);
         }
      }

      public SortedMap<K, V> headMap(K toKey) {
         return new Maps.FilteredEntrySortedMap(this.sortedMap().headMap(toKey), this.predicate);
      }

      public SortedMap<K, V> subMap(K fromKey, K toKey) {
         return new Maps.FilteredEntrySortedMap(this.sortedMap().subMap(fromKey, toKey), this.predicate);
      }

      public SortedMap<K, V> tailMap(K fromKey) {
         return new Maps.FilteredEntrySortedMap(this.sortedMap().tailMap(fromKey), this.predicate);
      }

      class SortedKeySet extends Maps.FilteredEntryMap<K, V>.KeySet implements SortedSet<K> {
         SortedKeySet() {
            super();
         }

         public Comparator<? super K> comparator() {
            return FilteredEntrySortedMap.this.sortedMap().comparator();
         }

         public SortedSet<K> subSet(K fromElement, K toElement) {
            return (SortedSet)FilteredEntrySortedMap.this.subMap(fromElement, toElement).keySet();
         }

         public SortedSet<K> headSet(K toElement) {
            return (SortedSet)FilteredEntrySortedMap.this.headMap(toElement).keySet();
         }

         public SortedSet<K> tailSet(K fromElement) {
            return (SortedSet)FilteredEntrySortedMap.this.tailMap(fromElement).keySet();
         }

         public K first() {
            return FilteredEntrySortedMap.this.firstKey();
         }

         public K last() {
            return FilteredEntrySortedMap.this.lastKey();
         }
      }
   }

   static class FilteredEntryMap<K, V> extends Maps.AbstractFilteredMap<K, V> {
      final Set<Entry<K, V>> filteredEntrySet;

      FilteredEntryMap(Map<K, V> unfiltered, Predicate<? super Entry<K, V>> entryPredicate) {
         super(unfiltered, entryPredicate);
         this.filteredEntrySet = Sets.filter(unfiltered.entrySet(), this.predicate);
      }

      protected Set<Entry<K, V>> createEntrySet() {
         return new Maps.FilteredEntryMap.EntrySet();
      }

      Set<K> createKeySet() {
         return new Maps.FilteredEntryMap.KeySet();
      }

      class KeySet extends Maps.KeySet<K, V> {
         KeySet() {
            super(FilteredEntryMap.this);
         }

         public boolean remove(Object o) {
            if (FilteredEntryMap.this.containsKey(o)) {
               FilteredEntryMap.this.unfiltered.remove(o);
               return true;
            } else {
               return false;
            }
         }

         private boolean removeIf(Predicate<? super K> keyPredicate) {
            return Iterables.removeIf(FilteredEntryMap.this.unfiltered.entrySet(), Predicates.and(FilteredEntryMap.this.predicate, Maps.keyPredicateOnEntries(keyPredicate)));
         }

         public boolean removeAll(Collection<?> c) {
            return this.removeIf(Predicates.in(c));
         }

         public boolean retainAll(Collection<?> c) {
            return this.removeIf(Predicates.not(Predicates.in(c)));
         }

         public Object[] toArray() {
            return Lists.newArrayList(this.iterator()).toArray();
         }

         public <T> T[] toArray(T[] array) {
            return Lists.newArrayList(this.iterator()).toArray(array);
         }
      }

      private class EntrySet extends ForwardingSet<Entry<K, V>> {
         private EntrySet() {
         }

         protected Set<Entry<K, V>> delegate() {
            return FilteredEntryMap.this.filteredEntrySet;
         }

         public Iterator<Entry<K, V>> iterator() {
            return new TransformedIterator<Entry<K, V>, Entry<K, V>>(FilteredEntryMap.this.filteredEntrySet.iterator()) {
               Entry<K, V> transform(final Entry<K, V> entry) {
                  return new ForwardingMapEntry<K, V>() {
                     protected Entry<K, V> delegate() {
                        return entry;
                     }

                     public V setValue(V newValue) {
                        Preconditions.checkArgument(FilteredEntryMap.this.apply(this.getKey(), newValue));
                        return super.setValue(newValue);
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

   private static class FilteredKeyMap<K, V> extends Maps.AbstractFilteredMap<K, V> {
      Predicate<? super K> keyPredicate;

      FilteredKeyMap(Map<K, V> unfiltered, Predicate<? super K> keyPredicate, Predicate<? super Entry<K, V>> entryPredicate) {
         super(unfiltered, entryPredicate);
         this.keyPredicate = keyPredicate;
      }

      protected Set<Entry<K, V>> createEntrySet() {
         return Sets.filter(this.unfiltered.entrySet(), this.predicate);
      }

      Set<K> createKeySet() {
         return Sets.filter(this.unfiltered.keySet(), this.keyPredicate);
      }

      public boolean containsKey(Object key) {
         return this.unfiltered.containsKey(key) && this.keyPredicate.apply(key);
      }
   }

   private static final class FilteredMapValues<K, V> extends Maps.Values<K, V> {
      Map<K, V> unfiltered;
      Predicate<? super Entry<K, V>> predicate;

      FilteredMapValues(Map<K, V> filteredMap, Map<K, V> unfiltered, Predicate<? super Entry<K, V>> predicate) {
         super(filteredMap);
         this.unfiltered = unfiltered;
         this.predicate = predicate;
      }

      public boolean remove(Object o) {
         return Iterables.removeFirstMatching(this.unfiltered.entrySet(), Predicates.and(this.predicate, Maps.valuePredicateOnEntries(Predicates.equalTo(o)))) != null;
      }

      private boolean removeIf(Predicate<? super V> valuePredicate) {
         return Iterables.removeIf(this.unfiltered.entrySet(), Predicates.and(this.predicate, Maps.valuePredicateOnEntries(valuePredicate)));
      }

      public boolean removeAll(Collection<?> collection) {
         return this.removeIf(Predicates.in(collection));
      }

      public boolean retainAll(Collection<?> collection) {
         return this.removeIf(Predicates.not(Predicates.in(collection)));
      }

      public Object[] toArray() {
         return Lists.newArrayList(this.iterator()).toArray();
      }

      public <T> T[] toArray(T[] array) {
         return Lists.newArrayList(this.iterator()).toArray(array);
      }
   }

   private abstract static class AbstractFilteredMap<K, V> extends Maps.ViewCachingAbstractMap<K, V> {
      final Map<K, V> unfiltered;
      final Predicate<? super Entry<K, V>> predicate;

      AbstractFilteredMap(Map<K, V> unfiltered, Predicate<? super Entry<K, V>> predicate) {
         this.unfiltered = unfiltered;
         this.predicate = predicate;
      }

      boolean apply(@Nullable Object key, @Nullable V value) {
         return this.predicate.apply(Maps.immutableEntry(key, value));
      }

      public V put(K key, V value) {
         Preconditions.checkArgument(this.apply(key, value));
         return this.unfiltered.put(key, value);
      }

      public void putAll(Map<? extends K, ? extends V> map) {
         Iterator i$ = map.entrySet().iterator();

         while(i$.hasNext()) {
            Entry<? extends K, ? extends V> entry = (Entry)i$.next();
            Preconditions.checkArgument(this.apply(entry.getKey(), entry.getValue()));
         }

         this.unfiltered.putAll(map);
      }

      public boolean containsKey(Object key) {
         return this.unfiltered.containsKey(key) && this.apply(key, this.unfiltered.get(key));
      }

      public V get(Object key) {
         V value = this.unfiltered.get(key);
         return value != null && this.apply(key, value) ? value : null;
      }

      public boolean isEmpty() {
         return this.entrySet().isEmpty();
      }

      public V remove(Object key) {
         return this.containsKey(key) ? this.unfiltered.remove(key) : null;
      }

      Collection<V> createValues() {
         return new Maps.FilteredMapValues(this, this.unfiltered, this.predicate);
      }
   }

   @GwtIncompatible("NavigableMap")
   private static class TransformedEntriesNavigableMap<K, V1, V2> extends Maps.TransformedEntriesSortedMap<K, V1, V2> implements NavigableMap<K, V2> {
      TransformedEntriesNavigableMap(NavigableMap<K, V1> fromMap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer) {
         super(fromMap, transformer);
      }

      public Entry<K, V2> ceilingEntry(K key) {
         return this.transformEntry(this.fromMap().ceilingEntry(key));
      }

      public K ceilingKey(K key) {
         return this.fromMap().ceilingKey(key);
      }

      public NavigableSet<K> descendingKeySet() {
         return this.fromMap().descendingKeySet();
      }

      public NavigableMap<K, V2> descendingMap() {
         return Maps.transformEntries(this.fromMap().descendingMap(), this.transformer);
      }

      public Entry<K, V2> firstEntry() {
         return this.transformEntry(this.fromMap().firstEntry());
      }

      public Entry<K, V2> floorEntry(K key) {
         return this.transformEntry(this.fromMap().floorEntry(key));
      }

      public K floorKey(K key) {
         return this.fromMap().floorKey(key);
      }

      public NavigableMap<K, V2> headMap(K toKey) {
         return this.headMap(toKey, false);
      }

      public NavigableMap<K, V2> headMap(K toKey, boolean inclusive) {
         return Maps.transformEntries(this.fromMap().headMap(toKey, inclusive), this.transformer);
      }

      public Entry<K, V2> higherEntry(K key) {
         return this.transformEntry(this.fromMap().higherEntry(key));
      }

      public K higherKey(K key) {
         return this.fromMap().higherKey(key);
      }

      public Entry<K, V2> lastEntry() {
         return this.transformEntry(this.fromMap().lastEntry());
      }

      public Entry<K, V2> lowerEntry(K key) {
         return this.transformEntry(this.fromMap().lowerEntry(key));
      }

      public K lowerKey(K key) {
         return this.fromMap().lowerKey(key);
      }

      public NavigableSet<K> navigableKeySet() {
         return this.fromMap().navigableKeySet();
      }

      public Entry<K, V2> pollFirstEntry() {
         return this.transformEntry(this.fromMap().pollFirstEntry());
      }

      public Entry<K, V2> pollLastEntry() {
         return this.transformEntry(this.fromMap().pollLastEntry());
      }

      public NavigableMap<K, V2> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
         return Maps.transformEntries(this.fromMap().subMap(fromKey, fromInclusive, toKey, toInclusive), this.transformer);
      }

      public NavigableMap<K, V2> subMap(K fromKey, K toKey) {
         return this.subMap(fromKey, true, toKey, false);
      }

      public NavigableMap<K, V2> tailMap(K fromKey) {
         return this.tailMap(fromKey, true);
      }

      public NavigableMap<K, V2> tailMap(K fromKey, boolean inclusive) {
         return Maps.transformEntries(this.fromMap().tailMap(fromKey, inclusive), this.transformer);
      }

      @Nullable
      private Entry<K, V2> transformEntry(@Nullable Entry<K, V1> entry) {
         return entry == null ? null : Maps.transformEntry(this.transformer, entry);
      }

      protected NavigableMap<K, V1> fromMap() {
         return (NavigableMap)super.fromMap();
      }
   }

   static class TransformedEntriesSortedMap<K, V1, V2> extends Maps.TransformedEntriesMap<K, V1, V2> implements SortedMap<K, V2> {
      protected SortedMap<K, V1> fromMap() {
         return (SortedMap)this.fromMap;
      }

      TransformedEntriesSortedMap(SortedMap<K, V1> fromMap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer) {
         super(fromMap, transformer);
      }

      public Comparator<? super K> comparator() {
         return this.fromMap().comparator();
      }

      public K firstKey() {
         return this.fromMap().firstKey();
      }

      public SortedMap<K, V2> headMap(K toKey) {
         return Maps.transformEntries(this.fromMap().headMap(toKey), this.transformer);
      }

      public K lastKey() {
         return this.fromMap().lastKey();
      }

      public SortedMap<K, V2> subMap(K fromKey, K toKey) {
         return Maps.transformEntries(this.fromMap().subMap(fromKey, toKey), this.transformer);
      }

      public SortedMap<K, V2> tailMap(K fromKey) {
         return Maps.transformEntries(this.fromMap().tailMap(fromKey), this.transformer);
      }
   }

   static class TransformedEntriesMap<K, V1, V2> extends Maps.IteratorBasedAbstractMap<K, V2> {
      final Map<K, V1> fromMap;
      final Maps.EntryTransformer<? super K, ? super V1, V2> transformer;

      TransformedEntriesMap(Map<K, V1> fromMap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer) {
         this.fromMap = (Map)Preconditions.checkNotNull(fromMap);
         this.transformer = (Maps.EntryTransformer)Preconditions.checkNotNull(transformer);
      }

      public int size() {
         return this.fromMap.size();
      }

      public boolean containsKey(Object key) {
         return this.fromMap.containsKey(key);
      }

      public V2 get(Object key) {
         V1 value = this.fromMap.get(key);
         return value == null && !this.fromMap.containsKey(key) ? null : this.transformer.transformEntry(key, value);
      }

      public V2 remove(Object key) {
         return this.fromMap.containsKey(key) ? this.transformer.transformEntry(key, this.fromMap.remove(key)) : null;
      }

      public void clear() {
         this.fromMap.clear();
      }

      public Set<K> keySet() {
         return this.fromMap.keySet();
      }

      Iterator<Entry<K, V2>> entryIterator() {
         return Iterators.transform(this.fromMap.entrySet().iterator(), Maps.asEntryToEntryFunction(this.transformer));
      }

      public Collection<V2> values() {
         return new Maps.Values(this);
      }
   }

   public interface EntryTransformer<K, V1, V2> {
      V2 transformEntry(@Nullable K var1, @Nullable V1 var2);
   }

   private static class UnmodifiableBiMap<K, V> extends ForwardingMap<K, V> implements BiMap<K, V>, Serializable {
      final Map<K, V> unmodifiableMap;
      final BiMap<? extends K, ? extends V> delegate;
      BiMap<V, K> inverse;
      transient Set<V> values;
      private static final long serialVersionUID = 0L;

      UnmodifiableBiMap(BiMap<? extends K, ? extends V> delegate, @Nullable BiMap<V, K> inverse) {
         this.unmodifiableMap = Collections.unmodifiableMap(delegate);
         this.delegate = delegate;
         this.inverse = inverse;
      }

      protected Map<K, V> delegate() {
         return this.unmodifiableMap;
      }

      public V forcePut(K key, V value) {
         throw new UnsupportedOperationException();
      }

      public BiMap<V, K> inverse() {
         BiMap<V, K> result = this.inverse;
         return result == null ? (this.inverse = new Maps.UnmodifiableBiMap(this.delegate.inverse(), this)) : result;
      }

      public Set<V> values() {
         Set<V> result = this.values;
         return result == null ? (this.values = Collections.unmodifiableSet(this.delegate.values())) : result;
      }
   }

   private static final class BiMapConverter<A, B> extends Converter<A, B> implements Serializable {
      private final BiMap<A, B> bimap;
      private static final long serialVersionUID = 0L;

      BiMapConverter(BiMap<A, B> bimap) {
         this.bimap = (BiMap)Preconditions.checkNotNull(bimap);
      }

      protected B doForward(A a) {
         return convert(this.bimap, a);
      }

      protected A doBackward(B b) {
         return convert(this.bimap.inverse(), b);
      }

      private static <X, Y> Y convert(BiMap<X, Y> bimap, X input) {
         Y output = bimap.get(input);
         Preconditions.checkArgument(output != null, "No non-null mapping present for input: %s", input);
         return output;
      }

      public boolean equals(@Nullable Object object) {
         if (object instanceof Maps.BiMapConverter) {
            Maps.BiMapConverter<?, ?> that = (Maps.BiMapConverter)object;
            return this.bimap.equals(that.bimap);
         } else {
            return false;
         }
      }

      public int hashCode() {
         return this.bimap.hashCode();
      }

      public String toString() {
         return "Maps.asConverter(" + this.bimap + ")";
      }
   }

   static class UnmodifiableEntrySet<K, V> extends Maps.UnmodifiableEntries<K, V> implements Set<Entry<K, V>> {
      UnmodifiableEntrySet(Set<Entry<K, V>> entries) {
         super(entries);
      }

      public boolean equals(@Nullable Object object) {
         return Sets.equalsImpl(this, object);
      }

      public int hashCode() {
         return Sets.hashCodeImpl(this);
      }
   }

   static class UnmodifiableEntries<K, V> extends ForwardingCollection<Entry<K, V>> {
      private final Collection<Entry<K, V>> entries;

      UnmodifiableEntries(Collection<Entry<K, V>> entries) {
         this.entries = entries;
      }

      protected Collection<Entry<K, V>> delegate() {
         return this.entries;
      }

      public Iterator<Entry<K, V>> iterator() {
         return Maps.unmodifiableEntryIterator(this.entries.iterator());
      }

      public Object[] toArray() {
         return this.standardToArray();
      }

      public <T> T[] toArray(T[] array) {
         return this.standardToArray(array);
      }
   }

   @GwtIncompatible("NavigableMap")
   private static final class NavigableAsMapView<K, V> extends AbstractNavigableMap<K, V> {
      private final NavigableSet<K> set;
      private final Function<? super K, V> function;

      NavigableAsMapView(NavigableSet<K> ks, Function<? super K, V> vFunction) {
         this.set = (NavigableSet)Preconditions.checkNotNull(ks);
         this.function = (Function)Preconditions.checkNotNull(vFunction);
      }

      public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
         return Maps.asMap(this.set.subSet(fromKey, fromInclusive, toKey, toInclusive), this.function);
      }

      public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
         return Maps.asMap(this.set.headSet(toKey, inclusive), this.function);
      }

      public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
         return Maps.asMap(this.set.tailSet(fromKey, inclusive), this.function);
      }

      public Comparator<? super K> comparator() {
         return this.set.comparator();
      }

      @Nullable
      public V get(@Nullable Object key) {
         return Collections2.safeContains(this.set, key) ? this.function.apply(key) : null;
      }

      public void clear() {
         this.set.clear();
      }

      Iterator<Entry<K, V>> entryIterator() {
         return Maps.asMapEntryIterator(this.set, this.function);
      }

      Iterator<Entry<K, V>> descendingEntryIterator() {
         return this.descendingMap().entrySet().iterator();
      }

      public NavigableSet<K> navigableKeySet() {
         return Maps.removeOnlyNavigableSet(this.set);
      }

      public int size() {
         return this.set.size();
      }

      public NavigableMap<K, V> descendingMap() {
         return Maps.asMap(this.set.descendingSet(), this.function);
      }
   }

   private static class SortedAsMapView<K, V> extends Maps.AsMapView<K, V> implements SortedMap<K, V> {
      SortedAsMapView(SortedSet<K> set, Function<? super K, V> function) {
         super(set, function);
      }

      SortedSet<K> backingSet() {
         return (SortedSet)super.backingSet();
      }

      public Comparator<? super K> comparator() {
         return this.backingSet().comparator();
      }

      public Set<K> keySet() {
         return Maps.removeOnlySortedSet(this.backingSet());
      }

      public SortedMap<K, V> subMap(K fromKey, K toKey) {
         return Maps.asMap(this.backingSet().subSet(fromKey, toKey), this.function);
      }

      public SortedMap<K, V> headMap(K toKey) {
         return Maps.asMap(this.backingSet().headSet(toKey), this.function);
      }

      public SortedMap<K, V> tailMap(K fromKey) {
         return Maps.asMap(this.backingSet().tailSet(fromKey), this.function);
      }

      public K firstKey() {
         return this.backingSet().first();
      }

      public K lastKey() {
         return this.backingSet().last();
      }
   }

   private static class AsMapView<K, V> extends Maps.ViewCachingAbstractMap<K, V> {
      private final Set<K> set;
      final Function<? super K, V> function;

      Set<K> backingSet() {
         return this.set;
      }

      AsMapView(Set<K> set, Function<? super K, V> function) {
         this.set = (Set)Preconditions.checkNotNull(set);
         this.function = (Function)Preconditions.checkNotNull(function);
      }

      public Set<K> createKeySet() {
         return Maps.removeOnlySet(this.backingSet());
      }

      Collection<V> createValues() {
         return Collections2.transform(this.set, this.function);
      }

      public int size() {
         return this.backingSet().size();
      }

      public boolean containsKey(@Nullable Object key) {
         return this.backingSet().contains(key);
      }

      public V get(@Nullable Object key) {
         return Collections2.safeContains(this.backingSet(), key) ? this.function.apply(key) : null;
      }

      public V remove(@Nullable Object key) {
         return this.backingSet().remove(key) ? this.function.apply(key) : null;
      }

      public void clear() {
         this.backingSet().clear();
      }

      protected Set<Entry<K, V>> createEntrySet() {
         class EntrySetImpl extends Maps.EntrySet<K, V> {
            Map<K, V> map() {
               return AsMapView.this;
            }

            public Iterator<Entry<K, V>> iterator() {
               return Maps.asMapEntryIterator(AsMapView.this.backingSet(), AsMapView.this.function);
            }
         }

         return new EntrySetImpl();
      }
   }

   static class SortedMapDifferenceImpl<K, V> extends Maps.MapDifferenceImpl<K, V> implements SortedMapDifference<K, V> {
      SortedMapDifferenceImpl(SortedMap<K, V> onlyOnLeft, SortedMap<K, V> onlyOnRight, SortedMap<K, V> onBoth, SortedMap<K, MapDifference.ValueDifference<V>> differences) {
         super(onlyOnLeft, onlyOnRight, onBoth, differences);
      }

      public SortedMap<K, MapDifference.ValueDifference<V>> entriesDiffering() {
         return (SortedMap)super.entriesDiffering();
      }

      public SortedMap<K, V> entriesInCommon() {
         return (SortedMap)super.entriesInCommon();
      }

      public SortedMap<K, V> entriesOnlyOnLeft() {
         return (SortedMap)super.entriesOnlyOnLeft();
      }

      public SortedMap<K, V> entriesOnlyOnRight() {
         return (SortedMap)super.entriesOnlyOnRight();
      }
   }

   static class ValueDifferenceImpl<V> implements MapDifference.ValueDifference<V> {
      private final V left;
      private final V right;

      static <V> MapDifference.ValueDifference<V> create(@Nullable V left, @Nullable V right) {
         return new Maps.ValueDifferenceImpl(left, right);
      }

      private ValueDifferenceImpl(@Nullable V left, @Nullable V right) {
         this.left = left;
         this.right = right;
      }

      public V leftValue() {
         return this.left;
      }

      public V rightValue() {
         return this.right;
      }

      public boolean equals(@Nullable Object object) {
         if (!(object instanceof MapDifference.ValueDifference)) {
            return false;
         } else {
            MapDifference.ValueDifference<?> that = (MapDifference.ValueDifference)object;
            return Objects.equal(this.left, that.leftValue()) && Objects.equal(this.right, that.rightValue());
         }
      }

      public int hashCode() {
         return Objects.hashCode(this.left, this.right);
      }

      public String toString() {
         return "(" + this.left + ", " + this.right + ")";
      }
   }

   static class MapDifferenceImpl<K, V> implements MapDifference<K, V> {
      final Map<K, V> onlyOnLeft;
      final Map<K, V> onlyOnRight;
      final Map<K, V> onBoth;
      final Map<K, MapDifference.ValueDifference<V>> differences;

      MapDifferenceImpl(Map<K, V> onlyOnLeft, Map<K, V> onlyOnRight, Map<K, V> onBoth, Map<K, MapDifference.ValueDifference<V>> differences) {
         this.onlyOnLeft = Maps.unmodifiableMap(onlyOnLeft);
         this.onlyOnRight = Maps.unmodifiableMap(onlyOnRight);
         this.onBoth = Maps.unmodifiableMap(onBoth);
         this.differences = Maps.unmodifiableMap(differences);
      }

      public boolean areEqual() {
         return this.onlyOnLeft.isEmpty() && this.onlyOnRight.isEmpty() && this.differences.isEmpty();
      }

      public Map<K, V> entriesOnlyOnLeft() {
         return this.onlyOnLeft;
      }

      public Map<K, V> entriesOnlyOnRight() {
         return this.onlyOnRight;
      }

      public Map<K, V> entriesInCommon() {
         return this.onBoth;
      }

      public Map<K, MapDifference.ValueDifference<V>> entriesDiffering() {
         return this.differences;
      }

      public boolean equals(Object object) {
         if (object == this) {
            return true;
         } else if (!(object instanceof MapDifference)) {
            return false;
         } else {
            MapDifference<?, ?> other = (MapDifference)object;
            return this.entriesOnlyOnLeft().equals(other.entriesOnlyOnLeft()) && this.entriesOnlyOnRight().equals(other.entriesOnlyOnRight()) && this.entriesInCommon().equals(other.entriesInCommon()) && this.entriesDiffering().equals(other.entriesDiffering());
         }
      }

      public int hashCode() {
         return Objects.hashCode(this.entriesOnlyOnLeft(), this.entriesOnlyOnRight(), this.entriesInCommon(), this.entriesDiffering());
      }

      public String toString() {
         if (this.areEqual()) {
            return "equal";
         } else {
            StringBuilder result = new StringBuilder("not equal");
            if (!this.onlyOnLeft.isEmpty()) {
               result.append(": only on left=").append(this.onlyOnLeft);
            }

            if (!this.onlyOnRight.isEmpty()) {
               result.append(": only on right=").append(this.onlyOnRight);
            }

            if (!this.differences.isEmpty()) {
               result.append(": value differences=").append(this.differences);
            }

            return result.toString();
         }
      }
   }

   private static enum EntryFunction implements Function<Entry<?, ?>, Object> {
      KEY {
         @Nullable
         public Object apply(Entry<?, ?> entry) {
            return entry.getKey();
         }
      },
      VALUE {
         @Nullable
         public Object apply(Entry<?, ?> entry) {
            return entry.getValue();
         }
      };

      private EntryFunction() {
      }

      // $FF: synthetic method
      EntryFunction(Object x2) {
         this();
      }
   }
}
