package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Map.Entry;
import javax.annotation.Nullable;

/** @deprecated */
@Deprecated
@Beta
@GwtCompatible
public final class MapConstraints {
   private MapConstraints() {
   }

   public static MapConstraint<Object, Object> notNull() {
      return MapConstraints.NotNullMapConstraint.INSTANCE;
   }

   public static <K, V> Map<K, V> constrainedMap(Map<K, V> map, MapConstraint<? super K, ? super V> constraint) {
      return new MapConstraints.ConstrainedMap(map, constraint);
   }

   public static <K, V> Multimap<K, V> constrainedMultimap(Multimap<K, V> multimap, MapConstraint<? super K, ? super V> constraint) {
      return new MapConstraints.ConstrainedMultimap(multimap, constraint);
   }

   public static <K, V> ListMultimap<K, V> constrainedListMultimap(ListMultimap<K, V> multimap, MapConstraint<? super K, ? super V> constraint) {
      return new MapConstraints.ConstrainedListMultimap(multimap, constraint);
   }

   public static <K, V> SetMultimap<K, V> constrainedSetMultimap(SetMultimap<K, V> multimap, MapConstraint<? super K, ? super V> constraint) {
      return new MapConstraints.ConstrainedSetMultimap(multimap, constraint);
   }

   public static <K, V> SortedSetMultimap<K, V> constrainedSortedSetMultimap(SortedSetMultimap<K, V> multimap, MapConstraint<? super K, ? super V> constraint) {
      return new MapConstraints.ConstrainedSortedSetMultimap(multimap, constraint);
   }

   private static <K, V> Entry<K, V> constrainedEntry(final Entry<K, V> entry, final MapConstraint<? super K, ? super V> constraint) {
      Preconditions.checkNotNull(entry);
      Preconditions.checkNotNull(constraint);
      return new ForwardingMapEntry<K, V>() {
         protected Entry<K, V> delegate() {
            return entry;
         }

         public V setValue(V value) {
            constraint.checkKeyValue(this.getKey(), value);
            return entry.setValue(value);
         }
      };
   }

   private static <K, V> Entry<K, Collection<V>> constrainedAsMapEntry(final Entry<K, Collection<V>> entry, final MapConstraint<? super K, ? super V> constraint) {
      Preconditions.checkNotNull(entry);
      Preconditions.checkNotNull(constraint);
      return new ForwardingMapEntry<K, Collection<V>>() {
         protected Entry<K, Collection<V>> delegate() {
            return entry;
         }

         public Collection<V> getValue() {
            return Constraints.constrainedTypePreservingCollection((Collection)entry.getValue(), new Constraint<V>() {
               public V checkElement(V value) {
                  constraint.checkKeyValue(getKey(), value);
                  return value;
               }
            });
         }
      };
   }

   private static <K, V> Set<Entry<K, Collection<V>>> constrainedAsMapEntries(Set<Entry<K, Collection<V>>> entries, MapConstraint<? super K, ? super V> constraint) {
      return new MapConstraints.ConstrainedAsMapEntries(entries, constraint);
   }

   private static <K, V> Collection<Entry<K, V>> constrainedEntries(Collection<Entry<K, V>> entries, MapConstraint<? super K, ? super V> constraint) {
      return (Collection)(entries instanceof Set ? constrainedEntrySet((Set)entries, constraint) : new MapConstraints.ConstrainedEntries(entries, constraint));
   }

   private static <K, V> Set<Entry<K, V>> constrainedEntrySet(Set<Entry<K, V>> entries, MapConstraint<? super K, ? super V> constraint) {
      return new MapConstraints.ConstrainedEntrySet(entries, constraint);
   }

   public static <K, V> BiMap<K, V> constrainedBiMap(BiMap<K, V> map, MapConstraint<? super K, ? super V> constraint) {
      return new MapConstraints.ConstrainedBiMap(map, (BiMap)null, constraint);
   }

   private static <K, V> Collection<V> checkValues(K key, Iterable<? extends V> values, MapConstraint<? super K, ? super V> constraint) {
      Collection<V> copy = Lists.newArrayList(values);
      Iterator i$ = copy.iterator();

      while(i$.hasNext()) {
         V value = i$.next();
         constraint.checkKeyValue(key, value);
      }

      return copy;
   }

   private static <K, V> Map<K, V> checkMap(Map<? extends K, ? extends V> map, MapConstraint<? super K, ? super V> constraint) {
      Map<K, V> copy = new LinkedHashMap(map);
      Iterator i$ = copy.entrySet().iterator();

      while(i$.hasNext()) {
         Entry<K, V> entry = (Entry)i$.next();
         constraint.checkKeyValue(entry.getKey(), entry.getValue());
      }

      return copy;
   }

