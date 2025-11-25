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
final class ImmutableMapKeySet<K, V> extends ImmutableSet.Indexed<K> {
   @Weak
   private final ImmutableMap<K, V> map;

   ImmutableMapKeySet(ImmutableMap<K, V> map) {
      this.map = map;
   }

   public int size() {
      return this.map.size();
   }

   public UnmodifiableIterator<K> iterator() {
      return this.map.keyIterator();
   }

   public boolean contains(@Nullable Object object) {
      return this.map.containsKey(object);
   }

   K get(int index) {
      return ((Entry)this.map.entrySet().asList().get(index)).getKey();
   }

   boolean isPartialView() {
      return true;
   }

   @GwtIncompatible("serialization")
   Object writeReplace() {
      return new ImmutableMapKeySet.KeySetSerializedForm(this.map);
   }

   @GwtIncompatible("serialization")
   private static class KeySetSerializedForm<K> implements Serializable {
      final ImmutableMap<K, ?> map;
      private static final long serialVersionUID = 0L;

      KeySetSerializedForm(ImmutableMap<K, ?> map) {
         this.map = map;
      }

      Object readResolve() {
         return this.map.keySet();
      }
   }
}
