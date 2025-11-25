package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@GwtCompatible
@Immutable
final class DenseImmutableTable<R, C, V> extends RegularImmutableTable<R, C, V> {
   private final ImmutableMap<R, Integer> rowKeyToIndex;
   private final ImmutableMap<C, Integer> columnKeyToIndex;
   private final ImmutableMap<R, Map<C, V>> rowMap;
   private final ImmutableMap<C, Map<R, V>> columnMap;
   private final int[] rowCounts;
   private final int[] columnCounts;
   private final V[][] values;
   private final int[] iterationOrderRow;
   private final int[] iterationOrderColumn;

   DenseImmutableTable(ImmutableList<Table.Cell<R, C, V>> cellList, ImmutableSet<R> rowSpace, ImmutableSet<C> columnSpace) {
      V[][] array = (Object[][])(new Object[rowSpace.size()][columnSpace.size()]);
      this.values = array;
      this.rowKeyToIndex = Maps.indexMap(rowSpace);
      this.columnKeyToIndex = Maps.indexMap(columnSpace);
      this.rowCounts = new int[this.rowKeyToIndex.size()];
      this.columnCounts = new int[this.columnKeyToIndex.size()];
      int[] iterationOrderRow = new int[cellList.size()];
      int[] iterationOrderColumn = new int[cellList.size()];

      for(int i = 0; i < cellList.size(); ++i) {
         Table.Cell<R, C, V> cell = (Table.Cell)cellList.get(i);
         R rowKey = cell.getRowKey();
         C columnKey = cell.getColumnKey();
         int rowIndex = (Integer)this.rowKeyToIndex.get(rowKey);
         int columnIndex = (Integer)this.columnKeyToIndex.get(columnKey);
         V existingValue = this.values[rowIndex][columnIndex];
         Preconditions.checkArgument(existingValue == null, "duplicate key: (%s, %s)", rowKey, columnKey);
         this.values[rowIndex][columnIndex] = cell.getValue();
         int var10002 = this.rowCounts[rowIndex]++;
         var10002 = this.columnCounts[columnIndex]++;
         iterationOrderRow[i] = rowIndex;
         iterationOrderColumn[i] = columnIndex;
      }

      this.iterationOrderRow = iterationOrderRow;
      this.iterationOrderColumn = iterationOrderColumn;
      this.rowMap = new DenseImmutableTable.RowMap();
      this.columnMap = new DenseImmutableTable.ColumnMap();
   }

   public ImmutableMap<C, Map<R, V>> columnMap() {
      return this.columnMap;
   }

   public ImmutableMap<R, Map<C, V>> rowMap() {
      return this.rowMap;
   }

   public V get(@Nullable Object rowKey, @Nullable Object columnKey) {
      Integer rowIndex = (Integer)this.rowKeyToIndex.get(rowKey);
      Integer columnIndex = (Integer)this.columnKeyToIndex.get(columnKey);
      return rowIndex != null && columnIndex != null ? this.values[rowIndex][columnIndex] : null;
   }

   public int size() {
      return this.iterationOrderRow.length;
   }

   Table.Cell<R, C, V> getCell(int index) {
      int rowIndex = this.iterationOrderRow[index];
      int columnIndex = this.iterationOrderColumn[index];
      R rowKey = this.rowKeySet().asList().get(rowIndex);
      C columnKey = this.columnKeySet().asList().get(columnIndex);
      V value = this.values[rowIndex][columnIndex];
      return cellOf(rowKey, columnKey, value);
   }

   V getValue(int index) {
      return this.values[this.iterationOrderRow[index]][this.iterationOrderColumn[index]];
   }

   private final class ColumnMap extends DenseImmutableTable.ImmutableArrayMap<C, Map<R, V>> {
      private ColumnMap() {
         super(DenseImmutableTable.this.columnCounts.length);
      }

      ImmutableMap<C, Integer> keyToIndex() {
         return DenseImmutableTable.this.columnKeyToIndex;
      }

      Map<R, V> getValue(int keyIndex) {
         return DenseImmutableTable.this.new Column(keyIndex);
      }

      boolean isPartialView() {
         return false;
      }

      // $FF: synthetic method
      ColumnMap(Object x1) {
         this();
      }
   }

   private final class RowMap extends DenseImmutableTable.ImmutableArrayMap<R, Map<C, V>> {
      private RowMap() {
         super(DenseImmutableTable.this.rowCounts.length);
      }

      ImmutableMap<R, Integer> keyToIndex() {
         return DenseImmutableTable.this.rowKeyToIndex;
      }

      Map<C, V> getValue(int keyIndex) {
         return DenseImmutableTable.this.new Row(keyIndex);
      }

      boolean isPartialView() {
         return false;
      }

      // $FF: synthetic method
      RowMap(Object x1) {
         this();
      }
   }

   private final class Column extends DenseImmutableTable.ImmutableArrayMap<R, V> {
      private final int columnIndex;

      Column(int columnIndex) {
         super(DenseImmutableTable.this.columnCounts[columnIndex]);
         this.columnIndex = columnIndex;
      }

      ImmutableMap<R, Integer> keyToIndex() {
         return DenseImmutableTable.this.rowKeyToIndex;
      }

      V getValue(int keyIndex) {
         return DenseImmutableTable.this.values[keyIndex][this.columnIndex];
      }

      boolean isPartialView() {
         return true;
      }
   }

   private final class Row extends DenseImmutableTable.ImmutableArrayMap<C, V> {
      private final int rowIndex;

      Row(int rowIndex) {
         super(DenseImmutableTable.this.rowCounts[rowIndex]);
         this.rowIndex = rowIndex;
      }

      ImmutableMap<C, Integer> keyToIndex() {
         return DenseImmutableTable.this.columnKeyToIndex;
      }

      V getValue(int keyIndex) {
         return DenseImmutableTable.this.values[this.rowIndex][keyIndex];
      }

      boolean isPartialView() {
         return true;
      }
   }

   private abstract static class ImmutableArrayMap<K, V> extends ImmutableMap.IteratorBasedImmutableMap<K, V> {
      private final int size;

      ImmutableArrayMap(int size) {
         this.size = size;
      }

      abstract ImmutableMap<K, Integer> keyToIndex();

      private boolean isFull() {
         return this.size == this.keyToIndex().size();
      }

      K getKey(int index) {
         return this.keyToIndex().keySet().asList().get(index);
      }

      @Nullable
      abstract V getValue(int var1);

      ImmutableSet<K> createKeySet() {
         return this.isFull() ? this.keyToIndex().keySet() : super.createKeySet();
      }

      public int size() {
         return this.size;
      }

      public V get(@Nullable Object key) {
         Integer keyIndex = (Integer)this.keyToIndex().get(key);
         return keyIndex == null ? null : this.getValue(keyIndex);
      }

      UnmodifiableIterator<Entry<K, V>> entryIterator() {
         return new AbstractIterator<Entry<K, V>>() {
            private int index = -1;
            private final int maxIndex = ImmutableArrayMap.this.keyToIndex().size();

            protected Entry<K, V> computeNext() {
               ++this.index;

               while(this.index < this.maxIndex) {
                  V value = ImmutableArrayMap.this.getValue(this.index);
                  if (value != null) {
                     return Maps.immutableEntry(ImmutableArrayMap.this.getKey(this.index), value);
                  }

                  ++this.index;
               }

               return (Entry)this.endOfData();
            }
         };
      }
   }
}
