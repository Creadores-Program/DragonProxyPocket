package com.google.common.collect;

import java.util.Iterator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.Map.Entry;
import javax.annotation.Nullable;

abstract class AbstractNavigableMap<K, V> extends Maps.IteratorBasedAbstractMap<K, V> implements NavigableMap<K, V> {
   @Nullable
   public abstract V get(@Nullable Object var1);

   @Nullable
   public Entry<K, V> firstEntry() {
      return (Entry)Iterators.getNext(this.entryIterator(), (Object)null);
   }

   @Nullable
   public Entry<K, V> lastEntry() {
      return (Entry)Iterators.getNext(this.descendingEntryIterator(), (Object)null);
   }

   @Nullable
   public Entry<K, V> pollFirstEntry() {
      return (Entry)Iterators.pollNext(this.entryIterator());
   }

   @Nullable
   public Entry<K, V> pollLastEntry() {
      return (Entry)Iterators.pollNext(this.descendingEntryIterator());
   }

   public K firstKey() {
      Entry<K, V> entry = this.firstEntry();
      if (entry == null) {
         throw new NoSuchElementException();
      } else {
         return entry.getKey();
      }
   }

   public K lastKey() {
      Entry<K, V> entry = this.lastEntry();
      if (entry == null) {
         throw new NoSuchElementException();
      } else {
         return entry.getKey();
      }
   }

   @Nullable
   public Entry<K, V> lowerEntry(K key) {
      return this.headMap(key, false).lastEntry();
   }

   @Nullable
   public Entry<K, V> floorEntry(K key) {
      return this.headMap(key, true).lastEntry();
   }

   @Nullable
   public Entry<K, V> ceilingEntry(K key) {
      return this.tailMap(key, true).firstEntry();
   }

   @Nullable
   public Entry<K, V> higherEntry(K key) {
      return this.tailMap(key, false).firstEntry();
   }

   public K lowerKey(K key) {
      return Maps.keyOrNull(this.lowerEntry(key));
   }

   public K floorKey(K key) {
      return Maps.keyOrNull(this.floorEntry(key));
   }

   public K ceilingKey(K key) {
      return Maps.keyOrNull(this.ceilingEntry(key));
   }

   public K higherKey(K key) {
      return Maps.keyOrNull(this.higherEntry(key));
   }

   abstract Iterator<Entry<K, V>> descendingEntryIterator();

   public SortedMap<K, V> subMap(K fromKey, K toKey) {
      return this.subMap(fromKey, true, toKey, false);
   }

   public SortedMap<K, V> headMap(K toKey) {
      return this.headMap(toKey, false);
   }

   public SortedMap<K, V> tailMap(K fromKey) {
      return this.tailMap(fromKey, true);
   }

   public NavigableSet<K> navigableKeySet() {
      return new Maps.NavigableKeySet(this);
   }

   public Set<K> keySet() {
      return this.navigableKeySet();
   }

   public NavigableSet<K> descendingKeySet() {
      return this.descendingMap().navigableKeySet();
   }

   public NavigableMap<K, V> descendingMap() {
      return new AbstractNavigableMap.DescendingMap();
   }

   private final class DescendingMap extends Maps.DescendingMap<K, V> {
      private DescendingMap() {
      }

      NavigableMap<K, V> forward() {
         return AbstractNavigableMap.this;
      }

      Iterator<Entry<K, V>> entryIterator() {
         return AbstractNavigableMap.this.descendingEntryIterator();
      }

      // $FF: synthetic method
      DescendingMap(Object x1) {
         this();
      }
   }
}
