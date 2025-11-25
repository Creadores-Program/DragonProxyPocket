package com.google.common.cache;

import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingObject;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nullable;

public abstract class ForwardingCache<K, V> extends ForwardingObject implements Cache<K, V> {
   protected ForwardingCache() {
   }

   protected abstract Cache<K, V> delegate();

   @Nullable
   public V getIfPresent(Object key) {
      return this.delegate().getIfPresent(key);
   }

   public V get(K key, Callable<? extends V> valueLoader) throws ExecutionException {
      return this.delegate().get(key, valueLoader);
   }

   public ImmutableMap<K, V> getAllPresent(Iterable<?> keys) {
      return this.delegate().getAllPresent(keys);
   }

   public void put(K key, V value) {
      this.delegate().put(key, value);
   }

   public void putAll(Map<? extends K, ? extends V> m) {
      this.delegate().putAll(m);
   }

   public void invalidate(Object key) {
      this.delegate().invalidate(key);
   }

   public void invalidateAll(Iterable<?> keys) {
      this.delegate().invalidateAll(keys);
   }

   public void invalidateAll() {
      this.delegate().invalidateAll();
   }

   public long size() {
      return this.delegate().size();
   }

   public CacheStats stats() {
      return this.delegate().stats();
   }

   public ConcurrentMap<K, V> asMap() {
      return this.delegate().asMap();
   }

   public void cleanUp() {
      this.delegate().cleanUp();
   }

   public abstract static class SimpleForwardingCache<K, V> extends ForwardingCache<K, V> {
      private final Cache<K, V> delegate;

      protected SimpleForwardingCache(Cache<K, V> delegate) {
         this.delegate = (Cache)Preconditions.checkNotNull(delegate);
      }

      protected final Cache<K, V> delegate() {
         return this.delegate;
      }
   }
}
