package com.google.common.collect;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Equivalence;
import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import com.google.common.primitives.Ints;
import com.google.j2objc.annotations.Weak;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import java.util.concurrent.CancellationException;
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

class MapMakerInternalMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>, Serializable {
   static final int MAXIMUM_CAPACITY = 1073741824;
   static final int MAX_SEGMENTS = 65536;
   static final int CONTAINS_VALUE_RETRIES = 3;
   static final int DRAIN_THRESHOLD = 63;
   static final int DRAIN_MAX = 16;
   static final long CLEANUP_EXECUTOR_DELAY_SECS = 60L;
   private static final Logger logger = Logger.getLogger(MapMakerInternalMap.class.getName());
   final transient int segmentMask;
   final transient int segmentShift;
   final transient MapMakerInternalMap.Segment<K, V>[] segments;
   final int concurrencyLevel;
   final Equivalence<Object> keyEquivalence;
   final Equivalence<Object> valueEquivalence;
   final MapMakerInternalMap.Strength keyStrength;
   final MapMakerInternalMap.Strength valueStrength;
   final int maximumSize;
   final long expireAfterAccessNanos;
   final long expireAfterWriteNanos;
   final Queue<MapMaker.RemovalNotification<K, V>> removalNotificationQueue;
   final MapMaker.RemovalListener<K, V> removalListener;
   final transient MapMakerInternalMap.EntryFactory entryFactory;
   final Ticker ticker;
   static final MapMakerInternalMap.ValueReference<Object, Object> UNSET = new MapMakerInternalMap.ValueReference<Object, Object>() {
      public Object get() {
         return null;
      }

      public MapMakerInternalMap.ReferenceEntry<Object, Object> getEntry() {
         return null;
      }

      public MapMakerInternalMap.ValueReference<Object, Object> copyFor(ReferenceQueue<Object> queue, @Nullable Object value, MapMakerInternalMap.ReferenceEntry<Object, Object> entry) {
         return this;
      }

      public boolean isComputingReference() {
         return false;
      }

      public Object waitForValue() {
         return null;
      }

      public void clear(MapMakerInternalMap.ValueReference<Object, Object> newValue) {
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
         return Iterators.emptyIterator();
      }
   };
   transient Set<K> keySet;
   transient Collection<V> values;
   transient Set<Entry<K, V>> entrySet;
   private static final long serialVersionUID = 5L;

