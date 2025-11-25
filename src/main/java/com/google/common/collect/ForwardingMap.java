package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible
public abstract class ForwardingMap<K, V> extends ForwardingObject implements Map<K, V> {
   protected ForwardingMap() {
   }

   protected abstract Map<K, V> delegate();

   public int size() {
      return this.delegate().size();
   }

   public boolean isEmpty() {
      return this.delegate().isEmpty();
   }

   public V remove(Object object) {
      return this.delegate().remove(object);
   }

   public void clear() {
      this.delegate().clear();
   }

   public boolean containsKey(@Nullable Object key) {
      return this.delegate().containsKey(key);
   }

   public boolean containsValue(@Nullable Object value) {
      return this.delegate().containsValue(value);
   }

   public V get(@Nullable Object key) {
      return this.delegate().get(key);
   }

   public V put(K key, V value) {
      return this.delegate().put(key, value);
   }

   public void putAll(Map<? extends K, ? extends V> map) {
      this.delegate().putAll(map);
   }

   public Set<K> keySet() {
      return this.delegate().keySet();
   }

   public Collection<V> values() {
      return this.delegate().values();
   }

   public Set<Entry<K, V>> entrySet() {
      return this.delegate().entrySet();
   }

   public boolean equals(@Nullable Object object) {
      return object == this || this.delegate().equals(object);
   }

   public int hashCode() {
      return this.delegate().hashCode();
   }

   protected void standardPutAll(Map<? extends K, ? extends V> map) {
      Maps.putAllImpl(this, map);
   }

   @Beta
   protected V standardRemove(@Nullable Object key) {
      Iterator entryIterator = this.entrySet().iterator();

      Entry entry;
      do {
         if (!entryIterator.hasNext()) {
            return null;
         }

         entry = (Entry)entryIterator.next();
      } while(!Objects.equal(entry.getKey(), key));

      V value = entry.getValue();
      entryIterator.remove();
      return value;
   }

   protected void standardClear() {
      Iterators.clear(this.entrySet().iterator());
   }

   @Beta
   protected boolean standardContainsKey(@Nullable Object key) {
      return Maps.containsKeyImpl(this, key);
   }

   protected boolean standardContainsValue(@Nullable Object value) {
      return Maps.containsValueImpl(this, value);
   }

   protected boolean standardIsEmpty() {
      return !this.entrySet().iterator().hasNext();
   }

   protected boolean standardEquals(@Nullable Object object) {
      return Maps.equalsImpl(this, object);
   }

   protected int standardHashCode() {
      return Sets.hashCodeImpl(this.entrySet());
   }

   protected String standardToString() {
      return Maps.toStringImpl(this);
   }

   @Beta
   protected abstract class StandardEntrySet extends Maps.EntrySet<K, V> {
      public StandardEntrySet() {
      }

      Map<K, V> map() {
         return ForwardingMap.this;
      }
   }

   @Beta
   protected class StandardValues extends Maps.Values<K, V> {
      public StandardValues() {
         super(ForwardingMap.this);
      }
   }

   @Beta
   protected class StandardKeySet extends Maps.KeySet<K, V> {
      public StandardKeySet() {
         super(ForwardingMap.this);
      }
   }
}
