package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible(
   emulated = true
)
abstract class AbstractBiMap<K, V> extends ForwardingMap<K, V> implements BiMap<K, V>, Serializable {
   private transient Map<K, V> delegate;
   transient AbstractBiMap<V, K> inverse;
   private transient Set<K> keySet;
   private transient Set<V> valueSet;
   private transient Set<Entry<K, V>> entrySet;
   @GwtIncompatible("Not needed in emulated source.")
   private static final long serialVersionUID = 0L;

   AbstractBiMap(Map<K, V> forward, Map<V, K> backward) {
      this.setDelegates(forward, backward);
   }

   private AbstractBiMap(Map<K, V> backward, AbstractBiMap<V, K> forward) {
      this.delegate = backward;
      this.inverse = forward;
   }

   protected Map<K, V> delegate() {
      return this.delegate;
   }

   K checkKey(@Nullable K key) {
      return key;
   }

   V checkValue(@Nullable V value) {
      return value;
   }

   void setDelegates(Map<K, V> forward, Map<V, K> backward) {
      Preconditions.checkState(this.delegate == null);
      Preconditions.checkState(this.inverse == null);
      Preconditions.checkArgument(forward.isEmpty());
      Preconditions.checkArgument(backward.isEmpty());
      Preconditions.checkArgument(forward != backward);
      this.delegate = forward;
      this.inverse = new AbstractBiMap.Inverse(backward, this);
   }

   void setInverse(AbstractBiMap<V, K> inverse) {
      this.inverse = inverse;
   }

   public boolean containsValue(@Nullable Object value) {
      return this.inverse.containsKey(value);
   }

   public V put(@Nullable K key, @Nullable V value) {
      return this.putInBothMaps(key, value, false);
   }

   public V forcePut(@Nullable K key, @Nullable V value) {
      return this.putInBothMaps(key, value, true);
   }

   private V putInBothMaps(@Nullable K key, @Nullable V value, boolean force) {
      this.checkKey(key);
      this.checkValue(value);
      boolean containedKey = this.containsKey(key);
      if (containedKey && Objects.equal(value, this.get(key))) {
         return value;
      } else {
         if (force) {
            this.inverse().remove(value);
         } else {
            Preconditions.checkArgument(!this.containsValue(value), "value already present: %s", value);
         }

         V oldValue = this.delegate.put(key, value);
         this.updateInverseMap(key, containedKey, oldValue, value);
         return oldValue;
      }
   }

   private void updateInverseMap(K key, boolean containedKey, V oldValue, V newValue) {
      if (containedKey) {
         this.removeFromInverseMap(oldValue);
      }

      this.inverse.delegate.put(newValue, key);
   }

   public V remove(@Nullable Object key) {
      return this.containsKey(key) ? this.removeFromBothMaps(key) : null;
   }

   private V removeFromBothMaps(Object key) {
      V oldValue = this.delegate.remove(key);
      this.removeFromInverseMap(oldValue);
      return oldValue;
   }

   private void removeFromInverseMap(V oldValue) {
      this.inverse.delegate.remove(oldValue);
   }

   public void putAll(Map<? extends K, ? extends V> map) {
      Iterator i$ = map.entrySet().iterator();

      while(i$.hasNext()) {
         Entry<? extends K, ? extends V> entry = (Entry)i$.next();
         this.put(entry.getKey(), entry.getValue());
      }

   }

   public void clear() {
      this.delegate.clear();
      this.inverse.delegate.clear();
   }

   public BiMap<V, K> inverse() {
      return this.inverse;
   }

   public Set<K> keySet() {
      Set<K> result = this.keySet;
      return result == null ? (this.keySet = new AbstractBiMap.KeySet()) : result;
   }

   public Set<V> values() {
      Set<V> result = this.valueSet;
      return result == null ? (this.valueSet = new AbstractBiMap.ValueSet()) : result;
   }

   public Set<Entry<K, V>> entrySet() {
      Set<Entry<K, V>> result = this.entrySet;
      return result == null ? (this.entrySet = new AbstractBiMap.EntrySet()) : result;
   }

   // $FF: synthetic method
   AbstractBiMap(Map x0, AbstractBiMap x1, Object x2) {
      this(x0, x1);
   }

   private static class Inverse<K, V> extends AbstractBiMap<K, V> {
      @GwtIncompatible("Not needed in emulated source.")
      private static final long serialVersionUID = 0L;

      private Inverse(Map<K, V> backward, AbstractBiMap<V, K> forward) {
         super(backward, forward, null);
      }

      K checkKey(K key) {
         return this.inverse.checkValue(key);
      }

      V checkValue(V value) {
         return this.inverse.checkKey(value);
      }

