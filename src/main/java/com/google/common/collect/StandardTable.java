package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible
class StandardTable<R, C, V> extends AbstractTable<R, C, V> implements Serializable {
   @GwtTransient
   final Map<R, Map<C, V>> backingMap;
   @GwtTransient
   final Supplier<? extends Map<C, V>> factory;
   private transient Set<C> columnKeySet;
   private transient Map<R, Map<C, V>> rowMap;
   private transient StandardTable<R, C, V>.ColumnMap columnMap;
   private static final long serialVersionUID = 0L;

   StandardTable(Map<R, Map<C, V>> backingMap, Supplier<? extends Map<C, V>> factory) {
      this.backingMap = backingMap;
      this.factory = factory;
   }

   public boolean contains(@Nullable Object rowKey, @Nullable Object columnKey) {
      return rowKey != null && columnKey != null && super.contains(rowKey, columnKey);
   }

   public boolean containsColumn(@Nullable Object columnKey) {
      if (columnKey == null) {
         return false;
      } else {
         Iterator i$ = this.backingMap.values().iterator();

         Map map;
         do {
            if (!i$.hasNext()) {
               return false;
            }

            map = (Map)i$.next();
         } while(!Maps.safeContainsKey(map, columnKey));

         return true;
      }
   }

   public boolean containsRow(@Nullable Object rowKey) {
      return rowKey != null && Maps.safeContainsKey(this.backingMap, rowKey);
   }

   public boolean containsValue(@Nullable Object value) {
      return value != null && super.containsValue(value);
   }

   public V get(@Nullable Object rowKey, @Nullable Object columnKey) {
      return rowKey != null && columnKey != null ? super.get(rowKey, columnKey) : null;
   }

   public boolean isEmpty() {
      return this.backingMap.isEmpty();
   }

   public int size() {
      int size = 0;

      Map map;
      for(Iterator i$ = this.backingMap.values().iterator(); i$.hasNext(); size += map.size()) {
         map = (Map)i$.next();
      }

      return size;
   }

   public void clear() {
      this.backingMap.clear();
   }

   private Map<C, V> getOrCreate(R rowKey) {
      Map<C, V> map = (Map)this.backingMap.get(rowKey);
      if (map == null) {
         map = (Map)this.factory.get();
         this.backingMap.put(rowKey, map);
      }

      return map;
   }

   public V put(R rowKey, C columnKey, V value) {
      Preconditions.checkNotNull(rowKey);
      Preconditions.checkNotNull(columnKey);
      Preconditions.checkNotNull(value);
      return this.getOrCreate(rowKey).put(columnKey, value);
   }

   public V remove(@Nullable Object rowKey, @Nullable Object columnKey) {
      if (rowKey != null && columnKey != null) {
         Map<C, V> map = (Map)Maps.safeGet(this.backingMap, rowKey);
         if (map == null) {
            return null;
         } else {
            V value = map.remove(columnKey);
            if (map.isEmpty()) {
               this.backingMap.remove(rowKey);
            }

            return value;
         }
      } else {
         return null;
      }
   }

   private Map<R, V> removeColumn(Object column) {
      Map<R, V> output = new LinkedHashMap();
      Iterator iterator = this.backingMap.entrySet().iterator();

      while(iterator.hasNext()) {
         Entry<R, Map<C, V>> entry = (Entry)iterator.next();
         V value = ((Map)entry.getValue()).remove(column);
         if (value != null) {
            output.put(entry.getKey(), value);
            if (((Map)entry.getValue()).isEmpty()) {
               iterator.remove();
            }
         }
      }

      return output;
   }

   private boolean containsMapping(Object rowKey, Object columnKey, Object value) {
      return value != null && value.equals(this.get(rowKey, columnKey));
   }

   private boolean removeMapping(Object rowKey, Object columnKey, Object value) {
      if (this.containsMapping(rowKey, columnKey, value)) {
         this.remove(rowKey, columnKey);
         return true;
      } else {
         return false;
      }
   }

   public Set<Table.Cell<R, C, V>> cellSet() {
      return super.cellSet();
   }

