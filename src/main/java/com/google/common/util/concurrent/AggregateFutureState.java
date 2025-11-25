package com.google.common.util.concurrent;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Sets;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

@GwtCompatible(
   emulated = true
)
abstract class AggregateFutureState {
   private static final AtomicReferenceFieldUpdater<AggregateFutureState, Set<Throwable>> SEEN_EXCEPTIONS_UDPATER = AtomicReferenceFieldUpdater.newUpdater(AggregateFutureState.class, Set.class, "seenExceptions");
   private static final AtomicIntegerFieldUpdater<AggregateFutureState> REMAINING_COUNT_UPDATER = AtomicIntegerFieldUpdater.newUpdater(AggregateFutureState.class, "remaining");
   private volatile Set<Throwable> seenExceptions = null;
   private volatile int remaining;

   AggregateFutureState(int remainingFutures) {
      this.remaining = remainingFutures;
   }

   final Set<Throwable> getOrInitSeenExceptions() {
      Set<Throwable> seenExceptionsLocal = this.seenExceptions;
      if (seenExceptionsLocal == null) {
         seenExceptionsLocal = Sets.newConcurrentHashSet();
         this.addInitialException(seenExceptionsLocal);
         SEEN_EXCEPTIONS_UDPATER.compareAndSet(this, (Object)null, seenExceptionsLocal);
         seenExceptionsLocal = this.seenExceptions;
      }

      return seenExceptionsLocal;
   }

   abstract void addInitialException(Set<Throwable> var1);

   final int decrementRemainingAndGet() {
      return REMAINING_COUNT_UPDATER.decrementAndGet(this);
   }
}
