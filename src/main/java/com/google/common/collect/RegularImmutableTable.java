package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible
abstract class RegularImmutableTable<R, C, V> extends ImmutableTable<R, C, V> {
   abstract Table.Cell<R, C, V> getCell(int var1);

   final ImmutableSet<Table.Cell<R, C, V>> createCellSet() {
      return (ImmutableSet)(this.isEmpty() ? ImmutableSet.of() : new RegularImmutableTable.CellSet());
   }

   abstract V getValue(int var1);

   final ImmutableCollection<V> createValues() {
      return (ImmutableCollection)(this.isEmpty() ? ImmutableList.of() : new RegularImmutableTable.Values());
   }

   static <R, C, V> RegularImmutableTable<R, C, V> forCells(List<Table.Cell<R, C, V>> cells, @Nullable final Comparator<? super R> rowComparator, @Nullable final Comparator<? super C> columnComparator) {
      Preconditions.checkNotNull(cells);
      if (rowComparator != null || columnComparator != null) {
         Comparator<Table.Cell<R, C, V>> comparator = new Comparator<Table.Cell<R, C, V>>() {
            public int compare(Table.Cell<R, C, V> cell1, Table.Cell<R, C, V> cell2) {
               int rowCompare = rowComparator == null ? 0 : rowComparator.compare(cell1.getRowKey(), cell2.getRowKey());
               if (rowCompare != 0) {
                  return rowCompare;
               } else {
                  return columnComparator == null ? 0 : columnComparator.compare(cell1.getColumnKey(), cell2.getColumnKey());
               }
            }
         };
         Collections.sort(cells, comparator);
      }

      return forCellsInternal(cells, rowComparator, columnComparator);
   }

   static <R, C, V> RegularImmutableTable<R, C, V> forCells(Iterable<Table.Cell<R, C, V>> cells) {
      return forCellsInternal(cells, (Comparator)null, (Comparator)null);
   }

   private static final <R, C, V> RegularImmutableTable<R, C, V> forCellsInternal(Iterable<Table.Cell<R, C, V>> cells, @Nullable Comparator<? super R> rowComparator, @Nullable Comparator<? super C> columnComparator) {
      Set<R> rowSpaceBuilder = new LinkedHashSet();
      Set<C> columnSpaceBuilder = new LinkedHashSet();
      ImmutableList<Table.Cell<R, C, V>> cellList = ImmutableList.copyOf(cells);
      Iterator i$ = cells.iterator();

      while(i$.hasNext()) {
         Table.Cell<R, C, V> cell = (Table.Cell)i$.next();
         rowSpaceBuilder.add(cell.getRowKey());
         columnSpaceBuilder.add(cell.getColumnKey());
      }

      ImmutableSet<R> rowSpace = rowComparator == null ? ImmutableSet.copyOf((Collection)rowSpaceBuilder) : ImmutableSet.copyOf((Collection)Ordering.from(rowComparator).immutableSortedCopy(rowSpaceBuilder));
      ImmutableSet<C> columnSpace = columnComparator == null ? ImmutableSet.copyOf((Collection)columnSpaceBuilder) : ImmutableSet.copyOf((Collection)Ordering.from(columnComparator).immutableSortedCopy(columnSpaceBuilder));
      return (RegularImmutableTable)((long)cellList.size() > (long)rowSpace.size() * (long)columnSpace.size() / 2L ? new DenseImmutableTable(cellList, rowSpace, columnSpace) : new SparseImmutableTable(cellList, rowSpace, columnSpace));
   }

   private final class Values extends ImmutableList<V> {
      private Values() {
      }

      public int size() {
         return RegularImmutableTable.this.size();
      }

      public V get(int index) {
         return RegularImmutableTable.this.getValue(index);
      }

      boolean isPartialView() {
         return true;
      }

      // $FF: synthetic method
      Values(Object x1) {
         this();
      }
   }

   private final class CellSet extends ImmutableSet.Indexed<Table.Cell<R, C, V>> {
      private CellSet() {
      }

      public int size() {
         return RegularImmutableTable.this.size();
      }

      Table.Cell<R, C, V> get(int index) {
         return RegularImmutableTable.this.getCell(index);
      }

      public boolean contains(@Nullable Object object) {
         if (!(object instanceof Table.Cell)) {
            return false;
         } else {
            Table.Cell<?, ?, ?> cell = (Table.Cell)object;
            Object value = RegularImmutableTable.this.get(cell.getRowKey(), cell.getColumnKey());
            return value != null && value.equals(cell.getValue());
         }
      }

      boolean isPartialView() {
         return false;
      }

      // $FF: synthetic method
      CellSet(Object x1) {
         this();
      }
   }
}