   Iterator<Table.Cell<R, C, V>> cellIterator() {
      return new StandardTable.CellIterator();
   }

   public Map<C, V> row(R rowKey) {
      return new StandardTable.Row(rowKey);
   }

   public Map<R, V> column(C columnKey) {
      return new StandardTable.Column(columnKey);
   }

   public Set<R> rowKeySet() {
      return this.rowMap().keySet();
   }

   public Set<C> columnKeySet() {
      Set<C> result = this.columnKeySet;
      return result == null ? (this.columnKeySet = new StandardTable.ColumnKeySet()) : result;
   }

   Iterator<C> createColumnKeyIterator() {
      return new StandardTable.ColumnKeyIterator();
   }

   public Collection<V> values() {
      return super.values();
   }

   public Map<R, Map<C, V>> rowMap() {
      Map<R, Map<C, V>> result = this.rowMap;
      return result == null ? (this.rowMap = this.createRowMap()) : result;
   }

   Map<R, Map<C, V>> createRowMap() {
      return new StandardTable.RowMap();
   }

   public Map<C, Map<R, V>> columnMap() {
      StandardTable<R, C, V>.ColumnMap result = this.columnMap;
      return result == null ? (this.columnMap = new StandardTable.ColumnMap()) : result;
   }

   private class ColumnMap extends Maps.ViewCachingAbstractMap<C, Map<R, V>> {
      private ColumnMap() {
      }

      public Map<R, V> get(Object key) {
         return StandardTable.this.containsColumn(key) ? StandardTable.this.column(key) : null;
      }

      public boolean containsKey(Object key) {
         return StandardTable.this.containsColumn(key);
      }

      public Map<R, V> remove(Object key) {
         return StandardTable.this.containsColumn(key) ? StandardTable.this.removeColumn(key) : null;
      }

      public Set<Entry<C, Map<R, V>>> createEntrySet() {
         return new StandardTable.ColumnMap.ColumnMapEntrySet();
      }

      public Set<C> keySet() {
         return StandardTable.this.columnKeySet();
      }

      Collection<Map<R, V>> createValues() {
         return new StandardTable.ColumnMap.ColumnMapValues();
      }

      // $FF: synthetic method
      ColumnMap(Object x1) {
         this();
      }

      private class ColumnMapValues extends Maps.Values<C, Map<R, V>> {
         ColumnMapValues() {
            super(ColumnMap.this);
         }

         public boolean remove(Object obj) {
            Iterator i$ = ColumnMap.this.entrySet().iterator();

            Entry entry;
            do {
               if (!i$.hasNext()) {
                  return false;
               }

               entry = (Entry)i$.next();
            } while(!((Map)entry.getValue()).equals(obj));

            StandardTable.this.removeColumn(entry.getKey());
            return true;
         }

         public boolean removeAll(Collection<?> c) {
            Preconditions.checkNotNull(c);
            boolean changed = false;
            Iterator i$ = Lists.newArrayList(StandardTable.this.columnKeySet().iterator()).iterator();

            while(i$.hasNext()) {
               C columnKey = i$.next();
               if (c.contains(StandardTable.this.column(columnKey))) {
                  StandardTable.this.removeColumn(columnKey);
                  changed = true;
               }
            }

            return changed;
         }

         public boolean retainAll(Collection<?> c) {
            Preconditions.checkNotNull(c);
            boolean changed = false;
            Iterator i$ = Lists.newArrayList(StandardTable.this.columnKeySet().iterator()).iterator();

            while(i$.hasNext()) {
               C columnKey = i$.next();
               if (!c.contains(StandardTable.this.column(columnKey))) {
                  StandardTable.this.removeColumn(columnKey);
                  changed = true;
               }
            }

            return changed;
         }
      }

      class ColumnMapEntrySet extends StandardTable<R, C, V>.TableSet<Entry<C, Map<R, V>>> {
         ColumnMapEntrySet() {
            super(null);
         }

         public Iterator<Entry<C, Map<R, V>>> iterator() {
            return Maps.asMapEntryIterator(StandardTable.this.columnKeySet(), new Function<C, Map<R, V>>() {
               public Map<R, V> apply(C columnKey) {
                  return StandardTable.this.column(columnKey);
               }
            });
         }