   MapMakerInternalMap(MapMaker builder) {
      this.concurrencyLevel = Math.min(builder.getConcurrencyLevel(), 65536);
      this.keyStrength = builder.getKeyStrength();
      this.valueStrength = builder.getValueStrength();
      this.keyEquivalence = builder.getKeyEquivalence();
      this.valueEquivalence = this.valueStrength.defaultEquivalence();
      this.maximumSize = builder.maximumSize;
      this.expireAfterAccessNanos = builder.getExpireAfterAccessNanos();
      this.expireAfterWriteNanos = builder.getExpireAfterWriteNanos();
      this.entryFactory = MapMakerInternalMap.EntryFactory.getFactory(this.keyStrength, this.expires(), this.evictsBySize());
      this.ticker = builder.getTicker();
      this.removalListener = builder.getRemovalListener();
      this.removalNotificationQueue = (Queue)(this.removalListener == GenericMapMaker.NullListener.INSTANCE ? discardingQueue() : new ConcurrentLinkedQueue());
      int initialCapacity = Math.min(builder.getInitialCapacity(), 1073741824);
      if (this.evictsBySize()) {
         initialCapacity = Math.min(initialCapacity, this.maximumSize);
      }

      int segmentShift = 0;

      int segmentCount;
      for(segmentCount = 1; segmentCount < this.concurrencyLevel && (!this.evictsBySize() || segmentCount * 2 <= this.maximumSize); segmentCount <<= 1) {
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

      int maximumSegmentSize;
      if (this.evictsBySize()) {
         maximumSegmentSize = this.maximumSize / segmentCount + 1;
         int remainder = this.maximumSize % segmentCount;

         for(int i = 0; i < this.segments.length; ++i) {
            if (i == remainder) {
               --maximumSegmentSize;
            }

            this.segments[i] = this.createSegment(segmentSize, maximumSegmentSize);
         }
      } else {
         for(maximumSegmentSize = 0; maximumSegmentSize < this.segments.length; ++maximumSegmentSize) {
            this.segments[maximumSegmentSize] = this.createSegment(segmentSize, -1);
         }
      }

   }

   boolean evictsBySize() {
      return this.maximumSize != -1;
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

   boolean usesKeyReferences() {
      return this.keyStrength != MapMakerInternalMap.Strength.STRONG;
   }

   boolean usesValueReferences() {
      return this.valueStrength != MapMakerInternalMap.Strength.STRONG;
   }

   static <K, V> MapMakerInternalMap.ValueReference<K, V> unset() {
      return UNSET;
   }

   static <K, V> MapMakerInternalMap.ReferenceEntry<K, V> nullEntry() {
      return MapMakerInternalMap.NullEntry.INSTANCE;
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
   MapMakerInternalMap.ReferenceEntry<K, V> newEntry(K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
      return this.segmentFor(hash).newEntry(key, hash, next);
   }

   @VisibleForTesting
   MapMakerInternalMap.ReferenceEntry<K, V> copyEntry(MapMakerInternalMap.ReferenceEntry<K, V> original, MapMakerInternalMap.ReferenceEntry<K, V> newNext) {
      int hash = original.getHash();
      return this.segmentFor(hash).copyEntry(original, newNext);
   }

   @VisibleForTesting
   MapMakerInternalMap.ValueReference<K, V> newValueReference(MapMakerInternalMap.ReferenceEntry<K, V> entry, V value) {
      int hash = entry.getHash();
      return this.valueStrength.referenceValue(this.segmentFor(hash), entry, value);
   }

   int hash(Object key) {
      int h = this.keyEquivalence.hash(key);
      return rehash(h);
   }

   void reclaimValue(MapMakerInternalMap.ValueReference<K, V> valueReference) {
      MapMakerInternalMap.ReferenceEntry<K, V> entry = valueReference.getEntry();
      int hash = entry.getHash();
      this.segmentFor(hash).reclaimValue(entry.getKey(), hash, valueReference);
   }

   void reclaimKey(MapMakerInternalMap.ReferenceEntry<K, V> entry) {
      int hash = entry.getHash();
      this.segmentFor(hash).reclaimKey(entry, hash);
   }

   @VisibleForTesting
   boolean isLive(MapMakerInternalMap.ReferenceEntry<K, V> entry) {
      return this.segmentFor(entry.getHash()).getLiveValue(entry) != null;
   }

   MapMakerInternalMap.Segment<K, V> segmentFor(int hash) {
      return this.segments[hash >>> this.segmentShift & this.segmentMask];
   }

   MapMakerInternalMap.Segment<K, V> createSegment(int initialCapacity, int maxSegmentSize) {
      return new MapMakerInternalMap.Segment(this, initialCapacity, maxSegmentSize);
   }

   V getLiveValue(MapMakerInternalMap.ReferenceEntry<K, V> entry) {
      if (entry.getKey() == null) {
         return null;
      } else {
         V value = entry.getValueReference().get();
         if (value == null) {
            return null;
         } else {
            return this.expires() && this.isExpired(entry) ? null : value;
         }
      }
   }

   boolean isExpired(MapMakerInternalMap.ReferenceEntry<K, V> entry) {
      return this.isExpired(entry, this.ticker.read());
   }

   boolean isExpired(MapMakerInternalMap.ReferenceEntry<K, V> entry, long now) {
      return now - entry.getExpirationTime() > 0L;
   }

   static <K, V> void connectExpirables(MapMakerInternalMap.ReferenceEntry<K, V> previous, MapMakerInternalMap.ReferenceEntry<K, V> next) {
      previous.setNextExpirable(next);
      next.setPreviousExpirable(previous);
   }

   static <K, V> void nullifyExpirable(MapMakerInternalMap.ReferenceEntry<K, V> nulled) {
      MapMakerInternalMap.ReferenceEntry<K, V> nullEntry = nullEntry();
      nulled.setNextExpirable(nullEntry);
      nulled.setPreviousExpirable(nullEntry);
   }

   void processPendingNotifications() {
      MapMaker.RemovalNotification notification;
      while((notification = (MapMaker.RemovalNotification)this.removalNotificationQueue.poll()) != null) {
         try {
            this.removalListener.onRemoval(notification);
         } catch (Exception var3) {
            logger.log(Level.WARNING, "Exception thrown by removal listener", var3);
         }
      }

   }

   static <K, V> void connectEvictables(MapMakerInternalMap.ReferenceEntry<K, V> previous, MapMakerInternalMap.ReferenceEntry<K, V> next) {
      previous.setNextEvictable(next);
      next.setPreviousEvictable(previous);
   }

   static <K, V> void nullifyEvictable(MapMakerInternalMap.ReferenceEntry<K, V> nulled) {
      MapMakerInternalMap.ReferenceEntry<K, V> nullEntry = nullEntry();
      nulled.setNextEvictable(nullEntry);
      nulled.setPreviousEvictable(nullEntry);
   }

   final MapMakerInternalMap.Segment<K, V>[] newSegmentArray(int ssize) {
      return new MapMakerInternalMap.Segment[ssize];
   }

   public boolean isEmpty() {
      long sum = 0L;
      MapMakerInternalMap.Segment<K, V>[] segments = this.segments;

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

   public int size() {
      MapMakerInternalMap.Segment<K, V>[] segments = this.segments;
      long sum = 0L;

      for(int i = 0; i < segments.length; ++i) {
         sum += (long)segments[i].count;
      }

      return Ints.saturatedCast(sum);
   }

   public V get(@Nullable Object key) {
      if (key == null) {
         return null;
      } else {
         int hash = this.hash(key);
         return this.segmentFor(hash).get(key, hash);
      }
   }

   MapMakerInternalMap.ReferenceEntry<K, V> getEntry(@Nullable Object key) {
      if (key == null) {
         return null;
      } else {
         int hash = this.hash(key);
         return this.segmentFor(hash).getEntry(key, hash);
      }
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
         MapMakerInternalMap.Segment<K, V>[] segments = this.segments;
         long last = -1L;

         for(int i = 0; i < 3; ++i) {
            long sum = 0L;
            MapMakerInternalMap.Segment[] arr$ = segments;
            int len$ = segments.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               MapMakerInternalMap.Segment<K, V> segment = arr$[i$];
               int unused = segment.count;
               AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> table = segment.table;

               for(int j = 0; j < table.length(); ++j) {
                  for(MapMakerInternalMap.ReferenceEntry e = (MapMakerInternalMap.ReferenceEntry)table.get(j); e != null; e = e.getNext()) {
                     V v = segment.getLiveValue(e);
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
      MapMakerInternalMap.Segment[] arr$ = this.segments;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         MapMakerInternalMap.Segment<K, V> segment = arr$[i$];
         segment.clear();
      }

   }

   public Set<K> keySet() {
      Set<K> ks = this.keySet;
      return ks != null ? ks : (this.keySet = new MapMakerInternalMap.KeySet());
   }

   public Collection<V> values() {
      Collection<V> vs = this.values;
      return vs != null ? vs : (this.values = new MapMakerInternalMap.Values());
   }

   public Set<Entry<K, V>> entrySet() {
      Set<Entry<K, V>> es = this.entrySet;
      return es != null ? es : (this.entrySet = new MapMakerInternalMap.EntrySet());
   }

   private static <E> ArrayList<E> toArrayList(Collection<E> c) {
      ArrayList<E> result = new ArrayList(c.size());
      Iterators.addAll(result, c.iterator());
      return result;
   }

   Object writeReplace() {
      return new MapMakerInternalMap.SerializationProxy(this.keyStrength, this.valueStrength, this.keyEquivalence, this.valueEquivalence, this.expireAfterWriteNanos, this.expireAfterAccessNanos, this.maximumSize, this.concurrencyLevel, this.removalListener, this);
   }

   private static final class SerializationProxy<K, V> extends MapMakerInternalMap.AbstractSerializationProxy<K, V> {
      private static final long serialVersionUID = 3L;

      SerializationProxy(MapMakerInternalMap.Strength keyStrength, MapMakerInternalMap.Strength valueStrength, Equivalence<Object> keyEquivalence, Equivalence<Object> valueEquivalence, long expireAfterWriteNanos, long expireAfterAccessNanos, int maximumSize, int concurrencyLevel, MapMaker.RemovalListener<? super K, ? super V> removalListener, ConcurrentMap<K, V> delegate) {
         super(keyStrength, valueStrength, keyEquivalence, valueEquivalence, expireAfterWriteNanos, expireAfterAccessNanos, maximumSize, concurrencyLevel, removalListener, delegate);
      }

      private void writeObject(ObjectOutputStream out) throws IOException {
         out.defaultWriteObject();
         this.writeMapTo(out);
      }

      private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
         in.defaultReadObject();
         MapMaker mapMaker = this.readMapMaker(in);
         this.delegate = mapMaker.makeMap();
         this.readEntries(in);
      }

      private Object readResolve() {
         return this.delegate;
      }
   }

   abstract static class AbstractSerializationProxy<K, V> extends ForwardingConcurrentMap<K, V> implements Serializable {
      private static final long serialVersionUID = 3L;
      final MapMakerInternalMap.Strength keyStrength;
      final MapMakerInternalMap.Strength valueStrength;
      final Equivalence<Object> keyEquivalence;
      final Equivalence<Object> valueEquivalence;
      final long expireAfterWriteNanos;
      final long expireAfterAccessNanos;
      final int maximumSize;
      final int concurrencyLevel;
      final MapMaker.RemovalListener<? super K, ? super V> removalListener;
      transient ConcurrentMap<K, V> delegate;

      AbstractSerializationProxy(MapMakerInternalMap.Strength keyStrength, MapMakerInternalMap.Strength valueStrength, Equivalence<Object> keyEquivalence, Equivalence<Object> valueEquivalence, long expireAfterWriteNanos, long expireAfterAccessNanos, int maximumSize, int concurrencyLevel, MapMaker.RemovalListener<? super K, ? super V> removalListener, ConcurrentMap<K, V> delegate) {
         this.keyStrength = keyStrength;
         this.valueStrength = valueStrength;
         this.keyEquivalence = keyEquivalence;
         this.valueEquivalence = valueEquivalence;
         this.expireAfterWriteNanos = expireAfterWriteNanos;
         this.expireAfterAccessNanos = expireAfterAccessNanos;
         this.maximumSize = maximumSize;
         this.concurrencyLevel = concurrencyLevel;
         this.removalListener = removalListener;
         this.delegate = delegate;
      }

      protected ConcurrentMap<K, V> delegate() {
         return this.delegate;
      }

      void writeMapTo(ObjectOutputStream out) throws IOException {
         out.writeInt(this.delegate.size());
         Iterator i$ = this.delegate.entrySet().iterator();

         while(i$.hasNext()) {
            Entry<K, V> entry = (Entry)i$.next();
            out.writeObject(entry.getKey());
            out.writeObject(entry.getValue());
         }

         out.writeObject((Object)null);
      }

      MapMaker readMapMaker(ObjectInputStream in) throws IOException {
         int size = in.readInt();
         MapMaker mapMaker = (new MapMaker()).initialCapacity(size).setKeyStrength(this.keyStrength).setValueStrength(this.valueStrength).keyEquivalence(this.keyEquivalence).concurrencyLevel(this.concurrencyLevel);
         mapMaker.removalListener(this.removalListener);
         if (this.expireAfterWriteNanos > 0L) {
            mapMaker.expireAfterWrite(this.expireAfterWriteNanos, TimeUnit.NANOSECONDS);
         }

         if (this.expireAfterAccessNanos > 0L) {
            mapMaker.expireAfterAccess(this.expireAfterAccessNanos, TimeUnit.NANOSECONDS);
         }

         if (this.maximumSize != -1) {
            mapMaker.maximumSize(this.maximumSize);
         }

         return mapMaker;
      }

      void readEntries(ObjectInputStream in) throws IOException, ClassNotFoundException {
         while(true) {
            K key = in.readObject();
            if (key == null) {
               return;
            }

            V value = in.readObject();
            this.delegate.put(key, value);
         }
      }
   }

   private abstract static class SafeToArraySet<E> extends AbstractSet<E> {
      private SafeToArraySet() {
      }

      public Object[] toArray() {
         return MapMakerInternalMap.toArrayList(this).toArray();
      }

      public <E> E[] toArray(E[] a) {
         return MapMakerInternalMap.toArrayList(this).toArray(a);
      }

      // $FF: synthetic method
      SafeToArraySet(Object x0) {
         this();
      }
   }

   final class EntrySet extends MapMakerInternalMap.SafeToArraySet<Entry<K, V>> {
      EntrySet() {
         super(null);
      }

      public Iterator<Entry<K, V>> iterator() {
         return MapMakerInternalMap.this.new EntryIterator();
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
               V v = MapMakerInternalMap.this.get(key);
               return v != null && MapMakerInternalMap.this.valueEquivalence.equivalent(e.getValue(), v);
            }
         }
      }

      public boolean remove(Object o) {
         if (!(o instanceof Entry)) {
            return false;
         } else {
            Entry<?, ?> e = (Entry)o;
            Object key = e.getKey();
            return key != null && MapMakerInternalMap.this.remove(key, e.getValue());
         }
      }

      public int size() {
         return MapMakerInternalMap.this.size();
      }

      public boolean isEmpty() {
         return MapMakerInternalMap.this.isEmpty();
      }

      public void clear() {
         MapMakerInternalMap.this.clear();
      }
   }

   final class Values extends AbstractCollection<V> {
      public Iterator<V> iterator() {
         return MapMakerInternalMap.this.new ValueIterator();
      }

      public int size() {
         return MapMakerInternalMap.this.size();
      }

      public boolean isEmpty() {
         return MapMakerInternalMap.this.isEmpty();
      }

      public boolean contains(Object o) {
         return MapMakerInternalMap.this.containsValue(o);
      }

      public void clear() {
         MapMakerInternalMap.this.clear();
      }

      public Object[] toArray() {
         return MapMakerInternalMap.toArrayList(this).toArray();
      }

      public <E> E[] toArray(E[] a) {
         return MapMakerInternalMap.toArrayList(this).toArray(a);
      }
   }

   final class KeySet extends MapMakerInternalMap.SafeToArraySet<K> {
      KeySet() {
         super(null);
      }

      public Iterator<K> iterator() {
         return MapMakerInternalMap.this.new KeyIterator();
      }

      public int size() {
         return MapMakerInternalMap.this.size();
      }

      public boolean isEmpty() {
         return MapMakerInternalMap.this.isEmpty();
      }

      public boolean contains(Object o) {
         return MapMakerInternalMap.this.containsKey(o);
      }

      public boolean remove(Object o) {
         return MapMakerInternalMap.this.remove(o) != null;
      }

      public void clear() {
         MapMakerInternalMap.this.clear();
      }
   }

   final class EntryIterator extends MapMakerInternalMap<K, V>.HashIterator<Entry<K, V>> {
      EntryIterator() {
         super();
      }

      public Entry<K, V> next() {
         return this.nextEntry();
      }
   }

   final class WriteThroughEntry extends AbstractMapEntry<K, V> {
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
         V oldValue = MapMakerInternalMap.this.put(this.key, newValue);
         this.value = newValue;
         return oldValue;
      }
   }

   final class ValueIterator extends MapMakerInternalMap<K, V>.HashIterator<V> {
      ValueIterator() {
         super();
      }

      public V next() {
         return this.nextEntry().getValue();
      }
   }

   final class KeyIterator extends MapMakerInternalMap<K, V>.HashIterator<K> {
      KeyIterator() {
         super();
      }

      public K next() {
         return this.nextEntry().getKey();
      }
   }

   abstract class HashIterator<E> implements Iterator<E> {
      int nextSegmentIndex;
      int nextTableIndex;
      MapMakerInternalMap.Segment<K, V> currentSegment;
      AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> currentTable;
      MapMakerInternalMap.ReferenceEntry<K, V> nextEntry;
      MapMakerInternalMap<K, V>.WriteThroughEntry nextExternal;
      MapMakerInternalMap<K, V>.WriteThroughEntry lastReturned;

      HashIterator() {
         this.nextSegmentIndex = MapMakerInternalMap.this.segments.length - 1;
         this.nextTableIndex = -1;
         this.advance();
      }

      public abstract E next();

      final void advance() {
         this.nextExternal = null;
         if (!this.nextInChain()) {
            if (!this.nextInTable()) {
               while(this.nextSegmentIndex >= 0) {
                  this.currentSegment = MapMakerInternalMap.this.segments[this.nextSegmentIndex--];
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
               if ((this.nextEntry = (MapMakerInternalMap.ReferenceEntry)this.currentTable.get(this.nextTableIndex--)) == null || !this.advanceTo(this.nextEntry) && !this.nextInChain()) {
                  continue;
               }

               return true;
            }

            return false;
         }
      }

      boolean advanceTo(MapMakerInternalMap.ReferenceEntry<K, V> entry) {
         boolean var4;
         try {
            K key = entry.getKey();
            V value = MapMakerInternalMap.this.getLiveValue(entry);
            if (value != null) {
               this.nextExternal = MapMakerInternalMap.this.new WriteThroughEntry(key, value);
               var4 = true;
               return var4;
            }

            var4 = false;
         } finally {
            this.currentSegment.postReadCleanup();
         }

         return var4;
      }

      public boolean hasNext() {
         return this.nextExternal != null;
      }

      MapMakerInternalMap<K, V>.WriteThroughEntry nextEntry() {
         if (this.nextExternal == null) {
            throw new NoSuchElementException();
         } else {
            this.lastReturned = this.nextExternal;
            this.advance();
            return this.lastReturned;
         }
      }

      public void remove() {
         CollectPreconditions.checkRemove(this.lastReturned != null);
         MapMakerInternalMap.this.remove(this.lastReturned.getKey());
         this.lastReturned = null;
      }
   }

   static final class CleanupMapTask implements Runnable {
      final WeakReference<MapMakerInternalMap<?, ?>> mapReference;

      public CleanupMapTask(MapMakerInternalMap<?, ?> map) {
         this.mapReference = new WeakReference(map);
      }

      public void run() {
         MapMakerInternalMap<?, ?> map = (MapMakerInternalMap)this.mapReference.get();
         if (map == null) {
            throw new CancellationException();
         } else {
            MapMakerInternalMap.Segment[] arr$ = map.segments;
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               MapMakerInternalMap.Segment<?, ?> segment = arr$[i$];
               segment.runCleanup();
            }

         }
      }
   }

   static final class ExpirationQueue<K, V> extends AbstractQueue<MapMakerInternalMap.ReferenceEntry<K, V>> {
      final MapMakerInternalMap.ReferenceEntry<K, V> head = new MapMakerInternalMap.AbstractReferenceEntry<K, V>() {
         MapMakerInternalMap.ReferenceEntry<K, V> nextExpirable = this;
         MapMakerInternalMap.ReferenceEntry<K, V> previousExpirable = this;

         public long getExpirationTime() {
            return Long.MAX_VALUE;
         }

         public void setExpirationTime(long time) {
         }

         public MapMakerInternalMap.ReferenceEntry<K, V> getNextExpirable() {
            return this.nextExpirable;
         }

         public void setNextExpirable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
            this.nextExpirable = next;
         }

         public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousExpirable() {
            return this.previousExpirable;
         }

         public void setPreviousExpirable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
            this.previousExpirable = previous;
         }
      };

      public boolean offer(MapMakerInternalMap.ReferenceEntry<K, V> entry) {
         MapMakerInternalMap.connectExpirables(entry.getPreviousExpirable(), entry.getNextExpirable());
         MapMakerInternalMap.connectExpirables(this.head.getPreviousExpirable(), entry);
         MapMakerInternalMap.connectExpirables(entry, this.head);
         return true;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> peek() {
         MapMakerInternalMap.ReferenceEntry<K, V> next = this.head.getNextExpirable();
         return next == this.head ? null : next;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> poll() {
         MapMakerInternalMap.ReferenceEntry<K, V> next = this.head.getNextExpirable();
         if (next == this.head) {
            return null;
         } else {
            this.remove(next);
            return next;
         }
      }

      public boolean remove(Object o) {
         MapMakerInternalMap.ReferenceEntry<K, V> e = (MapMakerInternalMap.ReferenceEntry)o;
         MapMakerInternalMap.ReferenceEntry<K, V> previous = e.getPreviousExpirable();
         MapMakerInternalMap.ReferenceEntry<K, V> next = e.getNextExpirable();
         MapMakerInternalMap.connectExpirables(previous, next);
         MapMakerInternalMap.nullifyExpirable(e);
         return next != MapMakerInternalMap.NullEntry.INSTANCE;
      }

      public boolean contains(Object o) {
         MapMakerInternalMap.ReferenceEntry<K, V> e = (MapMakerInternalMap.ReferenceEntry)o;
         return e.getNextExpirable() != MapMakerInternalMap.NullEntry.INSTANCE;
      }

      public boolean isEmpty() {
         return this.head.getNextExpirable() == this.head;
      }

      public int size() {
         int size = 0;

         for(MapMakerInternalMap.ReferenceEntry e = this.head.getNextExpirable(); e != this.head; e = e.getNextExpirable()) {
            ++size;
         }

         return size;
      }

      public void clear() {
         MapMakerInternalMap.ReferenceEntry next;
         for(MapMakerInternalMap.ReferenceEntry e = this.head.getNextExpirable(); e != this.head; e = next) {
            next = e.getNextExpirable();
            MapMakerInternalMap.nullifyExpirable(e);
         }

         this.head.setNextExpirable(this.head);
         this.head.setPreviousExpirable(this.head);
      }

      public Iterator<MapMakerInternalMap.ReferenceEntry<K, V>> iterator() {
         return new AbstractSequentialIterator<MapMakerInternalMap.ReferenceEntry<K, V>>(this.peek()) {
            protected MapMakerInternalMap.ReferenceEntry<K, V> computeNext(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
               MapMakerInternalMap.ReferenceEntry<K, V> next = previous.getNextExpirable();
               return next == ExpirationQueue.this.head ? null : next;
            }
         };
      }
   }

   static final class EvictionQueue<K, V> extends AbstractQueue<MapMakerInternalMap.ReferenceEntry<K, V>> {
      final MapMakerInternalMap.ReferenceEntry<K, V> head = new MapMakerInternalMap.AbstractReferenceEntry<K, V>() {
         MapMakerInternalMap.ReferenceEntry<K, V> nextEvictable = this;
         MapMakerInternalMap.ReferenceEntry<K, V> previousEvictable = this;

         public MapMakerInternalMap.ReferenceEntry<K, V> getNextEvictable() {
            return this.nextEvictable;
         }

         public void setNextEvictable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
            this.nextEvictable = next;
         }

         public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousEvictable() {
            return this.previousEvictable;
         }

         public void setPreviousEvictable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
            this.previousEvictable = previous;
         }
      };

      public boolean offer(MapMakerInternalMap.ReferenceEntry<K, V> entry) {
         MapMakerInternalMap.connectEvictables(entry.getPreviousEvictable(), entry.getNextEvictable());
         MapMakerInternalMap.connectEvictables(this.head.getPreviousEvictable(), entry);
         MapMakerInternalMap.connectEvictables(entry, this.head);
         return true;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> peek() {
         MapMakerInternalMap.ReferenceEntry<K, V> next = this.head.getNextEvictable();
         return next == this.head ? null : next;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> poll() {
         MapMakerInternalMap.ReferenceEntry<K, V> next = this.head.getNextEvictable();
         if (next == this.head) {
            return null;
         } else {
            this.remove(next);
            return next;
         }
      }

      public boolean remove(Object o) {
         MapMakerInternalMap.ReferenceEntry<K, V> e = (MapMakerInternalMap.ReferenceEntry)o;
         MapMakerInternalMap.ReferenceEntry<K, V> previous = e.getPreviousEvictable();
         MapMakerInternalMap.ReferenceEntry<K, V> next = e.getNextEvictable();
         MapMakerInternalMap.connectEvictables(previous, next);
         MapMakerInternalMap.nullifyEvictable(e);
         return next != MapMakerInternalMap.NullEntry.INSTANCE;
      }

      public boolean contains(Object o) {
         MapMakerInternalMap.ReferenceEntry<K, V> e = (MapMakerInternalMap.ReferenceEntry)o;
         return e.getNextEvictable() != MapMakerInternalMap.NullEntry.INSTANCE;
      }

      public boolean isEmpty() {
         return this.head.getNextEvictable() == this.head;
      }

      public int size() {
         int size = 0;

         for(MapMakerInternalMap.ReferenceEntry e = this.head.getNextEvictable(); e != this.head; e = e.getNextEvictable()) {
            ++size;
         }

         return size;
      }

      public void clear() {
         MapMakerInternalMap.ReferenceEntry next;
         for(MapMakerInternalMap.ReferenceEntry e = this.head.getNextEvictable(); e != this.head; e = next) {
            next = e.getNextEvictable();
            MapMakerInternalMap.nullifyEvictable(e);
         }

         this.head.setNextEvictable(this.head);
         this.head.setPreviousEvictable(this.head);
      }

      public Iterator<MapMakerInternalMap.ReferenceEntry<K, V>> iterator() {
         return new AbstractSequentialIterator<MapMakerInternalMap.ReferenceEntry<K, V>>(this.peek()) {
            protected MapMakerInternalMap.ReferenceEntry<K, V> computeNext(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
               MapMakerInternalMap.ReferenceEntry<K, V> next = previous.getNextEvictable();
               return next == EvictionQueue.this.head ? null : next;
            }
         };
      }
   }

   static class Segment<K, V> extends ReentrantLock {
      @Weak
      final MapMakerInternalMap<K, V> map;
      volatile int count;
      int modCount;
      int threshold;
      volatile AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> table;
      final int maxSegmentSize;
      final ReferenceQueue<K> keyReferenceQueue;
      final ReferenceQueue<V> valueReferenceQueue;
      final Queue<MapMakerInternalMap.ReferenceEntry<K, V>> recencyQueue;
      final AtomicInteger readCount = new AtomicInteger();
      @GuardedBy("this")
      final Queue<MapMakerInternalMap.ReferenceEntry<K, V>> evictionQueue;
      @GuardedBy("this")
      final Queue<MapMakerInternalMap.ReferenceEntry<K, V>> expirationQueue;

      Segment(MapMakerInternalMap<K, V> map, int initialCapacity, int maxSegmentSize) {
         this.map = map;
         this.maxSegmentSize = maxSegmentSize;
         this.initTable(this.newEntryArray(initialCapacity));
         this.keyReferenceQueue = map.usesKeyReferences() ? new ReferenceQueue() : null;
         this.valueReferenceQueue = map.usesValueReferences() ? new ReferenceQueue() : null;
         this.recencyQueue = (Queue)(!map.evictsBySize() && !map.expiresAfterAccess() ? MapMakerInternalMap.discardingQueue() : new ConcurrentLinkedQueue());
         this.evictionQueue = (Queue)(map.evictsBySize() ? new MapMakerInternalMap.EvictionQueue() : MapMakerInternalMap.discardingQueue());
         this.expirationQueue = (Queue)(map.expires() ? new MapMakerInternalMap.ExpirationQueue() : MapMakerInternalMap.discardingQueue());
      }

      AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> newEntryArray(int size) {
         return new AtomicReferenceArray(size);
      }

      void initTable(AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> newTable) {
         this.threshold = newTable.length() * 3 / 4;
         if (this.threshold == this.maxSegmentSize) {
            ++this.threshold;
         }

         this.table = newTable;
      }

      @GuardedBy("this")
      MapMakerInternalMap.ReferenceEntry<K, V> newEntry(K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
         return this.map.entryFactory.newEntry(this, key, hash, next);
      }

      @GuardedBy("this")
      MapMakerInternalMap.ReferenceEntry<K, V> copyEntry(MapMakerInternalMap.ReferenceEntry<K, V> original, MapMakerInternalMap.ReferenceEntry<K, V> newNext) {
         if (original.getKey() == null) {
            return null;
         } else {
            MapMakerInternalMap.ValueReference<K, V> valueReference = original.getValueReference();
            V value = valueReference.get();
            if (value == null && !valueReference.isComputingReference()) {
               return null;
            } else {
               MapMakerInternalMap.ReferenceEntry<K, V> newEntry = this.map.entryFactory.copyEntry(this, original, newNext);
               newEntry.setValueReference(valueReference.copyFor(this.valueReferenceQueue, value, newEntry));
               return newEntry;
            }
         }
      }

      @GuardedBy("this")
      void setValue(MapMakerInternalMap.ReferenceEntry<K, V> entry, V value) {
         MapMakerInternalMap.ValueReference<K, V> valueReference = this.map.valueStrength.referenceValue(this, entry, value);
         entry.setValueReference(valueReference);
         this.recordWrite(entry);
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
            MapMakerInternalMap.ReferenceEntry<K, V> entry = (MapMakerInternalMap.ReferenceEntry)ref;
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
            MapMakerInternalMap.ValueReference<K, V> valueReference = (MapMakerInternalMap.ValueReference)ref;
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

      void recordRead(MapMakerInternalMap.ReferenceEntry<K, V> entry) {
         if (this.map.expiresAfterAccess()) {
            this.recordExpirationTime(entry, this.map.expireAfterAccessNanos);
         }

         this.recencyQueue.add(entry);
      }

      @GuardedBy("this")
      void recordLockedRead(MapMakerInternalMap.ReferenceEntry<K, V> entry) {
         this.evictionQueue.add(entry);
         if (this.map.expiresAfterAccess()) {
            this.recordExpirationTime(entry, this.map.expireAfterAccessNanos);
            this.expirationQueue.add(entry);
         }

      }

      @GuardedBy("this")
      void recordWrite(MapMakerInternalMap.ReferenceEntry<K, V> entry) {
         this.drainRecencyQueue();
         this.evictionQueue.add(entry);
         if (this.map.expires()) {
            long expiration = this.map.expiresAfterAccess() ? this.map.expireAfterAccessNanos : this.map.expireAfterWriteNanos;
            this.recordExpirationTime(entry, expiration);
            this.expirationQueue.add(entry);
         }

      }

      @GuardedBy("this")
      void drainRecencyQueue() {
         MapMakerInternalMap.ReferenceEntry e;
         while((e = (MapMakerInternalMap.ReferenceEntry)this.recencyQueue.poll()) != null) {
            if (this.evictionQueue.contains(e)) {
               this.evictionQueue.add(e);
            }

            if (this.map.expiresAfterAccess() && this.expirationQueue.contains(e)) {
               this.expirationQueue.add(e);
            }
         }

      }

      void recordExpirationTime(MapMakerInternalMap.ReferenceEntry<K, V> entry, long expirationNanos) {
         entry.setExpirationTime(this.map.ticker.read() + expirationNanos);
      }

      void tryExpireEntries() {
         if (this.tryLock()) {
            try {
               this.expireEntries();
            } finally {
               this.unlock();
            }
         }

      }

      @GuardedBy("this")
      void expireEntries() {
         this.drainRecencyQueue();
         if (!this.expirationQueue.isEmpty()) {
            long now = this.map.ticker.read();

            MapMakerInternalMap.ReferenceEntry e;
            while((e = (MapMakerInternalMap.ReferenceEntry)this.expirationQueue.peek()) != null && this.map.isExpired(e, now)) {
               if (!this.removeEntry(e, e.getHash(), MapMaker.RemovalCause.EXPIRED)) {
                  throw new AssertionError();
               }
            }

         }
      }

      void enqueueNotification(MapMakerInternalMap.ReferenceEntry<K, V> entry, MapMaker.RemovalCause cause) {
         this.enqueueNotification(entry.getKey(), entry.getHash(), entry.getValueReference().get(), cause);
      }

      void enqueueNotification(@Nullable K key, int hash, @Nullable V value, MapMaker.RemovalCause cause) {
         if (this.map.removalNotificationQueue != MapMakerInternalMap.DISCARDING_QUEUE) {
            MapMaker.RemovalNotification<K, V> notification = new MapMaker.RemovalNotification(key, value, cause);
            this.map.removalNotificationQueue.offer(notification);
         }

      }

      @GuardedBy("this")
      boolean evictEntries() {
         if (this.map.evictsBySize() && this.count >= this.maxSegmentSize) {
            this.drainRecencyQueue();
            MapMakerInternalMap.ReferenceEntry<K, V> e = (MapMakerInternalMap.ReferenceEntry)this.evictionQueue.remove();
            if (!this.removeEntry(e, e.getHash(), MapMaker.RemovalCause.SIZE)) {
               throw new AssertionError();
            } else {
               return true;
            }
         } else {
            return false;
         }
      }

      MapMakerInternalMap.ReferenceEntry<K, V> getFirst(int hash) {
         AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> table = this.table;
         return (MapMakerInternalMap.ReferenceEntry)table.get(hash & table.length() - 1);
      }

      MapMakerInternalMap.ReferenceEntry<K, V> getEntry(Object key, int hash) {
         if (this.count != 0) {
            for(MapMakerInternalMap.ReferenceEntry e = this.getFirst(hash); e != null; e = e.getNext()) {
               if (e.getHash() == hash) {
                  K entryKey = e.getKey();
                  if (entryKey == null) {
                     this.tryDrainReferenceQueues();
                  } else if (this.map.keyEquivalence.equivalent(key, entryKey)) {
                     return e;
                  }
               }
            }
         }

         return null;
      }

      MapMakerInternalMap.ReferenceEntry<K, V> getLiveEntry(Object key, int hash) {
         MapMakerInternalMap.ReferenceEntry<K, V> e = this.getEntry(key, hash);
         if (e == null) {
            return null;
         } else if (this.map.expires() && this.map.isExpired(e)) {
            this.tryExpireEntries();
            return null;
         } else {
            return e;
         }
      }

      V get(Object key, int hash) {
         Object value;
         try {
            MapMakerInternalMap.ReferenceEntry<K, V> e = this.getLiveEntry(key, hash);
            if (e != null) {
               value = e.getValueReference().get();
               if (value != null) {
                  this.recordRead(e);
               } else {
                  this.tryDrainReferenceQueues();
               }

               Object var5 = value;
               return var5;
            }

            value = null;
         } finally {
            this.postReadCleanup();
         }

         return value;
      }

      boolean containsKey(Object key, int hash) {
         boolean var4;
         try {
            if (this.count == 0) {
               boolean var8 = false;
               return var8;
            }

            MapMakerInternalMap.ReferenceEntry<K, V> e = this.getLiveEntry(key, hash);
            if (e == null) {
               var4 = false;
               return var4;
            }

            var4 = e.getValueReference().get() != null;
         } finally {
            this.postReadCleanup();
         }

         return var4;
      }

      @VisibleForTesting
      boolean containsValue(Object value) {
         boolean var11;
         try {
            if (this.count != 0) {
               AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> table = this.table;
               int length = table.length();

               for(int i = 0; i < length; ++i) {
                  for(MapMakerInternalMap.ReferenceEntry e = (MapMakerInternalMap.ReferenceEntry)table.get(i); e != null; e = e.getNext()) {
                     V entryValue = this.getLiveValue(e);
                     if (entryValue != null && this.map.valueEquivalence.equivalent(value, entryValue)) {
                        boolean var7 = true;
                        return var7;
                     }
                  }
               }
            }

            var11 = false;
         } finally {
            this.postReadCleanup();
         }

         return var11;
      }

      V put(K key, int hash, V value, boolean onlyIfAbsent) {
         this.lock();

         try {
            this.preWriteCleanup();
            int newCount = this.count + 1;
            if (newCount > this.threshold) {
               this.expand();
               newCount = this.count + 1;
            }

            AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            MapMakerInternalMap.ReferenceEntry<K, V> first = (MapMakerInternalMap.ReferenceEntry)table.get(index);

            MapMakerInternalMap.ReferenceEntry e;
            Object entryKey;
            for(e = first; e != null; e = e.getNext()) {
               entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  MapMakerInternalMap.ValueReference<K, V> valueReference = e.getValueReference();
                  V entryValue = valueReference.get();
                  Object var13;
                  if (entryValue == null) {
                     ++this.modCount;
                     this.setValue(e, value);
                     if (!valueReference.isComputingReference()) {
                        this.enqueueNotification(key, hash, entryValue, MapMaker.RemovalCause.COLLECTED);
                        newCount = this.count;
                     } else if (this.evictEntries()) {
                        newCount = this.count + 1;
                     }

                     this.count = newCount;
                     var13 = null;
                     return var13;
                  }

                  if (onlyIfAbsent) {
                     this.recordLockedRead(e);
                     var13 = entryValue;
                     return var13;
                  }

                  ++this.modCount;
                  this.enqueueNotification(key, hash, entryValue, MapMaker.RemovalCause.REPLACED);
                  this.setValue(e, value);
                  var13 = entryValue;
                  return var13;
               }
            }

            ++this.modCount;
            e = this.newEntry(key, hash, first);
            this.setValue(e, value);
            table.set(index, e);
            if (this.evictEntries()) {
               newCount = this.count + 1;
            }

            this.count = newCount;
            entryKey = null;
            return entryKey;
         } finally {
            this.unlock();
            this.postWriteCleanup();
         }
      }

      @GuardedBy("this")
      void expand() {
         AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> oldTable = this.table;
         int oldCapacity = oldTable.length();
         if (oldCapacity < 1073741824) {
            int newCount = this.count;
            AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> newTable = this.newEntryArray(oldCapacity << 1);
            this.threshold = newTable.length() * 3 / 4;
            int newMask = newTable.length() - 1;

            for(int oldIndex = 0; oldIndex < oldCapacity; ++oldIndex) {
               MapMakerInternalMap.ReferenceEntry<K, V> head = (MapMakerInternalMap.ReferenceEntry)oldTable.get(oldIndex);
               if (head != null) {
                  MapMakerInternalMap.ReferenceEntry<K, V> next = head.getNext();
                  int headIndex = head.getHash() & newMask;
                  if (next == null) {
                     newTable.set(headIndex, head);
                  } else {
                     MapMakerInternalMap.ReferenceEntry<K, V> tail = head;
                     int tailIndex = headIndex;

                     MapMakerInternalMap.ReferenceEntry e;
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
                        MapMakerInternalMap.ReferenceEntry<K, V> newNext = (MapMakerInternalMap.ReferenceEntry)newTable.get(newIndex);
                        MapMakerInternalMap.ReferenceEntry<K, V> newFirst = this.copyEntry(e, newNext);
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
            this.preWriteCleanup();
            AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            MapMakerInternalMap.ReferenceEntry<K, V> first = (MapMakerInternalMap.ReferenceEntry)table.get(index);

            for(MapMakerInternalMap.ReferenceEntry e = first; e != null; e = e.getNext()) {
               K entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  MapMakerInternalMap.ValueReference<K, V> valueReference = e.getValueReference();
                  V entryValue = valueReference.get();
                  boolean var18;
                  if (entryValue != null) {
                     if (this.map.valueEquivalence.equivalent(oldValue, entryValue)) {
                        ++this.modCount;
                        this.enqueueNotification(key, hash, entryValue, MapMaker.RemovalCause.REPLACED);
                        this.setValue(e, newValue);
                        var18 = true;
                        return var18;
                     }

                     this.recordLockedRead(e);
                     var18 = false;
                     return var18;
                  }

                  if (this.isCollected(valueReference)) {
                     int newCount = this.count - 1;
                     ++this.modCount;
                     this.enqueueNotification(entryKey, hash, entryValue, MapMaker.RemovalCause.COLLECTED);
                     MapMakerInternalMap.ReferenceEntry<K, V> newFirst = this.removeFromChain(first, e);
                     newCount = this.count - 1;
                     table.set(index, newFirst);
                     this.count = newCount;
                  }

                  var18 = false;
                  return var18;
               }
            }

            boolean var17 = false;
            return var17;
         } finally {
            this.unlock();
            this.postWriteCleanup();
         }
      }

      V replace(K key, int hash, V newValue) {
         this.lock();

         MapMakerInternalMap.ReferenceEntry e;
         try {
            this.preWriteCleanup();
            AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            MapMakerInternalMap.ReferenceEntry<K, V> first = (MapMakerInternalMap.ReferenceEntry)table.get(index);

            for(e = first; e != null; e = e.getNext()) {
               K entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  MapMakerInternalMap.ValueReference<K, V> valueReference = e.getValueReference();
                  V entryValue = valueReference.get();
                  Object var11;
                  if (entryValue == null) {
                     if (this.isCollected(valueReference)) {
                        int newCount = this.count - 1;
                        ++this.modCount;
                        this.enqueueNotification(entryKey, hash, entryValue, MapMaker.RemovalCause.COLLECTED);
                        MapMakerInternalMap.ReferenceEntry<K, V> newFirst = this.removeFromChain(first, e);
                        newCount = this.count - 1;
                        table.set(index, newFirst);
                        this.count = newCount;
                     }

                     var11 = null;
                     return var11;
                  }

                  ++this.modCount;
                  this.enqueueNotification(key, hash, entryValue, MapMaker.RemovalCause.REPLACED);
                  this.setValue(e, newValue);
                  var11 = entryValue;
                  return var11;
               }
            }

            e = null;
         } finally {
            this.unlock();
            this.postWriteCleanup();
         }

         return e;
      }

      V remove(Object key, int hash) {
         this.lock();

         MapMakerInternalMap.ReferenceEntry e;
         try {
            this.preWriteCleanup();
            int newCount = this.count - 1;
            AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            MapMakerInternalMap.ReferenceEntry<K, V> first = (MapMakerInternalMap.ReferenceEntry)table.get(index);

            for(e = first; e != null; e = e.getNext()) {
               K entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  MapMakerInternalMap.ValueReference<K, V> valueReference = e.getValueReference();
                  V entryValue = valueReference.get();
                  MapMaker.RemovalCause cause;
                  MapMakerInternalMap.ReferenceEntry newFirst;
                  if (entryValue != null) {
                     cause = MapMaker.RemovalCause.EXPLICIT;
                  } else {
                     if (!this.isCollected(valueReference)) {
                        newFirst = null;
                        return newFirst;
                     }

                     cause = MapMaker.RemovalCause.COLLECTED;
                  }

                  ++this.modCount;
                  this.enqueueNotification(entryKey, hash, entryValue, cause);
                  newFirst = this.removeFromChain(first, e);
                  newCount = this.count - 1;
                  table.set(index, newFirst);
                  this.count = newCount;
                  Object var13 = entryValue;
                  return var13;
               }
            }

            e = null;
         } finally {
            this.unlock();
            this.postWriteCleanup();
         }

         return e;
      }

      boolean remove(Object key, int hash, Object value) {
         this.lock();

         try {
            this.preWriteCleanup();
            int newCount = this.count - 1;
            AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            MapMakerInternalMap.ReferenceEntry<K, V> first = (MapMakerInternalMap.ReferenceEntry)table.get(index);

            for(MapMakerInternalMap.ReferenceEntry e = first; e != null; e = e.getNext()) {
               K entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  MapMakerInternalMap.ValueReference<K, V> valueReference = e.getValueReference();
                  V entryValue = valueReference.get();
                  MapMaker.RemovalCause cause;
                  if (this.map.valueEquivalence.equivalent(value, entryValue)) {
                     cause = MapMaker.RemovalCause.EXPLICIT;
                  } else {
                     if (!this.isCollected(valueReference)) {
                        boolean var19 = false;
                        return var19;
                     }

                     cause = MapMaker.RemovalCause.COLLECTED;
                  }

                  ++this.modCount;
                  this.enqueueNotification(entryKey, hash, entryValue, cause);
                  MapMakerInternalMap.ReferenceEntry<K, V> newFirst = this.removeFromChain(first, e);
                  newCount = this.count - 1;
                  table.set(index, newFirst);
                  this.count = newCount;
                  boolean var14 = cause == MapMaker.RemovalCause.EXPLICIT;
                  return var14;
               }
            }

            boolean var18 = false;
            return var18;
         } finally {
            this.unlock();
            this.postWriteCleanup();
         }
      }

      void clear() {
         if (this.count != 0) {
            this.lock();

            try {
               AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> table = this.table;
               int i;
               if (this.map.removalNotificationQueue != MapMakerInternalMap.DISCARDING_QUEUE) {
                  for(i = 0; i < table.length(); ++i) {
                     for(MapMakerInternalMap.ReferenceEntry e = (MapMakerInternalMap.ReferenceEntry)table.get(i); e != null; e = e.getNext()) {
                        if (!e.getValueReference().isComputingReference()) {
                           this.enqueueNotification(e, MapMaker.RemovalCause.EXPLICIT);
                        }
                     }
                  }
               }

               for(i = 0; i < table.length(); ++i) {
                  table.set(i, (Object)null);
               }

               this.clearReferenceQueues();
               this.evictionQueue.clear();
               this.expirationQueue.clear();
               this.readCount.set(0);
               ++this.modCount;
               this.count = 0;
            } finally {
               this.unlock();
               this.postWriteCleanup();
            }
         }

      }

      @GuardedBy("this")
      MapMakerInternalMap.ReferenceEntry<K, V> removeFromChain(MapMakerInternalMap.ReferenceEntry<K, V> first, MapMakerInternalMap.ReferenceEntry<K, V> entry) {
         this.evictionQueue.remove(entry);
         this.expirationQueue.remove(entry);
         int newCount = this.count;
         MapMakerInternalMap.ReferenceEntry<K, V> newFirst = entry.getNext();

         for(MapMakerInternalMap.ReferenceEntry e = first; e != entry; e = e.getNext()) {
            MapMakerInternalMap.ReferenceEntry<K, V> next = this.copyEntry(e, newFirst);
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

      void removeCollectedEntry(MapMakerInternalMap.ReferenceEntry<K, V> entry) {
         this.enqueueNotification(entry, MapMaker.RemovalCause.COLLECTED);
         this.evictionQueue.remove(entry);
         this.expirationQueue.remove(entry);
      }

      boolean reclaimKey(MapMakerInternalMap.ReferenceEntry<K, V> entry, int hash) {
         this.lock();

         boolean var13;
         try {
            int newCount = this.count - 1;
            AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            MapMakerInternalMap.ReferenceEntry<K, V> first = (MapMakerInternalMap.ReferenceEntry)table.get(index);

            for(MapMakerInternalMap.ReferenceEntry e = first; e != null; e = e.getNext()) {
               if (e == entry) {
                  ++this.modCount;
                  this.enqueueNotification(e.getKey(), hash, e.getValueReference().get(), MapMaker.RemovalCause.COLLECTED);
                  MapMakerInternalMap.ReferenceEntry<K, V> newFirst = this.removeFromChain(first, e);
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

      boolean reclaimValue(K key, int hash, MapMakerInternalMap.ValueReference<K, V> valueReference) {
         this.lock();

         try {
            int newCount = this.count - 1;
            AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            MapMakerInternalMap.ReferenceEntry<K, V> first = (MapMakerInternalMap.ReferenceEntry)table.get(index);

            for(MapMakerInternalMap.ReferenceEntry e = first; e != null; e = e.getNext()) {
               K entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  MapMakerInternalMap.ValueReference<K, V> v = e.getValueReference();
                  if (v != valueReference) {
                     boolean var17 = false;
                     return var17;
                  }

                  ++this.modCount;
                  this.enqueueNotification(key, hash, valueReference.get(), MapMaker.RemovalCause.COLLECTED);
                  MapMakerInternalMap.ReferenceEntry<K, V> newFirst = this.removeFromChain(first, e);
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

      boolean clearValue(K key, int hash, MapMakerInternalMap.ValueReference<K, V> valueReference) {
         this.lock();

         try {
            AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> table = this.table;
            int index = hash & table.length() - 1;
            MapMakerInternalMap.ReferenceEntry<K, V> first = (MapMakerInternalMap.ReferenceEntry)table.get(index);

            for(MapMakerInternalMap.ReferenceEntry e = first; e != null; e = e.getNext()) {
               K entryKey = e.getKey();
               if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                  MapMakerInternalMap.ValueReference<K, V> v = e.getValueReference();
                  if (v == valueReference) {
                     MapMakerInternalMap.ReferenceEntry<K, V> newFirst = this.removeFromChain(first, e);
                     table.set(index, newFirst);
                     boolean var11 = true;
                     return var11;
                  }

                  boolean var10 = false;
                  return var10;
               }
            }

            boolean var16 = false;
            return var16;
         } finally {
            this.unlock();
            this.postWriteCleanup();
         }
      }

      @GuardedBy("this")
      boolean removeEntry(MapMakerInternalMap.ReferenceEntry<K, V> entry, int hash, MapMaker.RemovalCause cause) {
         int newCount = this.count - 1;
         AtomicReferenceArray<MapMakerInternalMap.ReferenceEntry<K, V>> table = this.table;
         int index = hash & table.length() - 1;
         MapMakerInternalMap.ReferenceEntry<K, V> first = (MapMakerInternalMap.ReferenceEntry)table.get(index);

         for(MapMakerInternalMap.ReferenceEntry e = first; e != null; e = e.getNext()) {
            if (e == entry) {
               ++this.modCount;
               this.enqueueNotification(e.getKey(), hash, e.getValueReference().get(), cause);
               MapMakerInternalMap.ReferenceEntry<K, V> newFirst = this.removeFromChain(first, e);
               newCount = this.count - 1;
               table.set(index, newFirst);
               this.count = newCount;
               return true;
            }
         }

         return false;
      }

      boolean isCollected(MapMakerInternalMap.ValueReference<K, V> valueReference) {
         if (valueReference.isComputingReference()) {
            return false;
         } else {
            return valueReference.get() == null;
         }
      }

      V getLiveValue(MapMakerInternalMap.ReferenceEntry<K, V> entry) {
         if (entry.getKey() == null) {
            this.tryDrainReferenceQueues();
            return null;
         } else {
            V value = entry.getValueReference().get();
            if (value == null) {
               this.tryDrainReferenceQueues();
               return null;
            } else if (this.map.expires() && this.map.isExpired(entry)) {
               this.tryExpireEntries();
               return null;
            } else {
               return value;
            }
         }
      }

      void postReadCleanup() {
         if ((this.readCount.incrementAndGet() & 63) == 0) {
            this.runCleanup();
         }

      }

      @GuardedBy("this")
      void preWriteCleanup() {
         this.runLockedCleanup();
      }

      void postWriteCleanup() {
         this.runUnlockedCleanup();
      }

      void runCleanup() {
         this.runLockedCleanup();
         this.runUnlockedCleanup();
      }

      void runLockedCleanup() {
         if (this.tryLock()) {
            try {
               this.drainReferenceQueues();
               this.expireEntries();
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

   static final class StrongValueReference<K, V> implements MapMakerInternalMap.ValueReference<K, V> {
      final V referent;

      StrongValueReference(V referent) {
         this.referent = referent;
      }

      public V get() {
         return this.referent;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getEntry() {
         return null;
      }

      public MapMakerInternalMap.ValueReference<K, V> copyFor(ReferenceQueue<V> queue, V value, MapMakerInternalMap.ReferenceEntry<K, V> entry) {
         return this;
      }

      public boolean isComputingReference() {
         return false;
      }

      public V waitForValue() {
         return this.get();
      }

      public void clear(MapMakerInternalMap.ValueReference<K, V> newValue) {
      }
   }

   static final class SoftValueReference<K, V> extends SoftReference<V> implements MapMakerInternalMap.ValueReference<K, V> {
      final MapMakerInternalMap.ReferenceEntry<K, V> entry;

      SoftValueReference(ReferenceQueue<V> queue, V referent, MapMakerInternalMap.ReferenceEntry<K, V> entry) {
         super(referent, queue);
         this.entry = entry;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getEntry() {
         return this.entry;
      }

      public void clear(MapMakerInternalMap.ValueReference<K, V> newValue) {
         this.clear();
      }

      public MapMakerInternalMap.ValueReference<K, V> copyFor(ReferenceQueue<V> queue, V value, MapMakerInternalMap.ReferenceEntry<K, V> entry) {
         return new MapMakerInternalMap.SoftValueReference(queue, value, entry);
      }

      public boolean isComputingReference() {
         return false;
      }

      public V waitForValue() {
         return this.get();
      }
   }

   static final class WeakValueReference<K, V> extends WeakReference<V> implements MapMakerInternalMap.ValueReference<K, V> {
      final MapMakerInternalMap.ReferenceEntry<K, V> entry;

      WeakValueReference(ReferenceQueue<V> queue, V referent, MapMakerInternalMap.ReferenceEntry<K, V> entry) {
         super(referent, queue);
         this.entry = entry;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getEntry() {
         return this.entry;
      }

      public void clear(MapMakerInternalMap.ValueReference<K, V> newValue) {
         this.clear();
      }

      public MapMakerInternalMap.ValueReference<K, V> copyFor(ReferenceQueue<V> queue, V value, MapMakerInternalMap.ReferenceEntry<K, V> entry) {
         return new MapMakerInternalMap.WeakValueReference(queue, value, entry);
      }

      public boolean isComputingReference() {
         return false;
      }

      public V waitForValue() {
         return this.get();
      }
   }

   static final class WeakExpirableEvictableEntry<K, V> extends MapMakerInternalMap.WeakEntry<K, V> implements MapMakerInternalMap.ReferenceEntry<K, V> {
      volatile long time = Long.MAX_VALUE;
      MapMakerInternalMap.ReferenceEntry<K, V> nextExpirable = MapMakerInternalMap.nullEntry();
      MapMakerInternalMap.ReferenceEntry<K, V> previousExpirable = MapMakerInternalMap.nullEntry();
      MapMakerInternalMap.ReferenceEntry<K, V> nextEvictable = MapMakerInternalMap.nullEntry();
      MapMakerInternalMap.ReferenceEntry<K, V> previousEvictable = MapMakerInternalMap.nullEntry();

      WeakExpirableEvictableEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
         super(queue, key, hash, next);
      }

      public long getExpirationTime() {
         return this.time;
      }

      public void setExpirationTime(long time) {
         this.time = time;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextExpirable() {
         return this.nextExpirable;
      }

      public void setNextExpirable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         this.nextExpirable = next;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousExpirable() {
         return this.previousExpirable;
      }

      public void setPreviousExpirable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         this.previousExpirable = previous;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextEvictable() {
         return this.nextEvictable;
      }

      public void setNextEvictable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         this.nextEvictable = next;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousEvictable() {
         return this.previousEvictable;
      }

      public void setPreviousEvictable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         this.previousEvictable = previous;
      }
   }

   static final class WeakEvictableEntry<K, V> extends MapMakerInternalMap.WeakEntry<K, V> implements MapMakerInternalMap.ReferenceEntry<K, V> {
      MapMakerInternalMap.ReferenceEntry<K, V> nextEvictable = MapMakerInternalMap.nullEntry();
      MapMakerInternalMap.ReferenceEntry<K, V> previousEvictable = MapMakerInternalMap.nullEntry();

      WeakEvictableEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
         super(queue, key, hash, next);
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextEvictable() {
         return this.nextEvictable;
      }

      public void setNextEvictable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         this.nextEvictable = next;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousEvictable() {
         return this.previousEvictable;
      }

      public void setPreviousEvictable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         this.previousEvictable = previous;
      }
   }

   static final class WeakExpirableEntry<K, V> extends MapMakerInternalMap.WeakEntry<K, V> implements MapMakerInternalMap.ReferenceEntry<K, V> {
      volatile long time = Long.MAX_VALUE;
      MapMakerInternalMap.ReferenceEntry<K, V> nextExpirable = MapMakerInternalMap.nullEntry();
      MapMakerInternalMap.ReferenceEntry<K, V> previousExpirable = MapMakerInternalMap.nullEntry();

      WeakExpirableEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
         super(queue, key, hash, next);
      }

      public long getExpirationTime() {
         return this.time;
      }

      public void setExpirationTime(long time) {
         this.time = time;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextExpirable() {
         return this.nextExpirable;
      }

      public void setNextExpirable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         this.nextExpirable = next;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousExpirable() {
         return this.previousExpirable;
      }

      public void setPreviousExpirable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         this.previousExpirable = previous;
      }
   }

   static class WeakEntry<K, V> extends WeakReference<K> implements MapMakerInternalMap.ReferenceEntry<K, V> {
      final int hash;
      final MapMakerInternalMap.ReferenceEntry<K, V> next;
      volatile MapMakerInternalMap.ValueReference<K, V> valueReference = MapMakerInternalMap.unset();

      WeakEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
         super(key, queue);
         this.hash = hash;
         this.next = next;
      }

      public K getKey() {
         return this.get();
      }

      public long getExpirationTime() {
         throw new UnsupportedOperationException();
      }

      public void setExpirationTime(long time) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextExpirable() {
         throw new UnsupportedOperationException();
      }

      public void setNextExpirable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousExpirable() {
         throw new UnsupportedOperationException();
      }

      public void setPreviousExpirable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextEvictable() {
         throw new UnsupportedOperationException();
      }

      public void setNextEvictable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousEvictable() {
         throw new UnsupportedOperationException();
      }

      public void setPreviousEvictable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ValueReference<K, V> getValueReference() {
         return this.valueReference;
      }

      public void setValueReference(MapMakerInternalMap.ValueReference<K, V> valueReference) {
         MapMakerInternalMap.ValueReference<K, V> previous = this.valueReference;
         this.valueReference = valueReference;
         previous.clear(valueReference);
      }

      public int getHash() {
         return this.hash;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNext() {
         return this.next;
      }
   }

   static final class SoftExpirableEvictableEntry<K, V> extends MapMakerInternalMap.SoftEntry<K, V> implements MapMakerInternalMap.ReferenceEntry<K, V> {
      volatile long time = Long.MAX_VALUE;
      MapMakerInternalMap.ReferenceEntry<K, V> nextExpirable = MapMakerInternalMap.nullEntry();
      MapMakerInternalMap.ReferenceEntry<K, V> previousExpirable = MapMakerInternalMap.nullEntry();
      MapMakerInternalMap.ReferenceEntry<K, V> nextEvictable = MapMakerInternalMap.nullEntry();
      MapMakerInternalMap.ReferenceEntry<K, V> previousEvictable = MapMakerInternalMap.nullEntry();

      SoftExpirableEvictableEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
         super(queue, key, hash, next);
      }

      public long getExpirationTime() {
         return this.time;
      }

      public void setExpirationTime(long time) {
         this.time = time;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextExpirable() {
         return this.nextExpirable;
      }

      public void setNextExpirable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         this.nextExpirable = next;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousExpirable() {
         return this.previousExpirable;
      }

      public void setPreviousExpirable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         this.previousExpirable = previous;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextEvictable() {
         return this.nextEvictable;
      }

      public void setNextEvictable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         this.nextEvictable = next;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousEvictable() {
         return this.previousEvictable;
      }

      public void setPreviousEvictable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         this.previousEvictable = previous;
      }
   }

   static final class SoftEvictableEntry<K, V> extends MapMakerInternalMap.SoftEntry<K, V> implements MapMakerInternalMap.ReferenceEntry<K, V> {
      MapMakerInternalMap.ReferenceEntry<K, V> nextEvictable = MapMakerInternalMap.nullEntry();
      MapMakerInternalMap.ReferenceEntry<K, V> previousEvictable = MapMakerInternalMap.nullEntry();

      SoftEvictableEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
         super(queue, key, hash, next);
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextEvictable() {
         return this.nextEvictable;
      }

      public void setNextEvictable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         this.nextEvictable = next;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousEvictable() {
         return this.previousEvictable;
      }

      public void setPreviousEvictable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         this.previousEvictable = previous;
      }
   }

   static final class SoftExpirableEntry<K, V> extends MapMakerInternalMap.SoftEntry<K, V> implements MapMakerInternalMap.ReferenceEntry<K, V> {
      volatile long time = Long.MAX_VALUE;
      MapMakerInternalMap.ReferenceEntry<K, V> nextExpirable = MapMakerInternalMap.nullEntry();
      MapMakerInternalMap.ReferenceEntry<K, V> previousExpirable = MapMakerInternalMap.nullEntry();

      SoftExpirableEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
         super(queue, key, hash, next);
      }

      public long getExpirationTime() {
         return this.time;
      }

      public void setExpirationTime(long time) {
         this.time = time;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextExpirable() {
         return this.nextExpirable;
      }

      public void setNextExpirable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         this.nextExpirable = next;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousExpirable() {
         return this.previousExpirable;
      }

      public void setPreviousExpirable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         this.previousExpirable = previous;
      }
   }

   static class SoftEntry<K, V> extends SoftReference<K> implements MapMakerInternalMap.ReferenceEntry<K, V> {
      final int hash;
      final MapMakerInternalMap.ReferenceEntry<K, V> next;
      volatile MapMakerInternalMap.ValueReference<K, V> valueReference = MapMakerInternalMap.unset();

      SoftEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
         super(key, queue);
         this.hash = hash;
         this.next = next;
      }

      public K getKey() {
         return this.get();
      }

      public long getExpirationTime() {
         throw new UnsupportedOperationException();
      }

      public void setExpirationTime(long time) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextExpirable() {
         throw new UnsupportedOperationException();
      }

      public void setNextExpirable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousExpirable() {
         throw new UnsupportedOperationException();
      }

      public void setPreviousExpirable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextEvictable() {
         throw new UnsupportedOperationException();
      }

      public void setNextEvictable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousEvictable() {
         throw new UnsupportedOperationException();
      }

      public void setPreviousEvictable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ValueReference<K, V> getValueReference() {
         return this.valueReference;
      }

      public void setValueReference(MapMakerInternalMap.ValueReference<K, V> valueReference) {
         MapMakerInternalMap.ValueReference<K, V> previous = this.valueReference;
         this.valueReference = valueReference;
         previous.clear(valueReference);
      }

      public int getHash() {
         return this.hash;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNext() {
         return this.next;
      }
   }

   static final class StrongExpirableEvictableEntry<K, V> extends MapMakerInternalMap.StrongEntry<K, V> implements MapMakerInternalMap.ReferenceEntry<K, V> {
      volatile long time = Long.MAX_VALUE;
      MapMakerInternalMap.ReferenceEntry<K, V> nextExpirable = MapMakerInternalMap.nullEntry();
      MapMakerInternalMap.ReferenceEntry<K, V> previousExpirable = MapMakerInternalMap.nullEntry();
      MapMakerInternalMap.ReferenceEntry<K, V> nextEvictable = MapMakerInternalMap.nullEntry();
      MapMakerInternalMap.ReferenceEntry<K, V> previousEvictable = MapMakerInternalMap.nullEntry();

      StrongExpirableEvictableEntry(K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
         super(key, hash, next);
      }

      public long getExpirationTime() {
         return this.time;
      }

      public void setExpirationTime(long time) {
         this.time = time;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextExpirable() {
         return this.nextExpirable;
      }

      public void setNextExpirable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         this.nextExpirable = next;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousExpirable() {
         return this.previousExpirable;
      }

      public void setPreviousExpirable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         this.previousExpirable = previous;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextEvictable() {
         return this.nextEvictable;
      }

      public void setNextEvictable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         this.nextEvictable = next;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousEvictable() {
         return this.previousEvictable;
      }

      public void setPreviousEvictable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         this.previousEvictable = previous;
      }
   }

   static final class StrongEvictableEntry<K, V> extends MapMakerInternalMap.StrongEntry<K, V> implements MapMakerInternalMap.ReferenceEntry<K, V> {
      MapMakerInternalMap.ReferenceEntry<K, V> nextEvictable = MapMakerInternalMap.nullEntry();
      MapMakerInternalMap.ReferenceEntry<K, V> previousEvictable = MapMakerInternalMap.nullEntry();

      StrongEvictableEntry(K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
         super(key, hash, next);
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextEvictable() {
         return this.nextEvictable;
      }

      public void setNextEvictable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         this.nextEvictable = next;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousEvictable() {
         return this.previousEvictable;
      }

      public void setPreviousEvictable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         this.previousEvictable = previous;
      }
   }

   static final class StrongExpirableEntry<K, V> extends MapMakerInternalMap.StrongEntry<K, V> implements MapMakerInternalMap.ReferenceEntry<K, V> {
      volatile long time = Long.MAX_VALUE;
      MapMakerInternalMap.ReferenceEntry<K, V> nextExpirable = MapMakerInternalMap.nullEntry();
      MapMakerInternalMap.ReferenceEntry<K, V> previousExpirable = MapMakerInternalMap.nullEntry();

      StrongExpirableEntry(K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
         super(key, hash, next);
      }

      public long getExpirationTime() {
         return this.time;
      }

      public void setExpirationTime(long time) {
         this.time = time;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextExpirable() {
         return this.nextExpirable;
      }

      public void setNextExpirable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         this.nextExpirable = next;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousExpirable() {
         return this.previousExpirable;
      }

      public void setPreviousExpirable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         this.previousExpirable = previous;
      }
   }

   static class StrongEntry<K, V> implements MapMakerInternalMap.ReferenceEntry<K, V> {
      final K key;
      final int hash;
      final MapMakerInternalMap.ReferenceEntry<K, V> next;
      volatile MapMakerInternalMap.ValueReference<K, V> valueReference = MapMakerInternalMap.unset();

      StrongEntry(K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
         this.key = key;
         this.hash = hash;
         this.next = next;
      }

      public K getKey() {
         return this.key;
      }

      public long getExpirationTime() {
         throw new UnsupportedOperationException();
      }

      public void setExpirationTime(long time) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextExpirable() {
         throw new UnsupportedOperationException();
      }

      public void setNextExpirable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousExpirable() {
         throw new UnsupportedOperationException();
      }

      public void setPreviousExpirable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextEvictable() {
         throw new UnsupportedOperationException();
      }

      public void setNextEvictable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousEvictable() {
         throw new UnsupportedOperationException();
      }

      public void setPreviousEvictable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ValueReference<K, V> getValueReference() {
         return this.valueReference;
      }

      public void setValueReference(MapMakerInternalMap.ValueReference<K, V> valueReference) {
         MapMakerInternalMap.ValueReference<K, V> previous = this.valueReference;
         this.valueReference = valueReference;
         previous.clear(valueReference);
      }

      public int getHash() {
         return this.hash;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNext() {
         return this.next;
      }
   }

   abstract static class AbstractReferenceEntry<K, V> implements MapMakerInternalMap.ReferenceEntry<K, V> {
      public MapMakerInternalMap.ValueReference<K, V> getValueReference() {
         throw new UnsupportedOperationException();
      }

      public void setValueReference(MapMakerInternalMap.ValueReference<K, V> valueReference) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNext() {
         throw new UnsupportedOperationException();
      }

      public int getHash() {
         throw new UnsupportedOperationException();
      }

      public K getKey() {
         throw new UnsupportedOperationException();
      }

      public long getExpirationTime() {
         throw new UnsupportedOperationException();
      }

      public void setExpirationTime(long time) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextExpirable() {
         throw new UnsupportedOperationException();
      }

      public void setNextExpirable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousExpirable() {
         throw new UnsupportedOperationException();
      }

      public void setPreviousExpirable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getNextEvictable() {
         throw new UnsupportedOperationException();
      }

      public void setNextEvictable(MapMakerInternalMap.ReferenceEntry<K, V> next) {
         throw new UnsupportedOperationException();
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getPreviousEvictable() {
         throw new UnsupportedOperationException();
      }

      public void setPreviousEvictable(MapMakerInternalMap.ReferenceEntry<K, V> previous) {
         throw new UnsupportedOperationException();
      }
   }

   private static enum NullEntry implements MapMakerInternalMap.ReferenceEntry<Object, Object> {
      INSTANCE;

      public MapMakerInternalMap.ValueReference<Object, Object> getValueReference() {
         return null;
      }

      public void setValueReference(MapMakerInternalMap.ValueReference<Object, Object> valueReference) {
      }

      public MapMakerInternalMap.ReferenceEntry<Object, Object> getNext() {
         return null;
      }

      public int getHash() {
         return 0;
      }

      public Object getKey() {
         return null;
      }

      public long getExpirationTime() {
         return 0L;
      }

      public void setExpirationTime(long time) {
      }

      public MapMakerInternalMap.ReferenceEntry<Object, Object> getNextExpirable() {
         return this;
      }

      public void setNextExpirable(MapMakerInternalMap.ReferenceEntry<Object, Object> next) {
      }

      public MapMakerInternalMap.ReferenceEntry<Object, Object> getPreviousExpirable() {
         return this;
      }

      public void setPreviousExpirable(MapMakerInternalMap.ReferenceEntry<Object, Object> previous) {
      }

      public MapMakerInternalMap.ReferenceEntry<Object, Object> getNextEvictable() {
         return this;
      }

      public void setNextEvictable(MapMakerInternalMap.ReferenceEntry<Object, Object> next) {
      }

      public MapMakerInternalMap.ReferenceEntry<Object, Object> getPreviousEvictable() {
         return this;
      }

      public void setPreviousEvictable(MapMakerInternalMap.ReferenceEntry<Object, Object> previous) {
      }
   }

   interface ReferenceEntry<K, V> {
      MapMakerInternalMap.ValueReference<K, V> getValueReference();

      void setValueReference(MapMakerInternalMap.ValueReference<K, V> var1);

      MapMakerInternalMap.ReferenceEntry<K, V> getNext();

      int getHash();

      K getKey();

      long getExpirationTime();

      void setExpirationTime(long var1);

      MapMakerInternalMap.ReferenceEntry<K, V> getNextExpirable();

      void setNextExpirable(MapMakerInternalMap.ReferenceEntry<K, V> var1);

      MapMakerInternalMap.ReferenceEntry<K, V> getPreviousExpirable();

      void setPreviousExpirable(MapMakerInternalMap.ReferenceEntry<K, V> var1);

      MapMakerInternalMap.ReferenceEntry<K, V> getNextEvictable();

      void setNextEvictable(MapMakerInternalMap.ReferenceEntry<K, V> var1);

      MapMakerInternalMap.ReferenceEntry<K, V> getPreviousEvictable();

      void setPreviousEvictable(MapMakerInternalMap.ReferenceEntry<K, V> var1);
   }

   interface ValueReference<K, V> {
      V get();

      V waitForValue() throws ExecutionException;

      MapMakerInternalMap.ReferenceEntry<K, V> getEntry();

      MapMakerInternalMap.ValueReference<K, V> copyFor(ReferenceQueue<V> var1, @Nullable V var2, MapMakerInternalMap.ReferenceEntry<K, V> var3);

      void clear(@Nullable MapMakerInternalMap.ValueReference<K, V> var1);

      boolean isComputingReference();
   }

   static enum EntryFactory {
      STRONG {
         <K, V> MapMakerInternalMap.ReferenceEntry<K, V> newEntry(MapMakerInternalMap.Segment<K, V> segment, K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
            return new MapMakerInternalMap.StrongEntry(key, hash, next);
         }
      },
      STRONG_EXPIRABLE {
         <K, V> MapMakerInternalMap.ReferenceEntry<K, V> newEntry(MapMakerInternalMap.Segment<K, V> segment, K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
            return new MapMakerInternalMap.StrongExpirableEntry(key, hash, next);
         }

         <K, V> MapMakerInternalMap.ReferenceEntry<K, V> copyEntry(MapMakerInternalMap.Segment<K, V> segment, MapMakerInternalMap.ReferenceEntry<K, V> original, MapMakerInternalMap.ReferenceEntry<K, V> newNext) {
            MapMakerInternalMap.ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
            this.copyExpirableEntry(original, newEntry);
            return newEntry;
         }
      },
      STRONG_EVICTABLE {
         <K, V> MapMakerInternalMap.ReferenceEntry<K, V> newEntry(MapMakerInternalMap.Segment<K, V> segment, K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
            return new MapMakerInternalMap.StrongEvictableEntry(key, hash, next);
         }

         <K, V> MapMakerInternalMap.ReferenceEntry<K, V> copyEntry(MapMakerInternalMap.Segment<K, V> segment, MapMakerInternalMap.ReferenceEntry<K, V> original, MapMakerInternalMap.ReferenceEntry<K, V> newNext) {
            MapMakerInternalMap.ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
            this.copyEvictableEntry(original, newEntry);
            return newEntry;
         }
      },
      STRONG_EXPIRABLE_EVICTABLE {
         <K, V> MapMakerInternalMap.ReferenceEntry<K, V> newEntry(MapMakerInternalMap.Segment<K, V> segment, K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
            return new MapMakerInternalMap.StrongExpirableEvictableEntry(key, hash, next);
         }

         <K, V> MapMakerInternalMap.ReferenceEntry<K, V> copyEntry(MapMakerInternalMap.Segment<K, V> segment, MapMakerInternalMap.ReferenceEntry<K, V> original, MapMakerInternalMap.ReferenceEntry<K, V> newNext) {
            MapMakerInternalMap.ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
            this.copyExpirableEntry(original, newEntry);
            this.copyEvictableEntry(original, newEntry);
            return newEntry;
         }
      },
      WEAK {
         <K, V> MapMakerInternalMap.ReferenceEntry<K, V> newEntry(MapMakerInternalMap.Segment<K, V> segment, K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
            return new MapMakerInternalMap.WeakEntry(segment.keyReferenceQueue, key, hash, next);
         }
      },
      WEAK_EXPIRABLE {
         <K, V> MapMakerInternalMap.ReferenceEntry<K, V> newEntry(MapMakerInternalMap.Segment<K, V> segment, K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
            return new MapMakerInternalMap.WeakExpirableEntry(segment.keyReferenceQueue, key, hash, next);
         }

         <K, V> MapMakerInternalMap.ReferenceEntry<K, V> copyEntry(MapMakerInternalMap.Segment<K, V> segment, MapMakerInternalMap.ReferenceEntry<K, V> original, MapMakerInternalMap.ReferenceEntry<K, V> newNext) {
            MapMakerInternalMap.ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
            this.copyExpirableEntry(original, newEntry);
            return newEntry;
         }
      },
      WEAK_EVICTABLE {
         <K, V> MapMakerInternalMap.ReferenceEntry<K, V> newEntry(MapMakerInternalMap.Segment<K, V> segment, K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
            return new MapMakerInternalMap.WeakEvictableEntry(segment.keyReferenceQueue, key, hash, next);
         }

         <K, V> MapMakerInternalMap.ReferenceEntry<K, V> copyEntry(MapMakerInternalMap.Segment<K, V> segment, MapMakerInternalMap.ReferenceEntry<K, V> original, MapMakerInternalMap.ReferenceEntry<K, V> newNext) {
            MapMakerInternalMap.ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
            this.copyEvictableEntry(original, newEntry);
            return newEntry;
         }
      },
      WEAK_EXPIRABLE_EVICTABLE {
         <K, V> MapMakerInternalMap.ReferenceEntry<K, V> newEntry(MapMakerInternalMap.Segment<K, V> segment, K key, int hash, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> next) {
            return new MapMakerInternalMap.WeakExpirableEvictableEntry(segment.keyReferenceQueue, key, hash, next);
         }

         <K, V> MapMakerInternalMap.ReferenceEntry<K, V> copyEntry(MapMakerInternalMap.Segment<K, V> segment, MapMakerInternalMap.ReferenceEntry<K, V> original, MapMakerInternalMap.ReferenceEntry<K, V> newNext) {
            MapMakerInternalMap.ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
            this.copyExpirableEntry(original, newEntry);
            this.copyEvictableEntry(original, newEntry);
            return newEntry;
         }
      };

      static final int EXPIRABLE_MASK = 1;
      static final int EVICTABLE_MASK = 2;
      static final MapMakerInternalMap.EntryFactory[][] factories = new MapMakerInternalMap.EntryFactory[][]{{STRONG, STRONG_EXPIRABLE, STRONG_EVICTABLE, STRONG_EXPIRABLE_EVICTABLE}, new MapMakerInternalMap.EntryFactory[0], {WEAK, WEAK_EXPIRABLE, WEAK_EVICTABLE, WEAK_EXPIRABLE_EVICTABLE}};

      private EntryFactory() {
      }

      static MapMakerInternalMap.EntryFactory getFactory(MapMakerInternalMap.Strength keyStrength, boolean expireAfterWrite, boolean evictsBySize) {
         int flags = (expireAfterWrite ? 1 : 0) | (evictsBySize ? 2 : 0);
         return factories[keyStrength.ordinal()][flags];
      }

      abstract <K, V> MapMakerInternalMap.ReferenceEntry<K, V> newEntry(MapMakerInternalMap.Segment<K, V> var1, K var2, int var3, @Nullable MapMakerInternalMap.ReferenceEntry<K, V> var4);

      <K, V> MapMakerInternalMap.ReferenceEntry<K, V> copyEntry(MapMakerInternalMap.Segment<K, V> segment, MapMakerInternalMap.ReferenceEntry<K, V> original, MapMakerInternalMap.ReferenceEntry<K, V> newNext) {
         return this.newEntry(segment, original.getKey(), original.getHash(), newNext);
      }

      <K, V> void copyExpirableEntry(MapMakerInternalMap.ReferenceEntry<K, V> original, MapMakerInternalMap.ReferenceEntry<K, V> newEntry) {
         newEntry.setExpirationTime(original.getExpirationTime());
         MapMakerInternalMap.connectExpirables(original.getPreviousExpirable(), newEntry);
         MapMakerInternalMap.connectExpirables(newEntry, original.getNextExpirable());
         MapMakerInternalMap.nullifyExpirable(original);
      }

      <K, V> void copyEvictableEntry(MapMakerInternalMap.ReferenceEntry<K, V> original, MapMakerInternalMap.ReferenceEntry<K, V> newEntry) {
         MapMakerInternalMap.connectEvictables(original.getPreviousEvictable(), newEntry);
         MapMakerInternalMap.connectEvictables(newEntry, original.getNextEvictable());
         MapMakerInternalMap.nullifyEvictable(original);
      }

      // $FF: synthetic method
      EntryFactory(Object x2) {
         this();
      }
   }

   static enum Strength {
      STRONG {
         <K, V> MapMakerInternalMap.ValueReference<K, V> referenceValue(MapMakerInternalMap.Segment<K, V> segment, MapMakerInternalMap.ReferenceEntry<K, V> entry, V value) {
            return new MapMakerInternalMap.StrongValueReference(value);
         }

         Equivalence<Object> defaultEquivalence() {
            return Equivalence.equals();
         }
      },
      SOFT {
         <K, V> MapMakerInternalMap.ValueReference<K, V> referenceValue(MapMakerInternalMap.Segment<K, V> segment, MapMakerInternalMap.ReferenceEntry<K, V> entry, V value) {
            return new MapMakerInternalMap.SoftValueReference(segment.valueReferenceQueue, value, entry);
         }

         Equivalence<Object> defaultEquivalence() {
            return Equivalence.identity();
         }
      },
      WEAK {
         <K, V> MapMakerInternalMap.ValueReference<K, V> referenceValue(MapMakerInternalMap.Segment<K, V> segment, MapMakerInternalMap.ReferenceEntry<K, V> entry, V value) {
            return new MapMakerInternalMap.WeakValueReference(segment.valueReferenceQueue, value, entry);
         }

         Equivalence<Object> defaultEquivalence() {
            return Equivalence.identity();
         }
      };

      private Strength() {
      }

      abstract <K, V> MapMakerInternalMap.ValueReference<K, V> referenceValue(MapMakerInternalMap.Segment<K, V> var1, MapMakerInternalMap.ReferenceEntry<K, V> var2, V var3);

      abstract Equivalence<Object> defaultEquivalence();

      // $FF: synthetic method
      Strength(Object x2) {
         this();
      }
   }
}
