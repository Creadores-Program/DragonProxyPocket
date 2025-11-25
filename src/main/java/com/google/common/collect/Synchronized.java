package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible(
   emulated = true
)
final class Synchronized {
   private Synchronized() {
   }

   private static <E> Collection<E> collection(Collection<E> collection, @Nullable Object mutex) {
      return new Synchronized.SynchronizedCollection(collection, mutex);
   }

   @VisibleForTesting
   static <E> Set<E> set(Set<E> set, @Nullable Object mutex) {
      return new Synchronized.SynchronizedSet(set, mutex);
   }

   private static <E> SortedSet<E> sortedSet(SortedSet<E> set, @Nullable Object mutex) {
      return new Synchronized.SynchronizedSortedSet(set, mutex);
   }

   private static <E> List<E> list(List<E> list, @Nullable Object mutex) {
      return (List)(list instanceof RandomAccess ? new Synchronized.SynchronizedRandomAccessList(list, mutex) : new Synchronized.SynchronizedList(list, mutex));
   }

   static <E> Multiset<E> multiset(Multiset<E> multiset, @Nullable Object mutex) {
      return (Multiset)(!(multiset instanceof Synchronized.SynchronizedMultiset) && !(multiset instanceof ImmutableMultiset) ? new Synchronized.SynchronizedMultiset(multiset, mutex) : multiset);
   }

   static <K, V> Multimap<K, V> multimap(Multimap<K, V> multimap, @Nullable Object mutex) {
      return (Multimap)(!(multimap instanceof Synchronized.SynchronizedMultimap) && !(multimap instanceof ImmutableMultimap) ? new Synchronized.SynchronizedMultimap(multimap, mutex) : multimap);
   }

   static <K, V> ListMultimap<K, V> listMultimap(ListMultimap<K, V> multimap, @Nullable Object mutex) {
      return (ListMultimap)(!(multimap instanceof Synchronized.SynchronizedListMultimap) && !(multimap instanceof ImmutableListMultimap) ? new Synchronized.SynchronizedListMultimap(multimap, mutex) : multimap);
   }

   static <K, V> SetMultimap<K, V> setMultimap(SetMultimap<K, V> multimap, @Nullable Object mutex) {
      return (SetMultimap)(!(multimap instanceof Synchronized.SynchronizedSetMultimap) && !(multimap instanceof ImmutableSetMultimap) ? new Synchronized.SynchronizedSetMultimap(multimap, mutex) : multimap);
   }

   static <K, V> SortedSetMultimap<K, V> sortedSetMultimap(SortedSetMultimap<K, V> multimap, @Nullable Object mutex) {
      return (SortedSetMultimap)(multimap instanceof Synchronized.SynchronizedSortedSetMultimap ? multimap : new Synchronized.SynchronizedSortedSetMultimap(multimap, mutex));
   }

   private static <E> Collection<E> typePreservingCollection(Collection<E> collection, @Nullable Object mutex) {
      if (collection instanceof SortedSet) {
         return sortedSet((SortedSet)collection, mutex);
      } else if (collection instanceof Set) {
         return set((Set)collection, mutex);
      } else {
         return (Collection)(collection instanceof List ? list((List)collection, mutex) : collection(collection, mutex));
      }
   }

   private static <E> Set<E> typePreservingSet(Set<E> set, @Nullable Object mutex) {
      return (Set)(set instanceof SortedSet ? sortedSet((SortedSet)set, mutex) : set(set, mutex));
   }

   @VisibleForTesting
   static <K, V> Map<K, V> map(Map<K, V> map, @Nullable Object mutex) {
      return new Synchronized.SynchronizedMap(map, mutex);
   }

   static <K, V> SortedMap<K, V> sortedMap(SortedMap<K, V> sortedMap, @Nullable Object mutex) {
      return new Synchronized.SynchronizedSortedMap(sortedMap, mutex);
   }

   static <K, V> BiMap<K, V> biMap(BiMap<K, V> bimap, @Nullable Object mutex) {
      return (BiMap)(!(bimap instanceof Synchronized.SynchronizedBiMap) && !(bimap instanceof ImmutableBiMap) ? new Synchronized.SynchronizedBiMap(bimap, mutex, (BiMap)null) : bimap);
   }

   @GwtIncompatible("NavigableSet")
   static <E> NavigableSet<E> navigableSet(NavigableSet<E> navigableSet, @Nullable Object mutex) {
      return new Synchronized.SynchronizedNavigableSet(navigableSet, mutex);
   }

   @GwtIncompatible("NavigableSet")
   static <E> NavigableSet<E> navigableSet(NavigableSet<E> navigableSet) {
      return navigableSet(navigableSet, (Object)null);
   }

   @GwtIncompatible("NavigableMap")
   static <K, V> NavigableMap<K, V> navigableMap(NavigableMap<K, V> navigableMap) {
      return navigableMap(navigableMap, (Object)null);
   }

   @GwtIncompatible("NavigableMap")
   static <K, V> NavigableMap<K, V> navigableMap(NavigableMap<K, V> navigableMap, @Nullable Object mutex) {
      return new Synchronized.SynchronizedNavigableMap(navigableMap, mutex);
   }

   @GwtIncompatible("works but is needed only for NavigableMap")
   private static <K, V> Entry<K, V> nullableSynchronizedEntry(@Nullable Entry<K, V> entry, @Nullable Object mutex) {
      return entry == null ? null : new Synchronized.SynchronizedEntry(entry, mutex);
   }

