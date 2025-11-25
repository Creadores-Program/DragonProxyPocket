package com.google.common.collect;

import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReferenceArray;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

class ComputingConcurrentHashMap<K, V> extends MapMakerInternalMap<K, V> {
   final Function<? super K, ? extends V> computingFunction;
   private static final long serialVersionUID = 4L;

   ComputingConcurrentHashMap(MapMaker builder, Function<? super K, ? extends V> computingFunction) {
      super(builder);
      this.computingFunction = (Function)Preconditions.checkNotNull(computingFunction);
   }

   MapMakerInternalMap.Segment<K, V> createSegment(int initialCapacity, int maxSegmentSize) {
      return new ComputingConcurrentHashMap.ComputingSegment(this, initialCapacity, maxSegmentSize);
   }

   ComputingConcurrentHashMap.ComputingSegment<K, V> segmentFor(int hash) {
      return (ComputingConcurrentHashMap.ComputingSegment)super.segmentFor(hash);
   }

   V getOrCompute(K key) throws ExecutionException {
      int hash = this.hash(Preconditions.checkNotNull(key));
      return this.segmentFor(hash).getOrCompute(key, hash, this.computingFunction);
   }

   Object writeReplace() {
      return new ComputingConcurrentHashMap.ComputingSerializationProxy(this.keyStrength, this.valueStrength, this.keyEquivalence, this.valueEquivalence, this.expireAfterWriteNanos, this.expireAfterAccessNanos, this.maximumSize, this.concurrencyLevel, this.removalListener, this, this.computingFunction);
   }

   static final class ComputingSerializationProxy<K, V> extends MapMakerInternalMap.AbstractSerializationProxy<K, V> {
      final Function<? super K, ? extends V> computingFunction;
      private static final long serialVersionUID = 4L;

      ComputingSerializationProxy(MapMakerInternalMap.Strength keyStrength, MapMakerInternalMap.Strength valueStrength, Equivalence<Object> keyEquivalence, Equivalence<Object> valueEquivalence, long expireAfterWriteNanos, long expireAfterAccessNanos, int maximumSize, int concurrencyLevel, MapMaker.RemovalListener<? super K, ? super V> removalListener, ConcurrentMap<K, V> delegate, Function<? super K, ? extends V> computingFunction) {
         super(keyStrength, valueStrength, keyEquivalence, valueEquivalence, expireAfterWriteNanos, expireAfterAccessNanos, maximumSize, concurrencyLevel, removalListener, delegate);
         this.computingFunction = computingFunction;
      }

      private void writeObject(ObjectOutputStream out) throws IOException {
         out.defaultWriteObject();
         this.writeMapTo(out);
      }

      private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
         in.defaultReadObject();
         MapMaker mapMaker = this.readMapMaker(in);
         this.delegate = mapMaker.makeComputingMap(this.computingFunction);
         this.readEntries(in);
      }

