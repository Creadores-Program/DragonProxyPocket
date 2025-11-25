package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible
public abstract class ForwardingMultimap<K, V> extends ForwardingObject implements Multimap<K, V> {
   protected ForwardingMultimap() {
   }

   protected abstract Multimap<K, V> delegate();

   public Map<K, Collection<V>> asMap() {
      return this.delegate().asMap();
   }

   public void clear() {
      this.delegate().clear();
   }

   public boolean containsEntry(@Nullable Object key, @Nullable Object value) {
      return this.delegate().containsEntry(key, value);
   }

   public boolean containsKey(@Nullable Object key) {
      return this.delegate().containsKey(key);
   }

   public boolean containsValue(@Nullable Object value) {
      return this.delegate().containsValue(value);
   }

   public Collection<Entry<K, V>> entries() {
      return this.delegate().entries();
   }

   public Collection<V> get(@Nullable K key) {
      return this.delegate().get(key);
   }

   public boolean isEmpty() {
      return this.delegate().isEmpty();
   }

   public Multiset<K> keys() {
      return this.delegate().keys();
   }

   public Set<K> keySet() {
      return this.delegate().keySet();
   }

   public boolean put(K key, V value) {
      return this.delegate().put(key, value);
   }

   public boolean putAll(K key, Iterable<? extends V> values) {
      return this.delegate().putAll(key, values);
   }

   public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
      return this.delegate().putAll(multimap);
   }

   public boolean remove(@Nullable Object key, @Nullable Object value) {
      return this.delegate().remove(key, value);
   }

   public Collection<V> removeAll(@Nullable Object key) {
      return this.delegate().removeAll(key);
   }

   public Collection<V> replaceValues(K key, Iterable<? extends V> values) {
      return this.delegate().replaceValues(key, values);
   }

   public int size() {
      return this.delegate().size();
   }

   public Collection<V> values() {
      return this.delegate().values();
   }

   public boolean equals(@Nullable Object object) {
      return object == this || this.delegate().equals(object);
   }

   public int hashCode() {
      return this.delegate().hashCode();
   }
}
