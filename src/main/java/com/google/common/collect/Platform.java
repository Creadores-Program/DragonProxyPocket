package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Map.Entry;

@GwtCompatible(
   emulated = true
)
final class Platform {
   static <T> T[] newArray(T[] reference, int length) {
      Class<?> type = reference.getClass().getComponentType();
      T[] result = (Object[])((Object[])Array.newInstance(type, length));
      return result;
   }

   static <E> Set<E> newSetFromMap(Map<E, Boolean> map) {
      return Collections.newSetFromMap(map);
   }

   static MapMaker tryWeakKeys(MapMaker mapMaker) {
      return mapMaker.weakKeys();
   }

   static <K, V1, V2> SortedMap<K, V2> mapsTransformEntriesSortedMap(SortedMap<K, V1> fromMap, Maps.EntryTransformer<? super K, ? super V1, V2> transformer) {
      return (SortedMap)(fromMap instanceof NavigableMap ? Maps.transformEntries((NavigableMap)fromMap, transformer) : Maps.transformEntriesIgnoreNavigable(fromMap, transformer));
   }

   static <K, V> SortedMap<K, V> mapsAsMapSortedSet(SortedSet<K> set, Function<? super K, V> function) {
      return (SortedMap)(set instanceof NavigableSet ? Maps.asMap((NavigableSet)set, function) : Maps.asMapSortedIgnoreNavigable(set, function));
   }

   static <E> SortedSet<E> setsFilterSortedSet(SortedSet<E> set, Predicate<? super E> predicate) {
      return (SortedSet)(set instanceof NavigableSet ? Sets.filter((NavigableSet)set, predicate) : Sets.filterSortedIgnoreNavigable(set, predicate));
   }

   static <K, V> SortedMap<K, V> mapsFilterSortedMap(SortedMap<K, V> map, Predicate<? super Entry<K, V>> predicate) {
      return (SortedMap)(map instanceof NavigableMap ? Maps.filterEntries((NavigableMap)map, predicate) : Maps.filterSortedIgnoreNavigable(map, predicate));
   }

   static <E> Queue<E> newFastestQueue(int initialCapacity) {
      return new ArrayDeque(initialCapacity);
   }

   private Platform() {
   }
}
