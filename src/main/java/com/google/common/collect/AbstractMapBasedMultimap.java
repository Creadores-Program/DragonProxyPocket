package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible(
   emulated = true
)
abstract class AbstractMapBasedMultimap<K, V> extends AbstractMultimap<K, V> implements Serializable {
   private transient Map<K, Collection<V>> map;
   private transient int totalSize;
   private static final long serialVersionUID = 2447537837011683357L;

   protected AbstractMapBasedMultimap(Map<K, Collection<V>> map) {
      Preconditions.checkArgument(map.isEmpty());
      this.map = map;
   }

   final void setMap(Map<K, Collection<V>> map) {
      this.map = map;
      this.totalSize = 0;

      Collection values;
      for(Iterator i$ = map.values().iterator(); i$.hasNext(); this.totalSize += values.size()) {
         values = (Collection)i$.next();
         Preconditions.checkArgument(!values.isEmpty());
      }

   }

   Collection<V> createUnmodifiableEmptyCollection() {
      return this.unmodifiableCollectionSubclass(this.createCollection());
   }

   abstract Collection<V> createCollection();

   Collection<V> createCollection(@Nullable K key) {
      return this.createCollection();
   }

   Map<K, Collection<V>> backingMap() {
      return this.map;
   }

   public int size() {
      return this.totalSize;
   }

   public boolean containsKey(@Nullable Object key) {
      return this.map.containsKey(key);
   }

   public boolean put(@Nullable K key, @Nullable V value) {
      Collection<V> collection = (Collection)this.map.get(key);
      if (collection == null) {
         collection = this.createCollection(key);
         if (collection.add(value)) {
            ++this.totalSize;
            this.map.put(key, collection);
            return true;
         } else {
            throw new AssertionError("New Collection violated the Collection spec");
         }
      } else if (collection.add(value)) {
         ++this.totalSize;
         return true;
      } else {
         return false;
      }
   }

   private Collection<V> getOrCreateCollection(@Nullable K key) {
      Collection<V> collection = (Collection)this.map.get(key);
      if (collection == null) {
         collection = this.createCollection(key);
         this.map.put(key, collection);
      }

      return collection;
   }

   public Collection<V> replaceValues(@Nullable K key, Iterable<? extends V> values) {
      Iterator<? extends V> iterator = values.iterator();
      if (!iterator.hasNext()) {
         return this.removeAll(key);
      } else {
         Collection<V> collection = this.getOrCreateCollection(key);
         Collection<V> oldValues = this.createCollection();
         oldValues.addAll(collection);
         this.totalSize -= collection.size();
         collection.clear();

         while(iterator.hasNext()) {
            if (collection.add(iterator.next())) {
               ++this.totalSize;
            }
         }

         return this.unmodifiableCollectionSubclass(oldValues);
      }
   }

   public Collection<V> removeAll(@Nullable Object key) {
      Collection<V> collection = (Collection)this.map.remove(key);
      if (collection == null) {
         return this.createUnmodifiableEmptyCollection();
      } else {
         Collection<V> output = this.createCollection();
         output.addAll(collection);
         this.totalSize -= collection.size();
         collection.clear();
         return this.unmodifiableCollectionSubclass(output);
      }
   }

   Collection<V> unmodifiableCollectionSubclass(Collection<V> collection) {
      if (collection instanceof SortedSet) {
         return Collections.unmodifiableSortedSet((SortedSet)collection);
      } else if (collection instanceof Set) {
         return Collections.unmodifiableSet((Set)collection);
      } else {
         return (Collection)(collection instanceof List ? Collections.unmodifiableList((List)collection) : Collections.unmodifiableCollection(collection));
      }
   }

   public void clear() {
      Iterator i$ = this.map.values().iterator();

      while(i$.hasNext()) {
         Collection<V> collection = (Collection)i$.next();
         collection.clear();
      }

      this.map.clear();
      this.totalSize = 0;
   }

   public Collection<V> get(@Nullable K key) {
      Collection<V> collection = (Collection)this.map.get(key);
      if (collection == null) {
         collection = this.createCollection(key);
      }

      return this.wrapCollection(key, collection);
   }

   Collection<V> wrapCollection(@Nullable K key, Collection<V> collection) {
      if (collection instanceof SortedSet) {
         return new AbstractMapBasedMultimap.WrappedSortedSet(key, (SortedSet)collection, (AbstractMapBasedMultimap.WrappedCollection)null);
      } else if (collection instanceof Set) {
         return new AbstractMapBasedMultimap.WrappedSet(key, (Set)collection);
      } else {
         return (Collection)(collection instanceof List ? this.wrapList(key, (List)collection, (AbstractMapBasedMultimap.WrappedCollection)null) : new AbstractMapBasedMultimap.WrappedCollection(key, collection, (AbstractMapBasedMultimap.WrappedCollection)null));
      }
   }

