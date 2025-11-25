package com.google.common.cache;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public abstract class AbstractLoadingCache<K, V> extends AbstractCache<K, V> implements LoadingCache<K, V> {
   protected AbstractLoadingCache() {
   }

   public V getUnchecked(K key) {
      try {
         return this.get(key);
      } catch (ExecutionException var3) {
         throw new UncheckedExecutionException(var3.getCause());
      }
   }

   public ImmutableMap<K, V> getAll(Iterable<? extends K> keys) throws ExecutionException {
      Map<K, V> result = Maps.newLinkedHashMap();
      Iterator i$ = keys.iterator();

      while(i$.hasNext()) {
         K key = i$.next();
         if (!result.containsKey(key)) {
            result.put(key, this.get(key));
         }
      }

      return ImmutableMap.copyOf((Map)result);
   }

   public final V apply(K key) {
      return this.getUnchecked(key);
   }

   public void refresh(K key) {
      throw new UnsupportedOperationException();
   }
}