         public int size() {
            return StandardTable.this.columnKeySet().size();
         }

         public boolean contains(Object obj) {
            if (obj instanceof Entry) {
               Entry<?, ?> entry = (Entry)obj;
               if (StandardTable.this.containsColumn(entry.getKey())) {
                  C columnKey = entry.getKey();
                  return ColumnMap.this.get(columnKey).equals(entry.getValue());
               }
            }

            return false;
         }

         public boolean remove(Object obj) {
            if (this.contains(obj)) {
               Entry<?, ?> entry = (Entry)obj;
               StandardTable.this.removeColumn(entry.getKey());
               return true;
            } else {
               return false;
            }
         }

         public boolean removeAll(Collection<?> c) {
            Preconditions.checkNotNull(c);
            return Sets.removeAllImpl(this, (Iterator)c.iterator());
         }

         public boolean retainAll(Collection<?> c) {
            Preconditions.checkNotNull(c);
            boolean changed = false;
            Iterator i$ = Lists.newArrayList(StandardTable.this.columnKeySet().iterator()).iterator();

            while(i$.hasNext()) {
               C columnKey = i$.next();
               if (!c.contains(Maps.immutableEntry(columnKey, StandardTable.this.column(columnKey)))) {
                  StandardTable.this.removeColumn(columnKey);
                  changed = true;
               }
            }

            return changed;
         }
      }
   }

   class RowMap extends Maps.ViewCachingAbstractMap<R, Map<C, V>> {
      public boolean containsKey(Object key) {
         return StandardTable.this.containsRow(key);
      }

      public Map<C, V> get(Object key) {
         return StandardTable.this.containsRow(key) ? StandardTable.this.row(key) : null;
      }

      public Map<C, V> remove(Object key) {
         return key == null ? null : (Map)StandardTable.this.backingMap.remove(key);
      }

      protected Set<Entry<R, Map<C, V>>> createEntrySet() {
         return new StandardTable.RowMap.EntrySet();
      }

      class EntrySet extends StandardTable<R, C, V>.TableSet<Entry<R, Map<C, V>>> {
         EntrySet() {
            super(null);
         }

         public Iterator<Entry<R, Map<C, V>>> iterator() {
            return Maps.asMapEntryIterator(StandardTable.this.backingMap.keySet(), new Function<R, Map<C, V>>() {
               public Map<C, V> apply(R rowKey) {
                  return StandardTable.this.row(rowKey);
               }
            });
         }

         public int size() {
            return StandardTable.this.backingMap.size();
         }

         public boolean contains(Object obj) {
            if (!(obj instanceof Entry)) {
               return false;
            } else {
               Entry<?, ?> entry = (Entry)obj;
               return entry.getKey() != null && entry.getValue() instanceof Map && Collections2.safeContains(StandardTable.this.backingMap.entrySet(), entry);
            }
         }

         public boolean remove(Object obj) {
            if (!(obj instanceof Entry)) {
               return false;
            } else {
               Entry<?, ?> entry = (Entry)obj;
               return entry.getKey() != null && entry.getValue() instanceof Map && StandardTable.this.backingMap.entrySet().remove(entry);
            }
         }
      }
   }

   private class ColumnKeyIterator extends AbstractIterator<C> {
      final Map<C, V> seen;
      final Iterator<Map<C, V>> mapIterator;
      Iterator<Entry<C, V>> entryIterator;

      private ColumnKeyIterator() {
         this.seen = (Map)StandardTable.this.factory.get();
         this.mapIterator = StandardTable.this.backingMap.values().iterator();
         this.entryIterator = Iterators.emptyIterator();
      }

      protected C computeNext() {
         while(true) {
            if (this.entryIterator.hasNext()) {
               Entry<C, V> entry = (Entry)this.entryIterator.next();
               if (!this.seen.containsKey(entry.getKey())) {
                  this.seen.put(entry.getKey(), entry.getValue());
                  return entry.getKey();
               }
            } else {
               if (!this.mapIterator.hasNext()) {
                  return this.endOfData();
               }

               this.entryIterator = ((Map)this.mapIterator.next()).entrySet().iterator();
            }
         }
      }