   static <E> Queue<E> queue(Queue<E> queue, @Nullable Object mutex) {
      return (Queue)(queue instanceof Synchronized.SynchronizedQueue ? queue : new Synchronized.SynchronizedQueue(queue, mutex));
   }

   @GwtIncompatible("Deque")
   static <E> Deque<E> deque(Deque<E> deque, @Nullable Object mutex) {
      return new Synchronized.SynchronizedDeque(deque, mutex);
   }

   @GwtIncompatible("Deque")
   private static final class SynchronizedDeque<E> extends Synchronized.SynchronizedQueue<E> implements Deque<E> {
      private static final long serialVersionUID = 0L;

      SynchronizedDeque(Deque<E> delegate, @Nullable Object mutex) {
         super(delegate, mutex);
      }

      Deque<E> delegate() {
         return (Deque)super.delegate();
      }

      public void addFirst(E e) {
         synchronized(this.mutex) {
            this.delegate().addFirst(e);
         }
      }

      public void addLast(E e) {
         synchronized(this.mutex) {
            this.delegate().addLast(e);
         }
      }

      public boolean offerFirst(E e) {
         synchronized(this.mutex) {
            return this.delegate().offerFirst(e);
         }
      }

      public boolean offerLast(E e) {
         synchronized(this.mutex) {
            return this.delegate().offerLast(e);
         }
      }

      public E removeFirst() {
         synchronized(this.mutex) {
            return this.delegate().removeFirst();
         }
      }

      public E removeLast() {
         synchronized(this.mutex) {
            return this.delegate().removeLast();
         }
      }

      public E pollFirst() {
         synchronized(this.mutex) {
            return this.delegate().pollFirst();
         }
      }

      public E pollLast() {
         synchronized(this.mutex) {
            return this.delegate().pollLast();
         }
      }

      public E getFirst() {
         synchronized(this.mutex) {
            return this.delegate().getFirst();
         }
      }

      public E getLast() {
         synchronized(this.mutex) {
            return this.delegate().getLast();
         }
      }

      public E peekFirst() {
         synchronized(this.mutex) {
            return this.delegate().peekFirst();
         }
      }

      public E peekLast() {
         synchronized(this.mutex) {
            return this.delegate().peekLast();
         }
      }

      public boolean removeFirstOccurrence(Object o) {
         synchronized(this.mutex) {
            return this.delegate().removeFirstOccurrence(o);
         }
      }

      public boolean removeLastOccurrence(Object o) {
         synchronized(this.mutex) {
            return this.delegate().removeLastOccurrence(o);
         }
      }

      public void push(E e) {
         synchronized(this.mutex) {
            this.delegate().push(e);
         }
      }

      public E pop() {
         synchronized(this.mutex) {
            return this.delegate().pop();
         }
      }

      public Iterator<E> descendingIterator() {
         synchronized(this.mutex) {
            return this.delegate().descendingIterator();
         }
      }
   }

   private static class SynchronizedQueue<E> extends Synchronized.SynchronizedCollection<E> implements Queue<E> {
      private static final long serialVersionUID = 0L;

      SynchronizedQueue(Queue<E> delegate, @Nullable Object mutex) {
         super(delegate, mutex, null);
      }

      Queue<E> delegate() {
         return (Queue)super.delegate();
      }

      public E element() {
         synchronized(this.mutex) {
            return this.delegate().element();
         }
      }

      public boolean offer(E e) {
         synchronized(this.mutex) {
            return this.delegate().offer(e);
         }
      }

      public E peek() {
         synchronized(this.mutex) {
            return this.delegate().peek();
         }
      }

      public E poll() {
         synchronized(this.mutex) {
            return this.delegate().poll();
         }
      }

      public E remove() {
         synchronized(this.mutex) {
            return this.delegate().remove();
         }
      }
   }

   @GwtIncompatible("works but is needed only for NavigableMap")
   private static class SynchronizedEntry<K, V> extends Synchronized.SynchronizedObject implements Entry<K, V> {
      private static final long serialVersionUID = 0L;

      SynchronizedEntry(Entry<K, V> delegate, @Nullable Object mutex) {
         super(delegate, mutex);
      }

      Entry<K, V> delegate() {
         return (Entry)super.delegate();
      }

      public boolean equals(Object obj) {
         synchronized(this.mutex) {
            return this.delegate().equals(obj);
         }
      }

      public int hashCode() {
         synchronized(this.mutex) {
            return this.delegate().hashCode();
         }
      }

      public K getKey() {
         synchronized(this.mutex) {
            return this.delegate().getKey();
         }
      }

      public V getValue() {
         synchronized(this.mutex) {
            return this.delegate().getValue();
         }
      }

      public V setValue(V value) {
         synchronized(this.mutex) {
            return this.delegate().setValue(value);
         }
      }
   }

   @GwtIncompatible("NavigableMap")
   @VisibleForTesting
   static class SynchronizedNavigableMap<K, V> extends Synchronized.SynchronizedSortedMap<K, V> implements NavigableMap<K, V> {
      transient NavigableSet<K> descendingKeySet;
      transient NavigableMap<K, V> descendingMap;
      transient NavigableSet<K> navigableKeySet;
      private static final long serialVersionUID = 0L;