   private static class ConstrainedSortedSetMultimap<K, V> extends MapConstraints.ConstrainedSetMultimap<K, V> implements SortedSetMultimap<K, V> {
      ConstrainedSortedSetMultimap(SortedSetMultimap<K, V> delegate, MapConstraint<? super K, ? super V> constraint) {
         super(delegate, constraint);
      }

      public SortedSet<V> get(K key) {
         return (SortedSet)super.get(key);
      }

      public SortedSet<V> removeAll(Object key) {
         return (SortedSet)super.removeAll(key);
      }

      public SortedSet<V> replaceValues(K key, Iterable<? extends V> values) {
         return (SortedSet)super.replaceValues(key, values);
      }

      public Comparator<? super V> valueComparator() {
         return ((SortedSetMultimap)this.delegate()).valueComparator();
      }
   }

   private static class ConstrainedSetMultimap<K, V> extends MapConstraints.ConstrainedMultimap<K, V> implements SetMultimap<K, V> {
      ConstrainedSetMultimap(SetMultimap<K, V> delegate, MapConstraint<? super K, ? super V> constraint) {
         super(delegate, constraint);
      }

      public Set<V> get(K key) {
         return (Set)super.get(key);
      }

      public Set<Entry<K, V>> entries() {
         return (Set)super.entries();
      }

      public Set<V> removeAll(Object key) {
         return (Set)super.removeAll(key);
      }

      public Set<V> replaceValues(K key, Iterable<? extends V> values) {
         return (Set)super.replaceValues(key, values);
      }
   }

   private static class ConstrainedListMultimap<K, V> extends MapConstraints.ConstrainedMultimap<K, V> implements ListMultimap<K, V> {
      ConstrainedListMultimap(ListMultimap<K, V> delegate, MapConstraint<? super K, ? super V> constraint) {
         super(delegate, constraint);
      }

      public List<V> get(K key) {
         return (List)super.get(key);
      }

      public List<V> removeAll(Object key) {
         return (List)super.removeAll(key);
      }

      public List<V> replaceValues(K key, Iterable<? extends V> values) {
         return (List)super.replaceValues(key, values);
      }
   }

   static class ConstrainedAsMapEntries<K, V> extends ForwardingSet<Entry<K, Collection<V>>> {
      private final MapConstraint<? super K, ? super V> constraint;
      private final Set<Entry<K, Collection<V>>> entries;

      ConstrainedAsMapEntries(Set<Entry<K, Collection<V>>> entries, MapConstraint<? super K, ? super V> constraint) {
         this.entries = entries;
         this.constraint = constraint;
      }

      protected Set<Entry<K, Collection<V>>> delegate() {
         return this.entries;
      }

      public Iterator<Entry<K, Collection<V>>> iterator() {
         return new TransformedIterator<Entry<K, Collection<V>>, Entry<K, Collection<V>>>(this.entries.iterator()) {
            Entry<K, Collection<V>> transform(Entry<K, Collection<V>> from) {
               return MapConstraints.constrainedAsMapEntry(from, ConstrainedAsMapEntries.this.constraint);
            }
         };
      }

      public Object[] toArray() {
         return this.standardToArray();
      }

      public <T> T[] toArray(T[] array) {
         return this.standardToArray(array);
      }

      public boolean contains(Object o) {
         return Maps.containsEntryImpl(this.delegate(), o);
      }

      public boolean containsAll(Collection<?> c) {
         return this.standardContainsAll(c);
      }

      public boolean equals(@Nullable Object object) {
         return this.standardEquals(object);
      }

      public int hashCode() {
         return this.standardHashCode();
      }

      public boolean remove(Object o) {
         return Maps.removeEntryImpl(this.delegate(), o);
      }

      public boolean removeAll(Collection<?> c) {
         return this.standardRemoveAll(c);
      }

      public boolean retainAll(Collection<?> c) {
         return this.standardRetainAll(c);
      }
   }

   static class ConstrainedEntrySet<K, V> extends MapConstraints.ConstrainedEntries<K, V> implements Set<Entry<K, V>> {
      ConstrainedEntrySet(Set<Entry<K, V>> entries, MapConstraint<? super K, ? super V> constraint) {
         super(entries, constraint);
      }

      public boolean equals(@Nullable Object object) {
         return Sets.equalsImpl(this, object);
      }

      public int hashCode() {
         return Sets.hashCodeImpl(this);
      }
   }

   private static class ConstrainedEntries<K, V> extends ForwardingCollection<Entry<K, V>> {
      final MapConstraint<? super K, ? super V> constraint;
      final Collection<Entry<K, V>> entries;

