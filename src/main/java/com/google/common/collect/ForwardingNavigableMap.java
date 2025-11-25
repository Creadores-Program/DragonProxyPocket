package com.google.common.collect;

import com.google.common.annotations.Beta;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.Map.Entry;

public abstract class ForwardingNavigableMap<K, V> extends ForwardingSortedMap<K, V> implements NavigableMap<K, V> {
   protected ForwardingNavigableMap() {
   }

   protected abstract NavigableMap<K, V> delegate();

   public Entry<K, V> lowerEntry(K key) {
      return this.delegate().lowerEntry(key);
   }

   protected Entry<K, V> standardLowerEntry(K key) {
      return this.headMap(key, false).lastEntry();
   }

   public K lowerKey(K key) {
      return this.delegate().lowerKey(key);
   }

   protected K standardLowerKey(K key) {
      return Maps.keyOrNull(this.lowerEntry(key));
   }

   public Entry<K, V> floorEntry(K key) {
      return this.delegate().floorEntry(key);
   }

   protected Entry<K, V> standardFloorEntry(K key) {
      return this.headMap(key, true).lastEntry();
   }

   public K floorKey(K key) {
      return this.delegate().floorKey(key);
   }

   protected K standardFloorKey(K key) {
      return Maps.keyOrNull(this.floorEntry(key));
   }

   public Entry<K, V> ceilingEntry(K key) {
      return this.delegate().ceilingEntry(key);
   }

   protected Entry<K, V> standardCeilingEntry(K key) {
      return this.tailMap(key, true).firstEntry();
   }

   public K ceilingKey(K key) {
      return this.delegate().ceilingKey(key);
   }

   protected K standardCeilingKey(K key) {
      return Maps.keyOrNull(this.ceilingEntry(key));
   }

   public Entry<K, V> higherEntry(K key) {
      return this.delegate().higherEntry(key);
   }

   protected Entry<K, V> standardHigherEntry(K key) {
      return this.tailMap(key, false).firstEntry();
   }

   public K higherKey(K key) {
      return this.delegate().higherKey(key);
   }

   protected K standardHigherKey(K key) {
      return Maps.keyOrNull(this.higherEntry(key));
   }

   public Entry<K, V> firstEntry() {
      return this.delegate().firstEntry();
   }

   protected Entry<K, V> standardFirstEntry() {
      return (Entry)Iterables.getFirst(this.entrySet(), (Object)null);
   }

   protected K standardFirstKey() {
      Entry<K, V> entry = this.firstEntry();
      if (entry == null) {
         throw new NoSuchElementException();
      } else {
         return entry.getKey();
      }
   }

   public Entry<K, V> lastEntry() {
      return this.delegate().lastEntry();
   }

   protected Entry<K, V> standardLastEntry() {
      return (Entry)Iterables.getFirst(this.descendingMap().entrySet(), (Object)null);
   }

   protected K standardLastKey() {
      Entry<K, V> entry = this.lastEntry();
      if (entry == null) {
         throw new NoSuchElementException();
      } else {
         return entry.getKey();
      }
   }

   public Entry<K, V> pollFirstEntry() {
      return this.delegate().pollFirstEntry();
   }

   protected Entry<K, V> standardPollFirstEntry() {
      return (Entry)Iterators.pollNext(this.entrySet().iterator());
   }

   public Entry<K, V> pollLastEntry() {
      return this.delegate().pollLastEntry();
   }

   protected Entry<K, V> standardPollLastEntry() {
      return (Entry)Iterators.pollNext(this.descendingMap().entrySet().iterator());
   }

   public NavigableMap<K, V> descendingMap() {
      return this.delegate().descendingMap();
   }

   public NavigableSet<K> navigableKeySet() {
      return this.delegate().navigableKeySet();
   }

   public NavigableSet<K> descendingKeySet() {
      return this.delegate().descendingKeySet();
   }

   @Beta
   protected NavigableSet<K> standardDescendingKeySet() {
      return this.descendingMap().navigableKeySet();
   }

   protected SortedMap<K, V> standardSubMap(K fromKey, K toKey) {
      return this.subMap(fromKey, true, toKey, false);
   }

   public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
      return this.delegate().subMap(fromKey, fromInclusive, toKey, toInclusive);
   }

   public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
      return this.delegate().headMap(toKey, inclusive);
   }

   public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
      return this.delegate().tailMap(fromKey, inclusive);
   }

   protected SortedMap<K, V> standardHeadMap(K toKey) {
      return this.headMap(toKey, false);
   }

   protected SortedMap<K, V> standardTailMap(K fromKey) {
      return this.tailMap(fromKey, true);
   }

   @Beta
   protected class StandardNavigableKeySet extends Maps.NavigableKeySet<K, V> {
      public StandardNavigableKeySet() {
         super(ForwardingNavigableMap.this);
      }
   }

   @Beta
   protected class StandardDescendingMap extends Maps.DescendingMap<K, V> {
      public StandardDescendingMap() {
      }

      NavigableMap<K, V> forward() {
         return ForwardingNavigableMap.this;
      }

      protected Iterator<Entry<K, V>> entryIterator() {
         return new Iterator<Entry<K, V>>() {
            private Entry<K, V> toRemove = null;
            private Entry<K, V> nextOrNull = StandardDescendingMap.this.forward().lastEntry();

            public boolean hasNext() {
               return this.nextOrNull != null;
            }

            public Entry<K, V> next() {
               if (!this.hasNext()) {
                  throw new NoSuchElementException();
               } else {
                  Entry var1;
                  try {
                     var1 = this.nextOrNull;
                  } finally {
                     this.toRemove = this.nextOrNull;
                     this.nextOrNull = StandardDescendingMap.this.forward().lowerEntry(this.nextOrNull.getKey());
                  }

                  return var1;
               }
            }

            public void remove() {
               CollectPreconditions.checkRemove(this.toRemove != null);
               StandardDescendingMap.this.forward().remove(this.toRemove.getKey());
               this.toRemove = null;
            }
         };
      }
   }
}