      Object readResolve() {
         return this.delegate;
      }
   }

   private static final class ComputingValueReference<K, V> implements MapMakerInternalMap.ValueReference<K, V> {
      final Function<? super K, ? extends V> computingFunction;
      @GuardedBy("this")
      volatile MapMakerInternalMap.ValueReference<K, V> computedReference = MapMakerInternalMap.unset();

      public ComputingValueReference(Function<? super K, ? extends V> computingFunction) {
         this.computingFunction = computingFunction;
      }

      public V get() {
         return null;
      }

      public MapMakerInternalMap.ReferenceEntry<K, V> getEntry() {
         return null;
      }

      public MapMakerInternalMap.ValueReference<K, V> copyFor(ReferenceQueue<V> queue, @Nullable V value, MapMakerInternalMap.ReferenceEntry<K, V> entry) {
         return this;
      }

      public boolean isComputingReference() {
         return true;
      }

      public V waitForValue() throws ExecutionException {
         if (this.computedReference == MapMakerInternalMap.UNSET) {
            boolean interrupted = false;

            try {
               synchronized(this) {
                  while(this.computedReference == MapMakerInternalMap.UNSET) {
                     try {
                        this.wait();
                     } catch (InterruptedException var9) {
                        interrupted = true;
                     }
                  }
               }
            } finally {
               if (interrupted) {
                  Thread.currentThread().interrupt();
               }

            }
         }

         return this.computedReference.waitForValue();
      }

      public void clear(MapMakerInternalMap.ValueReference<K, V> newValue) {
         this.setValueReference(newValue);
      }

      V compute(K key, int hash) throws ExecutionException {
         Object value;
         try {
            value = this.computingFunction.apply(key);
         } catch (Throwable var5) {
            this.setValueReference(new ComputingConcurrentHashMap.ComputationExceptionReference(var5));
            throw new ExecutionException(var5);
         }

         this.setValueReference(new ComputingConcurrentHashMap.ComputedReference(value));
         return value;
      }

      void setValueReference(MapMakerInternalMap.ValueReference<K, V> valueReference) {
         synchronized(this) {
            if (this.computedReference == MapMakerInternalMap.UNSET) {
               this.computedReference = valueReference;
               this.notifyAll();
            }

         }
      }
   }

   private static final class ComputedReference<K, V> implements MapMakerInternalMap.ValueReference<K, V> {
      final V value;

      ComputedReference(@Nullable V value) {
         this.value = value;
      }

      public V get() {
         return this.value;
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

   private static final class ComputationExceptionReference<K, V> implements MapMakerInternalMap.ValueReference<K, V> {
      final Throwable t;

      ComputationExceptionReference(Throwable t) {
         this.t = t;
      }

      public V get() {
         return null;
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

      public V waitForValue() throws ExecutionException {
         throw new ExecutionException(this.t);
      }

      public void clear(MapMakerInternalMap.ValueReference<K, V> newValue) {
      }
   }

   static final class ComputingSegment<K, V> extends MapMakerInternalMap.Segment<K, V> {
      ComputingSegment(MapMakerInternalMap<K, V> map, int initialCapacity, int maxSegmentSize) {
         super(map, initialCapacity, maxSegmentSize);
      }

      V getOrCompute(K key, int hash, Function<? super K, ? extends V> computingFunction) throws ExecutionException {
         while(true) {
            Object var24;
            try {
               MapMakerInternalMap.ReferenceEntry<K, V> e = this.getEntry(key, hash);
               Object value;
               if (e != null) {
                  value = this.getLiveValue(e);
                  if (value != null) {
                     this.recordRead(e);
                     var24 = value;
                     return var24;
                  }
               }

               if (e == null || !e.getValueReference().isComputingReference()) {
                  boolean createNewEntry = true;
                  ComputingConcurrentHashMap.ComputingValueReference<K, V> computingValueReference = null;
                  this.lock();

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
                           if (valueReference.isComputingReference()) {
                              createNewEntry = false;
                           } else {
                              V value = e.getValueReference().get();
                              if (value == null) {
                                 this.enqueueNotification(entryKey, hash, value, MapMaker.RemovalCause.COLLECTED);
                              } else {
                                 if (!this.map.expires() || !this.map.isExpired(e)) {
                                    this.recordLockedRead(e);
                                    Object var14 = value;
                                    return var14;
                                 }

                                 this.enqueueNotification(entryKey, hash, value, MapMaker.RemovalCause.EXPIRED);
                              }

                              this.evictionQueue.remove(e);
                              this.expirationQueue.remove(e);
                              this.count = newCount;
                           }
                           break;
                        }
                     }

                     if (createNewEntry) {
                        computingValueReference = new ComputingConcurrentHashMap.ComputingValueReference(computingFunction);
                        if (e == null) {
                           e = this.newEntry(key, hash, first);
                           e.setValueReference(computingValueReference);
                           table.set(index, e);
                        } else {
                           e.setValueReference(computingValueReference);
                        }
                     }
                  } finally {
                     this.unlock();
                     this.postWriteCleanup();
                  }

                  if (createNewEntry) {
                     Object var25 = this.compute(key, hash, e, computingValueReference);
                     return var25;
                  }
               }

               Preconditions.checkState(!Thread.holdsLock(e), "Recursive computation");
               value = e.getValueReference().waitForValue();
               if (value == null) {
                  continue;
               }

               this.recordRead(e);
               var24 = value;
            } finally {
               this.postReadCleanup();
            }

            return var24;
         }
      }

      V compute(K key, int hash, MapMakerInternalMap.ReferenceEntry<K, V> e, ComputingConcurrentHashMap.ComputingValueReference<K, V> computingValueReference) throws ExecutionException {
         V value = null;
         long start = System.nanoTime();
         long end = 0L;

         Object oldValue;
         try {
            synchronized(e) {
               value = computingValueReference.compute(key, hash);
               end = System.nanoTime();
            }

            if (value != null) {
               oldValue = this.put(key, hash, value, true);
               if (oldValue != null) {
                  this.enqueueNotification(key, hash, value, MapMaker.RemovalCause.REPLACED);
               }
            }

            oldValue = value;
         } finally {
            if (end == 0L) {
               end = System.nanoTime();
            }

            if (value == null) {
               this.clearValue(key, hash, computingValueReference);
            }

         }

         return oldValue;
      }
   }
}
