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
abstract class ImmutableMapEntrySet<K, V> extends ImmutableSet<Entry<K, V>> {
   abstract ImmutableMap<K, V> map();

   public int size() {
      return this.map().size();
   }

   public boolean contains(@Nullable Object object) {
      if (!(object instanceof Entry)) {
         return false;
      } else {
         Entry<?, ?> entry = (Entry)object;
         V value = this.map().get(entry.getKey());
         return value != null && value.equals(entry.getValue());
      }
   }

   boolean isPartialView() {
      return this.map().isPartialView();
   }

   @GwtIncompatible("not used in GWT")
   boolean isHashCodeFast() {
      return this.map().isHashCodeFast();
   }

   public int hashCode() {
      return this.map().hashCode();
   }

   @GwtIncompatible("serialization")
   Object writeReplace() {
      return new ImmutableMapEntrySet.EntrySetSerializedForm(this.map());
   }

   @GwtIncompatible("serialization")
   private static class EntrySetSerializedForm<K, V> implements Serializable {
      final ImmutableMap<K, V> map;
      private static final long serialVersionUID = 0L;

      EntrySetSerializedForm(ImmutableMap<K, V> map) {
         this.map = map;
      }

      Object readResolve() {
         return this.map.entrySet();
      }
   }

   static final class RegularEntrySet<K, V> extends ImmutableMapEntrySet<K, V> {
      @Weak
      private final transient ImmutableMap<K, V> map;
      private final transient Entry<K, V>[] entries;

      RegularEntrySet(ImmutableMap<K, V> map, Entry<K, V>[] entries) {
         this.map = map;
         this.entries = entries;
      }

      ImmutableMap<K, V> map() {
         return this.map;
      }

      public UnmodifiableIterator<Entry<K, V>> iterator() {
         return this.asList().iterator();
      }

      ImmutableList<Entry<K, V>> createAsList() {
         return new RegularImmutableAsList(this, this.entries);
      }
   }
}