      ConstrainedEntries(Collection<Entry<K, V>> entries, MapConstraint<? super K, ? super V> constraint) {
         this.entries = entries;
         this.constraint = constraint;
      }

      protected Collection<Entry<K, V>> delegate() {
         return this.entries;
      }

      public Iterator<Entry<K, V>> iterator() {
         return new TransformedIterator<Entry<K, V>, Entry<K, V>>(this.entries.iterator()) {
            Entry<K, V> transform(Entry<K, V> from) {
               return MapConstraints.constrainedEntry(from, ConstrainedEntries.this.constraint);
            }
         };
      }

      public Object[] toArray() {
         return this.standardToArray();
      }

      public <T> T[] toArray(T[] array) {
         return this.standardToArray(array);
      }

      public boolean contains(Object o) {
         return Maps.containsEntryImpl(this.delegate(), o);
      }

      public boolean containsAll(Collection<?> c) {
         return this.standardContainsAll(c);
      }

      public boolean remove(Object o) {
         return Maps.removeEntryImpl(this.delegate(), o);
      }

      public boolean removeAll(Collection<?> c) {
         return this.standardRemoveAll(c);
      }

      public boolean retainAll(Collection<?> c) {
         return this.standardRetainAll(c);
      }
   }

   private static class ConstrainedAsMapValues<K, V> extends ForwardingCollection<Collection<V>> {
      final Collection<Collection<V>> delegate;
      final Set<Entry<K, Collection<V>>> entrySet;

      ConstrainedAsMapValues(Collection<Collection<V>> delegate, Set<Entry<K, Collection<V>>> entrySet) {
         this.delegate = delegate;
         this.entrySet = entrySet;
      }

      protected Collection<Collection<V>> delegate() {
         return this.delegate;
      }

      public Iterator<Collection<V>> iterator() {
         final Iterator<Entry<K, Collection<V>>> iterator = this.entrySet.iterator();
         return new Iterator<Collection<V>>() {
            public boolean hasNext() {
               return iterator.hasNext();
            }

            public Collection<V> next() {
               return (Collection)((Entry)iterator.next()).getValue();
            }

            public void remove() {
               iterator.remove();
            }
         };
      }

      public Object[] toArray() {
         return this.standardToArray();
      }

      public <T> T[] toArray(T[] array) {
         return this.standardToArray(array);
      }

      public boolean contains(Object o) {
         return this.standardContains(o);
      }

      public boolean containsAll(Collection<?> c) {
         return this.standardContainsAll(c);
      }

      public boolean remove(Object o) {
         return this.standardRemove(o);
      }

      public boolean removeAll(Collection<?> c) {
         return this.standardRemoveAll(c);
      }

      public boolean retainAll(Collection<?> c) {
         return this.standardRetainAll(c);
      }
   }

   private static class ConstrainedMultimap<K, V> extends ForwardingMultimap<K, V> implements Serializable {
      final MapConstraint<? super K, ? super V> constraint;
      final Multimap<K, V> delegate;
      transient Collection<Entry<K, V>> entries;
      transient Map<K, Collection<V>> asMap;

      public ConstrainedMultimap(Multimap<K, V> delegate, MapConstraint<? super K, ? super V> constraint) {
         this.delegate = (Multimap)Preconditions.checkNotNull(delegate);
         this.constraint = (MapConstraint)Preconditions.checkNotNull(constraint);
      }

      protected Multimap<K, V> delegate() {
         return this.delegate;
      }

      public Map<K, Collection<V>> asMap() {
         Map<K, Collection<V>> result = this.asMap;
         if (result == null) {
            final Map<K, Collection<V>> asMapDelegate = this.delegate.asMap();

            class AsMap extends ForwardingMap<K, Collection<V>> {
               Set<Entry<K, Collection<V>>> entrySet;
               Collection<Collection<V>> values;

               protected Map<K, Collection<V>> delegate() {
                  return asMapDelegate;
               }

               public Set<Entry<K, Collection<V>>> entrySet() {
                  Set<Entry<K, Collection<V>>> result = this.entrySet;
                  if (result == null) {
                     this.entrySet = result = MapConstraints.constrainedAsMapEntries(asMapDelegate.entrySet(), ConstrainedMultimap.this.constraint);
                  }

                  return result;
               }

               public Collection<V> get(Object key) {
                  try {
                     Collection<V> collection = ConstrainedMultimap.this.get(key);
                     return collection.isEmpty() ? null : collection;
                  } catch (ClassCastException var3) {
                     return null;
                  }
               }

               public Collection<Collection<V>> values() {
                  Collection<Collection<V>> result = this.values;
                  if (result == null) {
                     this.values = (Collection)(result = new MapConstraints.ConstrainedAsMapValues(this.delegate().values(), this.entrySet()));
                  }

                  return (Collection)result;
               }

               public boolean containsValue(Object o) {
                  return this.values().contains(o);
               }
            }

            this.asMap = (Map)(result = new AsMap());
         }

         return (Map)result;
      }