      // $FF: synthetic method
      ColumnKeyIterator(Object x1) {
         this();
      }
   }

   private class ColumnKeySet extends StandardTable<R, C, V>.TableSet<C> {
      private ColumnKeySet() {
         super(null);
      }

      public Iterator<C> iterator() {
         return StandardTable.this.createColumnKeyIterator();
      }

      public int size() {
         return Iterators.size(this.iterator());
      }

      public boolean remove(Object obj) {
         if (obj == null) {
            return false;
         } else {
            boolean changed = false;
            Iterator iterator = StandardTable.this.backingMap.values().iterator();

            while(iterator.hasNext()) {
               Map<C, V> map = (Map)iterator.next();
               if (map.keySet().remove(obj)) {
                  changed = true;
                  if (map.isEmpty()) {
                     iterator.remove();
                  }
               }
            }

            return changed;
         }
      }

      public boolean removeAll(Collection<?> c) {
         Preconditions.checkNotNull(c);
         boolean changed = false;
         Iterator iterator = StandardTable.this.backingMap.values().iterator();

         while(iterator.hasNext()) {
            Map<C, V> map = (Map)iterator.next();
            if (Iterators.removeAll(map.keySet().iterator(), c)) {
               changed = true;
               if (map.isEmpty()) {
                  iterator.remove();
               }
            }
         }

         return changed;
      }

      public boolean retainAll(Collection<?> c) {
         Preconditions.checkNotNull(c);
         boolean changed = false;
         Iterator iterator = StandardTable.this.backingMap.values().iterator();

         while(iterator.hasNext()) {
            Map<C, V> map = (Map)iterator.next();
            if (map.keySet().retainAll(c)) {
               changed = true;
               if (map.isEmpty()) {
                  iterator.remove();
               }
            }
         }

         return changed;
      }

      public boolean contains(Object obj) {
         return StandardTable.this.containsColumn(obj);
      }

      // $FF: synthetic method
      ColumnKeySet(Object x1) {
         this();
      }
   }

   private class Column extends Maps.ViewCachingAbstractMap<R, V> {
      final C columnKey;

      Column(C columnKey) {
         this.columnKey = Preconditions.checkNotNull(columnKey);
      }

      public V put(R key, V value) {
         return StandardTable.this.put(key, this.columnKey, value);
      }

      public V get(Object key) {
         return StandardTable.this.get(key, this.columnKey);
      }

      public boolean containsKey(Object key) {
         return StandardTable.this.contains(key, this.columnKey);
      }

      public V remove(Object key) {
         return StandardTable.this.remove(key, this.columnKey);
      }

      boolean removeFromColumnIf(Predicate<? super Entry<R, V>> predicate) {
         boolean changed = false;
         Iterator iterator = StandardTable.this.backingMap.entrySet().iterator();

         while(iterator.hasNext()) {
            Entry<R, Map<C, V>> entry = (Entry)iterator.next();
            Map<C, V> map = (Map)entry.getValue();
            V value = map.get(this.columnKey);
            if (value != null && predicate.apply(Maps.immutableEntry(entry.getKey(), value))) {
               map.remove(this.columnKey);
               changed = true;
               if (map.isEmpty()) {
                  iterator.remove();
               }
            }
         }

         return changed;
      }

      Set<Entry<R, V>> createEntrySet() {
         return new StandardTable.Column.EntrySet();
      }

      Set<R> createKeySet() {
         return new StandardTable.Column.KeySet();
      }

      Collection<V> createValues() {
         return new StandardTable.Column.Values();
      }

      private class Values extends Maps.Values<R, V> {
         Values() {
            super(Column.this);
         }

         public boolean remove(Object obj) {
            return obj != null && Column.this.removeFromColumnIf(Maps.valuePredicateOnEntries(Predicates.equalTo(obj)));
         }

         public boolean removeAll(Collection<?> c) {
            return Column.this.removeFromColumnIf(Maps.valuePredicateOnEntries(Predicates.in(c)));
         }