   private List<V> wrapList(@Nullable K key, List<V> list, @Nullable AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor) {
      return (List)(list instanceof RandomAccess ? new AbstractMapBasedMultimap.RandomAccessWrappedList(key, list, ancestor) : new AbstractMapBasedMultimap.WrappedList(key, list, ancestor));
   }

   private Iterator<V> iteratorOrListIterator(Collection<V> collection) {
      return (Iterator)(collection instanceof List ? ((List)collection).listIterator() : collection.iterator());
   }

   Set<K> createKeySet() {
      return (Set)(this.map instanceof SortedMap ? new AbstractMapBasedMultimap.SortedKeySet((SortedMap)this.map) : new AbstractMapBasedMultimap.KeySet(this.map));
   }

   private int removeValuesForKey(Object key) {
      Collection<V> collection = (Collection)Maps.safeRemove(this.map, key);
      int count = 0;
      if (collection != null) {
         count = collection.size();
         collection.clear();
         this.totalSize -= count;
      }

      return count;
   }

   public Collection<V> values() {
      return super.values();
   }

   Iterator<V> valueIterator() {
      return new AbstractMapBasedMultimap<K, V>.Itr<V>() {
         V output(K key, V value) {
            return value;
         }
      };
   }

   public Collection<Entry<K, V>> entries() {
      return super.entries();
   }

   Iterator<Entry<K, V>> entryIterator() {
      return new AbstractMapBasedMultimap<K, V>.Itr<Entry<K, V>>() {
         Entry<K, V> output(K key, V value) {
            return Maps.immutableEntry(key, value);
         }
      };
   }

   Map<K, Collection<V>> createAsMap() {
      return (Map)(this.map instanceof SortedMap ? new AbstractMapBasedMultimap.SortedAsMap((SortedMap)this.map) : new AbstractMapBasedMultimap.AsMap(this.map));
   }

   @GwtIncompatible("NavigableAsMap")
   class NavigableAsMap extends AbstractMapBasedMultimap<K, V>.SortedAsMap implements NavigableMap<K, Collection<V>> {
      NavigableAsMap(NavigableMap<K, Collection<V>> submap) {
         super(submap);
      }

      NavigableMap<K, Collection<V>> sortedMap() {
         return (NavigableMap)super.sortedMap();
      }

      public Entry<K, Collection<V>> lowerEntry(K key) {
         Entry<K, Collection<V>> entry = this.sortedMap().lowerEntry(key);
         return entry == null ? null : this.wrapEntry(entry);
      }

      public K lowerKey(K key) {
         return this.sortedMap().lowerKey(key);
      }

      public Entry<K, Collection<V>> floorEntry(K key) {
         Entry<K, Collection<V>> entry = this.sortedMap().floorEntry(key);
         return entry == null ? null : this.wrapEntry(entry);
      }

      public K floorKey(K key) {
         return this.sortedMap().floorKey(key);
      }

      public Entry<K, Collection<V>> ceilingEntry(K key) {
         Entry<K, Collection<V>> entry = this.sortedMap().ceilingEntry(key);
         return entry == null ? null : this.wrapEntry(entry);
      }

      public K ceilingKey(K key) {
         return this.sortedMap().ceilingKey(key);
      }

      public Entry<K, Collection<V>> higherEntry(K key) {
         Entry<K, Collection<V>> entry = this.sortedMap().higherEntry(key);
         return entry == null ? null : this.wrapEntry(entry);
      }

      public K higherKey(K key) {
         return this.sortedMap().higherKey(key);
      }

      public Entry<K, Collection<V>> firstEntry() {
         Entry<K, Collection<V>> entry = this.sortedMap().firstEntry();
         return entry == null ? null : this.wrapEntry(entry);
      }

      public Entry<K, Collection<V>> lastEntry() {
         Entry<K, Collection<V>> entry = this.sortedMap().lastEntry();
         return entry == null ? null : this.wrapEntry(entry);
      }

      public Entry<K, Collection<V>> pollFirstEntry() {
         return this.pollAsMapEntry(this.entrySet().iterator());
      }

      public Entry<K, Collection<V>> pollLastEntry() {
         return this.pollAsMapEntry(this.descendingMap().entrySet().iterator());
      }