      public Collection<Entry<K, V>> entries() {
         Collection<Entry<K, V>> result = this.entries;
         if (result == null) {
            this.entries = result = MapConstraints.constrainedEntries(this.delegate.entries(), this.constraint);
         }

         return result;
      }

      public Collection<V> get(final K key) {
         return Constraints.constrainedTypePreservingCollection(this.delegate.get(key), new Constraint<V>() {
            public V checkElement(V value) {
               ConstrainedMultimap.this.constraint.checkKeyValue(key, value);
               return value;
            }
         });
      }

      public boolean put(K key, V value) {
         this.constraint.checkKeyValue(key, value);
         return this.delegate.put(key, value);
      }

      public boolean putAll(K key, Iterable<? extends V> values) {
         return this.delegate.putAll(key, MapConstraints.checkValues(key, values, this.constraint));
      }

      public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
         boolean changed = false;

         Entry entry;
         for(Iterator i$ = multimap.entries().iterator(); i$.hasNext(); changed |= this.put(entry.getKey(), entry.getValue())) {
            entry = (Entry)i$.next();
         }

         return changed;
      }

      public Collection<V> replaceValues(K key, Iterable<? extends V> values) {
         return this.delegate.replaceValues(key, MapConstraints.checkValues(key, values, this.constraint));
      }
   }

   private static class InverseConstraint<K, V> implements MapConstraint<K, V> {
      final MapConstraint<? super V, ? super K> constraint;

      public InverseConstraint(MapConstraint<? super V, ? super K> constraint) {
         this.constraint = (MapConstraint)Preconditions.checkNotNull(constraint);
      }

      public void checkKeyValue(K key, V value) {
         this.constraint.checkKeyValue(value, key);
      }
   }

   private static class ConstrainedBiMap<K, V> extends MapConstraints.ConstrainedMap<K, V> implements BiMap<K, V> {
      volatile BiMap<V, K> inverse;

      ConstrainedBiMap(BiMap<K, V> delegate, @Nullable BiMap<V, K> inverse, MapConstraint<? super K, ? super V> constraint) {
         super(delegate, constraint);
         this.inverse = inverse;
      }

      protected BiMap<K, V> delegate() {
         return (BiMap)super.delegate();
      }

      public V forcePut(K key, V value) {
         this.constraint.checkKeyValue(key, value);
         return this.delegate().forcePut(key, value);
      }

      public BiMap<V, K> inverse() {
         if (this.inverse == null) {
            this.inverse = new MapConstraints.ConstrainedBiMap(this.delegate().inverse(), this, new MapConstraints.InverseConstraint(this.constraint));
         }

         return this.inverse;
      }

      public Set<V> values() {
         return this.delegate().values();
      }
   }

   static class ConstrainedMap<K, V> extends ForwardingMap<K, V> {
      private final Map<K, V> delegate;
      final MapConstraint<? super K, ? super V> constraint;
      private transient Set<Entry<K, V>> entrySet;

      ConstrainedMap(Map<K, V> delegate, MapConstraint<? super K, ? super V> constraint) {
         this.delegate = (Map)Preconditions.checkNotNull(delegate);
         this.constraint = (MapConstraint)Preconditions.checkNotNull(constraint);
      }

      protected Map<K, V> delegate() {
         return this.delegate;
      }

      public Set<Entry<K, V>> entrySet() {
         Set<Entry<K, V>> result = this.entrySet;
         if (result == null) {
            this.entrySet = result = MapConstraints.constrainedEntrySet(this.delegate.entrySet(), this.constraint);
         }

         return result;
      }

      public V put(K key, V value) {
         this.constraint.checkKeyValue(key, value);
         return this.delegate.put(key, value);
      }

      public void putAll(Map<? extends K, ? extends V> map) {
         this.delegate.putAll(MapConstraints.checkMap(map, this.constraint));
      }
   }

   private static enum NotNullMapConstraint implements MapConstraint<Object, Object> {
      INSTANCE;

      public void checkKeyValue(Object key, Object value) {
         Preconditions.checkNotNull(key);
         Preconditions.checkNotNull(value);
      }

      public String toString() {
         return "Not null";
      }
   }
}
