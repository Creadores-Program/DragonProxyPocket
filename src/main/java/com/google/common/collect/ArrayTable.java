package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@Beta
@GwtCompatible(
   emulated = true
)
public final class ArrayTable<R, C, V> extends AbstractTable<R, C, V> implements Serializable {
   private final ImmutableList<R> rowList;
   private final ImmutableList<C> columnList;
   private final ImmutableMap<R, Integer> rowKeyToIndex;
   private final ImmutableMap<C, Integer> columnKeyToIndex;
   private final V[][] array;
   private transient ArrayTable<R, C, V>.ColumnMap columnMap;
   private transient ArrayTable<R, C, V>.RowMap rowMap;
   private static final long serialVersionUID = 0L;

   public static <R, C, V> ArrayTable<R, C, V> create(Iterable<? extends R> rowKeys, Iterable<? extends C> columnKeys) {
      return new ArrayTable(rowKeys, columnKeys);
   }

   public static <R, C, V> ArrayTable<R, C, V> create(Table<R, C, V> table) {
      return table instanceof ArrayTable ? new ArrayTable((ArrayTable)table) : new ArrayTable(table);
   }

   private ArrayTable(Iterable<? extends R> rowKeys, Iterable<? extends C> columnKeys) {
      this.rowList = ImmutableList.copyOf(rowKeys);
      this.columnList = ImmutableList.copyOf(columnKeys);
      Preconditions.checkArgument(!this.rowList.isEmpty());
      Preconditions.checkArgument(!this.columnList.isEmpty());
      this.rowKeyToIndex = Maps.indexMap(this.rowList);
      this.columnKeyToIndex = Maps.indexMap(this.columnList);
      V[][] tmpArray = (Object[][])(new Object[this.rowList.size()][this.columnList.size()]);
      this.array = tmpArray;
      this.eraseAll();
   }

   private ArrayTable(Table<R, C, V> table) {
      this(table.rowKeySet(), table.columnKeySet());
      this.putAll(table);
   }

   private ArrayTable(ArrayTable<R, C, V> table) {
      this.rowList = table.rowList;
      this.columnList = table.columnList;
      this.rowKeyToIndex = table.rowKeyToIndex;
      this.columnKeyToIndex = table.columnKeyToIndex;
      V[][] copy = (Object[][])(new Object[this.rowList.size()][this.columnList.size()]);
      this.array = copy;
      this.eraseAll();

      for(int i = 0; i < this.rowList.size(); ++i) {
         System.arraycopy(table.array[i], 0, copy[i], 0, table.array[i].length);
      }

   }

   public ImmutableList<R> rowKeyList() {
      return this.rowList;
   }

   public ImmutableList<C> columnKeyList() {
      return this.columnList;
   }

   public V at(int rowIndex, int columnIndex) {
      Preconditions.checkElementIndex(rowIndex, this.rowList.size());
      Preconditions.checkElementIndex(columnIndex, this.columnList.size());
      return this.array[rowIndex][columnIndex];
   }

   public V set(int rowIndex, int columnIndex, @Nullable V value) {
      Preconditions.checkElementIndex(rowIndex, this.rowList.size());
      Preconditions.checkElementIndex(columnIndex, this.columnList.size());
      V oldValue = this.array[rowIndex][columnIndex];
      this.array[rowIndex][columnIndex] = value;
      return oldValue;
   }

   @GwtIncompatible("reflection")
   public V[][] toArray(Class<V> valueClass) {
      V[][] copy = (Object[][])((Object[][])Array.newInstance(valueClass, new int[]{this.rowList.size(), this.columnList.size()}));

      for(int i = 0; i < this.rowList.size(); ++i) {
         System.arraycopy(this.array[i], 0, copy[i], 0, this.array[i].length);
      }

      return copy;
   }

   /** @deprecated */
   @Deprecated
   public void clear() {
      throw new UnsupportedOperationException();
   }