         public boolean retainAll(Collection<?> c) {
            return Column.this.removeFromColumnIf(Maps.valuePredicateOnEntries(Predicates.not(Predicates.in(c))));
         }
      }

      private class KeySet extends Maps.KeySet<R, V> {
         KeySet() {
            super(Column.this);
         }

         public boolean contains(Object obj) {
            return StandardTable.this.contains(obj, Column.this.columnKey);
         }

         public boolean remove(Object obj) {
            return StandardTable.this.remove(obj, Column.this.columnKey) != null;
         }

         public boolean retainAll(Collection<?> c) {
            return Column.this.removeFromColumnIf(Maps.keyPredicateOnEntries(Predicates.not(Predicates.in(c))));
         }
      }

      private class EntrySetIterator extends AbstractIterator<Entry<R, V>> {
         final Iterator<Entry<R, Map<C, V>>> iterator;

         private EntrySetIterator() {
            this.iterator = StandardTable.this.backingMap.entrySet().iterator();
         }

         protected Entry<R, V> computeNext() {
            while(true) {
               if (this.iterator.hasNext()) {
                  final Entry<R, Map<C, V>> entry = (Entry)this.iterator.next();
                  if (!((Map)entry.getValue()).containsKey(Column.this.columnKey)) {
                     continue;
                  }

                  class EntryImpl extends AbstractMapEntry<R, V> {
                     public R getKey() {
                        return entry.getKey();
                     }

                     public V getValue() {
                        return ((Map)entry.getValue()).get(Column.this.columnKey);
                     }

                     public V setValue(V value) {
                        return ((Map)entry.getValue()).put(Column.this.columnKey, Preconditions.checkNotNull(value));
                     }
                  }

                  return new EntryImpl();
               }

               return (Entry)this.endOfData();
            }
         }

         // $FF: synthetic method
         EntrySetIterator(Object x1) {
            this();
         }
      }

      private class EntrySet extends Sets.ImprovedAbstractSet<Entry<R, V>> {
         private EntrySet() {
         }

         public Iterator<Entry<R, V>> iterator() {
            return Column.this.new EntrySetIterator();
         }

         public int size() {
            int size = 0;
            Iterator i$ = StandardTable.this.backingMap.values().iterator();

            while(i$.hasNext()) {
               Map<C, V> map = (Map)i$.next();
               if (map.containsKey(Column.this.columnKey)) {
                  ++size;
               }
            }

            return size;
         }

         public boolean isEmpty() {
            return !StandardTable.this.containsColumn(Column.this.columnKey);
         }

         public void clear() {
            Column.this.removeFromColumnIf(Predicates.alwaysTrue());
         }

         public boolean contains(Object o) {
            if (o instanceof Entry) {
               Entry<?, ?> entry = (Entry)o;
               return StandardTable.this.containsMapping(entry.getKey(), Column.this.columnKey, entry.getValue());
            } else {
               return false;
            }
         }

         public boolean remove(Object obj) {
            if (obj instanceof Entry) {
               Entry<?, ?> entry = (Entry)obj;
               return StandardTable.this.removeMapping(entry.getKey(), Column.this.columnKey, entry.getValue());
            } else {
               return false;
            }
         }

         public boolean retainAll(Collection<?> c) {
            return Column.this.removeFromColumnIf(Predicates.not(Predicates.in(c)));
         }

         // $FF: synthetic method
         EntrySet(Object x1) {
            this();
         }
      }
   }

   class Row extends Maps.IteratorBasedAbstractMap<C, V> {
      final R rowKey;
      Map<C, V> backingRowMap;

      Row(R rowKey) {
         this.rowKey = Preconditions.checkNotNull(rowKey);
      }

      Map<C, V> backingRowMap() {
         return this.backingRowMap != null && (!this.backingRowMap.isEmpty() || !StandardTable.this.backingMap.containsKey(this.rowKey)) ? this.backingRowMap : (this.backingRowMap = this.computeBackingRowMap());
      }

      Map<C, V> computeBackingRowMap() {
         return (Map)StandardTable.this.backingMap.get(this.rowKey);
      }