      Entry<K, Collection<V>> pollAsMapEntry(Iterator<Entry<K, Collection<V>>> entryIterator) {
         if (!entryIterator.hasNext()) {
            return null;
         } else {
            Entry<K, Collection<V>> entry = (Entry)entryIterator.next();
            Collection<V> output = AbstractMapBasedMultimap.this.createCollection();
            output.addAll((Collection)entry.getValue());
            entryIterator.remove();
            return Maps.immutableEntry(entry.getKey(), AbstractMapBasedMultimap.this.unmodifiableCollectionSubclass(output));
         }
      }

      public NavigableMap<K, Collection<V>> descendingMap() {
         return AbstractMapBasedMultimap.this.new NavigableAsMap(this.sortedMap().descendingMap());
      }

      public NavigableSet<K> keySet() {
         return (NavigableSet)super.keySet();
      }

      NavigableSet<K> createKeySet() {
         return AbstractMapBasedMultimap.this.new NavigableKeySet(this.sortedMap());
      }

      public NavigableSet<K> navigableKeySet() {
         return this.keySet();
      }

      public NavigableSet<K> descendingKeySet() {
         return this.descendingMap().navigableKeySet();
      }

      public NavigableMap<K, Collection<V>> subMap(K fromKey, K toKey) {
         return this.subMap(fromKey, true, toKey, false);
      }

      public NavigableMap<K, Collection<V>> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
         return AbstractMapBasedMultimap.this.new NavigableAsMap(this.sortedMap().subMap(fromKey, fromInclusive, toKey, toInclusive));
      }

      public NavigableMap<K, Collection<V>> headMap(K toKey) {
         return this.headMap(toKey, false);
      }

      public NavigableMap<K, Collection<V>> headMap(K toKey, boolean inclusive) {
         return AbstractMapBasedMultimap.this.new NavigableAsMap(this.sortedMap().headMap(toKey, inclusive));
      }

      public NavigableMap<K, Collection<V>> tailMap(K fromKey) {
         return this.tailMap(fromKey, true);
      }

