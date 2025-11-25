package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.j2objc.annotations.Weak;
import java.io.Serializable;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible(
   emulated = true
)
final class ImmutableMapValues<K, V> extends ImmutableCollection<V> {
   @Weak
   private final ImmutableMap<K, V> map;

   ImmutableMapValues(ImmutableMap<K, V> map) {
      this.map = map;
   }

   public int size() {
      return this.map.size();
   }

   public UnmodifiableIterator<V> iterator() {
      return new UnmodifiableIterator<V>() {
         final UnmodifiableIterator<Entry<K, V>> entryItr;

         {
            this.entryItr = ImmutableMapValues.this.map.entrySet().iterator();
         }

         public boolean hasNext() {
            return this.entryItr.hasNext();
         }

         public V next() {
            return ((Entry)this.entryItr.next()).getValue();
         }
      };
   }

   public boolean contains(@Nullable Object object) {
      return object != null && Iterators.contains(this.iterator(), object);
   }

   boolean isPartialView() {
      return true;
   }

   ImmutableList<V> createAsList() {
      final ImmutableList<Entry<K, V>> entryList = this.map.entrySet().asList();
      return new ImmutableAsList<V>() {
         public V get(int index) {
            return ((Entry)entryList.get(index)).getValue();
         }

         ImmutableCollection<V> delegateCollection() {
            return ImmutableMapValues.this;
         }
      };
   }

   @GwtIncompatible("serialization")
   Object writeReplace() {
      return new ImmutableMapValues.SerializedForm(this.map);
   }

   @GwtIncompatible("serialization")
   private static class SerializedForm<V> implements Serializable {
      final ImmutableMap<?, V> map;
      private static final long serialVersionUID = 0L;

      SerializedForm(ImmutableMap<?, V> map) {
         this.map = map;
      }

      Object readResolve() {
         return this.map.values();
      }
   }
}