   public void eraseAll() {
      Object[][] arr$ = this.array;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         V[] row = arr$[i$];
         Arrays.fill(row, (Object)null);
      }

   }

   public boolean contains(@Nullable Object rowKey, @Nullable Object columnKey) {
      return this.containsRow(rowKey) && this.containsColumn(columnKey);
   }

   public boolean containsColumn(@Nullable Object columnKey) {
      return this.columnKeyToIndex.containsKey(columnKey);
   }

   public boolean containsRow(@Nullable Object rowKey) {
      return this.rowKeyToIndex.containsKey(rowKey);
   }

   public boolean containsValue(@Nullable Object value) {
      Object[][] arr$ = this.array;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         V[] row = arr$[i$];
         Object[] arr$ = row;
         int len$ = row.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            V element = arr$[i$];
            if (Objects.equal(value, element)) {
               return true;
            }
         }
      }

      return false;
   }

   public V get(@Nullable Object rowKey, @Nullable Object columnKey) {
      Integer rowIndex = (Integer)this.rowKeyToIndex.get(rowKey);
      Integer columnIndex = (Integer)this.columnKeyToIndex.get(columnKey);
      return rowIndex != null && columnIndex != null ? this.at(rowIndex, columnIndex) : null;
   }

   public boolean isEmpty() {
      return false;
   }

   public V put(R rowKey, C columnKey, @Nullable V value) {
      Preconditions.checkNotNull(rowKey);
      Preconditions.checkNotNull(columnKey);
      Integer rowIndex = (Integer)this.rowKeyToIndex.get(rowKey);
      Preconditions.checkArgument(rowIndex != null, "Row %s not in %s", rowKey, this.rowList);
      Integer columnIndex = (Integer)this.columnKeyToIndex.get(columnKey);
      Preconditions.checkArgument(columnIndex != null, "Column %s not in %s", columnKey, this.columnList);
      return this.set(rowIndex, columnIndex, value);
   }

   public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
      super.putAll(table);
   }

   /** @deprecated */
   @Deprecated
   public V remove(Object rowKey, Object columnKey) {
      throw new UnsupportedOperationException();
   }

   public V erase(@Nullable Object rowKey, @Nullable Object columnKey) {
      Integer rowIndex = (Integer)this.rowKeyToIndex.get(rowKey);
      Integer columnIndex = (Integer)this.columnKeyToIndex.get(columnKey);
      return rowIndex != null && columnIndex != null ? this.set(rowIndex, columnIndex, (Object)null) : null;
   }

   public int size() {
      return this.rowList.size() * this.columnList.size();
   }

   public Set<Table.Cell<R, C, V>> cellSet() {
      return super.cellSet();
   }

   Iterator<Table.Cell<R, C, V>> cellIterator() {
      return new AbstractIndexedListIterator<Table.Cell<R, C, V>>(this.size()) {
         protected Table.Cell<R, C, V> get(final int index) {
            return new Tables.AbstractCell<R, C, V>() {
               final int rowIndex;
               final int columnIndex;

               {
                  this.rowIndex = index / ArrayTable.this.columnList.size();
                  this.columnIndex = index % ArrayTable.this.columnList.size();
               }

               public R getRowKey() {
                  return ArrayTable.this.rowList.get(this.rowIndex);
               }

               public C getColumnKey() {
                  return ArrayTable.this.columnList.get(this.columnIndex);
               }

               public V getValue() {
                  return ArrayTable.this.at(this.rowIndex, this.columnIndex);
               }
            };
         }
      };
   }

   public Map<R, V> column(C columnKey) {
      Preconditions.checkNotNull(columnKey);
      Integer columnIndex = (Integer)this.columnKeyToIndex.get(columnKey);
      return (Map)(columnIndex == null ? ImmutableMap.of() : new ArrayTable.Column(columnIndex));
   }

   public ImmutableSet<C> columnKeySet() {
      return this.columnKeyToIndex.keySet();
   }

   public Map<C, Map<R, V>> columnMap() {
      ArrayTable<R, C, V>.ColumnMap map = this.columnMap;
      return map == null ? (this.columnMap = new ArrayTable.ColumnMap()) : map;
   }

   public Map<C, V> row(R rowKey) {
      Preconditions.checkNotNull(rowKey);
      Integer rowIndex = (Integer)this.rowKeyToIndex.get(rowKey);
      return (Map)(rowIndex == null ? ImmutableMap.of() : new ArrayTable.Row(rowIndex));
   }

   public ImmutableSet<R> rowKeySet() {
      return this.rowKeyToIndex.keySet();
   }

   public Map<R, Map<C, V>> rowMap() {
      ArrayTable<R, C, V>.RowMap map = this.rowMap;
      return map == null ? (this.rowMap = new ArrayTable.RowMap()) : map;
   }

   public Collection<V> values() {
      return super.values();
   }

   private class RowMap extends ArrayTable.ArrayMap<R, Map<C, V>> {
      private RowMap() {
         super(ArrayTable.this.rowKeyToIndex, null);
      }

      String getKeyRole() {
         return "Row";
      }

      Map<C, V> getValue(int index) {
         return ArrayTable.this.new Row(index);
      }

      Map<C, V> setValue(int index, Map<C, V> newValue) {
         throw new UnsupportedOperationException();
      }

      public Map<C, V> put(R key, Map<C, V> value) {
         throw new UnsupportedOperationException();
      }

      // $FF: synthetic method
      RowMap(Object x1) {
         this();
      }
   }

   private class Row extends ArrayTable.ArrayMap<C, V> {
      final int rowIndex;

      Row(int rowIndex) {
         super(ArrayTable.this.columnKeyToIndex, null);
         this.rowIndex = rowIndex;
      }

      String getKeyRole() {
         return "Column";
      }

      V getValue(int index) {
         return ArrayTable.this.at(this.rowIndex, index);
      }

      V setValue(int index, V newValue) {
         return ArrayTable.this.set(this.rowIndex, index, newValue);
      }
   }

   private class ColumnMap extends ArrayTable.ArrayMap<C, Map<R, V>> {
      private ColumnMap() {
         super(ArrayTable.this.columnKeyToIndex, null);
      }

      String getKeyRole() {
         return "Column";
      }

      Map<R, V> getValue(int index) {
         return ArrayTable.this.new Column(index);
      }

      Map<R, V> setValue(int index, Map<R, V> newValue) {
         throw new UnsupportedOperationException();
      }

      public Map<R, V> put(C key, Map<R, V> value) {
         throw new UnsupportedOperationException();
      }

      // $FF: synthetic method
      ColumnMap(Object x1) {
         this();
      }
   }

   private class Column extends ArrayTable.ArrayMap<R, V> {
      final int columnIndex;

      Column(int columnIndex) {
         super(ArrayTable.this.rowKeyToIndex, null);
         this.columnIndex = columnIndex;
      }

      String getKeyRole() {
         return "Row";
      }

      V getValue(int index) {
         return ArrayTable.this.at(index, this.columnIndex);
      }

      V setValue(int index, V newValue) {
         return ArrayTable.this.set(index, this.columnIndex, newValue);
      }
   }

   private abstract static class ArrayMap<K, V> extends Maps.IteratorBasedAbstractMap<K, V> {
      private final ImmutableMap<K, Integer> keyIndex;

      private ArrayMap(ImmutableMap<K, Integer> keyIndex) {
         this.keyIndex = keyIndex;
      }

      public Set<K> keySet() {
         return this.keyIndex.keySet();
      }

      K getKey(int index) {
         return this.keyIndex.keySet().asList().get(index);
      }

      abstract String getKeyRole();

      @Nullable
      abstract V getValue(int var1);

      @Nullable
      abstract V setValue(int var1, V var2);

      public int size() {
         return this.keyIndex.size();
      }

      public boolean isEmpty() {
         return this.keyIndex.isEmpty();
      }

      Iterator<Entry<K, V>> entryIterator() {
         return new AbstractIndexedListIterator<Entry<K, V>>(this.size()) {
            protected Entry<K, V> get(final int index) {
               return new AbstractMapEntry<K, V>() {
                  public K getKey() {
                     return ArrayMap.this.getKey(index);
                  }

                  public V getValue() {
                     return ArrayMap.this.getValue(index);
                  }

                  public V setValue(V value) {
                     return ArrayMap.this.setValue(index, value);
                  }
               };
            }
         };
      }

      public boolean containsKey(@Nullable Object key) {
         return this.keyIndex.containsKey(key);
      }

      public V get(@Nullable Object key) {
         Integer index = (Integer)this.keyIndex.get(key);
         return index == null ? null : this.getValue(index);
      }

      public V put(K key, V value) {
         Integer index = (Integer)this.keyIndex.get(key);
         if (index == null) {
            throw new IllegalArgumentException(this.getKeyRole() + " " + key + " not in " + this.keyIndex.keySet());
         } else {
            return this.setValue(index, value);
         }
      }

      public V remove(Object key) {
         throw new UnsupportedOperationException();
      }

      public void clear() {
         throw new UnsupportedOperationException();
      }

      // $FF: synthetic method
      ArrayMap(ImmutableMap x0, Object x1) {
         this(x0);
      }
   }
}
