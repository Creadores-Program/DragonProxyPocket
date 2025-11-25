package com.google.common.cache;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.concurrent.ExecutionException;

public abstract class ForwardingLoadingCache<K, V> extends ForwardingCache<K, V> implements LoadingCache<K, V> {
   protected ForwardingLoadingCache() {
   }

   protected abstract LoadingCache<K, V> delegate();

   public V get(K key) throws ExecutionException {
      return this.delegate().get(key);
   }

   public V getUnchecked(K key) {
      return this.delegate().getUnchecked(key);
   }

   public ImmutableMap<K, V> getAll(Iterable<? extends K> keys) throws ExecutionException {
      return this.delegate().getAll(keys);
   }

   public V apply(K key) {
      return this.delegate().apply(key);
   }

   public void refresh(K key) {
      this.delegate().refresh(key);
   }

   public abstract static class SimpleForwardingLoadingCache<K, V> extends ForwardingLoadingCache<K, V> {
      private final LoadingCache<K, V> delegate;

      protected SimpleForwardingLoadingCache(LoadingCache<K, V> delegate) {
         this.delegate = (LoadingCache)Preconditions.checkNotNull(delegate);
      }

      protected final LoadingCache<K, V> delegate() {
         return this.delegate;
      }
   }
}