      public NavigableMap<K, Collection<V>> tailMap(K fromKey, boolean inclusive) {
         return AbstractMapBasedMultimap.this.new NavigableAsMap(this.sortedMap().tailMap(fromKey, inclusive));
      }
   }

   private class SortedAsMap extends AbstractMapBasedMultimap<K, V>.AsMap implements SortedMap<K, Collection<V>> {
      SortedSet<K> sortedKeySet;

      SortedAsMap(SortedMap<K, Collection<V>> submap) {
         super(submap);
      }

      SortedMap<K, Collection<V>> sortedMap() {
         return (SortedMap)this.submap;
      }

      public Comparator<? super K> comparator() {
         return this.sortedMap().comparator();
      }

      public K firstKey() {
         return this.sortedMap().firstKey();
      }

      public K lastKey() {
         return this.sortedMap().lastKey();
      }

      public SortedMap<K, Collection<V>> headMap(K toKey) {
         return AbstractMapBasedMultimap.this.new SortedAsMap(this.sortedMap().headMap(toKey));
      }

      public SortedMap<K, Collection<V>> subMap(K fromKey, K toKey) {
         return AbstractMapBasedMultimap.this.new SortedAsMap(this.sortedMap().subMap(fromKey, toKey));
      }

      public SortedMap<K, Collection<V>> tailMap(K fromKey) {
         return AbstractMapBasedMultimap.this.new SortedAsMap(this.sortedMap().tailMap(fromKey));
      }

      public SortedSet<K> keySet() {
         SortedSet<K> result = this.sortedKeySet;
         return result == null ? (this.sortedKeySet = this.createKeySet()) : result;
      }

      SortedSet<K> createKeySet() {
         return AbstractMapBasedMultimap.this.new SortedKeySet(this.sortedMap());
      }
   }

   private class AsMap extends Maps.ViewCachingAbstractMap<K, Collection<V>> {
      final transient Map<K, Collection<V>> submap;

      AsMap(Map<K, Collection<V>> submap) {
         this.submap = submap;
      }

      protected Set<Entry<K, Collection<V>>> createEntrySet() {
         return new AbstractMapBasedMultimap.AsMap.AsMapEntries();
      }

      public boolean containsKey(Object key) {
         return Maps.safeContainsKey(this.submap, key);
      }

      public Collection<V> get(Object key) {
         Collection<V> collection = (Collection)Maps.safeGet(this.submap, key);
         return collection == null ? null : AbstractMapBasedMultimap.this.wrapCollection(key, collection);
      }

      public Set<K> keySet() {
         return AbstractMapBasedMultimap.this.keySet();
      }

      public int size() {
         return this.submap.size();
      }

      public Collection<V> remove(Object key) {
         Collection<V> collection = (Collection)this.submap.remove(key);
         if (collection == null) {
            return null;
         } else {
            Collection<V> output = AbstractMapBasedMultimap.this.createCollection();
            output.addAll(collection);
            AbstractMapBasedMultimap.this.totalSize = collection.size();
            collection.clear();
            return output;
         }
      }

      public boolean equals(@Nullable Object object) {
         return this == object || this.submap.equals(object);
      }

      public int hashCode() {
         return this.submap.hashCode();
      }

      public String toString() {
         return this.submap.toString();
      }

      public void clear() {
         if (this.submap == AbstractMapBasedMultimap.this.map) {
            AbstractMapBasedMultimap.this.clear();
         } else {
            Iterators.clear(new AbstractMapBasedMultimap.AsMap.AsMapIterator());
         }

      }

      Entry<K, Collection<V>> wrapEntry(Entry<K, Collection<V>> entry) {
         K key = entry.getKey();
         return Maps.immutableEntry(key, AbstractMapBasedMultimap.this.wrapCollection(key, (Collection)entry.getValue()));
      }

      class AsMapIterator implements Iterator<Entry<K, Collection<V>>> {
         final Iterator<Entry<K, Collection<V>>> delegateIterator;
         Collection<V> collection;

         AsMapIterator() {
            this.delegateIterator = AsMap.this.submap.entrySet().iterator();
         }

         public boolean hasNext() {
            return this.delegateIterator.hasNext();
         }

         public Entry<K, Collection<V>> next() {
            Entry<K, Collection<V>> entry = (Entry)this.delegateIterator.next();
            this.collection = (Collection)entry.getValue();
            return AsMap.this.wrapEntry(entry);
         }

         public void remove() {
            this.delegateIterator.remove();
            AbstractMapBasedMultimap.this.totalSize = this.collection.size();
            this.collection.clear();
         }
      }

      class AsMapEntries extends Maps.EntrySet<K, Collection<V>> {
         Map<K, Collection<V>> map() {
            return AsMap.this;
         }

         public Iterator<Entry<K, Collection<V>>> iterator() {
            return AsMap.this.new AsMapIterator();
         }

         public boolean contains(Object o) {
            return Collections2.safeContains(AsMap.this.submap.entrySet(), o);
         }

         public boolean remove(Object o) {
            if (!this.contains(o)) {
               return false;
            } else {
               Entry<?, ?> entry = (Entry)o;
               AbstractMapBasedMultimap.this.removeValuesForKey(entry.getKey());
               return true;
            }
         }
      }
   }

   private abstract class Itr<T> implements Iterator<T> {
      final Iterator<Entry<K, Collection<V>>> keyIterator;
      K key;
      Collection<V> collection;
      Iterator<V> valueIterator;

      Itr() {
         this.keyIterator = AbstractMapBasedMultimap.this.map.entrySet().iterator();
         this.key = null;
         this.collection = null;
         this.valueIterator = Iterators.emptyModifiableIterator();
      }

      abstract T output(K var1, V var2);

      public boolean hasNext() {
         return this.keyIterator.hasNext() || this.valueIterator.hasNext();
      }

      public T next() {
         if (!this.valueIterator.hasNext()) {
            Entry<K, Collection<V>> mapEntry = (Entry)this.keyIterator.next();
            this.key = mapEntry.getKey();
            this.collection = (Collection)mapEntry.getValue();
            this.valueIterator = this.collection.iterator();
         }

         return this.output(this.key, this.valueIterator.next());
      }

      public void remove() {
         this.valueIterator.remove();
         if (this.collection.isEmpty()) {
            this.keyIterator.remove();
         }

         AbstractMapBasedMultimap.this.totalSize--;
      }
   }

   @GwtIncompatible("NavigableSet")
   class NavigableKeySet extends AbstractMapBasedMultimap<K, V>.SortedKeySet implements NavigableSet<K> {
      NavigableKeySet(NavigableMap<K, Collection<V>> subMap) {
         super(subMap);
      }

      NavigableMap<K, Collection<V>> sortedMap() {
         return (NavigableMap)super.sortedMap();
      }

      public K lower(K k) {
         return this.sortedMap().lowerKey(k);
      }

      public K floor(K k) {
         return this.sortedMap().floorKey(k);
      }

      public K ceiling(K k) {
         return this.sortedMap().ceilingKey(k);
      }

      public K higher(K k) {
         return this.sortedMap().higherKey(k);
      }

      public K pollFirst() {
         return Iterators.pollNext(this.iterator());
      }

      public K pollLast() {
         return Iterators.pollNext(this.descendingIterator());
      }

      public NavigableSet<K> descendingSet() {
         return AbstractMapBasedMultimap.this.new NavigableKeySet(this.sortedMap().descendingMap());
      }

      public Iterator<K> descendingIterator() {
         return this.descendingSet().iterator();
      }

      public NavigableSet<K> headSet(K toElement) {
         return this.headSet(toElement, false);
      }

      public NavigableSet<K> headSet(K toElement, boolean inclusive) {
         return AbstractMapBasedMultimap.this.new NavigableKeySet(this.sortedMap().headMap(toElement, inclusive));
      }

      public NavigableSet<K> subSet(K fromElement, K toElement) {
         return this.subSet(fromElement, true, toElement, false);
      }

      public NavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement, boolean toInclusive) {
         return AbstractMapBasedMultimap.this.new NavigableKeySet(this.sortedMap().subMap(fromElement, fromInclusive, toElement, toInclusive));
      }

      public NavigableSet<K> tailSet(K fromElement) {
         return this.tailSet(fromElement, true);
      }

      public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
         return AbstractMapBasedMultimap.this.new NavigableKeySet(this.sortedMap().tailMap(fromElement, inclusive));
      }
   }

   private class SortedKeySet extends AbstractMapBasedMultimap<K, V>.KeySet implements SortedSet<K> {
      SortedKeySet(SortedMap<K, Collection<V>> subMap) {
         super(subMap);
      }

      SortedMap<K, Collection<V>> sortedMap() {
         return (SortedMap)super.map();
      }

      public Comparator<? super K> comparator() {
         return this.sortedMap().comparator();
      }

      public K first() {
         return this.sortedMap().firstKey();
      }

      public SortedSet<K> headSet(K toElement) {
         return AbstractMapBasedMultimap.this.new SortedKeySet(this.sortedMap().headMap(toElement));
      }

      public K last() {
         return this.sortedMap().lastKey();
      }

      public SortedSet<K> subSet(K fromElement, K toElement) {
         return AbstractMapBasedMultimap.this.new SortedKeySet(this.sortedMap().subMap(fromElement, toElement));
      }

      public SortedSet<K> tailSet(K fromElement) {
         return AbstractMapBasedMultimap.this.new SortedKeySet(this.sortedMap().tailMap(fromElement));
      }
   }

   private class KeySet extends Maps.KeySet<K, Collection<V>> {
      KeySet(Map<K, Collection<V>> subMap) {
         super(subMap);
      }

      public Iterator<K> iterator() {
         final Iterator<Entry<K, Collection<V>>> entryIterator = this.map().entrySet().iterator();
         return new Iterator<K>() {
            Entry<K, Collection<V>> entry;

            public boolean hasNext() {
               return entryIterator.hasNext();
            }

            public K next() {
               this.entry = (Entry)entryIterator.next();
               return this.entry.getKey();
            }

            public void remove() {
               CollectPreconditions.checkRemove(this.entry != null);
               Collection<V> collection = (Collection)this.entry.getValue();
               entryIterator.remove();
               AbstractMapBasedMultimap.this.totalSize = collection.size();
               collection.clear();
            }
         };
      }

      public boolean remove(Object key) {
         int count = 0;
         Collection<V> collection = (Collection)this.map().remove(key);
         if (collection != null) {
            count = collection.size();
            collection.clear();
            AbstractMapBasedMultimap.this.totalSize = count;
         }

         return count > 0;
      }

      public void clear() {
         Iterators.clear(this.iterator());
      }

      public boolean containsAll(Collection<?> c) {
         return this.map().keySet().containsAll(c);
      }

      public boolean equals(@Nullable Object object) {
         return this == object || this.map().keySet().equals(object);
      }

      public int hashCode() {
         return this.map().keySet().hashCode();
      }
   }

   private class RandomAccessWrappedList extends AbstractMapBasedMultimap<K, V>.WrappedList implements RandomAccess {
      RandomAccessWrappedList(@Nullable K key, List<V> delegate, @Nullable AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor) {
         super(key, delegate, ancestor);
      }
   }

   private class WrappedList extends AbstractMapBasedMultimap<K, V>.WrappedCollection implements List<V> {
      WrappedList(@Nullable K key, List<V> delegate, @Nullable AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor) {
         super(key, delegate, ancestor);
      }

      List<V> getListDelegate() {
         return (List)this.getDelegate();
      }

      public boolean addAll(int index, Collection<? extends V> c) {
         if (c.isEmpty()) {
            return false;
         } else {
            int oldSize = this.size();
            boolean changed = this.getListDelegate().addAll(index, c);
            if (changed) {
               int newSize = this.getDelegate().size();
               AbstractMapBasedMultimap.this.totalSize = newSize - oldSize;
               if (oldSize == 0) {
                  this.addToMap();
               }
            }

            return changed;
         }
      }

      public V get(int index) {
         this.refreshIfEmpty();
         return this.getListDelegate().get(index);
      }

      public V set(int index, V element) {
         this.refreshIfEmpty();
         return this.getListDelegate().set(index, element);
      }

      public void add(int index, V element) {
         this.refreshIfEmpty();
         boolean wasEmpty = this.getDelegate().isEmpty();
         this.getListDelegate().add(index, element);
         AbstractMapBasedMultimap.this.totalSize++;
         if (wasEmpty) {
            this.addToMap();
         }

      }

      public V remove(int index) {
         this.refreshIfEmpty();
         V value = this.getListDelegate().remove(index);
         AbstractMapBasedMultimap.this.totalSize--;
         this.removeIfEmpty();
         return value;
      }

      public int indexOf(Object o) {
         this.refreshIfEmpty();
         return this.getListDelegate().indexOf(o);
      }

      public int lastIndexOf(Object o) {
         this.refreshIfEmpty();
         return this.getListDelegate().lastIndexOf(o);
      }

      public ListIterator<V> listIterator() {
         this.refreshIfEmpty();
         return new AbstractMapBasedMultimap.WrappedList.WrappedListIterator();
      }

      public ListIterator<V> listIterator(int index) {
         this.refreshIfEmpty();
         return new AbstractMapBasedMultimap.WrappedList.WrappedListIterator(index);
      }

      public List<V> subList(int fromIndex, int toIndex) {
         this.refreshIfEmpty();
         return AbstractMapBasedMultimap.this.wrapList(this.getKey(), this.getListDelegate().subList(fromIndex, toIndex), (AbstractMapBasedMultimap.WrappedCollection)(this.getAncestor() == null ? this : this.getAncestor()));
      }

      private class WrappedListIterator extends AbstractMapBasedMultimap<K, V>.WrappedCollection.WrappedIterator implements ListIterator<V> {
         WrappedListIterator() {
            super();
         }

         public WrappedListIterator(int index) {
            super(WrappedList.this.getListDelegate().listIterator(index));
         }

         private ListIterator<V> getDelegateListIterator() {
            return (ListIterator)this.getDelegateIterator();
         }

         public boolean hasPrevious() {
            return this.getDelegateListIterator().hasPrevious();
         }

         public V previous() {
            return this.getDelegateListIterator().previous();
         }

         public int nextIndex() {
            return this.getDelegateListIterator().nextIndex();
         }

         public int previousIndex() {
            return this.getDelegateListIterator().previousIndex();
         }

         public void set(V value) {
            this.getDelegateListIterator().set(value);
         }

         public void add(V value) {
            boolean wasEmpty = WrappedList.this.isEmpty();
            this.getDelegateListIterator().add(value);
            AbstractMapBasedMultimap.this.totalSize++;
            if (wasEmpty) {
               WrappedList.this.addToMap();
            }

         }
      }
   }

   @GwtIncompatible("NavigableSet")
   class WrappedNavigableSet extends AbstractMapBasedMultimap<K, V>.WrappedSortedSet implements NavigableSet<V> {
      WrappedNavigableSet(@Nullable K key, NavigableSet<V> delegate, @Nullable AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor) {
         super(key, delegate, ancestor);
      }

      NavigableSet<V> getSortedSetDelegate() {
         return (NavigableSet)super.getSortedSetDelegate();
      }

      public V lower(V v) {
         return this.getSortedSetDelegate().lower(v);
      }

      public V floor(V v) {
         return this.getSortedSetDelegate().floor(v);
      }

      public V ceiling(V v) {
         return this.getSortedSetDelegate().ceiling(v);
      }

      public V higher(V v) {
         return this.getSortedSetDelegate().higher(v);
      }

      public V pollFirst() {
         return Iterators.pollNext(this.iterator());
      }

      public V pollLast() {
         return Iterators.pollNext(this.descendingIterator());
      }

      private NavigableSet<V> wrap(NavigableSet<V> wrapped) {
         return AbstractMapBasedMultimap.this.new WrappedNavigableSet(this.key, wrapped, (AbstractMapBasedMultimap.WrappedCollection)(this.getAncestor() == null ? this : this.getAncestor()));
      }

      public NavigableSet<V> descendingSet() {
         return this.wrap(this.getSortedSetDelegate().descendingSet());
      }

      public Iterator<V> descendingIterator() {
         return new AbstractMapBasedMultimap.WrappedCollection.WrappedIterator(this.getSortedSetDelegate().descendingIterator());
      }

      public NavigableSet<V> subSet(V fromElement, boolean fromInclusive, V toElement, boolean toInclusive) {
         return this.wrap(this.getSortedSetDelegate().subSet(fromElement, fromInclusive, toElement, toInclusive));
      }

      public NavigableSet<V> headSet(V toElement, boolean inclusive) {
         return this.wrap(this.getSortedSetDelegate().headSet(toElement, inclusive));
      }

      public NavigableSet<V> tailSet(V fromElement, boolean inclusive) {
         return this.wrap(this.getSortedSetDelegate().tailSet(fromElement, inclusive));
      }
   }

   private class WrappedSortedSet extends AbstractMapBasedMultimap<K, V>.WrappedCollection implements SortedSet<V> {
      WrappedSortedSet(@Nullable K key, SortedSet<V> delegate, @Nullable AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor) {
         super(key, delegate, ancestor);
      }

      SortedSet<V> getSortedSetDelegate() {
         return (SortedSet)this.getDelegate();
      }

      public Comparator<? super V> comparator() {
         return this.getSortedSetDelegate().comparator();
      }

      public V first() {
         this.refreshIfEmpty();
         return this.getSortedSetDelegate().first();
      }

      public V last() {
         this.refreshIfEmpty();
         return this.getSortedSetDelegate().last();
      }

      public SortedSet<V> headSet(V toElement) {
         this.refreshIfEmpty();
         return AbstractMapBasedMultimap.this.new WrappedSortedSet(this.getKey(), this.getSortedSetDelegate().headSet(toElement), (AbstractMapBasedMultimap.WrappedCollection)(this.getAncestor() == null ? this : this.getAncestor()));
      }

      public SortedSet<V> subSet(V fromElement, V toElement) {
         this.refreshIfEmpty();
         return AbstractMapBasedMultimap.this.new WrappedSortedSet(this.getKey(), this.getSortedSetDelegate().subSet(fromElement, toElement), (AbstractMapBasedMultimap.WrappedCollection)(this.getAncestor() == null ? this : this.getAncestor()));
      }

      public SortedSet<V> tailSet(V fromElement) {
         this.refreshIfEmpty();
         return AbstractMapBasedMultimap.this.new WrappedSortedSet(this.getKey(), this.getSortedSetDelegate().tailSet(fromElement), (AbstractMapBasedMultimap.WrappedCollection)(this.getAncestor() == null ? this : this.getAncestor()));
      }
   }

   private class WrappedSet extends AbstractMapBasedMultimap<K, V>.WrappedCollection implements Set<V> {
      WrappedSet(@Nullable K key, Set<V> delegate) {
         super(key, delegate, (AbstractMapBasedMultimap.WrappedCollection)null);
      }

      public boolean removeAll(Collection<?> c) {
         if (c.isEmpty()) {
            return false;
         } else {
            int oldSize = this.size();
            boolean changed = Sets.removeAllImpl((Set)this.delegate, c);
            if (changed) {
               int newSize = this.delegate.size();
               AbstractMapBasedMultimap.this.totalSize = newSize - oldSize;
               this.removeIfEmpty();
            }

            return changed;
         }
      }
   }

   private class WrappedCollection extends AbstractCollection<V> {
      final K key;
      Collection<V> delegate;
      final AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor;
      final Collection<V> ancestorDelegate;

      WrappedCollection(@Nullable K key, Collection<V> delegate, @Nullable AbstractMapBasedMultimap<K, V>.WrappedCollection ancestor) {
         this.key = key;
         this.delegate = delegate;
         this.ancestor = ancestor;
         this.ancestorDelegate = ancestor == null ? null : ancestor.getDelegate();
      }

      void refreshIfEmpty() {
         if (this.ancestor != null) {
            this.ancestor.refreshIfEmpty();
            if (this.ancestor.getDelegate() != this.ancestorDelegate) {
               throw new ConcurrentModificationException();
            }
         } else if (this.delegate.isEmpty()) {
            Collection<V> newDelegate = (Collection)AbstractMapBasedMultimap.this.map.get(this.key);
            if (newDelegate != null) {
               this.delegate = newDelegate;
            }
         }

      }

      void removeIfEmpty() {
         if (this.ancestor != null) {
            this.ancestor.removeIfEmpty();
         } else if (this.delegate.isEmpty()) {
            AbstractMapBasedMultimap.this.map.remove(this.key);
         }

      }

      K getKey() {
         return this.key;
      }

      void addToMap() {
         if (this.ancestor != null) {
            this.ancestor.addToMap();
         } else {
            AbstractMapBasedMultimap.this.map.put(this.key, this.delegate);
         }

      }

      public int size() {
         this.refreshIfEmpty();
         return this.delegate.size();
      }

      public boolean equals(@Nullable Object object) {
         if (object == this) {
            return true;
         } else {
            this.refreshIfEmpty();
            return this.delegate.equals(object);
         }
      }

      public int hashCode() {
         this.refreshIfEmpty();
         return this.delegate.hashCode();
      }

      public String toString() {
         this.refreshIfEmpty();
         return this.delegate.toString();
      }

      Collection<V> getDelegate() {
         return this.delegate;
      }

      public Iterator<V> iterator() {
         this.refreshIfEmpty();
         return new AbstractMapBasedMultimap.WrappedCollection.WrappedIterator();
      }

      public boolean add(V value) {
         this.refreshIfEmpty();
         boolean wasEmpty = this.delegate.isEmpty();
         boolean changed = this.delegate.add(value);
         if (changed) {
            AbstractMapBasedMultimap.this.totalSize++;
            if (wasEmpty) {
               this.addToMap();
            }
         }

         return changed;
      }

      AbstractMapBasedMultimap<K, V>.WrappedCollection getAncestor() {
         return this.ancestor;
      }

      public boolean addAll(Collection<? extends V> collection) {
         if (collection.isEmpty()) {
            return false;
         } else {
            int oldSize = this.size();
            boolean changed = this.delegate.addAll(collection);
            if (changed) {
               int newSize = this.delegate.size();
               AbstractMapBasedMultimap.this.totalSize = newSize - oldSize;
               if (oldSize == 0) {
                  this.addToMap();
               }
            }

            return changed;
         }
      }

      public boolean contains(Object o) {
         this.refreshIfEmpty();
         return this.delegate.contains(o);
      }

      public boolean containsAll(Collection<?> c) {
         this.refreshIfEmpty();
         return this.delegate.containsAll(c);
      }

      public void clear() {
         int oldSize = this.size();
         if (oldSize != 0) {
            this.delegate.clear();
            AbstractMapBasedMultimap.this.totalSize = oldSize;
            this.removeIfEmpty();
         }
      }

      public boolean remove(Object o) {
         this.refreshIfEmpty();
         boolean changed = this.delegate.remove(o);
         if (changed) {
            AbstractMapBasedMultimap.this.totalSize--;
            this.removeIfEmpty();
         }

         return changed;
      }

      public boolean removeAll(Collection<?> c) {
         if (c.isEmpty()) {
            return false;
         } else {
            int oldSize = this.size();
            boolean changed = this.delegate.removeAll(c);
            if (changed) {
               int newSize = this.delegate.size();
               AbstractMapBasedMultimap.this.totalSize = newSize - oldSize;
               this.removeIfEmpty();
            }

            return changed;
         }
      }

      public boolean retainAll(Collection<?> c) {
         Preconditions.checkNotNull(c);
         int oldSize = this.size();
         boolean changed = this.delegate.retainAll(c);
         if (changed) {
            int newSize = this.delegate.size();
            AbstractMapBasedMultimap.this.totalSize = newSize - oldSize;
            this.removeIfEmpty();
         }

         return changed;
      }

      class WrappedIterator implements Iterator<V> {
         final Iterator<V> delegateIterator;
         final Collection<V> originalDelegate;

         WrappedIterator() {
            this.originalDelegate = WrappedCollection.this.delegate;
            this.delegateIterator = AbstractMapBasedMultimap.this.iteratorOrListIterator(WrappedCollection.this.delegate);
         }

         WrappedIterator(Iterator<V> delegateIterator) {
            this.originalDelegate = WrappedCollection.this.delegate;
            this.delegateIterator = delegateIterator;
         }

         void validateIterator() {
            WrappedCollection.this.refreshIfEmpty();
            if (WrappedCollection.this.delegate != this.originalDelegate) {
               throw new ConcurrentModificationException();
            }
         }

         public boolean hasNext() {
            this.validateIterator();
            return this.delegateIterator.hasNext();
         }

         public V next() {
            this.validateIterator();
            return this.delegateIterator.next();
         }

         public void remove() {
            this.delegateIterator.remove();
            AbstractMapBasedMultimap.this.totalSize--;
            WrappedCollection.this.removeIfEmpty();
         }

         Iterator<V> getDelegateIterator() {
            this.validateIterator();
            return this.delegateIterator;
         }
      }
   }
}
