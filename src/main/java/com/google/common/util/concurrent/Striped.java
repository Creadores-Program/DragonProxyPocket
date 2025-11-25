package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapMaker;
import com.google.common.math.IntMath;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Beta
public abstract class Striped<L> {
   private static final int LARGE_LAZY_CUTOFF = 1024;
   private static final Supplier<ReadWriteLock> READ_WRITE_LOCK_SUPPLIER = new Supplier<ReadWriteLock>() {
      public ReadWriteLock get() {
         return new ReentrantReadWriteLock();
      }
   };
   private static final int ALL_SET = -1;

   private Striped() {
   }

   public abstract L get(Object var1);

   public abstract L getAt(int var1);

   abstract int indexFor(Object var1);

   public abstract int size();

   public Iterable<L> bulkGet(Iterable<?> keys) {
      Object[] array = Iterables.toArray(keys, Object.class);
      if (array.length == 0) {
         return ImmutableList.of();
      } else {
         int[] stripes = new int[array.length];

         int previousStripe;
         for(previousStripe = 0; previousStripe < array.length; ++previousStripe) {
            stripes[previousStripe] = this.indexFor(array[previousStripe]);
         }

         Arrays.sort(stripes);
         previousStripe = stripes[0];
         array[0] = this.getAt(previousStripe);

         for(int i = 1; i < array.length; ++i) {
            int currentStripe = stripes[i];
            if (currentStripe == previousStripe) {
               array[i] = array[i - 1];
            } else {
               array[i] = this.getAt(currentStripe);
               previousStripe = currentStripe;
            }
         }

         List<L> asList = Arrays.asList(array);
         return Collections.unmodifiableList(asList);
      }
   }

   public static Striped<Lock> lock(int stripes) {
      return new Striped.CompactStriped(stripes, new Supplier<Lock>() {
         public Lock get() {
            return new Striped.PaddedLock();
         }
      });
   }

   public static Striped<Lock> lazyWeakLock(int stripes) {
      return lazy(stripes, new Supplier<Lock>() {
         public Lock get() {
            return new ReentrantLock(false);
         }
      });
   }

   private static <L> Striped<L> lazy(int stripes, Supplier<L> supplier) {
      return (Striped)(stripes < 1024 ? new Striped.SmallLazyStriped(stripes, supplier) : new Striped.LargeLazyStriped(stripes, supplier));
   }

   public static Striped<Semaphore> semaphore(int stripes, final int permits) {
      return new Striped.CompactStriped(stripes, new Supplier<Semaphore>() {
         public Semaphore get() {
            return new Striped.PaddedSemaphore(permits);
         }
      });
   }

   public static Striped<Semaphore> lazyWeakSemaphore(int stripes, final int permits) {
      return lazy(stripes, new Supplier<Semaphore>() {
         public Semaphore get() {
            return new Semaphore(permits, false);
         }
      });
   }

   public static Striped<ReadWriteLock> readWriteLock(int stripes) {
      return new Striped.CompactStriped(stripes, READ_WRITE_LOCK_SUPPLIER);
   }

   public static Striped<ReadWriteLock> lazyWeakReadWriteLock(int stripes) {
      return lazy(stripes, READ_WRITE_LOCK_SUPPLIER);
   }

   private static int ceilToPowerOfTwo(int x) {
      return 1 << IntMath.log2(x, RoundingMode.CEILING);
   }

   private static int smear(int hashCode) {
      hashCode ^= hashCode >>> 20 ^ hashCode >>> 12;
      return hashCode ^ hashCode >>> 7 ^ hashCode >>> 4;
   }

   // $FF: synthetic method
   Striped(Object x0) {
      this();
   }

   private static class PaddedSemaphore extends Semaphore {
      long unused1;
      long unused2;
      long unused3;

      PaddedSemaphore(int permits) {
         super(permits, false);
      }
   }

   private static class PaddedLock extends ReentrantLock {
      long unused1;
      long unused2;
      long unused3;

      PaddedLock() {
         super(false);
      }
   }

   @VisibleForTesting
   static class LargeLazyStriped<L> extends Striped.PowerOfTwoStriped<L> {
      final ConcurrentMap<Integer, L> locks;
      final Supplier<L> supplier;
      final int size;