      SynchronizedNavigableMap(NavigableMap<K, V> delegate, @Nullable Object mutex) {
         super(delegate, mutex);
      }

      NavigableMap<K, V> delegate() {
         return (NavigableMap)super.delegate();
      }

      public Entry<K, V> ceilingEntry(K key) {
         synchronized(this.mutex) {
            return Synchronized.nullableSynchronizedEntry(this.delegate().ceilingEntry(key), this.mutex);
         }
      }

      public K ceilingKey(K key) {
         synchronized(this.mutex) {
            return this.delegate().ceilingKey(key);
         }
      }

      public NavigableSet<K> descendingKeySet() {
         synchronized(this.mutex) {
            return this.descendingKeySet == null ? (this.descendingKeySet = Synchronized.navigableSet(this.delegate().descendingKeySet(), this.mutex)) : this.descendingKeySet;
         }
      }

      public NavigableMap<K, V> descendingMap() {
         synchronized(this.mutex) {
            return this.descendingMap == null ? (this.descendingMap = Synchronized.navigableMap(this.delegate().descendingMap(), this.mutex)) : this.descendingMap;
         }
      }

      public Entry<K, V> firstEntry() {
         synchronized(this.mutex) {
            return Synchronized.nullableSynchronizedEntry(this.delegate().firstEntry(), this.mutex);
         }
      }

      public Entry<K, V> floorEntry(K key) {
         synchronized(this.mutex) {
            return Synchronized.nullableSynchronizedEntry(this.delegate().floorEntry(key), this.mutex);
         }
      }

      public K floorKey(K key) {
         synchronized(this.mutex) {
            return this.delegate().floorKey(key);
         }
      }

      public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
         synchronized(this.mutex) {
            return Synchronized.navigableMap(this.delegate().headMap(toKey, inclusive), this.mutex);
         }
      }

      public Entry<K, V> higherEntry(K key) {
         synchronized(this.mutex) {
            return Synchronized.nullableSynchronizedEntry(this.delegate().higherEntry(key), this.mutex);
         }
      }

      public K higherKey(K key) {
         synchronized(this.mutex) {
            return this.delegate().higherKey(key);
         }
      }

      public Entry<K, V> lastEntry() {
         synchronized(this.mutex) {
            return Synchronized.nullableSynchronizedEntry(this.delegate().lastEntry(), this.mutex);
         }
      }

      public Entry<K, V> lowerEntry(K key) {
         synchronized(this.mutex) {
            return Synchronized.nullableSynchronizedEntry(this.delegate().lowerEntry(key), this.mutex);
         }
      }

      public K lowerKey(K key) {
         synchronized(this.mutex) {
            return this.delegate().lowerKey(key);
         }
      }

      public Set<K> keySet() {
         return this.navigableKeySet();
      }

      public NavigableSet<K> navigableKeySet() {
         synchronized(this.mutex) {
            return this.navigableKeySet == null ? (this.navigableKeySet = Synchronized.navigableSet(this.delegate().navigableKeySet(), this.mutex)) : this.navigableKeySet;
         }
      }

      public Entry<K, V> pollFirstEntry() {
         synchronized(this.mutex) {
            return Synchronized.nullableSynchronizedEntry(this.delegate().pollFirstEntry(), this.mutex);
         }
      }

      public Entry<K, V> pollLastEntry() {
         synchronized(this.mutex) {
            return Synchronized.nullableSynchronizedEntry(this.delegate().pollLastEntry(), this.mutex);
         }
      }

