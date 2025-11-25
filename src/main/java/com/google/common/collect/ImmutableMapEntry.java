package com.google.common.collect;

import com.google.common.annotations.GwtIncompatible;
import javax.annotation.Nullable;

@GwtIncompatible("unnecessary")
class ImmutableMapEntry<K, V> extends ImmutableEntry<K, V> {
   static <K, V> ImmutableMapEntry<K, V>[] createEntryArray(int size) {
      return new ImmutableMapEntry[size];
   }

   ImmutableMapEntry(K key, V value) {
      super(key, value);
      CollectPreconditions.checkEntryNotNull(key, value);
   }

   ImmutableMapEntry(ImmutableMapEntry<K, V> contents) {
      super(contents.getKey(), contents.getValue());
   }

   @Nullable
   ImmutableMapEntry<K, V> getNextInKeyBucket() {
      return null;
   }

   @Nullable
   ImmutableMapEntry<K, V> getNextInValueBucket() {
      return null;
   }

   boolean isReusable() {
      return true;
   }

   static final class NonTerminalImmutableBiMapEntry<K, V> extends ImmutableMapEntry.NonTerminalImmutableMapEntry<K, V> {
      private final transient ImmutableMapEntry<K, V> nextInValueBucket;

      NonTerminalImmutableBiMapEntry(K key, V value, ImmutableMapEntry<K, V> nextInKeyBucket, ImmutableMapEntry<K, V> nextInValueBucket) {
         super(key, value, nextInKeyBucket);
         this.nextInValueBucket = nextInValueBucket;
      }

      @Nullable
      ImmutableMapEntry<K, V> getNextInValueBucket() {
         return this.nextInValueBucket;
      }
   }

   static class NonTerminalImmutableMapEntry<K, V> extends ImmutableMapEntry<K, V> {
      private final transient ImmutableMapEntry<K, V> nextInKeyBucket;

      NonTerminalImmutableMapEntry(K key, V value, ImmutableMapEntry<K, V> nextInKeyBucket) {
         super(key, value);
         this.nextInKeyBucket = nextInKeyBucket;
      }

      @Nullable
      final ImmutableMapEntry<K, V> getNextInKeyBucket() {
         return this.nextInKeyBucket;
      }

      final boolean isReusable() {
         return false;
      }
   }
}