      LargeLazyStriped(int stripes, Supplier<L> supplier) {
         super(stripes);
         this.size = this.mask == -1 ? Integer.MAX_VALUE : this.mask + 1;
         this.supplier = supplier;
         this.locks = (new MapMaker()).weakValues().makeMap();
      }

      public L getAt(int index) {
         if (this.size != Integer.MAX_VALUE) {
            Preconditions.checkElementIndex(index, this.size());
         }

         L existing = this.locks.get(index);
         if (existing != null) {
            return existing;
         } else {
            L created = this.supplier.get();
            existing = this.locks.putIfAbsent(index, created);
            return MoreObjects.firstNonNull(existing, created);
         }
      }

      public int size() {
         return this.size;
      }
   }

   @VisibleForTesting
   static class SmallLazyStriped<L> extends Striped.PowerOfTwoStriped<L> {
      final AtomicReferenceArray<Striped.SmallLazyStriped.ArrayReference<? extends L>> locks;
      final Supplier<L> supplier;
      final int size;
      final ReferenceQueue<L> queue = new ReferenceQueue();

      SmallLazyStriped(int stripes, Supplier<L> supplier) {
         super(stripes);
         this.size = this.mask == -1 ? Integer.MAX_VALUE : this.mask + 1;
         this.locks = new AtomicReferenceArray(this.size);
         this.supplier = supplier;
      }

      public L getAt(int index) {
         if (this.size != Integer.MAX_VALUE) {
            Preconditions.checkElementIndex(index, this.size());
         }

         Striped.SmallLazyStriped.ArrayReference<? extends L> existingRef = (Striped.SmallLazyStriped.ArrayReference)this.locks.get(index);
         L existing = existingRef == null ? null : existingRef.get();
         if (existing != null) {
            return existing;
         } else {
            L created = this.supplier.get();
            Striped.SmallLazyStriped.ArrayReference newRef = new Striped.SmallLazyStriped.ArrayReference(created, index, this.queue);

            do {
               if (this.locks.compareAndSet(index, existingRef, newRef)) {
                  this.drainQueue();
                  return created;
               }

               existingRef = (Striped.SmallLazyStriped.ArrayReference)this.locks.get(index);
               existing = existingRef == null ? null : existingRef.get();
            } while(existing == null);

            return existing;
         }
      }

      private void drainQueue() {
         Reference ref;
         while((ref = this.queue.poll()) != null) {
            Striped.SmallLazyStriped.ArrayReference<? extends L> arrayRef = (Striped.SmallLazyStriped.ArrayReference)ref;
            this.locks.compareAndSet(arrayRef.index, arrayRef, (Object)null);
         }

      }

      public int size() {
         return this.size;
      }

      private static final class ArrayReference<L> extends WeakReference<L> {
         final int index;

         ArrayReference(L referent, int index, ReferenceQueue<L> queue) {
            super(referent, queue);
            this.index = index;
         }
      }
   }

   private static class CompactStriped<L> extends Striped.PowerOfTwoStriped<L> {
      private final Object[] array;

      private CompactStriped(int stripes, Supplier<L> supplier) {
         super(stripes);
         Preconditions.checkArgument(stripes <= 1073741824, "Stripes must be <= 2^30)");
         this.array = new Object[this.mask + 1];

         for(int i = 0; i < this.array.length; ++i) {
            this.array[i] = supplier.get();
         }

      }

      public L getAt(int index) {
         return this.array[index];
      }

      public int size() {
         return this.array.length;
      }

      // $FF: synthetic method
      CompactStriped(int x0, Supplier x1, Object x2) {
         this(x0, x1);
      }
   }

   private abstract static class PowerOfTwoStriped<L> extends Striped<L> {
      final int mask;

      PowerOfTwoStriped(int stripes) {
         super(null);
         Preconditions.checkArgument(stripes > 0, "Stripes must be positive");
         this.mask = stripes > 1073741824 ? -1 : Striped.ceilToPowerOfTwo(stripes) - 1;
      }

      final int indexFor(Object key) {
         int hash = Striped.smear(key.hashCode());
         return hash & this.mask;
      }

      public final L get(Object key) {
         return this.getAt(this.indexFor(key));
      }
   }
}
