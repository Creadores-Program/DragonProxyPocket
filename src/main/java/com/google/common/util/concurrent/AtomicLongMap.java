package com.google.common.util.concurrent;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@GwtCompatible
public final class AtomicLongMap<K> {
   private final ConcurrentHashMap<K, AtomicLong> map;
   private transient Map<K, Long> asMap;

   private AtomicLongMap(ConcurrentHashMap<K, AtomicLong> map) {
      this.map = (ConcurrentHashMap)Preconditions.checkNotNull(map);
   }

   public static <K> AtomicLongMap<K> create() {
      return new AtomicLongMap(new ConcurrentHashMap());
   }

   public static <K> AtomicLongMap<K> create(Map<? extends K, ? extends Long> m) {
      AtomicLongMap<K> result = create();
      result.putAll(m);
      return result;
   }

   public long get(K key) {
      AtomicLong atomic = (AtomicLong)this.map.get(key);
      return atomic == null ? 0L : atomic.get();
   }

   public long incrementAndGet(K key) {
      return this.addAndGet(key, 1L);
   }

   public long decrementAndGet(K key) {
      return this.addAndGet(key, -1L);
   }

   public long addAndGet(K key, long delta) {
      AtomicLong atomic;
      label23:
      do {
         atomic = (AtomicLong)this.map.get(key);
         if (atomic == null) {
            atomic = (AtomicLong)this.map.putIfAbsent(key, new AtomicLong(delta));
            if (atomic == null) {
               return delta;
            }
         }

         long oldValue;
         long newValue;
         do {
            oldValue = atomic.get();
            if (oldValue == 0L) {
               continue label23;
            }

            newValue = oldValue + delta;
         } while(!atomic.compareAndSet(oldValue, newValue));

         return newValue;
      } while(!this.map.replace(key, atomic, new AtomicLong(delta)));

      return delta;
   }

   public long getAndIncrement(K key) {
      return this.getAndAdd(key, 1L);
   }

   public long getAndDecrement(K key) {
      return this.getAndAdd(key, -1L);
   }

   public long getAndAdd(K key, long delta) {
      AtomicLong atomic;
      label23:
      do {
         atomic = (AtomicLong)this.map.get(key);
         if (atomic == null) {
            atomic = (AtomicLong)this.map.putIfAbsent(key, new AtomicLong(delta));
            if (atomic == null) {
               return 0L;
            }
         }

         long oldValue;
         long newValue;
         do {
            oldValue = atomic.get();
            if (oldValue == 0L) {
               continue label23;
            }

            newValue = oldValue + delta;
         } while(!atomic.compareAndSet(oldValue, newValue));

         return oldValue;
      } while(!this.map.replace(key, atomic, new AtomicLong(delta)));

      return 0L;
   }

   public long put(K key, long newValue) {
      AtomicLong atomic;
      label23:
      do {
         atomic = (AtomicLong)this.map.get(key);
         if (atomic == null) {
            atomic = (AtomicLong)this.map.putIfAbsent(key, new AtomicLong(newValue));
            if (atomic == null) {
               return 0L;
            }
         }

         long oldValue;
         do {
            oldValue = atomic.get();
            if (oldValue == 0L) {
               continue label23;
            }
         } while(!atomic.compareAndSet(oldValue, newValue));

         return oldValue;
      } while(!this.map.replace(key, atomic, new AtomicLong(newValue)));

      return 0L;
   }

   public void putAll(Map<? extends K, ? extends Long> m) {
      Iterator i$ = m.entrySet().iterator();

      while(i$.hasNext()) {
         Entry<? extends K, ? extends Long> entry = (Entry)i$.next();
         this.put(entry.getKey(), (Long)entry.getValue());
      }

   }

   public long remove(K key) {
      AtomicLong atomic = (AtomicLong)this.map.get(key);
      if (atomic == null) {
         return 0L;
      } else {
         long oldValue;
         do {
            oldValue = atomic.get();
         } while(oldValue != 0L && !atomic.compareAndSet(oldValue, 0L));

         this.map.remove(key, atomic);
         return oldValue;
      }
   }

   public void removeAllZeros() {
      Iterator entryIterator = this.map.entrySet().iterator();

      while(entryIterator.hasNext()) {
         Entry<K, AtomicLong> entry = (Entry)entryIterator.next();
         AtomicLong atomic = (AtomicLong)entry.getValue();
         if (atomic != null && atomic.get() == 0L) {
            entryIterator.remove();
         }
      }

   }

   public long sum() {
      long sum = 0L;

      AtomicLong value;
      for(Iterator i$ = this.map.values().iterator(); i$.hasNext(); sum += value.get()) {
         value = (AtomicLong)i$.next();
      }

      return sum;
   }

   public Map<K, Long> asMap() {
      Map<K, Long> result = this.asMap;
      return result == null ? (this.asMap = this.createAsMap()) : result;
   }

   private Map<K, Long> createAsMap() {
      return Collections.unmodifiableMap(Maps.transformValues((Map)this.map, new Function<AtomicLong, Long>() {
         public Long apply(AtomicLong atomic) {
            return atomic.get();
         }
      }));
   }

   public boolean containsKey(Object key) {
      return this.map.containsKey(key);
   }

   public int size() {
      return this.map.size();
   }

   public boolean isEmpty() {
      return this.map.isEmpty();
   }

   public void clear() {
      this.map.clear();
   }

   public String toString() {
      return this.map.toString();
   }

   long putIfAbsent(K key, long newValue) {
      while(true) {
         AtomicLong atomic = (AtomicLong)this.map.get(key);
         if (atomic == null) {
            atomic = (AtomicLong)this.map.putIfAbsent(key, new AtomicLong(newValue));
            if (atomic == null) {
               return 0L;
            }
         }

         long oldValue = atomic.get();
         if (oldValue == 0L) {
            if (!this.map.replace(key, atomic, new AtomicLong(newValue))) {
               continue;
            }

            return 0L;
         }

         return oldValue;
      }
   }

   boolean replace(K key, long expectedOldValue, long newValue) {
      if (expectedOldValue == 0L) {
         return this.putIfAbsent(key, newValue) == 0L;
      } else {
         AtomicLong atomic = (AtomicLong)this.map.get(key);
         return atomic == null ? false : atomic.compareAndSet(expectedOldValue, newValue);
      }
   }

   boolean remove(K key, long value) {
      AtomicLong atomic = (AtomicLong)this.map.get(key);
      if (atomic == null) {
         return false;
      } else {
         long oldValue = atomic.get();
         if (oldValue != value) {
            return false;
         } else if (oldValue != 0L && !atomic.compareAndSet(oldValue, 0L)) {
            return false;
         } else {
            this.map.remove(key, atomic);
            return true;
         }
      }
   }
}
