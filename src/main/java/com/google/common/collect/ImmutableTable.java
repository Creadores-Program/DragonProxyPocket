package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

@GwtCompatible
public abstract class ImmutableTable<R, C, V> extends AbstractTable<R, C, V> {
   private static final ImmutableTable<Object, Object, Object> EMPTY = new SparseImmutableTable(ImmutableList.of(), ImmutableSet.of(), ImmutableSet.of());

   public static <R, C, V> ImmutableTable<R, C, V> of() {
      return EMPTY;
   }

   public static <R, C, V> ImmutableTable<R, C, V> of(R rowKey, C columnKey, V value) {
      return new SingletonImmutableTable(rowKey, columnKey, value);
   }

   public static <R, C, V> ImmutableTable<R, C, V> copyOf(Table<? extends R, ? extends C, ? extends V> table) {
      if (table instanceof ImmutableTable) {
         ImmutableTable<R, C, V> parameterizedTable = (ImmutableTable)table;
         return parameterizedTable;
      } else {
         int size = table.size();
         switch(size) {
         case 0:
            return of();
         case 1:
            Table.Cell<? extends R, ? extends C, ? extends V> onlyCell = (Table.Cell)Iterables.getOnlyElement(table.cellSet());
            return of(onlyCell.getRowKey(), onlyCell.getColumnKey(), onlyCell.getValue());
         default:
            ImmutableSet.Builder<Table.Cell<R, C, V>> cellSetBuilder = new ImmutableSet.Builder(size);
            Iterator i$ = table.cellSet().iterator();

            while(i$.hasNext()) {
               Table.Cell<? extends R, ? extends C, ? extends V> cell = (Table.Cell)i$.next();
               cellSetBuilder.add((Object)cellOf(cell.getRowKey(), cell.getColumnKey(), cell.getValue()));
            }

            return RegularImmutableTable.forCells(cellSetBuilder.build());
         }
      }
   }

   public static <R, C, V> ImmutableTable.Builder<R, C, V> builder() {
      return new ImmutableTable.Builder();
   }

   static <R, C, V> Table.Cell<R, C, V> cellOf(R rowKey, C columnKey, V value) {
      return Tables.immutableCell(Preconditions.checkNotNull(rowKey), Preconditions.checkNotNull(columnKey), Preconditions.checkNotNull(value));
   }

   ImmutableTable() {
   }

   public ImmutableSet<Table.Cell<R, C, V>> cellSet() {
      return (ImmutableSet)super.cellSet();
   }

   abstract ImmutableSet<Table.Cell<R, C, V>> createCellSet();

   final UnmodifiableIterator<Table.Cell<R, C, V>> cellIterator() {
      throw new AssertionError("should never be called");
   }

   public ImmutableCollection<V> values() {
      return (ImmutableCollection)super.values();
   }

   abstract ImmutableCollection<V> createValues();

   final Iterator<V> valuesIterator() {
      throw new AssertionError("should never be called");
   }

   public ImmutableMap<R, V> column(C columnKey) {
      Preconditions.checkNotNull(columnKey);
      return (ImmutableMap)MoreObjects.firstNonNull((ImmutableMap)this.columnMap().get(columnKey), ImmutableMap.of());
   }

   public ImmutableSet<C> columnKeySet() {
      return this.columnMap().keySet();
   }

   public abstract ImmutableMap<C, Map<R, V>> columnMap();

   public ImmutableMap<C, V> row(R rowKey) {
      Preconditions.checkNotNull(rowKey);
      return (ImmutableMap)MoreObjects.firstNonNull((ImmutableMap)this.rowMap().get(rowKey), ImmutableMap.of());
   }

   public ImmutableSet<R> rowKeySet() {
      return this.rowMap().keySet();
   }

   public abstract ImmutableMap<R, Map<C, V>> rowMap();

   public boolean contains(@Nullable Object rowKey, @Nullable Object columnKey) {
      return this.get(rowKey, columnKey) != null;
   }

   public boolean containsValue(@Nullable Object value) {
      return this.values().contains(value);
   }

   /** @deprecated */
   @Deprecated
   public final void clear() {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   public final V put(R rowKey, C columnKey, V value) {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   public final void putAll(Table<? extends R, ? extends C, ? extends V> table) {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   public final V remove(Object rowKey, Object columnKey) {
      throw new UnsupportedOperationException();
   }

   public static final class Builder<R, C, V> {
      private final List<Table.Cell<R, C, V>> cells = Lists.newArrayList();
      private Comparator<? super R> rowComparator;
      private Comparator<? super C> columnComparator;

      public ImmutableTable.Builder<R, C, V> orderRowsBy(Comparator<? super R> rowComparator) {
         this.rowComparator = (Comparator)Preconditions.checkNotNull(rowComparator);
         return this;
      }

      public ImmutableTable.Builder<R, C, V> orderColumnsBy(Comparator<? super C> columnComparator) {
         this.columnComparator = (Comparator)Preconditions.checkNotNull(columnComparator);
         return this;
      }

      public ImmutableTable.Builder<R, C, V> put(R rowKey, C columnKey, V value) {
         this.cells.add(ImmutableTable.cellOf(rowKey, columnKey, value));
         return this;
      }

      public ImmutableTable.Builder<R, C, V> put(Table.Cell<? extends R, ? extends C, ? extends V> cell) {
         if (cell instanceof Tables.ImmutableCell) {
            Preconditions.checkNotNull(cell.getRowKey());
            Preconditions.checkNotNull(cell.getColumnKey());
            Preconditions.checkNotNull(cell.getValue());
            this.cells.add(cell);
         } else {
            this.put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
         }

         return this;
      }

      public ImmutableTable.Builder<R, C, V> putAll(Table<? extends R, ? extends C, ? extends V> table) {
         Iterator i$ = table.cellSet().iterator();

         while(i$.hasNext()) {
            Table.Cell<? extends R, ? extends C, ? extends V> cell = (Table.Cell)i$.next();
            this.put(cell);
         }

         return this;
      }

      public ImmutableTable<R, C, V> build() {
         int size = this.cells.size();
         switch(size) {
         case 0:
            return ImmutableTable.of();
         case 1:
            return new SingletonImmutableTable((Table.Cell)Iterables.getOnlyElement(this.cells));
         default:
            return RegularImmutableTable.forCells(this.cells, this.rowComparator, this.columnComparator);
         }
      }
   }
}