      void maintainEmptyInvariant() {
         if (this.backingRowMap() != null && this.backingRowMap.isEmpty()) {
            StandardTable.this.backingMap.remove(this.rowKey);
            this.backingRowMap = null;
         }

      }

      public boolean containsKey(Object key) {
         Map<C, V> backingRowMap = this.backingRowMap();
         return key != null && backingRowMap != null && Maps.safeContainsKey(backingRowMap, key);
      }

      public V get(Object key) {
         Map<C, V> backingRowMap = this.backingRowMap();
         return key != null && backingRowMap != null ? Maps.safeGet(backingRowMap, key) : null;
      }

      public V put(C key, V value) {
         Preconditions.checkNotNull(key);
         Preconditions.checkNotNull(value);
         return this.backingRowMap != null && !this.backingRowMap.isEmpty() ? this.backingRowMap.put(key, value) : StandardTable.this.put(this.rowKey, key, value);
      }

      public V remove(Object key) {
         Map<C, V> backingRowMap = this.backingRowMap();
         if (backingRowMap == null) {
            return null;
         } else {
            V result = Maps.safeRemove(backingRowMap, key);
            this.maintainEmptyInvariant();
            return result;
         }
      }

      public void clear() {
         Map<C, V> backingRowMap = this.backingRowMap();
         if (backingRowMap != null) {
            backingRowMap.clear();
         }

         this.maintainEmptyInvariant();
      }

      public int size() {
         Map<C, V> map = this.backingRowMap();
         return map == null ? 0 : map.size();
      }

      Iterator<Entry<C, V>> entryIterator() {
         Map<C, V> map = this.backingRowMap();
         if (map == null) {
            return Iterators.emptyModifiableIterator();
         } else {
            final Iterator<Entry<C, V>> iterator = map.entrySet().iterator();
            return new Iterator<Entry<C, V>>() {
               public boolean hasNext() {
                  return iterator.hasNext();
               }

               public Entry<C, V> next() {
                  final Entry<C, V> entry = (Entry)iterator.next();
                  return new ForwardingMapEntry<C, V>() {
                     protected Entry<C, V> delegate() {
                        return entry;
                     }

                     public V setValue(V value) {
                        return super.setValue(Preconditions.checkNotNull(value));
                     }

                     public boolean equals(Object object) {
                        return this.standardEquals(object);
                     }
                  };
               }

               public void remove() {
                  iterator.remove();
                  Row.this.maintainEmptyInvariant();
               }
            };
         }
      }
   }

   private class CellIterator implements Iterator<Table.Cell<R, C, V>> {
      final Iterator<Entry<R, Map<C, V>>> rowIterator;
      Entry<R, Map<C, V>> rowEntry;
      Iterator<Entry<C, V>> columnIterator;

      private CellIterator() {
         this.rowIterator = StandardTable.this.backingMap.entrySet().iterator();
         this.columnIterator = Iterators.emptyModifiableIterator();
      }

      public boolean hasNext() {
         return this.rowIterator.hasNext() || this.columnIterator.hasNext();
      }

      public Table.Cell<R, C, V> next() {
         if (!this.columnIterator.hasNext()) {
            this.rowEntry = (Entry)this.rowIterator.next();
            this.columnIterator = ((Map)this.rowEntry.getValue()).entrySet().iterator();
         }

         Entry<C, V> columnEntry = (Entry)this.columnIterator.next();
         return Tables.immutableCell(this.rowEntry.getKey(), columnEntry.getKey(), columnEntry.getValue());
      }

      public void remove() {
         this.columnIterator.remove();
         if (((Map)this.rowEntry.getValue()).isEmpty()) {
            this.rowIterator.remove();
         }

      }

      // $FF: synthetic method
      CellIterator(Object x1) {
         this();
      }
   }

   private abstract class TableSet<T> extends Sets.ImprovedAbstractSet<T> {
      private TableSet() {
      }

      public boolean isEmpty() {
         return StandardTable.this.backingMap.isEmpty();
      }

      public void clear() {
         StandardTable.this.backingMap.clear();
      }

      // $FF: synthetic method
      TableSet(Object x1) {
         this();
      }
   }
}