      @GwtIncompatible("java.io.ObjectOuputStream")
      private void writeObject(ObjectOutputStream stream) throws IOException {
         stream.defaultWriteObject();
         stream.writeObject(this.inverse());
      }

      @GwtIncompatible("java.io.ObjectInputStream")
      private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
         stream.defaultReadObject();
         this.setInverse((AbstractBiMap)stream.readObject());
      }

      @GwtIncompatible("Not needed in the emulated source.")
      Object readResolve() {
         return this.inverse().inverse();
      }

      // $FF: synthetic method
      Inverse(Map x0, AbstractBiMap x1, Object x2) {
         this(x0, x1);
      }
   }

   private class EntrySet extends ForwardingSet<Entry<K, V>> {
      final Set<Entry<K, V>> esDelegate;

      private EntrySet() {
         this.esDelegate = AbstractBiMap.this.delegate.entrySet();
      }

      protected Set<Entry<K, V>> delegate() {
         return this.esDelegate;
      }

      public void clear() {
         AbstractBiMap.this.clear();
      }

      public boolean remove(Object object) {
         if (!this.esDelegate.contains(object)) {
            return false;
         } else {
            Entry<?, ?> entry = (Entry)object;
            AbstractBiMap.this.inverse.delegate.remove(entry.getValue());
            this.esDelegate.remove(entry);
            return true;
         }
      }

      public Iterator<Entry<K, V>> iterator() {
         final Iterator<Entry<K, V>> iterator = this.esDelegate.iterator();
         return new Iterator<Entry<K, V>>() {
            Entry<K, V> entry;

            public boolean hasNext() {
               return iterator.hasNext();
            }

            public Entry<K, V> next() {
               this.entry = (Entry)iterator.next();
               final Entry<K, V> finalEntry = this.entry;
               return new ForwardingMapEntry<K, V>() {
                  protected Entry<K, V> delegate() {
                     return finalEntry;
                  }

                  public V setValue(V value) {
                     Preconditions.checkState(EntrySet.this.contains(this), "entry no longer in map");
                     if (Objects.equal(value, this.getValue())) {
                        return value;
                     } else {
                        Preconditions.checkArgument(!AbstractBiMap.this.containsValue(value), "value already present: %s", value);
                        V oldValue = finalEntry.setValue(value);
                        Preconditions.checkState(Objects.equal(value, AbstractBiMap.this.get(this.getKey())), "entry no longer in map");
                        AbstractBiMap.this.updateInverseMap(this.getKey(), true, oldValue, value);
                        return oldValue;
                     }
                  }
               };
            }

            public void remove() {
               CollectPreconditions.checkRemove(this.entry != null);
               V value = this.entry.getValue();
               iterator.remove();
               AbstractBiMap.this.removeFromInverseMap(value);
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

      public boolean removeAll(Collection<?> c) {
         return this.standardRemoveAll(c);
      }

      public boolean retainAll(Collection<?> c) {
         return this.standardRetainAll(c);
      }

      // $FF: synthetic method
      EntrySet(Object x1) {
         this();
      }
   }

   private class ValueSet extends ForwardingSet<V> {
      final Set<V> valuesDelegate;

      private ValueSet() {
         this.valuesDelegate = AbstractBiMap.this.inverse.keySet();
      }

      protected Set<V> delegate() {
         return this.valuesDelegate;
      }

      public Iterator<V> iterator() {
         return Maps.valueIterator(AbstractBiMap.this.entrySet().iterator());
      }

      public Object[] toArray() {
         return this.standardToArray();
      }

      public <T> T[] toArray(T[] array) {
         return this.standardToArray(array);
      }

      public String toString() {
         return this.standardToString();
      }

      // $FF: synthetic method
      ValueSet(Object x1) {
         this();
      }
   }

   private class KeySet extends ForwardingSet<K> {
      private KeySet() {
      }

      protected Set<K> delegate() {
         return AbstractBiMap.this.delegate.keySet();
      }

      public void clear() {
         AbstractBiMap.this.clear();
      }

      public boolean remove(Object key) {
         if (!this.contains(key)) {
            return false;
         } else {
            AbstractBiMap.this.removeFromBothMaps(key);
            return true;
         }
      }

      public boolean removeAll(Collection<?> keysToRemove) {
         return this.standardRemoveAll(keysToRemove);
      }

      public boolean retainAll(Collection<?> keysToRetain) {
         return this.standardRetainAll(keysToRetain);
      }

      public Iterator<K> iterator() {
         return Maps.keyIterator(AbstractBiMap.this.entrySet().iterator());
      }

      // $FF: synthetic method
      KeySet(Object x1) {
         this();
      }
   }
}
