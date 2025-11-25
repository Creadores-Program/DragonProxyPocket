package com.google.common.cache;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.AbstractSequentialIterator;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.j2objc.annotations.Weak;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

@GwtCompatible(
   emulated = true
)
class LocalCache<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
   static final int MAXIMUM_CAPACITY = 1073741824;
   static final int MAX_SEGMENTS = 65536;
   static final int CONTAINS_VALUE_RETRIES = 3;
   static final int DRAIN_THRESHOLD = 63;
   static final int DRAIN_MAX = 16;
   static final Logger logger = Logger.getLogger(LocalCache.class.getName());
   final int segmentMask;
   final int segmentShift;
   final LocalCache.Segment<K, V>[] segments;
   final int concurrencyLevel;
   final Equivalence<Object> keyEquivalence;
   final Equivalence<Object> valueEquivalence;
   final LocalCache.Strength keyStrength;
   final LocalCache.Strength valueStrength;
   final long maxWeight;
   final Weigher<K, V> weigher;
   final long expireAfterAccessNanos;
   final long expireAfterWriteNanos;
   final long refreshNanos;
   final Queue<RemovalNotification<K, V>> removalNotificationQueue;
   final RemovalListener<K, V> removalListener;
   final Ticker ticker;
   final LocalCache.EntryFactory entryFactory;
   final AbstractCache.StatsCounter globalStatsCounter;
   @Nullable
   final CacheLoader<? super K, V> defaultLoader;
   static final LocalCache.ValueReference<Object, Object> UNSET = new LocalCache.ValueReference<Object, Object>() {
      public Object get() {
         return null;
      }

      public int getWeight() {
         return 0;
      }

      public LocalCache.ReferenceEntry<Object, Object> getEntry() {
         return null;
      }

      public LocalCache.ValueReference<Object, Object> copyFor(ReferenceQueue<Object> queue, @Nullable Object value, LocalCache.ReferenceEntry<Object, Object> entry) {
         return this;
      }

      public boolean isLoading() {
         return false;
      }

      public boolean isActive() {
         return false;
      }

      public Object waitForValue() {
         return null;
      }

      public void notifyNewValue(Object newValue) {
      }
   };
   static final Queue<? extends Object> DISCARDING_QUEUE = new AbstractQueue<Object>() {
      public boolean offer(Object o) {
         return true;
      }

      public Object peek() {
         return null;
      }

      public Object poll() {
         return null;
      }

      public int size() {
         return 0;
      }

      public Iterator<Object> iterator() {
         return ImmutableSet.of().iterator();
      }
   };
   Set<K> keySet;
   Collection<V> values;
   Set<Entry<K, V>> entrySet;

   LocalCache(CacheBuilder<? super K, ? super V> builder, @Nullable CacheLoader<? super K, V> loader) {
      this.concurrencyLevel = Math.min(builder.getConcurrencyLevel(), 65536);
      this.keyStrength = builder.getKeyStrength();
      this.valueStrength = builder.getValueStrength();
      this.keyEquivalence = builder.getKeyEquivalence();
      this.valueEquivalence = builder.getValueEquivalence();
      this.maxWeight = builder.getMaximumWeight();
      this.weigher = builder.getWeigher();
      this.expireAfterAccessNanos = builder.getExpireAfterAccessNanos();
      this.expireAfterWriteNanos = builder.getExpireAfterWriteNanos();
      this.refreshNanos = builder.getRefreshNanos();
      this.removalListener = builder.getRemovalListener();
      this.removalNotificationQueue = (Queue)(this.removalListener == CacheBuilder.NullListener.INSTANCE ? discardingQueue() : new ConcurrentLinkedQueue());
      this.ticker = builder.getTicker(this.recordsTime());
      this.entryFactory = LocalCache.EntryFactory.getFactory(this.keyStrength, this.usesAccessEntries(), this.usesWriteEntries());
      this.globalStatsCounter = (AbstractCache.StatsCounter)builder.getStatsCounterSupplier().get();
      this.defaultLoader = loader;
      int initialCapacity = Math.min(builder.getInitialCapacity(), 1073741824);
      if (this.evictsBySize() && !this.customWeigher()) {
         initialCapacity = Math.min(initialCapacity, (int)this.maxWeight);
      }

      int segmentShift = 0;

      int segmentCount;
      for(segmentCount = 1; segmentCount < this.concurrencyLevel && (!this.evictsBySize() || (long)(segmentCount * 20) <= this.maxWeight); segmentCount <<= 1) {
         ++segmentShift;
      }

      this.segmentShift = 32 - segmentShift;
      this.segmentMask = segmentCount - 1;
      this.segments = this.newSegmentArray(segmentCount);
      int segmentCapacity = initialCapacity / segmentCount;
      if (segmentCapacity * segmentCount < initialCapacity) {
         ++segmentCapacity;
      }

      int segmentSize;
      for(segmentSize = 1; segmentSize < segmentCapacity; segmentSize <<= 1) {
      }

      if (this.evictsBySize()) {
         long maxSegmentWeight = this.maxWeight / (long)segmentCount + 1L;
         long remainder = this.maxWeight % (long)segmentCount;

         for(int i = 0; i < this.segments.length; ++i) {
            if ((long)i == remainder) {
               --maxSegmentWeight;
            }

            this.segments[i] = this.createSegment(segmentSize, maxSegmentWeight, (AbstractCache.StatsCounter)builder.getStatsCounterSupplier().get());
         }
      } else {
         for(int i = 0; i < this.segments.length; ++i) {
            this.segments[i] = this.createSegment(segmentSize, -1L, (AbstractCache.StatsCounter)builder.getStatsCounterSupplier().get());
         }
      }

   }

   boolean evictsBySize() {
      return this.maxWeight >= 0L;
   }

   boolean customWeigher() {
      return this.weigher != CacheBuilder.OneWeigher.INSTANCE;
   }

   boolean expires() {
      return this.expiresAfterWrite() || this.expiresAfterAccess();
   }

   boolean expiresAfterWrite() {
      return this.expireAfterWriteNanos > 0L;
   }

   boolean expiresAfterAccess() {
      return this.expireAfterAccessNanos > 0L;
   }

   boolean refreshes() {
      return this.refreshNanos > 0L;
   }

   boolean usesAccessQueue() {
      return this.expiresAfterAccess() || this.evictsBySize();
   }

   boolean usesWriteQueue() {
      return this.expiresAfterWrite();
   }

   boolean recordsWrite() {
      return this.expiresAfterWrite() || this.refreshes();
   }

   boolean recordsAccess() {
      return this.expiresAfterAccess();
   }

   boolean recordsTime() {
      return this.recordsWrite() || this.recordsAccess();
   }

   boolean usesWriteEntries() {
      return this.usesWriteQueue() || this.recordsWrite();
   }

   boolean usesAccessEntries() {
      return this.usesAccessQueue() || this.recordsAccess();
   }

   boolean usesKeyReferences() {
      return this.keyStrength != LocalCache.Strength.STRONG;
   }

   boolean usesValueReferences() {
      return this.valueStrength != LocalCache.Strength.STRONG;
   }

   static <K, V> LocalCache.ValueReference<K, V> unset() {
      return UNSET;
   }

   static <K, V> LocalCache.ReferenceEntry<K, V> nullEntry() {
      return LocalCache.NullEntry.INSTANCE;
   }

   static <E> Queue<E> discardingQueue() {
      return DISCARDING_QUEUE;
   }

   static int rehash(int h) {
      h += h << 15 ^ -12931;
      h ^= h >>> 10;
      h += h << 3;
      h ^= h >>> 6;
      h += (h << 2) + (h << 14);
      return h ^ h >>> 16;
   }

   @VisibleForTesting
   LocalCache.ReferenceEntry<K, V> newEntry(K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
      LocalCache.Segment<K, V> segment = this.segmentFor(hash);
      segment.lock();

      LocalCache.ReferenceEntry var5;
      try {
         var5 = segment.newEntry(key, hash, next);
      } finally {
         segment.unlock();
      }

      return var5;
   }

   @VisibleForTesting
   LocalCache.ReferenceEntry<K, V> copyEntry(LocalCache.ReferenceEntry<K, V> original, LocalCache.ReferenceEntry<K, V> newNext) {
      int hash = original.getHash();
      return this.segmentFor(hash).copyEntry(original, newNext);
   }

   @VisibleForTesting
   LocalCache.ValueReference<K, V> newValueReference(LocalCache.ReferenceEntry<K, V> entry, V value, int weight) {
      int hash = entry.getHash();
      return this.valueStrength.referenceValue(this.segmentFor(hash), entry, Preconditions.checkNotNull(value), weight);
   }

   int hash(@Nullable Object key) {
      int h = this.keyEquivalence.hash(key);
      return rehash(h);
   }

   void reclaimValue(LocalCache.ValueReference<K, V> valueReference) {
      LocalCache.ReferenceEntry<K, V> entry = valueReference.getEntry();
      int hash = entry.getHash();
      this.segmentFor(hash).reclaimValue(entry.getKey(), hash, valueReference);
   }

   void reclaimKey(LocalCache.ReferenceEntry<K, V> entry) {
      int hash = entry.getHash();
      this.segmentFor(hash).reclaimKey(entry, hash);
   }

   @VisibleForTesting
   boolean isLive(LocalCache.ReferenceEntry<K, V> entry, long now) {
      return this.segmentFor(entry.getHash()).getLiveValue(entry, now) != null;
   }

   LocalCache.Segment<K, V> segmentFor(int hash) {
      return this.segments[hash >>> this.segmentShift & this.segmentMask];
   }

   LocalCache.Segment<K, V> createSegment(int initialCapacity, long maxSegmentWeight, AbstractCache.StatsCounter statsCounter) {
      return new LocalCache.Segment(this, initialCapacity, maxSegmentWeight, statsCounter);
   }

   @Nullable
   V getLiveValue(LocalCache.ReferenceEntry<K, V> entry, long now) {
      if (entry.getKey() == null) {
         return null;
      } else {
         V value = entry.getValueReference().get();
         if (value == null) {
            return null;
         } else {
            return this.isExpired(entry, now) ? null : value;
         }
      }
   }

   boolean isExpired(LocalCache.ReferenceEntry<K, V> entry, long now) {
      Preconditions.checkNotNull(entry);
      if (this.expiresAfterAccess() && now - entry.getAccessTime() >= this.expireAfterAccessNanos) {
         return true;
      } else {
         return this.expiresAfterWrite() && now - entry.getWriteTime() >= this.expireAfterWriteNanos;
      }
   }

   static <K, V> void connectAccessOrder(LocalCache.ReferenceEntry<K, V> previous, LocalCache.ReferenceEntry<K, V> next) {
      previous.setNextInAccessQueue(next);
      next.setPreviousInAccessQueue(previous);
   }

   static <K, V> void nullifyAccessOrder(LocalCache.ReferenceEntry<K, V> nulled) {
      LocalCache.ReferenceEntry<K, V> nullEntry = nullEntry();
      nulled.setNextInAccessQueue(nullEntry);
      nulled.setPreviousInAccessQueue(nullEntry);
   }

   static <K, V> void connectWriteOrder(LocalCache.ReferenceEntry<K, V> previous, LocalCache.ReferenceEntry<K, V> next) {
      previous.setNextInWriteQueue(next);
      next.setPreviousInWriteQueue(previous);
   }

   static <K, V> void nullifyWriteOrder(LocalCache.ReferenceEntry<K, V> nulled) {
      LocalCache.ReferenceEntry<K, V> nullEntry = nullEntry();
      nulled.setNextInWriteQueue(nullEntry);
      nulled.setPreviousInWriteQueue(nullEntry);
   }

   void processPendingNotifications() {
      RemovalNotification notification;
      while((notification = (RemovalNotification)this.removalNotificationQueue.poll()) != null) {
         try {
            this.removalListener.onRemoval(notification);
         } catch (Throwable var3) {
            logger.log(Level.WARNING, "Exception thrown by removal listener", var3);
         }
      }

   }

   final LocalCache.Segment<K, V>[] newSegmentArray(int ssize) {
      return new LocalCache.Segment[ssize];
   }

   public void cleanUp() {
      LocalCache.Segment[] arr$ = this.segments;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         LocalCache.Segment<?, ?> segment = arr$[i$];
         segment.cleanUp();
      }

   }

   public boolean isEmpty() {
      long sum = 0L;
      LocalCache.Segment<K, V>[] segments = this.segments;

      int i;
      for(i = 0; i < segments.length; ++i) {
         if (segments[i].count != 0) {
            return false;
         }

         sum += (long)segments[i].modCount;
      }

      if (sum != 0L) {
         for(i = 0; i < segments.length; ++i) {
            if (segments[i].count != 0) {
               return false;
            }

            sum -= (long)segments[i].modCount;
         }

         if (sum != 0L) {
            return false;
         }
      }

      return true;
   }

   long longSize() {
      LocalCache.Segment<K, V>[] segments = this.segments;
      long sum = 0L;

      for(int i = 0; i < segments.length; ++i) {
         sum += (long)Math.max(0, segments[i].count);
      }

      return sum;
   }

   public int size() {
      return Ints.saturatedCast(this.longSize());
   }

   @Nullable
   public V get(@Nullable Object key) {
      if (key == null) {
         return null;
      } else {
         int hash = this.hash(key);
         return this.segmentFor(hash).get(key, hash);
      }
   }

   @Nullable
   public V getIfPresent(Object key) {
      int hash = this.hash(Preconditions.checkNotNull(key));
      V value = this.segmentFor(hash).get(key, hash);
      if (value == null) {
         this.globalStatsCounter.recordMisses(1);
      } else {
         this.globalStatsCounter.recordHits(1);
      }

      return value;
   }

   V get(K key, CacheLoader<? super K, V> loader) throws ExecutionException {
      int hash = this.hash(Preconditions.checkNotNull(key));
      return this.segmentFor(hash).get(key, hash, loader);
   }

   V getOrLoad(K key) throws ExecutionException {
      return this.get(key, this.defaultLoader);
   }

   ImmutableMap<K, V> getAllPresent(Iterable<?> keys) {
      int hits = 0;
      int misses = 0;
      Map<K, V> result = Maps.newLinkedHashMap();
      Iterator i$ = keys.iterator();

      while(i$.hasNext()) {
         Object key = i$.next();
         V value = this.get(key);
         if (value == null) {
            ++misses;
         } else {
            result.put(key, value);
            ++hits;
         }
      }

      this.globalStatsCounter.recordHits(hits);
      this.globalStatsCounter.recordMisses(misses);
      return ImmutableMap.copyOf((Map)result);
   }

   ImmutableMap<K, V> getAll(Iterable<? extends K> keys) throws ExecutionException {
      int hits = 0;
      int misses = 0;
      Map<K, V> result = Maps.newLinkedHashMap();
      Set<K> keysToLoad = Sets.newLinkedHashSet();
      Iterator i$ = keys.iterator();

      Object key;
      while(i$.hasNext()) {
         K key = i$.next();
         key = this.get(key);
         if (!result.containsKey(key)) {
            result.put(key, key);
            if (key == null) {
               ++misses;
               keysToLoad.add(key);
            } else {
               ++hits;
            }
         }
      }

      ImmutableMap var16;
      try {
         if (!keysToLoad.isEmpty()) {
            Iterator i$;
            try {
               Map<K, V> newEntries = this.loadAll(keysToLoad, this.defaultLoader);
               i$ = keysToLoad.iterator();

               while(i$.hasNext()) {
                  key = i$.next();
                  V value = newEntries.get(key);
                  if (value == null) {
                     throw new CacheLoader.InvalidCacheLoadException("loadAll failed to return a value for " + key);
                  }

                  result.put(key, value);
               }
            } catch (CacheLoader.UnsupportedLoadingOperationException var13) {
               i$ = keysToLoad.iterator();

               while(i$.hasNext()) {
                  key = i$.next();
                  --misses;
                  result.put(key, this.get(key, this.defaultLoader));
               }
            }
         }

         var16 = ImmutableMap.copyOf((Map)result);
      } finally {
         this.globalStatsCounter.recordHits(hits);
         this.globalStatsCounter.recordMisses(misses);
      }

      return var16;
   }

   @Nullable
   Map<K, V> loadAll(Set<? extends K> keys, CacheLoader<? super K, V> loader) throws ExecutionException {
      Preconditions.checkNotNull(loader);
      Preconditions.checkNotNull(keys);
      Stopwatch stopwatch = Stopwatch.createStarted();
      boolean success = false;

      Map result;
      try {
         Map<K, V> map = loader.loadAll(keys);
         result = map;
         success = true;
      } catch (CacheLoader.UnsupportedLoadingOperationException var17) {
         success = true;
         throw var17;
      } catch (InterruptedException var18) {
         Thread.currentThread().interrupt();
         throw new ExecutionException(var18);
      } catch (RuntimeException var19) {
         throw new UncheckedExecutionException(var19);
      } catch (Exception var20) {
         throw new ExecutionException(var20);
      } catch (Error var21) {
         throw new ExecutionError(var21);
      } finally {
         if (!success) {
            this.globalStatsCounter.recordLoadException(stopwatch.elapsed(TimeUnit.NANOSECONDS));
         }

      }

      if (result == null) {
         this.globalStatsCounter.recordLoadException(stopwatch.elapsed(TimeUnit.NANOSECONDS));
         throw new CacheLoader.InvalidCacheLoadException(loader + " returned null map from loadAll");
      } else {
         stopwatch.stop();
         boolean nullsPresent = false;
         Iterator i$ = result.entrySet().iterator();

         while(true) {
            while(i$.hasNext()) {
               Entry<K, V> entry = (Entry)i$.next();
               K key = entry.getKey();
               V value = entry.getValue();
               if (key != null && value != null) {
                  this.put(key, value);
               } else {
                  nullsPresent = true;
               }
            }

            if (nullsPresent) {
               this.globalStatsCounter.recordLoadException(stopwatch.elapsed(TimeUnit.NANOSECONDS));
               throw new CacheLoader.InvalidCacheLoadException(loader + " returned null keys or values from loadAll");
            }

            this.globalStatsCounter.recordLoadSuccess(stopwatch.elapsed(TimeUnit.NANOSECONDS));
            return result;
         }
      }
   }

   LocalCache.ReferenceEntry<K, V> getEntry(@Nullable Object key) {
      if (key == null) {
         return null;
      } else {
         int hash = this.hash(key);
         return this.segmentFor(hash).getEntry(key, hash);
      }
   }

   void refresh(K key) {
      int hash = this.hash(Preconditions.checkNotNull(key));
      this.segmentFor(hash).refresh(key, hash, this.defaultLoader, false);
   }

   public boolean containsKey(@Nullable Object key) {
      if (key == null) {
         return false;
      } else {
         int hash = this.hash(key);
         return this.segmentFor(hash).containsKey(key, hash);
      }
   }

   public boolean containsValue(@Nullable Object value) {
      if (value == null) {
         return false;
      } else {
         long now = this.ticker.read();
         LocalCache.Segment<K, V>[] segments = this.segments;
         long last = -1L;

         for(int i = 0; i < 3; ++i) {
            long sum = 0L;
            LocalCache.Segment[] arr$ = segments;
            int len$ = segments.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               LocalCache.Segment<K, V> segment = arr$[i$];
               int unused = segment.count;
               AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table = segment.table;

               for(int j = 0; j < table.length(); ++j) {
                  for(LocalCache.ReferenceEntry e = (LocalCache.ReferenceEntry)table.get(j); e != null; e = e.getNext()) {
                     V v = segment.getLiveValue(e, now);
                     if (v != null && this.valueEquivalence.equivalent(value, v)) {
                        return true;
                     }
                  }
               }

               sum += (long)segment.modCount;
            }

            if (sum == last) {
               break;
            }

            last = sum;
         }

         return false;
      }
   }

   public V put(K key, V value) {
      Preconditions.checkNotNull(key);
      Preconditions.checkNotNull(value);
      int hash = this.hash(key);
      return this.segmentFor(hash).put(key, hash, value, false);
   }

   public V putIfAbsent(K key, V value) {
      Preconditions.checkNotNull(key);
      Preconditions.checkNotNull(value);
      int hash = this.hash(key);
      return this.segmentFor(hash).put(key, hash, value, true);
   }

   public void putAll(Map<? extends K, ? extends V> m) {
      Iterator i$ = m.entrySet().iterator();

      while(i$.hasNext()) {
         Entry<? extends K, ? extends V> e = (Entry)i$.next();
         this.put(e.getKey(), e.getValue());
      }

   }

   public V remove(@Nullable Object key) {
      if (key == null) {
         return null;
      } else {
         int hash = this.hash(key);
         return this.segmentFor(hash).remove(key, hash);
      }
   }

   public boolean remove(@Nullable Object key, @Nullable Object value) {
      if (key != null && value != null) {
         int hash = this.hash(key);
         return this.segmentFor(hash).remove(key, hash, value);
      } else {
         return false;
      }
   }

   public boolean replace(K key, @Nullable V oldValue, V newValue) {
      Preconditions.checkNotNull(key);
      Preconditions.checkNotNull(newValue);
      if (oldValue == null) {
         return false;
      } else {
         int hash = this.hash(key);
         return this.segmentFor(hash).replace(key, hash, oldValue, newValue);
      }
   }

   public V replace(K key, V value) {
      Preconditions.checkNotNull(key);
      Preconditions.checkNotNull(value);
      int hash = this.hash(key);
      return this.segmentFor(hash).replace(key, hash, value);
   }

   public void clear() {
      LocalCache.Segment[] arr$ = this.segments;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         LocalCache.Segment<K, V> segment = arr$[i$];
         segment.clear();
      }

   }

   void invalidateAll(Iterable<?> keys) {
      Iterator i$ = keys.iterator();

      while(i$.hasNext()) {
         Object key = i$.next();
         this.remove(key);
      }

   }

   public Set<K> keySet() {
      Set<K> ks = this.keySet;
      return ks != null ? ks : (this.keySet = new LocalCache.KeySet(this));
   }

   public Collection<V> values() {
      Collection<V> vs = this.values;
      return vs != null ? vs : (this.values = new LocalCache.Values(this));
   }

   @GwtIncompatible("Not supported.")
   public Set<Entry<K, V>> entrySet() {
      Set<Entry<K, V>> es = this.entrySet;
      return es != null ? es : (this.entrySet = new LocalCache.EntrySet(this));
   }

   private static <E> ArrayList<E> toArrayList(Collection<E> c) {
      ArrayList<E> result = new ArrayList(c.size());
      Iterators.addAll(result, c.iterator());
      return result;
   }

   static class LocalLoadingCache<K, V> extends LocalCache.LocalManualCache<K, V> implements LoadingCache<K, V> {
      private static final long serialVersionUID = 1L;

      LocalLoadingCache(CacheBuilder<? super K, ? super V> builder, CacheLoader<? super K, V> loader) {
         super(new LocalCache(builder, (CacheLoader)Preconditions.checkNotNull(loader)), null);
      }

      public V get(K key) throws ExecutionException {
         return this.localCache.getOrLoad(key);
      }

      public V getUnchecked(K key) {
         try {
            return this.get(key);
         } catch (ExecutionException var3) {
            throw new UncheckedExecutionException(var3.getCause());
         }
      }

      public ImmutableMap<K, V> getAll(Iterable<? extends K> keys) throws ExecutionException {
         return this.localCache.getAll(keys);
      }

      public void refresh(K key) {
         this.localCache.refresh(key);
      }

      public final V apply(K key) {
         return this.getUnchecked(key);
      }

      Object writeReplace() {
         return new LocalCache.LoadingSerializationProxy(this.localCache);
      }
   }

   static class LocalManualCache<K, V> implements Cache<K, V>, Serializable {
      final LocalCache<K, V> localCache;
      private static final long serialVersionUID = 1L;

      LocalManualCache(CacheBuilder<? super K, ? super V> builder) {
         this(new LocalCache(builder, (CacheLoader)null));
      }

      private LocalManualCache(LocalCache<K, V> localCache) {
         this.localCache = localCache;
      }

      @Nullable
      public V getIfPresent(Object key) {
         return this.localCache.getIfPresent(key);
      }

      public V get(K key, final Callable<? extends V> valueLoader) throws ExecutionException {
         Preconditions.checkNotNull(valueLoader);
         return this.localCache.get(key, new CacheLoader<Object, V>() {
            public V load(Object key) throws Exception {
               return valueLoader.call();
            }
         });
      }

      public ImmutableMap<K, V> getAllPresent(Iterable<?> keys) {
         return this.localCache.getAllPresent(keys);
      }

      public void put(K key, V value) {
         this.localCache.put(key, value);
      }

      public void putAll(Map<? extends K, ? extends V> m) {
         this.localCache.putAll(m);
      }

      public void invalidate(Object key) {
         Preconditions.checkNotNull(key);
         this.localCache.remove(key);
      }

      public void invalidateAll(Iterable<?> keys) {
         this.localCache.invalidateAll(keys);
      }

      public void invalidateAll() {
         this.localCache.clear();
      }

      public long size() {
         return this.localCache.longSize();
      }

      public ConcurrentMap<K, V> asMap() {
         return this.localCache;
      }

      public CacheStats stats() {
         AbstractCache.SimpleStatsCounter aggregator = new AbstractCache.SimpleStatsCounter();
         aggregator.incrementBy(this.localCache.globalStatsCounter);
         LocalCache.Segment[] arr$ = this.localCache.segments;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            LocalCache.Segment<K, V> segment = arr$[i$];
            aggregator.incrementBy(segment.statsCounter);
         }

         return aggregator.snapshot();
      }

      public void cleanUp() {
         this.localCache.cleanUp();
      }

      Object writeReplace() {
         return new LocalCache.ManualSerializationProxy(this.localCache);
      }

      // $FF: synthetic method
      LocalManualCache(LocalCache x0, Object x1) {
         this(x0);
      }
   }

   static final class LoadingSerializationProxy<K, V> extends LocalCache.ManualSerializationProxy<K, V> implements LoadingCache<K, V>, Serializable {
      private static final long serialVersionUID = 1L;
      transient LoadingCache<K, V> autoDelegate;

      LoadingSerializationProxy(LocalCache<K, V> cache) {
         super(cache);
      }

      private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
         in.defaultReadObject();
         CacheBuilder<K, V> builder = this.recreateCacheBuilder();
         this.autoDelegate = builder.build(this.loader);
      }

      public V get(K key) throws ExecutionException {
         return this.autoDelegate.get(key);
      }

      public V getUnchecked(K key) {
         return this.autoDelegate.getUnchecked(key);
      }

      public ImmutableMap<K, V> getAll(Iterable<? extends K> keys) throws ExecutionException {
         return this.autoDelegate.getAll(keys);
      }

      public final V apply(K key) {
         return this.autoDelegate.apply(key);
      }

      public void refresh(K key) {
         this.autoDelegate.refresh(key);
      }

      private Object readResolve() {
         return this.autoDelegate;
      }
   }

   static class ManualSerializationProxy<K, V> extends ForwardingCache<K, V> implements Serializable {
      private static final long serialVersionUID = 1L;
      final LocalCache.Strength keyStrength;
      final LocalCache.Strength valueStrength;
      final Equivalence<Object> keyEquivalence;
      final Equivalence<Object> valueEquivalence;
      final long expireAfterWriteNanos;
      final long expireAfterAccessNanos;
      final long maxWeight;
      final Weigher<K, V> weigher;
      final int concurrencyLevel;
      final RemovalListener<? super K, ? super V> removalListener;
      final Ticker ticker;
      final CacheLoader<? super K, V> loader;
      transient Cache<K, V> delegate;

      ManualSerializationProxy(LocalCache<K, V> cache) {
         this(cache.keyStrength, cache.valueStrength, cache.keyEquivalence, cache.valueEquivalence, cache.expireAfterWriteNanos, cache.expireAfterAccessNanos, cache.maxWeight, cache.weigher, cache.concurrencyLevel, cache.removalListener, cache.ticker, cache.defaultLoader);
      }

      private ManualSerializationProxy(LocalCache.Strength keyStrength, LocalCache.Strength valueStrength, Equivalence<Object> keyEquivalence, Equivalence<Object> valueEquivalence, long expireAfterWriteNanos, long expireAfterAccessNanos, long maxWeight, Weigher<K, V> weigher, int concurrencyLevel, RemovalListener<? super K, ? super V> removalListener, Ticker ticker, CacheLoader<? super K, V> loader) {
         this.keyStrength = keyStrength;
         this.valueStrength = valueStrength;
         this.keyEquivalence = keyEquivalence;
         this.valueEquivalence = valueEquivalence;
         this.expireAfterWriteNanos = expireAfterWriteNanos;
         this.expireAfterAccessNanos = expireAfterAccessNanos;
         this.maxWeight = maxWeight;
         this.weigher = weigher;
         this.concurrencyLevel = concurrencyLevel;
         this.removalListener = removalListener;
         this.ticker = ticker != Ticker.systemTicker() && ticker != CacheBuilder.NULL_TICKER ? ticker : null;
         this.loader = loader;
      }

      CacheBuilder<K, V> recreateCacheBuilder() {
         CacheBuilder<K, V> builder = CacheBuilder.newBuilder().setKeyStrength(this.keyStrength).setValueStrength(this.valueStrength).keyEquivalence(this.keyEquivalence).valueEquivalence(this.valueEquivalence).concurrencyLevel(this.concurrencyLevel).removalListener(this.removalListener);
         builder.strictParsing = false;
         if (this.expireAfterWriteNanos > 0L) {
            builder.expireAfterWrite(this.expireAfterWriteNanos, TimeUnit.NANOSECONDS);
         }

         if (this.expireAfterAccessNanos > 0L) {
            builder.expireAfterAccess(this.expireAfterAccessNanos, TimeUnit.NANOSECONDS);
         }

         if (this.weigher != CacheBuilder.OneWeigher.INSTANCE) {
            builder.weigher(this.weigher);
            if (this.maxWeight != -1L) {
               builder.maximumWeight(this.maxWeight);
            }
         } else if (this.maxWeight != -1L) {
            builder.maximumSize(this.maxWeight);
         }

         if (this.ticker != null) {
            builder.ticker(this.ticker);
         }

         return builder;
      }

      private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
         in.defaultReadObject();
         CacheBuilder<K, V> builder = this.recreateCacheBuilder();
         this.delegate = builder.build();
      }

      private Object readResolve() {
         return this.delegate;
      }

      protected Cache<K, V> delegate() {
         return this.delegate;
      }
   }

   final class EntrySet extends LocalCache<K, V>.AbstractCacheSet<Entry<K, V>> {
      EntrySet(ConcurrentMap<?, ?> map) {
         super(map);
      }

      public Iterator<Entry<K, V>> iterator() {
         return LocalCache.this.new EntryIterator();
      }

      public boolean contains(Object o) {
         if (!(o instanceof Entry)) {
            return false;
         } else {
            Entry<?, ?> e = (Entry)o;
            Object key = e.getKey();
            if (key == null) {
               return false;
            } else {
               V v = LocalCache.this.get(key);
               return v != null && LocalCache.this.valueEquivalence.equivalent(e.getValue(), v);
            }
         }
      }

      public boolean remove(Object o) {
         if (!(o instanceof Entry)) {
            return false;
         } else {
            Entry<?, ?> e = (Entry)o;
            Object key = e.getKey();
            return key != null && LocalCache.this.remove(key, e.getValue());
         }
      }
   }

   final class Values extends AbstractCollection<V> {
      private final ConcurrentMap<?, ?> map;

      Values(ConcurrentMap<?, ?> map) {
         this.map = map;
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

      public Iterator<V> iterator() {
         return LocalCache.this.new ValueIterator();
      }

      public boolean contains(Object o) {
         return this.map.containsValue(o);
      }

      public Object[] toArray() {
         return LocalCache.toArrayList(this).toArray();
      }

      public <E> E[] toArray(E[] a) {
         return LocalCache.toArrayList(this).toArray(a);
      }
   }

   final class KeySet extends LocalCache<K, V>.AbstractCacheSet<K> {
      KeySet(ConcurrentMap<?, ?> map) {
         super(map);
      }

      public Iterator<K> iterator() {
         return LocalCache.this.new KeyIterator();
      }

      public boolean contains(Object o) {
         return this.map.containsKey(o);
      }

      public boolean remove(Object o) {
         return this.map.remove(o) != null;
      }
   }

   abstract class AbstractCacheSet<T> extends AbstractSet<T> {
      @Weak
      final ConcurrentMap<?, ?> map;

      AbstractCacheSet(ConcurrentMap<?, ?> map) {
         this.map = map;
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

      public Object[] toArray() {
         return LocalCache.toArrayList(this).toArray();
      }

      public <E> E[] toArray(E[] a) {
         return LocalCache.toArrayList(this).toArray(a);
      }
   }

   final class EntryIterator extends LocalCache<K, V>.HashIterator<Entry<K, V>> {
      EntryIterator() {
         super();
      }

      public Entry<K, V> next() {
         return this.nextEntry();
      }
   }

   final class WriteThroughEntry implements Entry<K, V> {
      final K key;
      V value;

      WriteThroughEntry(K key, V value) {
         this.key = key;
         this.value = value;
      }

      public K getKey() {
         return this.key;
      }

      public V getValue() {
         return this.value;
      }

      public boolean equals(@Nullable Object object) {
         if (!(object instanceof Entry)) {
            return false;
         } else {
            Entry<?, ?> that = (Entry)object;
            return this.key.equals(that.getKey()) && this.value.equals(that.getValue());
         }
      }

      public int hashCode() {
         return this.key.hashCode() ^ this.value.hashCode();
      }

      public V setValue(V newValue) {
         throw new UnsupportedOperationException();
      }

      public String toString() {
         return this.getKey() + "=" + this.getValue();
      }
   }

   final class ValueIterator extends LocalCache<K, V>.HashIterator<V> {
      ValueIterator() {
         super();
      }

      public V next() {
         return this.nextEntry().getValue();
      }
   }

   final class KeyIterator extends LocalCache<K, V>.HashIterator<K> {
      KeyIterator() {
         super();
      }

      public K next() {
         return this.nextEntry().getKey();
      }
   }

   abstract class HashIterator<T> implements Iterator<T> {
      int nextSegmentIndex;
      int nextTableIndex;
      LocalCache.Segment<K, V> currentSegment;
      AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> currentTable;
      LocalCache.ReferenceEntry<K, V> nextEntry;
      LocalCache<K, V>.WriteThroughEntry nextExternal;
      LocalCache<K, V>.WriteThroughEntry lastReturned;

      HashIterator() {
         this.nextSegmentIndex = LocalCache.this.segments.length - 1;
         this.nextTableIndex = -1;
         this.advance();
      }

      public abstract T next();

      final void advance() {
         this.nextExternal = null;
         if (!this.nextInChain()) {
            if (!this.nextInTable()) {
               while(this.nextSegmentIndex >= 0) {
                  this.currentSegment = LocalCache.this.segments[this.nextSegmentIndex--];
                  if (this.currentSegment.count != 0) {
                     this.currentTable = this.currentSegment.table;
                     this.nextTableIndex = this.currentTable.length() - 1;
                     if (this.nextInTable()) {
                        return;
                     }
                  }
               }

            }
         }
      }

      boolean nextInChain() {
         if (this.nextEntry != null) {
            for(this.nextEntry = this.nextEntry.getNext(); this.nextEntry != null; this.nextEntry = this.nextEntry.getNext()) {
               if (this.advanceTo(this.nextEntry)) {
                  return true;
               }
            }
         }

         return false;
      }

      boolean nextInTable() {
         while(true) {
            if (this.nextTableIndex >= 0) {
               if ((this.nextEntry = (LocalCache.ReferenceEntry)this.currentTable.get(this.nextTableIndex--)) == null || !this.advanceTo(this.nextEntry) && !this.nextInChain()) {
                  continue;
               }

               return true;
            }

            return false;
         }
      }

      boolean advanceTo(LocalCache.ReferenceEntry<K, V> entry) {
         boolean var6;
         try {
            long now = LocalCache.this.ticker.read();
            K key = entry.getKey();
            V value = LocalCache.this.getLiveValue(entry, now);
            if (value != null) {
               this.nextExternal = LocalCache.this.new WriteThroughEntry(key, value);
               var6 = true;
               return var6;
            }

            var6 = false;
         } finally {
            this.currentSegment.postReadCleanup();
         }

         return var6;
      }

      public boolean hasNext() {
         return this.nextExternal != null;
      }

      LocalCache<K, V>.WriteThroughEntry nextEntry() {
         if (this.nextExternal == null) {
            throw new NoSuchElementException();
         } else {
            this.lastReturned = this.nextExternal;
            this.advance();
            return this.lastReturned;
         }
      }

      public void remove() {
         Preconditions.checkState(this.lastReturned != null);
         LocalCache.this.remove(this.lastReturned.getKey());
         this.lastReturned = null;
      }
   }

   static final class AccessQueue<K, V> extends AbstractQueue<LocalCache.ReferenceEntry<K, V>> {
      final LocalCache.ReferenceEntry<K, V> head = new LocalCache.AbstractReferenceEntry<K, V>() {
         LocalCache.ReferenceEntry<K, V> nextAccess = this;
         LocalCache.ReferenceEntry<K, V> previousAccess = this;

         public long getAccessTime() {
            return Long.MAX_VALUE;
         }

         public void setAccessTime(long time) {
         }

         public LocalCache.ReferenceEntry<K, V> getNextInAccessQueue() {
            return this.nextAccess;
         }

         public void setNextInAccessQueue(LocalCache.ReferenceEntry<K, V> next) {
            this.nextAccess = next;
         }

         public LocalCache.ReferenceEntry<K, V> getPreviousInAccessQueue() {
            return this.previousAccess;
         }

         public void setPreviousInAccessQueue(LocalCache.ReferenceEntry<K, V> previous) {
            this.previousAccess = previous;
         }
      };

      public boolean offer(LocalCache.ReferenceEntry<K, V> entry) {
         LocalCache.connectAccessOrder(entry.getPreviousInAccessQueue(), entry.getNextInAccessQueue());
         LocalCache.connectAccessOrder(this.head.getPreviousInAccessQueue(), entry);
         LocalCache.connectAccessOrder(entry, this.head);
         return true;
      }

      public LocalCache.ReferenceEntry<K, V> peek() {
         LocalCache.ReferenceEntry<K, V> next = this.head.getNextInAccessQueue();
         return next == this.head ? null : next;
      }

      public LocalCache.ReferenceEntry<K, V> poll() {
         LocalCache.ReferenceEntry<K, V> next = this.head.getNextInAccessQueue();
         if (next == this.head) {
            return null;
         } else {
            this.remove(next);
            return next;
         }
      }

      public boolean remove(Object o) {
         LocalCache.ReferenceEntry<K, V> e = (LocalCache.ReferenceEntry)o;
         LocalCache.ReferenceEntry<K, V> previous = e.getPreviousInAccessQueue();
         LocalCache.ReferenceEntry<K, V> next = e.getNextInAccessQueue();
         LocalCache.connectAccessOrder(previous, next);
         LocalCache.nullifyAccessOrder(e);
         return next != LocalCache.NullEntry.INSTANCE;
      }

      public boolean contains(Object o) {
         LocalCache.ReferenceEntry<K, V> e = (LocalCache.ReferenceEntry)o;
         return e.getNextInAccessQueue() != LocalCache.NullEntry.INSTANCE;
      }

      public boolean isEmpty() {
         return this.head.getNextInAccessQueue() == this.head;
      }

      public int size() {
         int size = 0;

         for(LocalCache.ReferenceEntry e = this.head.getNextInAccessQueue(); e != this.head; e = e.getNextInAccessQueue()) {
            ++size;
         }

         return size;
      }

      public void clear() {
         LocalCache.ReferenceEntry next;
         for(LocalCache.ReferenceEntry e = this.head.getNextInAccessQueue(); e != this.head; e = next) {
            next = e.getNextInAccessQueue();
            LocalCache.nullifyAccessOrder(e);
         }

         this.head.setNextInAccessQueue(this.head);
         this.head.setPreviousInAccessQueue(this.head);
      }

      public Iterator<LocalCache.ReferenceEntry<K, V>> iterator() {
         return new AbstractSequentialIterator<LocalCache.ReferenceEntry<K, V>>(this.peek()) {
            protected LocalCache.ReferenceEntry<K, V> computeNext(LocalCache.ReferenceEntry<K, V> previous) {
               LocalCache.ReferenceEntry<K, V> next = previous.getNextInAccessQueue();
               return next == AccessQueue.this.head ? null : next;
            }
         };
      }
   }

   static final class WriteQueue<K, V> extends AbstractQueue<LocalCache.ReferenceEntry<K, V>> {
      final LocalCache.ReferenceEntry<K, V> head = new LocalCache.AbstractReferenceEntry<K, V>() {
         LocalCache.ReferenceEntry<K, V> nextWrite = this;
         LocalCache.ReferenceEntry<K, V> previousWrite = this;

         public long getWriteTime() {
            return Long.MAX_VALUE;
         }

         public void setWriteTime(long time) {
         }

         public LocalCache.ReferenceEntry<K, V> getNextInWriteQueue() {
            return this.nextWrite;
         }

         public void setNextInWriteQueue(LocalCache.ReferenceEntry<K, V> next) {
            this.nextWrite = next;
         }

         public LocalCache.ReferenceEntry<K, V> getPreviousInWriteQueue() {
            return this.previousWrite;
         }

         public void setPreviousInWriteQueue(LocalCache.ReferenceEntry<K, V> previous) {
            this.previousWrite = previous;
         }
      };

      public boolean offer(LocalCache.ReferenceEntry<K, V> entry) {
         LocalCache.connectWriteOrder(entry.getPreviousInWriteQueue(), entry.getNextInWriteQueue());
         LocalCache.connectWriteOrder(this.head.getPreviousInWriteQueue(), entry);
         LocalCache.connectWriteOrder(entry, this.head);
         return true;
      }

      public LocalCache.ReferenceEntry<K, V> peek() {
         LocalCache.ReferenceEntry<K, V> next = this.head.getNextInWriteQueue();
         return next == this.head ? null : next;
      }

      public LocalCache.ReferenceEntry<K, V> poll() {
         LocalCache.ReferenceEntry<K, V> next = this.head.getNextInWriteQueue();
         if (next == this.head) {
            return null;
         } else {
            this.remove(next);
            return next;
         }
      }

      public boolean remove(Object o) {
         LocalCache.ReferenceEntry<K, V> e = (LocalCache.ReferenceEntry)o;
         LocalCache.ReferenceEntry<K, V> previous = e.getPreviousInWriteQueue();
         LocalCache.ReferenceEntry<K, V> next = e.getNextInWriteQueue();
         LocalCache.connectWriteOrder(previous, next);
         LocalCache.nullifyWriteOrder(e);
         return next != LocalCache.NullEntry.INSTANCE;
      }

      public boolean contains(Object o) {
         LocalCache.ReferenceEntry<K, V> e = (LocalCache.ReferenceEntry)o;
         return e.getNextInWriteQueue() != LocalCache.NullEntry.INSTANCE;
      }

      public boolean isEmpty() {
         return this.head.getNextInWriteQueue() == this.head;
      }

      public int size() {
         int size = 0;

         for(LocalCache.ReferenceEntry e = this.head.getNextInWriteQueue(); e != this.head; e = e.getNextInWriteQueue()) {
            ++size;
         }

         return size;
      }

      public void clear() {
         LocalCache.ReferenceEntry next;
         for(LocalCache.ReferenceEntry e = this.head.getNextInWriteQueue(); e != this.head; e = next) {
            next = e.getNextInWriteQueue();
            LocalCache.nullifyWriteOrder(e);
         }

         this.head.setNextInWriteQueue(this.head);
         this.head.setPreviousInWriteQueue(this.head);
      }

      public Iterator<LocalCache.ReferenceEntry<K, V>> iterator() {
         return new AbstractSequentialIterator<LocalCache.ReferenceEntry<K, V>>(this.peek()) {
            protected LocalCache.ReferenceEntry<K, V> computeNext(LocalCache.ReferenceEntry<K, V> previous) {
               LocalCache.ReferenceEntry<K, V> next = previous.getNextInWriteQueue();
               return next == WriteQueue.this.head ? null : next;
            }
         };
      }
   }

   static class LoadingValueReference<K, V> implements LocalCache.ValueReference<K, V> {
      volatile LocalCache.ValueReference<K, V> oldValue;
      final SettableFuture<V> futureValue;
      final Stopwatch stopwatch;

      public LoadingValueReference() {
         this(LocalCache.unset());
      }

      public LoadingValueReference(LocalCache.ValueReference<K, V> oldValue) {
         this.futureValue = SettableFuture.create();
         this.stopwatch = Stopwatch.createUnstarted();
         this.oldValue = oldValue;
      }

      public boolean isLoading() {
         return true;
      }

      public boolean isActive() {
         return this.oldValue.isActive();
      }

      public int getWeight() {
         return this.oldValue.getWeight();
      }

      public boolean set(@Nullable V newValue) {
         return this.futureValue.set(newValue);
      }

      public boolean setException(Throwable t) {
         return this.futureValue.setException(t);
      }

      private ListenableFuture<V> fullyFailedFuture(Throwable t) {
         return Futures.immediateFailedFuture(t);
      }

      public void notifyNewValue(@Nullable V newValue) {
         if (newValue != null) {
            this.set(newValue);
         } else {
            this.oldValue = LocalCache.unset();
         }

      }

      public ListenableFuture<V> loadFuture(K key, CacheLoader<? super K, V> loader) {
         Object result;
         try {
            this.stopwatch.start();
            V previousValue = this.oldValue.get();
            if (previousValue == null) {
               result = loader.load(key);
               return (ListenableFuture)(this.set(result) ? this.futureValue : Futures.immediateFuture(result));
            } else {
               ListenableFuture<V> newValue = loader.reload(key, previousValue);
               return newValue == null ? Futures.immediateFuture((Object)null) : Futures.transform(newValue, new Function<V, V>() {
                  public V apply(V newValue) {
                     LoadingValueReference.this.set(newValue);
                     return newValue;
                  }
               });
            }
         } catch (Throwable var5) {
            result = this.setException(var5) ? this.futureValue : this.fullyFailedFuture(var5);
            if (var5 instanceof InterruptedException) {
               Thread.currentThread().interrupt();
            }

            return (ListenableFuture)result;
         }
      }

      public long elapsedNanos() {
         return this.stopwatch.elapsed(TimeUnit.NANOSECONDS);
      }

      public V waitForValue() throws ExecutionException {
         return Uninterruptibles.getUninterruptibly(this.futureValue);
      }

      public V get() {
         return this.oldValue.get();
      }

      public LocalCache.ValueReference<K, V> getOldValue() {
         return this.oldValue;
      }

      public LocalCache.ReferenceEntry<K, V> getEntry() {
         return null;
      }

      public LocalCache.ValueReference<K, V> copyFor(ReferenceQueue<V> queue, @Nullable V value, LocalCache.ReferenceEntry<K, V> entry) {
         return this;
      }
   }

   static class Segment<K, V> extends ReentrantLock {
      @Weak
      final LocalCache<K, V> map;
      volatile int count;
      @GuardedBy("this")
      long totalWeight;
      int modCount;
      int threshold;
      volatile AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table;
      final long maxSegmentWeight;
      final ReferenceQueue<K> keyReferenceQueue;
      final ReferenceQueue<V> valueReferenceQueue;
      final Queue<LocalCache.ReferenceEntry<K, V>> recencyQueue;
      final AtomicInteger readCount = new AtomicInteger();
      @GuardedBy("this")
      final Queue<LocalCache.ReferenceEntry<K, V>> writeQueue;
      @GuardedBy("this")
      final Queue<LocalCache.ReferenceEntry<K, V>> accessQueue;
      final AbstractCache.StatsCounter statsCounter;

      Segment(LocalCache<K, V> map, int initialCapacity, long maxSegmentWeight, AbstractCache.StatsCounter statsCounter) {
         this.map = map;
         this.maxSegmentWeight = maxSegmentWeight;
         this.statsCounter = (AbstractCache.StatsCounter)Preconditions.checkNotNull(statsCounter);
         this.initTable(this.newEntryArray(initialCapacity));
         this.keyReferenceQueue = map.usesKeyReferences() ? new ReferenceQueue() : null;
         this.valueReferenceQueue = map.usesValueReferences() ? new ReferenceQueue() : null;
         this.recencyQueue = (Queue)(map.usesAccessQueue() ? new ConcurrentLinkedQueue() : LocalCache.discardingQueue());
         this.writeQueue = (Queue)(map.usesWriteQueue() ? new LocalCache.WriteQueue() : LocalCache.discardingQueue());
         this.accessQueue = (Queue)(map.usesAccessQueue() ? new LocalCache.AccessQueue() : LocalCache.discardingQueue());
      }

      AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> newEntryArray(int size) {
         return new AtomicReferenceArray(size);
      }

      void initTable(AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> newTable) {
         this.threshold = newTable.length() * 3 / 4;
         if (!this.map.customWeigher() && (long)this.threshold == this.maxSegmentWeight) {
            ++this.threshold;
         }

         this.table = newTable;
      }

      @GuardedBy("this")
      LocalCache.ReferenceEntry<K, V> newEntry(K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
         return this.map.entryFactory.newEntry(this, Preconditions.checkNotNull(key), hash, next);
      }

      @GuardedBy("this")
      LocalCache.ReferenceEntry<K, V> copyEntry(LocalCache.ReferenceEntry<K, V> original, LocalCache.ReferenceEntry<K, V> newNext) {
         if (original.getKey() == null) {
            return null;
         } else {
            LocalCache.ValueReference<K, V> valueReference = original.getValueReference();
            V value = valueReference.get();
            if (value == null && valueReference.isActive()) {
               return null;
            } else {
               LocalCache.ReferenceEntry<K, V> newEntry = this.map.entryFactory.copyEntry(this, original, newNext);
               newEntry.setValueReference(valueReference.copyFor(this.valueReferenceQueue, value, newEntry));
               return newEntry;
            }
         }
      }

      @GuardedBy("this")
      void setValue(LocalCache.ReferenceEntry<K, V> entry, K key, V value, long now) {
         LocalCache.ValueReference<K, V> previous = entry.getValueReference();
         int weight = this.map.weigher.weigh(key, value);
         Preconditions.checkState(weight >= 0, "Weights must be non-negative");
         LocalCache.ValueReference<K, V> valueReference = this.map.valueStrength.referenceValue(this, entry, value, weight);
         entry.setValueReference(valueReference);
         this.recordWrite(entry, weight, now);
         previous.notifyNewValue(value);
      }

      V get(K key, int hash, CacheLoader<? super K, V> loader) throws ExecutionException {
         Preconditions.checkNotNull(key);
         Preconditions.checkNotNull(loader);

         try {
            if (this.count != 0) {
               LocalCache.ReferenceEntry<K, V> e = this.getEntry(key, hash);
               if (e != null) {
                  long now = this.map.ticker.read();
                  V value = this.getLiveValue(e, now);
                  if (value != null) {
                     this.recordRead(e, now);
                     this.statsCounter.recordHits(1);
                     Object var17 = this.scheduleRefresh(e, key, hash, value, now, loader);
                     return var17;
                  }

                  LocalCache.ValueReference<K, V> valueReference = e.getValueReference();
                  if (valueReference.isLoading()) {
                     Object var9 = this.waitForLoadingValue(e, key, valueReference);
                     return var9;
                  }
               }
            }

            Object var15 = this.lockedGetOrLoad(key, hash, loader);
            return var15;
         } catch (ExecutionException var13) {
            Throwable cause = var13.getCause();
            if (cause instanceof Error) {
               throw new ExecutionError((Error)cause);
            } else if (cause instanceof RuntimeException) {
               throw new UncheckedExecutionException(cause);
            } else {
               throw var13;
            }
         } finally {
            this.postReadCleanup();
         }
      }

      V lockedGetOrLoad(K key, int hash, CacheLoader<? super K, V> loader) throws ExecutionException {
         LocalCache.ValueReference<K, V> valueReference = null;
         LocalCache.LoadingValueReference<K, V> loadingValueReference = null;
         boolean createNewEntry = true;
         this.lock();

         LocalCache.ReferenceEntry e;
         try {
            long now = this.map.ticker.read();
            this.preWriteCleanup(now);
            int newCount = this.count - 1;
            AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            LocalCache.ReferenceEntry<K, V> first = (LocalCache.ReferenceEntry)table.get(index);

            for(e = first; e != null; e = e.getNext()) {
               K entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  valueReference = e.getValueReference();
                  if (valueReference.isLoading()) {
                     createNewEntry = false;
                  } else {
                     V value = valueReference.get();
                     if (value == null) {
                        this.enqueueNotification(entryKey, hash, valueReference, RemovalCause.COLLECTED);
                     } else {
                        if (!this.map.isExpired(e, now)) {
                           this.recordLockedRead(e, now);
                           this.statsCounter.recordHits(1);
                           Object var16 = value;
                           return var16;
                        }

                        this.enqueueNotification(entryKey, hash, valueReference, RemovalCause.EXPIRED);
                     }

                     this.writeQueue.remove(e);
                     this.accessQueue.remove(e);
                     this.count = newCount;
                  }
                  break;
               }
            }

            if (createNewEntry) {
               loadingValueReference = new LocalCache.LoadingValueReference();
               if (e == null) {
                  e = this.newEntry(key, hash, first);
                  e.setValueReference(loadingValueReference);
                  table.set(index, e);
               } else {
                  e.setValueReference(loadingValueReference);
               }
            }
         } finally {
            this.unlock();
            this.postWriteCleanup();
         }

         if (createNewEntry) {
            Object var9;
            try {
               synchronized(e) {
                  var9 = this.loadSync(key, hash, loadingValueReference, loader);
               }
            } finally {
               this.statsCounter.recordMisses(1);
            }

            return var9;
         } else {
            return this.waitForLoadingValue(e, key, valueReference);
         }
      }

      V waitForLoadingValue(LocalCache.ReferenceEntry<K, V> e, K key, LocalCache.ValueReference<K, V> valueReference) throws ExecutionException {
         if (!valueReference.isLoading()) {
            throw new AssertionError();
         } else {
            Preconditions.checkState(!Thread.holdsLock(e), "Recursive load of: %s", key);

            Object var7;
            try {
               V value = valueReference.waitForValue();
               if (value == null) {
                  throw new CacheLoader.InvalidCacheLoadException("CacheLoader returned null for key " + key + ".");
               }

               long now = this.map.ticker.read();
               this.recordRead(e, now);
               var7 = value;
            } finally {
               this.statsCounter.recordMisses(1);
            }

            return var7;
         }
      }

      V loadSync(K key, int hash, LocalCache.LoadingValueReference<K, V> loadingValueReference, CacheLoader<? super K, V> loader) throws ExecutionException {
         ListenableFuture<V> loadingFuture = loadingValueReference.loadFuture(key, loader);
         return this.getAndRecordStats(key, hash, loadingValueReference, loadingFuture);
      }

      ListenableFuture<V> loadAsync(final K key, final int hash, final LocalCache.LoadingValueReference<K, V> loadingValueReference, CacheLoader<? super K, V> loader) {
         final ListenableFuture<V> loadingFuture = loadingValueReference.loadFuture(key, loader);
         loadingFuture.addListener(new Runnable() {
            public void run() {
               try {
                  Object var1 = Segment.this.getAndRecordStats(key, hash, loadingValueReference, loadingFuture);
               } catch (Throwable var2) {
                  LocalCache.logger.log(Level.WARNING, "Exception thrown during refresh", var2);
                  loadingValueReference.setException(var2);
               }

            }
         }, MoreExecutors.directExecutor());
         return loadingFuture;
      }

      V getAndRecordStats(K key, int hash, LocalCache.LoadingValueReference<K, V> loadingValueReference, ListenableFuture<V> newValue) throws ExecutionException {
         Object value = null;

         Object var6;
         try {
            value = Uninterruptibles.getUninterruptibly(newValue);
            if (value == null) {
               throw new CacheLoader.InvalidCacheLoadException("CacheLoader returned null for key " + key + ".");
            }

            this.statsCounter.recordLoadSuccess(loadingValueReference.elapsedNanos());
            this.storeLoadedValue(key, hash, loadingValueReference, value);
            var6 = value;
         } finally {
            if (value == null) {
               this.statsCounter.recordLoadException(loadingValueReference.elapsedNanos());
               this.removeLoadingValue(key, hash, loadingValueReference);
            }

         }

         return var6;
      }

      V scheduleRefresh(LocalCache.ReferenceEntry<K, V> entry, K key, int hash, V oldValue, long now, CacheLoader<? super K, V> loader) {
         if (this.map.refreshes() && now - entry.getWriteTime() > this.map.refreshNanos && !entry.getValueReference().isLoading()) {
            V newValue = this.refresh(key, hash, loader, true);
            if (newValue != null) {
               return newValue;
            }
         }

         return oldValue;
      }

      @Nullable
      V refresh(K key, int hash, CacheLoader<? super K, V> loader, boolean checkTime) {
         LocalCache.LoadingValueReference<K, V> loadingValueReference = this.insertLoadingValueReference(key, hash, checkTime);
         if (loadingValueReference == null) {
            return null;
         } else {
            ListenableFuture<V> result = this.loadAsync(key, hash, loadingValueReference, loader);
            if (result.isDone()) {
               try {
                  return Uninterruptibles.getUninterruptibly(result);
               } catch (Throwable var8) {
               }
            }

            return null;
         }
      }

      @Nullable
      LocalCache.LoadingValueReference<K, V> insertLoadingValueReference(K key, int hash, boolean checkTime) {
         LocalCache.ReferenceEntry<K, V> e = null;
         this.lock();

         try {
            long now = this.map.ticker.read();
            this.preWriteCleanup(now);
            AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            LocalCache.ReferenceEntry<K, V> first = (LocalCache.ReferenceEntry)table.get(index);

            for(e = first; e != null; e = e.getNext()) {
               K entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  LocalCache.ValueReference<K, V> valueReference = e.getValueReference();
                  LocalCache.LoadingValueReference loadingValueReference;
                  if (valueReference.isLoading() || checkTime && now - e.getWriteTime() < this.map.refreshNanos) {
                     loadingValueReference = null;
                     return loadingValueReference;
                  }

                  ++this.modCount;
                  loadingValueReference = new LocalCache.LoadingValueReference(valueReference);
                  e.setValueReference(loadingValueReference);
                  LocalCache.LoadingValueReference var13 = loadingValueReference;
                  return var13;
               }
            }

            ++this.modCount;
            LocalCache.LoadingValueReference<K, V> loadingValueReference = new LocalCache.LoadingValueReference();
            e = this.newEntry(key, hash, first);
            e.setValueReference(loadingValueReference);
            table.set(index, e);
            LocalCache.LoadingValueReference var18 = loadingValueReference;
            return var18;
         } finally {
            this.unlock();
            this.postWriteCleanup();
         }
      }

      void tryDrainReferenceQueues() {
         if (this.tryLock()) {
            try {
               this.drainReferenceQueues();
            } finally {
               this.unlock();
            }
         }

      }

      @GuardedBy("this")
      void drainReferenceQueues() {
         if (this.map.usesKeyReferences()) {
            this.drainKeyReferenceQueue();
         }

         if (this.map.usesValueReferences()) {
            this.drainValueReferenceQueue();
         }

      }

      @GuardedBy("this")
      void drainKeyReferenceQueue() {
         int i = 0;

         Reference ref;
         while((ref = this.keyReferenceQueue.poll()) != null) {
            LocalCache.ReferenceEntry<K, V> entry = (LocalCache.ReferenceEntry)ref;
            this.map.reclaimKey(entry);
            ++i;
            if (i == 16) {
               break;
            }
         }

      }

      @GuardedBy("this")
      void drainValueReferenceQueue() {
         int i = 0;

         Reference ref;
         while((ref = this.valueReferenceQueue.poll()) != null) {
            LocalCache.ValueReference<K, V> valueReference = (LocalCache.ValueReference)ref;
            this.map.reclaimValue(valueReference);
            ++i;
            if (i == 16) {
               break;
            }
         }

      }

      void clearReferenceQueues() {
         if (this.map.usesKeyReferences()) {
            this.clearKeyReferenceQueue();
         }

         if (this.map.usesValueReferences()) {
            this.clearValueReferenceQueue();
         }

      }

      void clearKeyReferenceQueue() {
         while(this.keyReferenceQueue.poll() != null) {
         }

      }

      void clearValueReferenceQueue() {
         while(this.valueReferenceQueue.poll() != null) {
         }

      }

      void recordRead(LocalCache.ReferenceEntry<K, V> entry, long now) {
         if (this.map.recordsAccess()) {
            entry.setAccessTime(now);
         }

         this.recencyQueue.add(entry);
      }

      @GuardedBy("this")
      void recordLockedRead(LocalCache.ReferenceEntry<K, V> entry, long now) {
         if (this.map.recordsAccess()) {
            entry.setAccessTime(now);
         }

         this.accessQueue.add(entry);
      }

      @GuardedBy("this")
      void recordWrite(LocalCache.ReferenceEntry<K, V> entry, int weight, long now) {
         this.drainRecencyQueue();
         this.totalWeight += (long)weight;
         if (this.map.recordsAccess()) {
            entry.setAccessTime(now);
         }

         if (this.map.recordsWrite()) {
            entry.setWriteTime(now);
         }

         this.accessQueue.add(entry);
         this.writeQueue.add(entry);
      }

      @GuardedBy("this")
      void drainRecencyQueue() {
         LocalCache.ReferenceEntry e;
         while((e = (LocalCache.ReferenceEntry)this.recencyQueue.poll()) != null) {
            if (this.accessQueue.contains(e)) {
               this.accessQueue.add(e);
            }
         }

      }

      void tryExpireEntries(long now) {
         if (this.tryLock()) {
            try {
               this.expireEntries(now);
            } finally {
               this.unlock();
            }
         }

      }

      @GuardedBy("this")
      void expireEntries(long now) {
         this.drainRecencyQueue();

         LocalCache.ReferenceEntry e;
         while((e = (LocalCache.ReferenceEntry)this.writeQueue.peek()) != null && this.map.isExpired(e, now)) {
            if (!this.removeEntry(e, e.getHash(), RemovalCause.EXPIRED)) {
               throw new AssertionError();
            }
         }

         while((e = (LocalCache.ReferenceEntry)this.accessQueue.peek()) != null && this.map.isExpired(e, now)) {
            if (!this.removeEntry(e, e.getHash(), RemovalCause.EXPIRED)) {
               throw new AssertionError();
            }
         }

      }

      @GuardedBy("this")
      void enqueueNotification(LocalCache.ReferenceEntry<K, V> entry, RemovalCause cause) {
         this.enqueueNotification(entry.getKey(), entry.getHash(), entry.getValueReference(), cause);
      }

      @GuardedBy("this")
      void enqueueNotification(@Nullable K key, int hash, LocalCache.ValueReference<K, V> valueReference, RemovalCause cause) {
         this.totalWeight -= (long)valueReference.getWeight();
         if (cause.wasEvicted()) {
            this.statsCounter.recordEviction();
         }

         if (this.map.removalNotificationQueue != LocalCache.DISCARDING_QUEUE) {
            V value = valueReference.get();
            RemovalNotification<K, V> notification = RemovalNotification.create(key, value, cause);
            this.map.removalNotificationQueue.offer(notification);
         }

      }

      @GuardedBy("this")
      void evictEntries(LocalCache.ReferenceEntry<K, V> newest) {
         if (this.map.evictsBySize()) {
            this.drainRecencyQueue();
            if ((long)newest.getValueReference().getWeight() > this.maxSegmentWeight && !this.removeEntry(newest, newest.getHash(), RemovalCause.SIZE)) {
               throw new AssertionError();
            } else {
               LocalCache.ReferenceEntry e;
               do {
                  if (this.totalWeight <= this.maxSegmentWeight) {
                     return;
                  }

                  e = this.getNextEvictable();
               } while(this.removeEntry(e, e.getHash(), RemovalCause.SIZE));

               throw new AssertionError();
            }
         }
      }

      @GuardedBy("this")
      LocalCache.ReferenceEntry<K, V> getNextEvictable() {
         Iterator i$ = this.accessQueue.iterator();

         LocalCache.ReferenceEntry e;
         int weight;
         do {
            if (!i$.hasNext()) {
               throw new AssertionError();
            }

            e = (LocalCache.ReferenceEntry)i$.next();
            weight = e.getValueReference().getWeight();
         } while(weight <= 0);

         return e;
      }

      LocalCache.ReferenceEntry<K, V> getFirst(int hash) {
         AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table = this.table;
         return (LocalCache.ReferenceEntry)table.get(hash & table.length() - 1);
      }

      @Nullable
      LocalCache.ReferenceEntry<K, V> getEntry(Object key, int hash) {
         for(LocalCache.ReferenceEntry e = this.getFirst(hash); e != null; e = e.getNext()) {
            if (e.getHash() == hash) {
               K entryKey = e.getKey();
               if (entryKey == null) {
                  this.tryDrainReferenceQueues();
               } else if (this.map.keyEquivalence.equivalent(key, entryKey)) {
                  return e;
               }
            }
         }

         return null;
      }

      @Nullable
      LocalCache.ReferenceEntry<K, V> getLiveEntry(Object key, int hash, long now) {
         LocalCache.ReferenceEntry<K, V> e = this.getEntry(key, hash);
         if (e == null) {
            return null;
         } else if (this.map.isExpired(e, now)) {
            this.tryExpireEntries(now);
            return null;
         } else {
            return e;
         }
      }

      V getLiveValue(LocalCache.ReferenceEntry<K, V> entry, long now) {
         if (entry.getKey() == null) {
            this.tryDrainReferenceQueues();
            return null;
         } else {
            V value = entry.getValueReference().get();
            if (value == null) {
               this.tryDrainReferenceQueues();
               return null;
            } else if (this.map.isExpired(entry, now)) {
               this.tryExpireEntries(now);
               return null;
            } else {
               return value;
            }
         }
      }

      @Nullable
      V get(Object key, int hash) {
         Object var11;
         try {
            if (this.count != 0) {
               long now = this.map.ticker.read();
               LocalCache.ReferenceEntry<K, V> e = this.getLiveEntry(key, hash, now);
               Object value;
               if (e == null) {
                  value = null;
                  return value;
               }

               value = e.getValueReference().get();
               if (value != null) {
                  this.recordRead(e, now);
                  Object var7 = this.scheduleRefresh(e, e.getKey(), hash, value, now, this.map.defaultLoader);
                  return var7;
               }

               this.tryDrainReferenceQueues();
            }

            var11 = null;
         } finally {
            this.postReadCleanup();
         }

         return var11;
      }

      boolean containsKey(Object key, int hash) {
         boolean var6;
         try {
            if (this.count == 0) {
               boolean var10 = false;
               return var10;
            }

            long now = this.map.ticker.read();
            LocalCache.ReferenceEntry<K, V> e = this.getLiveEntry(key, hash, now);
            if (e != null) {
               var6 = e.getValueReference().get() != null;
               return var6;
            }

            var6 = false;
         } finally {
            this.postReadCleanup();
         }

         return var6;
      }

      @VisibleForTesting
      boolean containsValue(Object value) {
         try {
            if (this.count != 0) {
               long now = this.map.ticker.read();
               AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table = this.table;
               int length = table.length();

               for(int i = 0; i < length; ++i) {
                  for(LocalCache.ReferenceEntry e = (LocalCache.ReferenceEntry)table.get(i); e != null; e = e.getNext()) {
                     V entryValue = this.getLiveValue(e, now);
                     if (entryValue != null && this.map.valueEquivalence.equivalent(value, entryValue)) {
                        boolean var9 = true;
                        return var9;
                     }
                  }
               }
            }

            boolean var13 = false;
            return var13;
         } finally {
            this.postReadCleanup();
         }
      }

      @Nullable
      V put(K key, int hash, V value, boolean onlyIfAbsent) {
         this.lock();

         Object entryKey;
         try {
            long now = this.map.ticker.read();
            this.preWriteCleanup(now);
            int newCount = this.count + 1;
            if (newCount > this.threshold) {
               this.expand();
               newCount = this.count + 1;
            }

            AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            LocalCache.ReferenceEntry<K, V> first = (LocalCache.ReferenceEntry)table.get(index);

            LocalCache.ReferenceEntry e;
            for(e = first; e != null; e = e.getNext()) {
               entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  LocalCache.ValueReference<K, V> valueReference = e.getValueReference();
                  V entryValue = valueReference.get();
                  Object var15;
                  if (entryValue == null) {
                     ++this.modCount;
                     if (valueReference.isActive()) {
                        this.enqueueNotification(key, hash, valueReference, RemovalCause.COLLECTED);
                        this.setValue(e, key, value, now);
                        newCount = this.count;
                     } else {
                        this.setValue(e, key, value, now);
                        newCount = this.count + 1;
                     }

                     this.count = newCount;
                     this.evictEntries(e);
                     var15 = null;
                     return var15;
                  }

                  if (onlyIfAbsent) {
                     this.recordLockedRead(e, now);
                     var15 = entryValue;
                     return var15;
                  }

                  ++this.modCount;
                  this.enqueueNotification(key, hash, valueReference, RemovalCause.REPLACED);
                  this.setValue(e, key, value, now);
                  this.evictEntries(e);
                  var15 = entryValue;
                  return var15;
               }
            }

            ++this.modCount;
            e = this.newEntry(key, hash, first);
            this.setValue(e, key, value, now);
            table.set(index, e);
            newCount = this.count + 1;
            this.count = newCount;
            this.evictEntries(e);
            entryKey = null;
         } finally {
            this.unlock();
            this.postWriteCleanup();
         }

         return entryKey;
      }

      @GuardedBy("this")
      void expand() {
         AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> oldTable = this.table;
         int oldCapacity = oldTable.length();
         if (oldCapacity < 1073741824) {
            int newCount = this.count;
            AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> newTable = this.newEntryArray(oldCapacity << 1);
            this.threshold = newTable.length() * 3 / 4;
            int newMask = newTable.length() - 1;

            for(int oldIndex = 0; oldIndex < oldCapacity; ++oldIndex) {
               LocalCache.ReferenceEntry<K, V> head = (LocalCache.ReferenceEntry)oldTable.get(oldIndex);
               if (head != null) {
                  LocalCache.ReferenceEntry<K, V> next = head.getNext();
                  int headIndex = head.getHash() & newMask;
                  if (next == null) {
                     newTable.set(headIndex, head);
                  } else {
                     LocalCache.ReferenceEntry<K, V> tail = head;
                     int tailIndex = headIndex;

                     LocalCache.ReferenceEntry e;
                     int newIndex;
                     for(e = next; e != null; e = e.getNext()) {
                        newIndex = e.getHash() & newMask;
                        if (newIndex != tailIndex) {
                           tailIndex = newIndex;
                           tail = e;
                        }
                     }

                     newTable.set(tailIndex, tail);

                     for(e = head; e != tail; e = e.getNext()) {
                        newIndex = e.getHash() & newMask;
                        LocalCache.ReferenceEntry<K, V> newNext = (LocalCache.ReferenceEntry)newTable.get(newIndex);
                        LocalCache.ReferenceEntry<K, V> newFirst = this.copyEntry(e, newNext);
                        if (newFirst != null) {
                           newTable.set(newIndex, newFirst);
                        } else {
                           this.removeCollectedEntry(e);
                           --newCount;
                        }
                     }
                  }
               }
            }

            this.table = newTable;
            this.count = newCount;
         }
      }

      boolean replace(K key, int hash, V oldValue, V newValue) {
         this.lock();

         try {
            long now = this.map.ticker.read();
            this.preWriteCleanup(now);
            AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            LocalCache.ReferenceEntry<K, V> first = (LocalCache.ReferenceEntry)table.get(index);

            for(LocalCache.ReferenceEntry e = first; e != null; e = e.getNext()) {
               K entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  LocalCache.ValueReference<K, V> valueReference = e.getValueReference();
                  V entryValue = valueReference.get();
                  boolean var14;
                  if (entryValue == null) {
                     if (valueReference.isActive()) {
                        int newCount = this.count - 1;
                        ++this.modCount;
                        LocalCache.ReferenceEntry<K, V> newFirst = this.removeValueFromChain(first, e, entryKey, hash, valueReference, RemovalCause.COLLECTED);
                        newCount = this.count - 1;
                        table.set(index, newFirst);
                        this.count = newCount;
                     }

                     var14 = false;
                     return var14;
                  }

                  if (!this.map.valueEquivalence.equivalent(oldValue, entryValue)) {
                     this.recordLockedRead(e, now);
                     var14 = false;
                     return var14;
                  }

                  ++this.modCount;
                  this.enqueueNotification(key, hash, valueReference, RemovalCause.REPLACED);
                  this.setValue(e, key, newValue, now);
                  this.evictEntries(e);
                  var14 = true;
                  return var14;
               }
            }

            boolean var19 = false;
            return var19;
         } finally {
            this.unlock();
            this.postWriteCleanup();
         }
      }

      @Nullable
      V replace(K key, int hash, V newValue) {
         this.lock();

         try {
            long now = this.map.ticker.read();
            this.preWriteCleanup(now);
            AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            LocalCache.ReferenceEntry<K, V> first = (LocalCache.ReferenceEntry)table.get(index);

            LocalCache.ReferenceEntry e;
            for(e = first; e != null; e = e.getNext()) {
               K entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  LocalCache.ValueReference<K, V> valueReference = e.getValueReference();
                  V entryValue = valueReference.get();
                  Object var18;
                  if (entryValue != null) {
                     ++this.modCount;
                     this.enqueueNotification(key, hash, valueReference, RemovalCause.REPLACED);
                     this.setValue(e, key, newValue, now);
                     this.evictEntries(e);
                     var18 = entryValue;
                     return var18;
                  }

                  if (valueReference.isActive()) {
                     int newCount = this.count - 1;
                     ++this.modCount;
                     LocalCache.ReferenceEntry<K, V> newFirst = this.removeValueFromChain(first, e, entryKey, hash, valueReference, RemovalCause.COLLECTED);
                     newCount = this.count - 1;
                     table.set(index, newFirst);
                     this.count = newCount;
                  }

                  var18 = null;
                  return var18;
               }
            }

            e = null;
            return e;
         } finally {
            this.unlock();
            this.postWriteCleanup();
         }
      }

      @Nullable
      V remove(Object key, int hash) {
         this.lock();

         LocalCache.ReferenceEntry e;
         try {
            long now = this.map.ticker.read();
            this.preWriteCleanup(now);
            int newCount = this.count - 1;
            AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            LocalCache.ReferenceEntry<K, V> first = (LocalCache.ReferenceEntry)table.get(index);

            for(e = first; e != null; e = e.getNext()) {
               K entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  LocalCache.ValueReference<K, V> valueReference = e.getValueReference();
                  V entryValue = valueReference.get();
                  RemovalCause cause;
                  LocalCache.ReferenceEntry newFirst;
                  if (entryValue != null) {
                     cause = RemovalCause.EXPLICIT;
                  } else {
                     if (!valueReference.isActive()) {
                        newFirst = null;
                        return newFirst;
                     }

                     cause = RemovalCause.COLLECTED;
                  }

                  ++this.modCount;
                  newFirst = this.removeValueFromChain(first, e, entryKey, hash, valueReference, cause);
                  newCount = this.count - 1;
                  table.set(index, newFirst);
                  this.count = newCount;
                  Object var15 = entryValue;
                  return var15;
               }
            }

            e = null;
         } finally {
            this.unlock();
            this.postWriteCleanup();
         }

         return e;
      }

      boolean storeLoadedValue(K key, int hash, LocalCache.LoadingValueReference<K, V> oldValueReference, V newValue) {
         this.lock();

         try {
            long now = this.map.ticker.read();
            this.preWriteCleanup(now);
            int newCount = this.count + 1;
            if (newCount > this.threshold) {
               this.expand();
               newCount = this.count + 1;
            }

            AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            LocalCache.ReferenceEntry<K, V> first = (LocalCache.ReferenceEntry)table.get(index);

            LocalCache.ReferenceEntry e;
            for(e = first; e != null; e = e.getNext()) {
               K entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  LocalCache.ValueReference<K, V> valueReference = e.getValueReference();
                  V entryValue = valueReference.get();
                  boolean var21;
                  if (oldValueReference != valueReference && (entryValue != null || valueReference == LocalCache.UNSET)) {
                     LocalCache.ValueReference<K, V> valueReference = new LocalCache.WeightedStrongValueReference(newValue, 0);
                     this.enqueueNotification(key, hash, valueReference, RemovalCause.REPLACED);
                     var21 = false;
                     return var21;
                  }

                  ++this.modCount;
                  if (oldValueReference.isActive()) {
                     RemovalCause cause = entryValue == null ? RemovalCause.COLLECTED : RemovalCause.REPLACED;
                     this.enqueueNotification(key, hash, oldValueReference, cause);
                     --newCount;
                  }

                  this.setValue(e, key, newValue, now);
                  this.count = newCount;
                  this.evictEntries(e);
                  var21 = true;
                  return var21;
               }
            }

            ++this.modCount;
            e = this.newEntry(key, hash, first);
            this.setValue(e, key, newValue, now);
            table.set(index, e);
            this.count = newCount;
            this.evictEntries(e);
            boolean var19 = true;
            return var19;
         } finally {
            this.unlock();
            this.postWriteCleanup();
         }
      }

      boolean remove(Object key, int hash, Object value) {
         this.lock();

         boolean var20;
         try {
            long now = this.map.ticker.read();
            this.preWriteCleanup(now);
            int newCount = this.count - 1;
            AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            LocalCache.ReferenceEntry<K, V> first = (LocalCache.ReferenceEntry)table.get(index);

            for(LocalCache.ReferenceEntry e = first; e != null; e = e.getNext()) {
               K entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  LocalCache.ValueReference<K, V> valueReference = e.getValueReference();
                  V entryValue = valueReference.get();
                  RemovalCause cause;
                  if (this.map.valueEquivalence.equivalent(value, entryValue)) {
                     cause = RemovalCause.EXPLICIT;
                  } else {
                     if (entryValue != null || !valueReference.isActive()) {
                        boolean var15 = false;
                        return var15;
                     }

                     cause = RemovalCause.COLLECTED;
                  }

                  ++this.modCount;
                  LocalCache.ReferenceEntry<K, V> newFirst = this.removeValueFromChain(first, e, entryKey, hash, valueReference, cause);
                  newCount = this.count - 1;
                  table.set(index, newFirst);
                  this.count = newCount;
                  boolean var16 = cause == RemovalCause.EXPLICIT;
                  return var16;
               }
            }

            var20 = false;
         } finally {
            this.unlock();
            this.postWriteCleanup();
         }

         return var20;
      }

      void clear() {
         if (this.count != 0) {
            this.lock();

            try {
               AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table = this.table;

               int i;
               for(i = 0; i < table.length(); ++i) {
                  for(LocalCache.ReferenceEntry e = (LocalCache.ReferenceEntry)table.get(i); e != null; e = e.getNext()) {
                     if (e.getValueReference().isActive()) {
                        this.enqueueNotification(e, RemovalCause.EXPLICIT);
                     }
                  }
               }

               for(i = 0; i < table.length(); ++i) {
                  table.set(i, (Object)null);
               }

               this.clearReferenceQueues();
               this.writeQueue.clear();
               this.accessQueue.clear();
               this.readCount.set(0);
               ++this.modCount;
               this.count = 0;
            } finally {
               this.unlock();
               this.postWriteCleanup();
            }
         }

      }

      @Nullable
      @GuardedBy("this")
      LocalCache.ReferenceEntry<K, V> removeValueFromChain(LocalCache.ReferenceEntry<K, V> first, LocalCache.ReferenceEntry<K, V> entry, @Nullable K key, int hash, LocalCache.ValueReference<K, V> valueReference, RemovalCause cause) {
         this.enqueueNotification(key, hash, valueReference, cause);
         this.writeQueue.remove(entry);
         this.accessQueue.remove(entry);
         if (valueReference.isLoading()) {
            valueReference.notifyNewValue((Object)null);
            return first;
         } else {
            return this.removeEntryFromChain(first, entry);
         }
      }

      @Nullable
      @GuardedBy("this")
      LocalCache.ReferenceEntry<K, V> removeEntryFromChain(LocalCache.ReferenceEntry<K, V> first, LocalCache.ReferenceEntry<K, V> entry) {
         int newCount = this.count;
         LocalCache.ReferenceEntry<K, V> newFirst = entry.getNext();

         for(LocalCache.ReferenceEntry e = first; e != entry; e = e.getNext()) {
            LocalCache.ReferenceEntry<K, V> next = this.copyEntry(e, newFirst);
            if (next != null) {
               newFirst = next;
            } else {
               this.removeCollectedEntry(e);
               --newCount;
            }
         }

         this.count = newCount;
         return newFirst;
      }

      @GuardedBy("this")
      void removeCollectedEntry(LocalCache.ReferenceEntry<K, V> entry) {
         this.enqueueNotification(entry, RemovalCause.COLLECTED);
         this.writeQueue.remove(entry);
         this.accessQueue.remove(entry);
      }

      boolean reclaimKey(LocalCache.ReferenceEntry<K, V> entry, int hash) {
         this.lock();

         boolean var13;
         try {
            int newCount = this.count - 1;
            AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            LocalCache.ReferenceEntry<K, V> first = (LocalCache.ReferenceEntry)table.get(index);

            for(LocalCache.ReferenceEntry e = first; e != null; e = e.getNext()) {
               if (e == entry) {
                  ++this.modCount;
                  LocalCache.ReferenceEntry<K, V> newFirst = this.removeValueFromChain(first, e, e.getKey(), hash, e.getValueReference(), RemovalCause.COLLECTED);
                  newCount = this.count - 1;
                  table.set(index, newFirst);
                  this.count = newCount;
                  boolean var9 = true;
                  return var9;
               }
            }

            var13 = false;
         } finally {
            this.unlock();
            this.postWriteCleanup();
         }

         return var13;
      }

      boolean reclaimValue(K key, int hash, LocalCache.ValueReference<K, V> valueReference) {
         this.lock();

         try {
            int newCount = this.count - 1;
            AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            LocalCache.ReferenceEntry<K, V> first = (LocalCache.ReferenceEntry)table.get(index);

            for(LocalCache.ReferenceEntry e = first; e != null; e = e.getNext()) {
               K entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  LocalCache.ValueReference<K, V> v = e.getValueReference();
                  if (v != valueReference) {
                     boolean var17 = false;
                     return var17;
                  }

                  ++this.modCount;
                  LocalCache.ReferenceEntry<K, V> newFirst = this.removeValueFromChain(first, e, entryKey, hash, valueReference, RemovalCause.COLLECTED);
                  newCount = this.count - 1;
                  table.set(index, newFirst);
                  this.count = newCount;
                  boolean var12 = true;
                  return var12;
               }
            }

            boolean var16 = false;
            return var16;
         } finally {
            this.unlock();
            if (!this.isHeldByCurrentThread()) {
               this.postWriteCleanup();
            }

         }
      }

      boolean removeLoadingValue(K key, int hash, LocalCache.LoadingValueReference<K, V> valueReference) {
         this.lock();

         try {
            AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            LocalCache.ReferenceEntry<K, V> first = (LocalCache.ReferenceEntry)table.get(index);

            for(LocalCache.ReferenceEntry e = first; e != null; e = e.getNext()) {
               K entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  LocalCache.ValueReference<K, V> v = e.getValueReference();
                  boolean var14;
                  if (v != valueReference) {
                     var14 = false;
                     return var14;
                  }

                  if (valueReference.isActive()) {
                     e.setValueReference(valueReference.getOldValue());
                  } else {
                     LocalCache.ReferenceEntry<K, V> newFirst = this.removeEntryFromChain(first, e);
                     table.set(index, newFirst);
                  }

                  var14 = true;
                  return var14;
               }
            }

            boolean var15 = false;
            return var15;
         } finally {
            this.unlock();
            this.postWriteCleanup();
         }
      }

      @GuardedBy("this")
      boolean removeEntry(LocalCache.ReferenceEntry<K, V> entry, int hash, RemovalCause cause) {
         int newCount = this.count - 1;
         AtomicReferenceArray<LocalCache.ReferenceEntry<K, V>> table = this.table;
         int index = hash & table.length() - 1;
         LocalCache.ReferenceEntry<K, V> first = (LocalCache.ReferenceEntry)table.get(index);

         for(LocalCache.ReferenceEntry e = first; e != null; e = e.getNext()) {
            if (e == entry) {
               ++this.modCount;
               LocalCache.ReferenceEntry<K, V> newFirst = this.removeValueFromChain(first, e, e.getKey(), hash, e.getValueReference(), cause);
               newCount = this.count - 1;
               table.set(index, newFirst);
               this.count = newCount;
               return true;
            }
         }

         return false;
      }

      void postReadCleanup() {
         if ((this.readCount.incrementAndGet() & 63) == 0) {
            this.cleanUp();
         }

      }

      @GuardedBy("this")
      void preWriteCleanup(long now) {
         this.runLockedCleanup(now);
      }

      void postWriteCleanup() {
         this.runUnlockedCleanup();
      }

      void cleanUp() {
         long now = this.map.ticker.read();
         this.runLockedCleanup(now);
         this.runUnlockedCleanup();
      }

      void runLockedCleanup(long now) {
         if (this.tryLock()) {
            try {
               this.drainReferenceQueues();
               this.expireEntries(now);
               this.readCount.set(0);
            } finally {
               this.unlock();
            }
         }

      }

      void runUnlockedCleanup() {
         if (!this.isHeldByCurrentThread()) {
            this.map.processPendingNotifications();
         }

      }
   }

   static final class WeightedStrongValueReference<K, V> extends LocalCache.StrongValueReference<K, V> {
      final int weight;

      WeightedStrongValueReference(V referent, int weight) {
         super(referent);
         this.weight = weight;
      }

      public int getWeight() {
         return this.weight;
      }
   }

   static final class WeightedSoftValueReference<K, V> extends LocalCache.SoftValueReference<K, V> {
      final int weight;

      WeightedSoftValueReference(ReferenceQueue<V> queue, V referent, LocalCache.ReferenceEntry<K, V> entry, int weight) {
         super(queue, referent, entry);
         this.weight = weight;
      }

      public int getWeight() {
         return this.weight;
      }

      public LocalCache.ValueReference<K, V> copyFor(ReferenceQueue<V> queue, V value, LocalCache.ReferenceEntry<K, V> entry) {
         return new LocalCache.WeightedSoftValueReference(queue, value, entry, this.weight);
      }
   }

   static final class WeightedWeakValueReference<K, V> extends LocalCache.WeakValueReference<K, V> {
      final int weight;

      WeightedWeakValueReference(ReferenceQueue<V> queue, V referent, LocalCache.ReferenceEntry<K, V> entry, int weight) {
         super(queue, referent, entry);
         this.weight = weight;
      }

      public int getWeight() {
         return this.weight;
      }

      public LocalCache.ValueReference<K, V> copyFor(ReferenceQueue<V> queue, V value, LocalCache.ReferenceEntry<K, V> entry) {
         return new LocalCache.WeightedWeakValueReference(queue, value, entry, this.weight);
      }
   }

   static class StrongValueReference<K, V> implements LocalCache.ValueReference<K, V> {
      final V referent;

      StrongValueReference(V referent) {
         this.referent = referent;
      }

      public V get() {
         return this.referent;
      }

      public int getWeight() {
         return 1;
      }

      public LocalCache.ReferenceEntry<K, V> getEntry() {
         return null;
      }

      public LocalCache.ValueReference<K, V> copyFor(ReferenceQueue<V> queue, V value, LocalCache.ReferenceEntry<K, V> entry) {
         return this;
      }

      public boolean isLoading() {
         return false;
      }

      public boolean isActive() {
         return true;
      }

      public V waitForValue() {
         return this.get();
      }

      public void notifyNewValue(V newValue) {
      }
   }

   static class SoftValueReference<K, V> extends SoftReference<V> implements LocalCache.ValueReference<K, V> {
      final LocalCache.ReferenceEntry<K, V> entry;

      SoftValueReference(ReferenceQueue<V> queue, V referent, LocalCache.ReferenceEntry<K, V> entry) {
         super(referent, queue);
         this.entry = entry;
      }

      public int getWeight() {
         return 1;
      }

      public LocalCache.ReferenceEntry<K, V> getEntry() {
         return this.entry;
      }

      public void notifyNewValue(V newValue) {
      }

      public LocalCache.ValueReference<K, V> copyFor(ReferenceQueue<V> queue, V value, LocalCache.ReferenceEntry<K, V> entry) {
         return new LocalCache.SoftValueReference(queue, value, entry);
      }

      public boolean isLoading() {
         return false;
      }

      public boolean isActive() {
         return true;
      }

      public V waitForValue() {
         return this.get();
      }
   }

   static class WeakValueReference<K, V> extends WeakReference<V> implements LocalCache.ValueReference<K, V> {
      final LocalCache.ReferenceEntry<K, V> entry;

      WeakValueReference(ReferenceQueue<V> queue, V referent, LocalCache.ReferenceEntry<K, V> entry) {
         super(referent, queue);
         this.entry = entry;
      }

      public int getWeight() {
         return 1;
      }

      public LocalCache.ReferenceEntry<K, V> getEntry() {
         return this.entry;
      }

      public void notifyNewValue(V newValue) {
      }

      public LocalCache.ValueReference<K, V> copyFor(ReferenceQueue<V> queue, V value, LocalCache.ReferenceEntry<K, V> entry) {
         return new LocalCache.WeakValueReference(queue, value, entry);
      }

      public boolean isLoading() {
         return false;
      }

      public boolean isActive() {
         return true;
      }

      public V waitForValue() {
         return this.get();
      }
   }

   static final class WeakAccessWriteEntry<K, V> extends LocalCache.WeakEntry<K, V> {
      volatile long accessTime = Long.MAX_VALUE;
      LocalCache.ReferenceEntry<K, V> nextAccess = LocalCache.nullEntry();
      LocalCache.ReferenceEntry<K, V> previousAccess = LocalCache.nullEntry();
      volatile long writeTime = Long.MAX_VALUE;
      LocalCache.ReferenceEntry<K, V> nextWrite = LocalCache.nullEntry();
      LocalCache.ReferenceEntry<K, V> previousWrite = LocalCache.nullEntry();

      WeakAccessWriteEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
         super(queue, key, hash, next);
      }

      public long getAccessTime() {
         return this.accessTime;
      }

      public void setAccessTime(long time) {
         this.accessTime = time;
      }

      public LocalCache.ReferenceEntry<K, V> getNextInAccessQueue() {
         return this.nextAccess;
      }

      public void setNextInAccessQueue(LocalCache.ReferenceEntry<K, V> next) {
         this.nextAccess = next;
      }

      public LocalCache.ReferenceEntry<K, V> getPreviousInAccessQueue() {
         return this.previousAccess;
      }

      public void setPreviousInAccessQueue(LocalCache.ReferenceEntry<K, V> previous) {
         this.previousAccess = previous;
      }

      public long getWriteTime() {
         return this.writeTime;
      }

      public void setWriteTime(long time) {
         this.writeTime = time;
      }

      public LocalCache.ReferenceEntry<K, V> getNextInWriteQueue() {
         return this.nextWrite;
      }

      public void setNextInWriteQueue(LocalCache.ReferenceEntry<K, V> next) {
         this.nextWrite = next;
      }

      public LocalCache.ReferenceEntry<K, V> getPreviousInWriteQueue() {
         return this.previousWrite;
      }

      public void setPreviousInWriteQueue(LocalCache.ReferenceEntry<K, V> previous) {
         this.previousWrite = previous;
      }
   }

   static final class WeakWriteEntry<K, V> extends LocalCache.WeakEntry<K, V> {
      volatile long writeTime = Long.MAX_VALUE;
      LocalCache.ReferenceEntry<K, V> nextWrite = LocalCache.nullEntry();
      LocalCache.ReferenceEntry<K, V> previousWrite = LocalCache.nullEntry();

      WeakWriteEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
         super(queue, key, hash, next);
      }

      public long getWriteTime() {
         return this.writeTime;
      }

      public void setWriteTime(long time) {
         this.writeTime = time;
      }

      public LocalCache.ReferenceEntry<K, V> getNextInWriteQueue() {
         return this.nextWrite;
      }

      public void setNextInWriteQueue(LocalCache.ReferenceEntry<K, V> next) {
         this.nextWrite = next;
      }

      public LocalCache.ReferenceEntry<K, V> getPreviousInWriteQueue() {
         return this.previousWrite;
      }

      public void setPreviousInWriteQueue(LocalCache.ReferenceEntry<K, V> previous) {
         this.previousWrite = previous;
      }
   }

   static final class WeakAccessEntry<K, V> extends LocalCache.WeakEntry<K, V> {
      volatile long accessTime = Long.MAX_VALUE;
      LocalCache.ReferenceEntry<K, V> nextAccess = LocalCache.nullEntry();
      LocalCache.ReferenceEntry<K, V> previousAccess = LocalCache.nullEntry();

      WeakAccessEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
         super(queue, key, hash, next);
      }

      public long getAccessTime() {
         return this.accessTime;
      }

      public void setAccessTime(long time) {
         this.accessTime = time;
      }

      public LocalCache.ReferenceEntry<K, V> getNextInAccessQueue() {
         return this.nextAccess;
      }

      public void setNextInAccessQueue(LocalCache.ReferenceEntry<K, V> next) {
         this.nextAccess = next;
      }

      public LocalCache.ReferenceEntry<K, V> getPreviousInAccessQueue() {
         return this.previousAccess;
      }

      public void setPreviousInAccessQueue(LocalCache.ReferenceEntry<K, V> previous) {
         this.previousAccess = previous;
      }
   }

   static class WeakEntry<K, V> extends WeakReference<K> implements LocalCache.ReferenceEntry<K, V> {
      final int hash;
      final LocalCache.ReferenceEntry<K, V> next;
      volatile LocalCache.ValueReference<K, V> valueReference = LocalCache.unset();

      WeakEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
         super(key, queue);
         this.hash = hash;
         this.next = next;
      }

      public K getKey() {
         return this.get();
      }

      public long getAccessTime() {
         throw new UnsupportedOperationException();
      }

      public void setAccessTime(long time) {
         throw new UnsupportedOperationException();
      }

      public LocalCache.ReferenceEntry<K, V> getNextInAccessQueue() {
         throw new UnsupportedOperationException();
      }

      public void setNextInAccessQueue(LocalCache.ReferenceEntry<K, V> next) {
         throw new UnsupportedOperationException();
      }

      public LocalCache.ReferenceEntry<K, V> getPreviousInAccessQueue() {
         throw new UnsupportedOperationException();
      }

      public void setPreviousInAccessQueue(LocalCache.ReferenceEntry<K, V> previous) {
         throw new UnsupportedOperationException();
      }

      public long getWriteTime() {
         throw new UnsupportedOperationException();
      }

      public void setWriteTime(long time) {
         throw new UnsupportedOperationException();
      }

      public LocalCache.ReferenceEntry<K, V> getNextInWriteQueue() {
         throw new UnsupportedOperationException();
      }

      public void setNextInWriteQueue(LocalCache.ReferenceEntry<K, V> next) {
         throw new UnsupportedOperationException();
      }

      public LocalCache.ReferenceEntry<K, V> getPreviousInWriteQueue() {
         throw new UnsupportedOperationException();
      }

      public void setPreviousInWriteQueue(LocalCache.ReferenceEntry<K, V> previous) {
         throw new UnsupportedOperationException();
      }

      public LocalCache.ValueReference<K, V> getValueReference() {
         return this.valueReference;
      }

      public void setValueReference(LocalCache.ValueReference<K, V> valueReference) {
         this.valueReference = valueReference;
      }

      public int getHash() {
         return this.hash;
      }

      public LocalCache.ReferenceEntry<K, V> getNext() {
         return this.next;
      }
   }

   static final class StrongAccessWriteEntry<K, V> extends LocalCache.StrongEntry<K, V> {
      volatile long accessTime = Long.MAX_VALUE;
      LocalCache.ReferenceEntry<K, V> nextAccess = LocalCache.nullEntry();
      LocalCache.ReferenceEntry<K, V> previousAccess = LocalCache.nullEntry();
      volatile long writeTime = Long.MAX_VALUE;
      LocalCache.ReferenceEntry<K, V> nextWrite = LocalCache.nullEntry();
      LocalCache.ReferenceEntry<K, V> previousWrite = LocalCache.nullEntry();

      StrongAccessWriteEntry(K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
         super(key, hash, next);
      }

      public long getAccessTime() {
         return this.accessTime;
      }

      public void setAccessTime(long time) {
         this.accessTime = time;
      }

      public LocalCache.ReferenceEntry<K, V> getNextInAccessQueue() {
         return this.nextAccess;
      }

      public void setNextInAccessQueue(LocalCache.ReferenceEntry<K, V> next) {
         this.nextAccess = next;
      }

      public LocalCache.ReferenceEntry<K, V> getPreviousInAccessQueue() {
         return this.previousAccess;
      }

      public void setPreviousInAccessQueue(LocalCache.ReferenceEntry<K, V> previous) {
         this.previousAccess = previous;
      }

      public long getWriteTime() {
         return this.writeTime;
      }

      public void setWriteTime(long time) {
         this.writeTime = time;
      }

      public LocalCache.ReferenceEntry<K, V> getNextInWriteQueue() {
         return this.nextWrite;
      }

      public void setNextInWriteQueue(LocalCache.ReferenceEntry<K, V> next) {
         this.nextWrite = next;
      }

      public LocalCache.ReferenceEntry<K, V> getPreviousInWriteQueue() {
         return this.previousWrite;
      }

      public void setPreviousInWriteQueue(LocalCache.ReferenceEntry<K, V> previous) {
         this.previousWrite = previous;
      }
   }

   static final class StrongWriteEntry<K, V> extends LocalCache.StrongEntry<K, V> {
      volatile long writeTime = Long.MAX_VALUE;
      LocalCache.ReferenceEntry<K, V> nextWrite = LocalCache.nullEntry();
      LocalCache.ReferenceEntry<K, V> previousWrite = LocalCache.nullEntry();

      StrongWriteEntry(K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
         super(key, hash, next);
      }

      public long getWriteTime() {
         return this.writeTime;
      }

      public void setWriteTime(long time) {
         this.writeTime = time;
      }

      public LocalCache.ReferenceEntry<K, V> getNextInWriteQueue() {
         return this.nextWrite;
      }

      public void setNextInWriteQueue(LocalCache.ReferenceEntry<K, V> next) {
         this.nextWrite = next;
      }

      public LocalCache.ReferenceEntry<K, V> getPreviousInWriteQueue() {
         return this.previousWrite;
      }

      public void setPreviousInWriteQueue(LocalCache.ReferenceEntry<K, V> previous) {
         this.previousWrite = previous;
      }
   }

   static final class StrongAccessEntry<K, V> extends LocalCache.StrongEntry<K, V> {
      volatile long accessTime = Long.MAX_VALUE;
      LocalCache.ReferenceEntry<K, V> nextAccess = LocalCache.nullEntry();
      LocalCache.ReferenceEntry<K, V> previousAccess = LocalCache.nullEntry();

      StrongAccessEntry(K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
         super(key, hash, next);
      }

      public long getAccessTime() {
         return this.accessTime;
      }

      public void setAccessTime(long time) {
         this.accessTime = time;
      }

      public LocalCache.ReferenceEntry<K, V> getNextInAccessQueue() {
         return this.nextAccess;
      }

      public void setNextInAccessQueue(LocalCache.ReferenceEntry<K, V> next) {
         this.nextAccess = next;
      }

      public LocalCache.ReferenceEntry<K, V> getPreviousInAccessQueue() {
         return this.previousAccess;
      }

      public void setPreviousInAccessQueue(LocalCache.ReferenceEntry<K, V> previous) {
         this.previousAccess = previous;
      }
   }

   static class StrongEntry<K, V> extends LocalCache.AbstractReferenceEntry<K, V> {
      final K key;
      final int hash;
      final LocalCache.ReferenceEntry<K, V> next;
      volatile LocalCache.ValueReference<K, V> valueReference = LocalCache.unset();

      StrongEntry(K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
         this.key = key;
         this.hash = hash;
         this.next = next;
      }

      public K getKey() {
         return this.key;
      }

      public LocalCache.ValueReference<K, V> getValueReference() {
         return this.valueReference;
      }

      public void setValueReference(LocalCache.ValueReference<K, V> valueReference) {
         this.valueReference = valueReference;
      }

      public int getHash() {
         return this.hash;
      }

      public LocalCache.ReferenceEntry<K, V> getNext() {
         return this.next;
      }
   }

   abstract static class AbstractReferenceEntry<K, V> implements LocalCache.ReferenceEntry<K, V> {
      public LocalCache.ValueReference<K, V> getValueReference() {
         throw new UnsupportedOperationException();
      }

      public void setValueReference(LocalCache.ValueReference<K, V> valueReference) {
         throw new UnsupportedOperationException();
      }

      public LocalCache.ReferenceEntry<K, V> getNext() {
         throw new UnsupportedOperationException();
      }

      public int getHash() {
         throw new UnsupportedOperationException();
      }

      public K getKey() {
         throw new UnsupportedOperationException();
      }

      public long getAccessTime() {
         throw new UnsupportedOperationException();
      }

      public void setAccessTime(long time) {
         throw new UnsupportedOperationException();
      }

      public LocalCache.ReferenceEntry<K, V> getNextInAccessQueue() {
         throw new UnsupportedOperationException();
      }

      public void setNextInAccessQueue(LocalCache.ReferenceEntry<K, V> next) {
         throw new UnsupportedOperationException();
      }

      public LocalCache.ReferenceEntry<K, V> getPreviousInAccessQueue() {
         throw new UnsupportedOperationException();
      }

      public void setPreviousInAccessQueue(LocalCache.ReferenceEntry<K, V> previous) {
         throw new UnsupportedOperationException();
      }

      public long getWriteTime() {
         throw new UnsupportedOperationException();
      }

      public void setWriteTime(long time) {
         throw new UnsupportedOperationException();
      }

      public LocalCache.ReferenceEntry<K, V> getNextInWriteQueue() {
         throw new UnsupportedOperationException();
      }

      public void setNextInWriteQueue(LocalCache.ReferenceEntry<K, V> next) {
         throw new UnsupportedOperationException();
      }

      public LocalCache.ReferenceEntry<K, V> getPreviousInWriteQueue() {
         throw new UnsupportedOperationException();
      }

      public void setPreviousInWriteQueue(LocalCache.ReferenceEntry<K, V> previous) {
         throw new UnsupportedOperationException();
      }
   }

   private static enum NullEntry implements LocalCache.ReferenceEntry<Object, Object> {
      INSTANCE;

      public LocalCache.ValueReference<Object, Object> getValueReference() {
         return null;
      }

      public void setValueReference(LocalCache.ValueReference<Object, Object> valueReference) {
      }

      public LocalCache.ReferenceEntry<Object, Object> getNext() {
         return null;
      }

      public int getHash() {
         return 0;
      }

      public Object getKey() {
         return null;
      }

      public long getAccessTime() {
         return 0L;
      }

      public void setAccessTime(long time) {
      }

      public LocalCache.ReferenceEntry<Object, Object> getNextInAccessQueue() {
         return this;
      }

      public void setNextInAccessQueue(LocalCache.ReferenceEntry<Object, Object> next) {
      }

      public LocalCache.ReferenceEntry<Object, Object> getPreviousInAccessQueue() {
         return this;
      }

      public void setPreviousInAccessQueue(LocalCache.ReferenceEntry<Object, Object> previous) {
      }

      public long getWriteTime() {
         return 0L;
      }

      public void setWriteTime(long time) {
      }

      public LocalCache.ReferenceEntry<Object, Object> getNextInWriteQueue() {
         return this;
      }

      public void setNextInWriteQueue(LocalCache.ReferenceEntry<Object, Object> next) {
      }

      public LocalCache.ReferenceEntry<Object, Object> getPreviousInWriteQueue() {
         return this;
      }

      public void setPreviousInWriteQueue(LocalCache.ReferenceEntry<Object, Object> previous) {
      }
   }

   interface ReferenceEntry<K, V> {
      LocalCache.ValueReference<K, V> getValueReference();

      void setValueReference(LocalCache.ValueReference<K, V> var1);

      @Nullable
      LocalCache.ReferenceEntry<K, V> getNext();

      int getHash();

      @Nullable
      K getKey();

      long getAccessTime();

      void setAccessTime(long var1);

      LocalCache.ReferenceEntry<K, V> getNextInAccessQueue();

      void setNextInAccessQueue(LocalCache.ReferenceEntry<K, V> var1);

      LocalCache.ReferenceEntry<K, V> getPreviousInAccessQueue();

      void setPreviousInAccessQueue(LocalCache.ReferenceEntry<K, V> var1);

      long getWriteTime();

      void setWriteTime(long var1);

      LocalCache.ReferenceEntry<K, V> getNextInWriteQueue();

      void setNextInWriteQueue(LocalCache.ReferenceEntry<K, V> var1);

      LocalCache.ReferenceEntry<K, V> getPreviousInWriteQueue();

      void setPreviousInWriteQueue(LocalCache.ReferenceEntry<K, V> var1);
   }

   interface ValueReference<K, V> {
      @Nullable
      V get();

      V waitForValue() throws ExecutionException;

      int getWeight();

      @Nullable
      LocalCache.ReferenceEntry<K, V> getEntry();

      LocalCache.ValueReference<K, V> copyFor(ReferenceQueue<V> var1, @Nullable V var2, LocalCache.ReferenceEntry<K, V> var3);

      void notifyNewValue(@Nullable V var1);

      boolean isLoading();

      boolean isActive();
   }

   static enum EntryFactory {
      STRONG {
         <K, V> LocalCache.ReferenceEntry<K, V> newEntry(LocalCache.Segment<K, V> segment, K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
            return new LocalCache.StrongEntry(key, hash, next);
         }
      },
      STRONG_ACCESS {
         <K, V> LocalCache.ReferenceEntry<K, V> newEntry(LocalCache.Segment<K, V> segment, K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
            return new LocalCache.StrongAccessEntry(key, hash, next);
         }

         <K, V> LocalCache.ReferenceEntry<K, V> copyEntry(LocalCache.Segment<K, V> segment, LocalCache.ReferenceEntry<K, V> original, LocalCache.ReferenceEntry<K, V> newNext) {
            LocalCache.ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
            this.copyAccessEntry(original, newEntry);
            return newEntry;
         }
      },
      STRONG_WRITE {
         <K, V> LocalCache.ReferenceEntry<K, V> newEntry(LocalCache.Segment<K, V> segment, K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
            return new LocalCache.StrongWriteEntry(key, hash, next);
         }

         <K, V> LocalCache.ReferenceEntry<K, V> copyEntry(LocalCache.Segment<K, V> segment, LocalCache.ReferenceEntry<K, V> original, LocalCache.ReferenceEntry<K, V> newNext) {
            LocalCache.ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
            this.copyWriteEntry(original, newEntry);
            return newEntry;
         }
      },
      STRONG_ACCESS_WRITE {
         <K, V> LocalCache.ReferenceEntry<K, V> newEntry(LocalCache.Segment<K, V> segment, K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
            return new LocalCache.StrongAccessWriteEntry(key, hash, next);
         }

         <K, V> LocalCache.ReferenceEntry<K, V> copyEntry(LocalCache.Segment<K, V> segment, LocalCache.ReferenceEntry<K, V> original, LocalCache.ReferenceEntry<K, V> newNext) {
            LocalCache.ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
            this.copyAccessEntry(original, newEntry);
            this.copyWriteEntry(original, newEntry);
            return newEntry;
         }
      },
      WEAK {
         <K, V> LocalCache.ReferenceEntry<K, V> newEntry(LocalCache.Segment<K, V> segment, K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
            return new LocalCache.WeakEntry(segment.keyReferenceQueue, key, hash, next);
         }
      },
      WEAK_ACCESS {
         <K, V> LocalCache.ReferenceEntry<K, V> newEntry(LocalCache.Segment<K, V> segment, K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
            return new LocalCache.WeakAccessEntry(segment.keyReferenceQueue, key, hash, next);
         }

         <K, V> LocalCache.ReferenceEntry<K, V> copyEntry(LocalCache.Segment<K, V> segment, LocalCache.ReferenceEntry<K, V> original, LocalCache.ReferenceEntry<K, V> newNext) {
            LocalCache.ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
            this.copyAccessEntry(original, newEntry);
            return newEntry;
         }
      },
      WEAK_WRITE {
         <K, V> LocalCache.ReferenceEntry<K, V> newEntry(LocalCache.Segment<K, V> segment, K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
            return new LocalCache.WeakWriteEntry(segment.keyReferenceQueue, key, hash, next);
         }

         <K, V> LocalCache.ReferenceEntry<K, V> copyEntry(LocalCache.Segment<K, V> segment, LocalCache.ReferenceEntry<K, V> original, LocalCache.ReferenceEntry<K, V> newNext) {
            LocalCache.ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
            this.copyWriteEntry(original, newEntry);
            return newEntry;
         }
      },
      WEAK_ACCESS_WRITE {
         <K, V> LocalCache.ReferenceEntry<K, V> newEntry(LocalCache.Segment<K, V> segment, K key, int hash, @Nullable LocalCache.ReferenceEntry<K, V> next) {
            return new LocalCache.WeakAccessWriteEntry(segment.keyReferenceQueue, key, hash, next);
         }

         <K, V> LocalCache.ReferenceEntry<K, V> copyEntry(LocalCache.Segment<K, V> segment, LocalCache.ReferenceEntry<K, V> original, LocalCache.ReferenceEntry<K, V> newNext) {
            LocalCache.ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
            this.copyAccessEntry(original, newEntry);
            this.copyWriteEntry(original, newEntry);
            return newEntry;
         }
      };

      static final int ACCESS_MASK = 1;
      static final int WRITE_MASK = 2;
      static final int WEAK_MASK = 4;
      static final LocalCache.EntryFactory[] factories = new LocalCache.EntryFactory[]{STRONG, STRONG_ACCESS, STRONG_WRITE, STRONG_ACCESS_WRITE, WEAK, WEAK_ACCESS, WEAK_WRITE, WEAK_ACCESS_WRITE};

      private EntryFactory() {
      }

      static LocalCache.EntryFactory getFactory(LocalCache.Strength keyStrength, boolean usesAccessQueue, boolean usesWriteQueue) {
         int flags = (keyStrength == LocalCache.Strength.WEAK ? 4 : 0) | (usesAccessQueue ? 1 : 0) | (usesWriteQueue ? 2 : 0);
         return factories[flags];
      }

      abstract <K, V> LocalCache.ReferenceEntry<K, V> newEntry(LocalCache.Segment<K, V> var1, K var2, int var3, @Nullable LocalCache.ReferenceEntry<K, V> var4);

      <K, V> LocalCache.ReferenceEntry<K, V> copyEntry(LocalCache.Segment<K, V> segment, LocalCache.ReferenceEntry<K, V> original, LocalCache.ReferenceEntry<K, V> newNext) {
         return this.newEntry(segment, original.getKey(), original.getHash(), newNext);
      }

      <K, V> void copyAccessEntry(LocalCache.ReferenceEntry<K, V> original, LocalCache.ReferenceEntry<K, V> newEntry) {
         newEntry.setAccessTime(original.getAccessTime());
         LocalCache.connectAccessOrder(original.getPreviousInAccessQueue(), newEntry);
         LocalCache.connectAccessOrder(newEntry, original.getNextInAccessQueue());
         LocalCache.nullifyAccessOrder(original);
      }

      <K, V> void copyWriteEntry(LocalCache.ReferenceEntry<K, V> original, LocalCache.ReferenceEntry<K, V> newEntry) {
         newEntry.setWriteTime(original.getWriteTime());
         LocalCache.connectWriteOrder(original.getPreviousInWriteQueue(), newEntry);
         LocalCache.connectWriteOrder(newEntry, original.getNextInWriteQueue());
         LocalCache.nullifyWriteOrder(original);
      }

      // $FF: synthetic method
      EntryFactory(Object x2) {
         this();
      }
   }

   static enum Strength {
      STRONG {
         <K, V> LocalCache.ValueReference<K, V> referenceValue(LocalCache.Segment<K, V> segment, LocalCache.ReferenceEntry<K, V> entry, V value, int weight) {
            return (LocalCache.ValueReference)(weight == 1 ? new LocalCache.StrongValueReference(value) : new LocalCache.WeightedStrongValueReference(value, weight));
         }

         Equivalence<Object> defaultEquivalence() {
            return Equivalence.equals();
         }
      },
      SOFT {
         <K, V> LocalCache.ValueReference<K, V> referenceValue(LocalCache.Segment<K, V> segment, LocalCache.ReferenceEntry<K, V> entry, V value, int weight) {
            return (LocalCache.ValueReference)(weight == 1 ? new LocalCache.SoftValueReference(segment.valueReferenceQueue, value, entry) : new LocalCache.WeightedSoftValueReference(segment.valueReferenceQueue, value, entry, weight));
         }

         Equivalence<Object> defaultEquivalence() {
            return Equivalence.identity();
         }
      },
      WEAK {
         <K, V> LocalCache.ValueReference<K, V> referenceValue(LocalCache.Segment<K, V> segment, LocalCache.ReferenceEntry<K, V> entry, V value, int weight) {
            return (LocalCache.ValueReference)(weight == 1 ? new LocalCache.WeakValueReference(segment.valueReferenceQueue, value, entry) : new LocalCache.WeightedWeakValueReference(segment.valueReferenceQueue, value, entry, weight));
         }

         Equivalence<Object> defaultEquivalence() {
            return Equivalence.identity();
         }
      };

      private Strength() {
      }

      abstract <K, V> LocalCache.ValueReference<K, V> referenceValue(LocalCache.Segment<K, V> var1, LocalCache.ReferenceEntry<K, V> var2, V var3, int var4);

      abstract Equivalence<Object> defaultEquivalence();

      // $FF: synthetic method
      Strength(Object x2) {
         this();
      }
   }
}