      public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
         synchronized(this.mutex) {
            return Synchronized.navigableMap(this.delegate().subMap(fromKey, fromInclusive, toKey, toInclusive), this.mutex);
         }
      }

      public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
         synchronized(this.mutex) {
            return Synchronized.navigableMap(this.delegate().tailMap(fromKey, inclusive), this.mutex);
         }
      }

      public SortedMap<K, V> headMap(K toKey) {
         return this.headMap(toKey, false);
      }

      public SortedMap<K, V> subMap(K fromKey, K toKey) {
         return this.subMap(fromKey, true, toKey, false);
      }

      public SortedMap<K, V> tailMap(K fromKey) {
         return this.tailMap(fromKey, true);
      }
   }

   @GwtIncompatible("NavigableSet")
   @VisibleForTesting
   static class SynchronizedNavigableSet<E> extends Synchronized.SynchronizedSortedSet<E> implements NavigableSet<E> {
      transient NavigableSet<E> descendingSet;
      private static final long serialVersionUID = 0L;

      SynchronizedNavigableSet(NavigableSet<E> delegate, @Nullable Object mutex) {
         super(delegate, mutex);
      }

      NavigableSet<E> delegate() {
         return (NavigableSet)super.delegate();
      }

      public E ceiling(E e) {
         synchronized(this.mutex) {
            return this.delegate().ceiling(e);
         }
      }

      public Iterator<E> descendingIterator() {
         return this.delegate().descendingIterator();
      }

      public NavigableSet<E> descendingSet() {
         synchronized(this.mutex) {
            if (this.descendingSet == null) {
               NavigableSet<E> dS = Synchronized.navigableSet(this.delegate().descendingSet(), this.mutex);
               this.descendingSet = dS;
               return dS;
            } else {
               return this.descendingSet;
            }
         }
      }

      public E floor(E e) {
         synchronized(this.mutex) {
            return this.delegate().floor(e);
         }
      }

      public NavigableSet<E> headSet(E toElement, boolean inclusive) {
         synchronized(this.mutex) {
            return Synchronized.navigableSet(this.delegate().headSet(toElement, inclusive), this.mutex);
         }
      }

      public E higher(E e) {
         synchronized(this.mutex) {
            return this.delegate().higher(e);
         }
      }

      public E lower(E e) {
         synchronized(this.mutex) {
            return this.delegate().lower(e);
         }
      }

      public E pollFirst() {
         synchronized(this.mutex) {
            return this.delegate().pollFirst();
         }
      }

      public E pollLast() {
         synchronized(this.mutex) {
            return this.delegate().pollLast();
         }
      }

      public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
         synchronized(this.mutex) {
            return Synchronized.navigableSet(this.delegate().subSet(fromElement, fromInclusive, toElement, toInclusive), this.mutex);
         }
      }

      public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
         synchronized(this.mutex) {
            return Synchronized.navigableSet(this.delegate().tailSet(fromElement, inclusive), this.mutex);
         }
      }

      public SortedSet<E> headSet(E toElement) {
         return this.headSet(toElement, false);
      }

      public SortedSet<E> subSet(E fromElement, E toElement) {
         return this.subSet(fromElement, true, toElement, false);
      }

      public SortedSet<E> tailSet(E fromElement) {
         return this.tailSet(fromElement, true);
      }
   }

   private static class SynchronizedAsMapValues<V> extends Synchronized.SynchronizedCollection<Collection<V>> {
      private static final long serialVersionUID = 0L;

      SynchronizedAsMapValues(Collection<Collection<V>> delegate, @Nullable Object mutex) {
         super(delegate, mutex, null);
      }

      public Iterator<Collection<V>> iterator() {
         return new TransformedIterator<Collection<V>, Collection<V>>(super.iterator()) {
            Collection<V> transform(Collection<V> from) {
               return Synchronized.typePreservingCollection(from, SynchronizedAsMapValues.this.mutex);
            }
         };
      }
   }

   private static class SynchronizedAsMap<K, V> extends Synchronized.SynchronizedMap<K, Collection<V>> {
      transient Set<Entry<K, Collection<V>>> asMapEntrySet;
      transient Collection<Collection<V>> asMapValues;
      private static final long serialVersionUID = 0L;

      SynchronizedAsMap(Map<K, Collection<V>> delegate, @Nullable Object mutex) {
         super(delegate, mutex);
      }

      public Collection<V> get(Object key) {
         synchronized(this.mutex) {
            Collection<V> collection = (Collection)super.get(key);
            return collection == null ? null : Synchronized.typePreservingCollection(collection, this.mutex);
         }
      }

      public Set<Entry<K, Collection<V>>> entrySet() {
         synchronized(this.mutex) {
            if (this.asMapEntrySet == null) {
               this.asMapEntrySet = new Synchronized.SynchronizedAsMapEntries(this.delegate().entrySet(), this.mutex);
            }

            return this.asMapEntrySet;
         }
      }

      public Collection<Collection<V>> values() {
         synchronized(this.mutex) {
            if (this.asMapValues == null) {
               this.asMapValues = new Synchronized.SynchronizedAsMapValues(this.delegate().values(), this.mutex);
            }

            return this.asMapValues;
         }
      }

      public boolean containsValue(Object o) {
         return this.values().contains(o);
      }
   }

   @VisibleForTesting
   static class SynchronizedBiMap<K, V> extends Synchronized.SynchronizedMap<K, V> implements BiMap<K, V>, Serializable {
      private transient Set<V> valueSet;
      private transient BiMap<V, K> inverse;
      private static final long serialVersionUID = 0L;

      private SynchronizedBiMap(BiMap<K, V> delegate, @Nullable Object mutex, @Nullable BiMap<V, K> inverse) {
         super(delegate, mutex);
         this.inverse = inverse;
      }

      BiMap<K, V> delegate() {
         return (BiMap)super.delegate();
      }

      public Set<V> values() {
         synchronized(this.mutex) {
            if (this.valueSet == null) {
               this.valueSet = Synchronized.set(this.delegate().values(), this.mutex);
            }

            return this.valueSet;
         }
      }

      public V forcePut(K key, V value) {
         synchronized(this.mutex) {
            return this.delegate().forcePut(key, value);
         }
      }

      public BiMap<V, K> inverse() {
         synchronized(this.mutex) {
            if (this.inverse == null) {
               this.inverse = new Synchronized.SynchronizedBiMap(this.delegate().inverse(), this.mutex, this);
            }

            return this.inverse;
         }
      }

      // $FF: synthetic method
      SynchronizedBiMap(BiMap x0, Object x1, BiMap x2, Object x3) {
         this(x0, x1, x2);
      }
   }

   static class SynchronizedSortedMap<K, V> extends Synchronized.SynchronizedMap<K, V> implements SortedMap<K, V> {
      private static final long serialVersionUID = 0L;

      SynchronizedSortedMap(SortedMap<K, V> delegate, @Nullable Object mutex) {
         super(delegate, mutex);
      }

      SortedMap<K, V> delegate() {
         return (SortedMap)super.delegate();
      }

      public Comparator<? super K> comparator() {
         synchronized(this.mutex) {
            return this.delegate().comparator();
         }
      }

      public K firstKey() {
         synchronized(this.mutex) {
            return this.delegate().firstKey();
         }
      }

      public SortedMap<K, V> headMap(K toKey) {
         synchronized(this.mutex) {
            return Synchronized.sortedMap(this.delegate().headMap(toKey), this.mutex);
         }
      }

      public K lastKey() {
         synchronized(this.mutex) {
            return this.delegate().lastKey();
         }
      }

      public SortedMap<K, V> subMap(K fromKey, K toKey) {
         synchronized(this.mutex) {
            return Synchronized.sortedMap(this.delegate().subMap(fromKey, toKey), this.mutex);
         }
      }

      public SortedMap<K, V> tailMap(K fromKey) {
         synchronized(this.mutex) {
            return Synchronized.sortedMap(this.delegate().tailMap(fromKey), this.mutex);
         }
      }
   }

   private static class SynchronizedMap<K, V> extends Synchronized.SynchronizedObject implements Map<K, V> {
      transient Set<K> keySet;
      transient Collection<V> values;
      transient Set<Entry<K, V>> entrySet;
      private static final long serialVersionUID = 0L;

      SynchronizedMap(Map<K, V> delegate, @Nullable Object mutex) {
         super(delegate, mutex);
      }

      Map<K, V> delegate() {
         return (Map)super.delegate();
      }

      public void clear() {
         synchronized(this.mutex) {
            this.delegate().clear();
         }
      }

      public boolean containsKey(Object key) {
         synchronized(this.mutex) {
            return this.delegate().containsKey(key);
         }
      }

      public boolean containsValue(Object value) {
         synchronized(this.mutex) {
            return this.delegate().containsValue(value);
         }
      }

      public Set<Entry<K, V>> entrySet() {
         synchronized(this.mutex) {
            if (this.entrySet == null) {
               this.entrySet = Synchronized.set(this.delegate().entrySet(), this.mutex);
            }

            return this.entrySet;
         }
      }

      public V get(Object key) {
         synchronized(this.mutex) {
            return this.delegate().get(key);
         }
      }

      public boolean isEmpty() {
         synchronized(this.mutex) {
            return this.delegate().isEmpty();
         }
      }

      public Set<K> keySet() {
         synchronized(this.mutex) {
            if (this.keySet == null) {
               this.keySet = Synchronized.set(this.delegate().keySet(), this.mutex);
            }

            return this.keySet;
         }
      }

      public V put(K key, V value) {
         synchronized(this.mutex) {
            return this.delegate().put(key, value);
         }
      }

      public void putAll(Map<? extends K, ? extends V> map) {
         synchronized(this.mutex) {
            this.delegate().putAll(map);
         }
      }

      public V remove(Object key) {
         synchronized(this.mutex) {
            return this.delegate().remove(key);
         }
      }

      public int size() {
         synchronized(this.mutex) {
            return this.delegate().size();
         }
      }

      public Collection<V> values() {
         synchronized(this.mutex) {
            if (this.values == null) {
               this.values = Synchronized.collection(this.delegate().values(), this.mutex);
            }

            return this.values;
         }
      }

      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else {
            synchronized(this.mutex) {
               return this.delegate().equals(o);
            }
         }
      }

      public int hashCode() {
         synchronized(this.mutex) {
            return this.delegate().hashCode();
         }
      }
   }

   private static class SynchronizedAsMapEntries<K, V> extends Synchronized.SynchronizedSet<Entry<K, Collection<V>>> {
      private static final long serialVersionUID = 0L;

      SynchronizedAsMapEntries(Set<Entry<K, Collection<V>>> delegate, @Nullable Object mutex) {
         super(delegate, mutex);
      }

      public Iterator<Entry<K, Collection<V>>> iterator() {
         return new TransformedIterator<Entry<K, Collection<V>>, Entry<K, Collection<V>>>(super.iterator()) {
            Entry<K, Collection<V>> transform(final Entry<K, Collection<V>> entry) {
               return new ForwardingMapEntry<K, Collection<V>>() {
                  protected Entry<K, Collection<V>> delegate() {
                     return entry;
                  }

                  public Collection<V> getValue() {
                     return Synchronized.typePreservingCollection((Collection)entry.getValue(), SynchronizedAsMapEntries.this.mutex);
                  }
               };
            }
         };
      }

      public Object[] toArray() {
         synchronized(this.mutex) {
            return ObjectArrays.toArrayImpl(this.delegate());
         }
      }

      public <T> T[] toArray(T[] array) {
         synchronized(this.mutex) {
            return ObjectArrays.toArrayImpl(this.delegate(), array);
         }
      }

      public boolean contains(Object o) {
         synchronized(this.mutex) {
            return Maps.containsEntryImpl(this.delegate(), o);
         }
      }

      public boolean containsAll(Collection<?> c) {
         synchronized(this.mutex) {
            return Collections2.containsAllImpl(this.delegate(), c);
         }
      }

      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else {
            synchronized(this.mutex) {
               return Sets.equalsImpl(this.delegate(), o);
            }
         }
      }

      public boolean remove(Object o) {
         synchronized(this.mutex) {
            return Maps.removeEntryImpl(this.delegate(), o);
         }
      }

      public boolean removeAll(Collection<?> c) {
         synchronized(this.mutex) {
            return Iterators.removeAll(this.delegate().iterator(), c);
         }
      }

      public boolean retainAll(Collection<?> c) {
         synchronized(this.mutex) {
            return Iterators.retainAll(this.delegate().iterator(), c);
         }
      }
   }

   private static class SynchronizedSortedSetMultimap<K, V> extends Synchronized.SynchronizedSetMultimap<K, V> implements SortedSetMultimap<K, V> {
      private static final long serialVersionUID = 0L;

      SynchronizedSortedSetMultimap(SortedSetMultimap<K, V> delegate, @Nullable Object mutex) {
         super(delegate, mutex);
      }

      SortedSetMultimap<K, V> delegate() {
         return (SortedSetMultimap)super.delegate();
      }

      public SortedSet<V> get(K key) {
         synchronized(this.mutex) {
            return Synchronized.sortedSet(this.delegate().get(key), this.mutex);
         }
      }

      public SortedSet<V> removeAll(Object key) {
         synchronized(this.mutex) {
            return this.delegate().removeAll(key);
         }
      }

      public SortedSet<V> replaceValues(K key, Iterable<? extends V> values) {
         synchronized(this.mutex) {
            return this.delegate().replaceValues(key, values);
         }
      }

      public Comparator<? super V> valueComparator() {
         synchronized(this.mutex) {
            return this.delegate().valueComparator();
         }
      }
   }

   private static class SynchronizedSetMultimap<K, V> extends Synchronized.SynchronizedMultimap<K, V> implements SetMultimap<K, V> {
      transient Set<Entry<K, V>> entrySet;
      private static final long serialVersionUID = 0L;

      SynchronizedSetMultimap(SetMultimap<K, V> delegate, @Nullable Object mutex) {
         super(delegate, mutex);
      }

      SetMultimap<K, V> delegate() {
         return (SetMultimap)super.delegate();
      }

      public Set<V> get(K key) {
         synchronized(this.mutex) {
            return Synchronized.set(this.delegate().get(key), this.mutex);
         }
      }

      public Set<V> removeAll(Object key) {
         synchronized(this.mutex) {
            return this.delegate().removeAll(key);
         }
      }

      public Set<V> replaceValues(K key, Iterable<? extends V> values) {
         synchronized(this.mutex) {
            return this.delegate().replaceValues(key, values);
         }
      }

      public Set<Entry<K, V>> entries() {
         synchronized(this.mutex) {
            if (this.entrySet == null) {
               this.entrySet = Synchronized.set(this.delegate().entries(), this.mutex);
            }

            return this.entrySet;
         }
      }
   }

   private static class SynchronizedListMultimap<K, V> extends Synchronized.SynchronizedMultimap<K, V> implements ListMultimap<K, V> {
      private static final long serialVersionUID = 0L;

      SynchronizedListMultimap(ListMultimap<K, V> delegate, @Nullable Object mutex) {
         super(delegate, mutex);
      }

      ListMultimap<K, V> delegate() {
         return (ListMultimap)super.delegate();
      }

      public List<V> get(K key) {
         synchronized(this.mutex) {
            return Synchronized.list(this.delegate().get(key), this.mutex);
         }
      }

      public List<V> removeAll(Object key) {
         synchronized(this.mutex) {
            return this.delegate().removeAll(key);
         }
      }

      public List<V> replaceValues(K key, Iterable<? extends V> values) {
         synchronized(this.mutex) {
            return this.delegate().replaceValues(key, values);
         }
      }
   }

   private static class SynchronizedMultimap<K, V> extends Synchronized.SynchronizedObject implements Multimap<K, V> {
      transient Set<K> keySet;
      transient Collection<V> valuesCollection;
      transient Collection<Entry<K, V>> entries;
      transient Map<K, Collection<V>> asMap;
      transient Multiset<K> keys;
      private static final long serialVersionUID = 0L;

      Multimap<K, V> delegate() {
         return (Multimap)super.delegate();
      }

      SynchronizedMultimap(Multimap<K, V> delegate, @Nullable Object mutex) {
         super(delegate, mutex);
      }

      public int size() {
         synchronized(this.mutex) {
            return this.delegate().size();
         }
      }

      public boolean isEmpty() {
         synchronized(this.mutex) {
            return this.delegate().isEmpty();
         }
      }

      public boolean containsKey(Object key) {
         synchronized(this.mutex) {
            return this.delegate().containsKey(key);
         }
      }

      public boolean containsValue(Object value) {
         synchronized(this.mutex) {
            return this.delegate().containsValue(value);
         }
      }

      public boolean containsEntry(Object key, Object value) {
         synchronized(this.mutex) {
            return this.delegate().containsEntry(key, value);
         }
      }

      public Collection<V> get(K key) {
         synchronized(this.mutex) {
            return Synchronized.typePreservingCollection(this.delegate().get(key), this.mutex);
         }
      }

      public boolean put(K key, V value) {
         synchronized(this.mutex) {
            return this.delegate().put(key, value);
         }
      }

      public boolean putAll(K key, Iterable<? extends V> values) {
         synchronized(this.mutex) {
            return this.delegate().putAll(key, values);
         }
      }

      public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
         synchronized(this.mutex) {
            return this.delegate().putAll(multimap);
         }
      }

      public Collection<V> replaceValues(K key, Iterable<? extends V> values) {
         synchronized(this.mutex) {
            return this.delegate().replaceValues(key, values);
         }
      }

      public boolean remove(Object key, Object value) {
         synchronized(this.mutex) {
            return this.delegate().remove(key, value);
         }
      }

      public Collection<V> removeAll(Object key) {
         synchronized(this.mutex) {
            return this.delegate().removeAll(key);
         }
      }

      public void clear() {
         synchronized(this.mutex) {
            this.delegate().clear();
         }
      }

      public Set<K> keySet() {
         synchronized(this.mutex) {
            if (this.keySet == null) {
               this.keySet = Synchronized.typePreservingSet(this.delegate().keySet(), this.mutex);
            }

            return this.keySet;
         }
      }

      public Collection<V> values() {
         synchronized(this.mutex) {
            if (this.valuesCollection == null) {
               this.valuesCollection = Synchronized.collection(this.delegate().values(), this.mutex);
            }

            return this.valuesCollection;
         }
      }

      public Collection<Entry<K, V>> entries() {
         synchronized(this.mutex) {
            if (this.entries == null) {
               this.entries = Synchronized.typePreservingCollection(this.delegate().entries(), this.mutex);
            }

            return this.entries;
         }
      }

      public Map<K, Collection<V>> asMap() {
         synchronized(this.mutex) {
            if (this.asMap == null) {
               this.asMap = new Synchronized.SynchronizedAsMap(this.delegate().asMap(), this.mutex);
            }

            return this.asMap;
         }
      }

      public Multiset<K> keys() {
         synchronized(this.mutex) {
            if (this.keys == null) {
               this.keys = Synchronized.multiset(this.delegate().keys(), this.mutex);
            }

            return this.keys;
         }
      }

      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else {
            synchronized(this.mutex) {
               return this.delegate().equals(o);
            }
         }
      }

      public int hashCode() {
         synchronized(this.mutex) {
            return this.delegate().hashCode();
         }
      }
   }

   private static class SynchronizedMultiset<E> extends Synchronized.SynchronizedCollection<E> implements Multiset<E> {
      transient Set<E> elementSet;
      transient Set<Multiset.Entry<E>> entrySet;
      private static final long serialVersionUID = 0L;

      SynchronizedMultiset(Multiset<E> delegate, @Nullable Object mutex) {
         super(delegate, mutex, null);
      }

      Multiset<E> delegate() {
         return (Multiset)super.delegate();
      }

      public int count(Object o) {
         synchronized(this.mutex) {
            return this.delegate().count(o);
         }
      }

      public int add(E e, int n) {
         synchronized(this.mutex) {
            return this.delegate().add(e, n);
         }
      }

      public int remove(Object o, int n) {
         synchronized(this.mutex) {
            return this.delegate().remove(o, n);
         }
      }

      public int setCount(E element, int count) {
         synchronized(this.mutex) {
            return this.delegate().setCount(element, count);
         }
      }

      public boolean setCount(E element, int oldCount, int newCount) {
         synchronized(this.mutex) {
            return this.delegate().setCount(element, oldCount, newCount);
         }
      }

      public Set<E> elementSet() {
         synchronized(this.mutex) {
            if (this.elementSet == null) {
               this.elementSet = Synchronized.typePreservingSet(this.delegate().elementSet(), this.mutex);
            }

            return this.elementSet;
         }
      }

      public Set<Multiset.Entry<E>> entrySet() {
         synchronized(this.mutex) {
            if (this.entrySet == null) {
               this.entrySet = Synchronized.typePreservingSet(this.delegate().entrySet(), this.mutex);
            }

            return this.entrySet;
         }
      }

      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else {
            synchronized(this.mutex) {
               return this.delegate().equals(o);
            }
         }
      }

      public int hashCode() {
         synchronized(this.mutex) {
            return this.delegate().hashCode();
         }
      }
   }

   private static class SynchronizedRandomAccessList<E> extends Synchronized.SynchronizedList<E> implements RandomAccess {
      private static final long serialVersionUID = 0L;

      SynchronizedRandomAccessList(List<E> list, @Nullable Object mutex) {
         super(list, mutex);
      }
   }

   private static class SynchronizedList<E> extends Synchronized.SynchronizedCollection<E> implements List<E> {
      private static final long serialVersionUID = 0L;

      SynchronizedList(List<E> delegate, @Nullable Object mutex) {
         super(delegate, mutex, null);
      }

      List<E> delegate() {
         return (List)super.delegate();
      }

      public void add(int index, E element) {
         synchronized(this.mutex) {
            this.delegate().add(index, element);
         }
      }

      public boolean addAll(int index, Collection<? extends E> c) {
         synchronized(this.mutex) {
            return this.delegate().addAll(index, c);
         }
      }

      public E get(int index) {
         synchronized(this.mutex) {
            return this.delegate().get(index);
         }
      }

      public int indexOf(Object o) {
         synchronized(this.mutex) {
            return this.delegate().indexOf(o);
         }
      }

      public int lastIndexOf(Object o) {
         synchronized(this.mutex) {
            return this.delegate().lastIndexOf(o);
         }
      }

      public ListIterator<E> listIterator() {
         return this.delegate().listIterator();
      }

      public ListIterator<E> listIterator(int index) {
         return this.delegate().listIterator(index);
      }

      public E remove(int index) {
         synchronized(this.mutex) {
            return this.delegate().remove(index);
         }
      }

      public E set(int index, E element) {
         synchronized(this.mutex) {
            return this.delegate().set(index, element);
         }
      }

      public List<E> subList(int fromIndex, int toIndex) {
         synchronized(this.mutex) {
            return Synchronized.list(this.delegate().subList(fromIndex, toIndex), this.mutex);
         }
      }

      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else {
            synchronized(this.mutex) {
               return this.delegate().equals(o);
            }
         }
      }

      public int hashCode() {
         synchronized(this.mutex) {
            return this.delegate().hashCode();
         }
      }
   }

   static class SynchronizedSortedSet<E> extends Synchronized.SynchronizedSet<E> implements SortedSet<E> {
      private static final long serialVersionUID = 0L;

      SynchronizedSortedSet(SortedSet<E> delegate, @Nullable Object mutex) {
         super(delegate, mutex);
      }

      SortedSet<E> delegate() {
         return (SortedSet)super.delegate();
      }

      public Comparator<? super E> comparator() {
         synchronized(this.mutex) {
            return this.delegate().comparator();
         }
      }

      public SortedSet<E> subSet(E fromElement, E toElement) {
         synchronized(this.mutex) {
            return Synchronized.sortedSet(this.delegate().subSet(fromElement, toElement), this.mutex);
         }
      }

      public SortedSet<E> headSet(E toElement) {
         synchronized(this.mutex) {
            return Synchronized.sortedSet(this.delegate().headSet(toElement), this.mutex);
         }
      }

      public SortedSet<E> tailSet(E fromElement) {
         synchronized(this.mutex) {
            return Synchronized.sortedSet(this.delegate().tailSet(fromElement), this.mutex);
         }
      }

      public E first() {
         synchronized(this.mutex) {
            return this.delegate().first();
         }
      }

      public E last() {
         synchronized(this.mutex) {
            return this.delegate().last();
         }
      }
   }

   static class SynchronizedSet<E> extends Synchronized.SynchronizedCollection<E> implements Set<E> {
      private static final long serialVersionUID = 0L;

      SynchronizedSet(Set<E> delegate, @Nullable Object mutex) {
         super(delegate, mutex, null);
      }

      Set<E> delegate() {
         return (Set)super.delegate();
      }

      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else {
            synchronized(this.mutex) {
               return this.delegate().equals(o);
            }
         }
      }

      public int hashCode() {
         synchronized(this.mutex) {
            return this.delegate().hashCode();
         }
      }
   }

   @VisibleForTesting
   static class SynchronizedCollection<E> extends Synchronized.SynchronizedObject implements Collection<E> {
      private static final long serialVersionUID = 0L;

      private SynchronizedCollection(Collection<E> delegate, @Nullable Object mutex) {
         super(delegate, mutex);
      }

      Collection<E> delegate() {
         return (Collection)super.delegate();
      }

      public boolean add(E e) {
         synchronized(this.mutex) {
            return this.delegate().add(e);
         }
      }

      public boolean addAll(Collection<? extends E> c) {
         synchronized(this.mutex) {
            return this.delegate().addAll(c);
         }
      }

      public void clear() {
         synchronized(this.mutex) {
            this.delegate().clear();
         }
      }

      public boolean contains(Object o) {
         synchronized(this.mutex) {
            return this.delegate().contains(o);
         }
      }

      public boolean containsAll(Collection<?> c) {
         synchronized(this.mutex) {
            return this.delegate().containsAll(c);
         }
      }

      public boolean isEmpty() {
         synchronized(this.mutex) {
            return this.delegate().isEmpty();
         }
      }

      public Iterator<E> iterator() {
         return this.delegate().iterator();
      }

      public boolean remove(Object o) {
         synchronized(this.mutex) {
            return this.delegate().remove(o);
         }
      }

      public boolean removeAll(Collection<?> c) {
         synchronized(this.mutex) {
            return this.delegate().removeAll(c);
         }
      }

      public boolean retainAll(Collection<?> c) {
         synchronized(this.mutex) {
            return this.delegate().retainAll(c);
         }
      }

      public int size() {
         synchronized(this.mutex) {
            return this.delegate().size();
         }
      }

      public Object[] toArray() {
         synchronized(this.mutex) {
            return this.delegate().toArray();
         }
      }

      public <T> T[] toArray(T[] a) {
         synchronized(this.mutex) {
            return this.delegate().toArray(a);
         }
      }

      // $FF: synthetic method
      SynchronizedCollection(Collection x0, Object x1, Object x2) {
         this(x0, x1);
      }
   }

   static class SynchronizedObject implements Serializable {
      final Object delegate;
      final Object mutex;
      @GwtIncompatible("not needed in emulated source")
      private static final long serialVersionUID = 0L;

      SynchronizedObject(Object delegate, @Nullable Object mutex) {
         this.delegate = Preconditions.checkNotNull(delegate);
         this.mutex = mutex == null ? this : mutex;
      }

      Object delegate() {
         return this.delegate;
      }

      public String toString() {
         synchronized(this.mutex) {
            return this.delegate.toString();
         }
      }

      @GwtIncompatible("java.io.ObjectOutputStream")
      private void writeObject(ObjectOutputStream stream) throws IOException {
         synchronized(this.mutex) {
            stream.defaultWriteObject();
         }
      }
   }
}
