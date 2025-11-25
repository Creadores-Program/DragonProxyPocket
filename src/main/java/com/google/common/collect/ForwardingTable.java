package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@GwtCompatible
public abstract class ForwardingTable<R, C, V> extends ForwardingObject implements Table<R, C, V> {
   protected ForwardingTable() {
   }

   protected abstract Table<R, C, V> delegate();

   public Set<Table.Cell<R, C, V>> cellSet() {
      return this.delegate().cellSet();
   }

   public void clear() {
      this.delegate().clear();
   }

   public Map<R, V> column(C columnKey) {
      return this.delegate().column(columnKey);
   }

   public Set<C> columnKeySet() {
      return this.delegate().columnKeySet();
   }

   public Map<C, Map<R, V>> columnMap() {
      return this.delegate().columnMap();
   }

   public boolean contains(Object rowKey, Object columnKey) {
      return this.delegate().contains(rowKey, columnKey);
   }

   public boolean containsColumn(Object columnKey) {
      return this.delegate().containsColumn(columnKey);
   }

   public boolean containsRow(Object rowKey) {
      return this.delegate().containsRow(rowKey);
   }

   public boolean containsValue(Object value) {
      return this.delegate().containsValue(value);
   }

   public V get(Object rowKey, Object columnKey) {
      return this.delegate().get(rowKey, columnKey);
   }

   public boolean isEmpty() {
      return this.delegate().isEmpty();
   }

   public V put(R rowKey, C columnKey, V value) {
      return this.delegate().put(rowKey, columnKey, value);
   }

   public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
      this.delegate().putAll(table);
   }

   public V remove(Object rowKey, Object columnKey) {
      return this.delegate().remove(rowKey, columnKey);
   }

   public Map<C, V> row(R rowKey) {
      return this.delegate().row(rowKey);
   }

   public Set<R> rowKeySet() {
      return this.delegate().rowKeySet();
   }

   public Map<R, Map<C, V>> rowMap() {
      return this.delegate().rowMap();
   }

   public int size() {
      return this.delegate().size();
   }

   public Collection<V> values() {
      return this.delegate().values();
   }

   public boolean equals(Object obj) {
      return obj == this || this.delegate().equals(obj);
   }

   public int hashCode() {
      return this.delegate().hashCode();
   }
}
