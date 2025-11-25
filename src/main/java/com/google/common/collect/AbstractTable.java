package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible
abstract class AbstractTable<R, C, V> implements Table<R, C, V> {
   private transient Set<Table.Cell<R, C, V>> cellSet;
   private transient Collection<V> values;

   public boolean containsRow(@Nullable Object rowKey) {
      return Maps.safeContainsKey(this.rowMap(), rowKey);
   }

   public boolean containsColumn(@Nullable Object columnKey) {
      return Maps.safeContainsKey(this.columnMap(), columnKey);
   }

   public Set<R> rowKeySet() {
      return this.rowMap().keySet();
   }

   public Set<C> columnKeySet() {
      return this.columnMap().keySet();
   }

   public boolean containsValue(@Nullable Object value) {
      Iterator i$ = this.rowMap().values().iterator();

      Map row;
      do {
         if (!i$.hasNext()) {
            return false;
         }

         row = (Map)i$.next();
      } while(!row.containsValue(value));

      return true;
   }

   public boolean contains(@Nullable Object rowKey, @Nullable Object columnKey) {
      Map<C, V> row = (Map)Maps.safeGet(this.rowMap(), rowKey);
      return row != null && Maps.safeContainsKey(row, columnKey);
   }

   public V get(@Nullable Object rowKey, @Nullable Object columnKey) {
      Map<C, V> row = (Map)Maps.safeGet(this.rowMap(), rowKey);
      return row == null ? null : Maps.safeGet(row, columnKey);
   }

   public boolean isEmpty() {
      return this.size() == 0;
   }

   public void clear() {
      Iterators.clear(this.cellSet().iterator());
   }

   public V remove(@Nullable Object rowKey, @Nullable Object columnKey) {
      Map<C, V> row = (Map)Maps.safeGet(this.rowMap(), rowKey);
      return row == null ? null : Maps.safeRemove(row, columnKey);
   }

   public V put(R rowKey, C columnKey, V value) {
      return this.row(rowKey).put(columnKey, value);
   }

   public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
      Iterator i$ = table.cellSet().iterator();

      while(i$.hasNext()) {
         Table.Cell<? extends R, ? extends C, ? extends V> cell = (Table.Cell)i$.next();
         this.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
      }

   }

   public Set<Table.Cell<R, C, V>> cellSet() {
      Set<Table.Cell<R, C, V>> result = this.cellSet;
      return result == null ? (this.cellSet = this.createCellSet()) : result;
   }

   Set<Table.Cell<R, C, V>> createCellSet() {
      return new AbstractTable.CellSet();
   }

   abstract Iterator<Table.Cell<R, C, V>> cellIterator();

   public Collection<V> values() {
      Collection<V> result = this.values;
      return result == null ? (this.values = this.createValues()) : result;
   }

   Collection<V> createValues() {
      return new AbstractTable.Values();
   }

   Iterator<V> valuesIterator() {
      return new TransformedIterator<Table.Cell<R, C, V>, V>(this.cellSet().iterator()) {
         V transform(Table.Cell<R, C, V> cell) {
            return cell.getValue();
         }
      };
   }

   public boolean equals(@Nullable Object obj) {
      return Tables.equalsImpl(this, obj);
   }

   public int hashCode() {
      return this.cellSet().hashCode();
   }

   public String toString() {
      return this.rowMap().toString();
   }

   class Values extends AbstractCollection<V> {
      public Iterator<V> iterator() {
         return AbstractTable.this.valuesIterator();
      }

      public boolean contains(Object o) {
         return AbstractTable.this.containsValue(o);
      }

      public void clear() {
         AbstractTable.this.clear();
      }

      public int size() {
         return AbstractTable.this.size();
      }
   }

   class CellSet extends AbstractSet<Table.Cell<R, C, V>> {
      public boolean contains(Object o) {
         if (!(o instanceof Table.Cell)) {
            return false;
         } else {
            Table.Cell<?, ?, ?> cell = (Table.Cell)o;
            Map<C, V> row = (Map)Maps.safeGet(AbstractTable.this.rowMap(), cell.getRowKey());
            return row != null && Collections2.safeContains(row.entrySet(), Maps.immutableEntry(cell.getColumnKey(), cell.getValue()));
         }
      }

      public boolean remove(@Nullable Object o) {
         if (!(o instanceof Table.Cell)) {
            return false;
         } else {
            Table.Cell<?, ?, ?> cell = (Table.Cell)o;
            Map<C, V> row = (Map)Maps.safeGet(AbstractTable.this.rowMap(), cell.getRowKey());
            return row != null && Collections2.safeRemove(row.entrySet(), Maps.immutableEntry(cell.getColumnKey(), cell.getValue()));
         }
      }

      public void clear() {
         AbstractTable.this.clear();
      }

      public Iterator<Table.Cell<R, C, V>> iterator() {
         return AbstractTable.this.cellIterator();
      }

      public int size() {
         return AbstractTable.this.size();
      }
   }
}
