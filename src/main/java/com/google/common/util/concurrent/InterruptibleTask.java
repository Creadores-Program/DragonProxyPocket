package com.google.common.util.concurrent;

import com.google.common.annotations.GwtCompatible;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

@GwtCompatible(
   emulated = true
)
abstract class InterruptibleTask implements Runnable {
   private static final AtomicReferenceFieldUpdater<InterruptibleTask, Thread> RUNNER = AtomicReferenceFieldUpdater.newUpdater(InterruptibleTask.class, Thread.class, "runner");
   private volatile Thread runner;
   private volatile boolean doneInterrupting;

   public final void run() {
      if (RUNNER.compareAndSet(this, (Object)null, Thread.currentThread())) {
         try {
            this.runInterruptibly();
         } finally {
            if (this.wasInterrupted()) {
               while(!this.doneInterrupting) {
                  Thread.yield();
               }
            }

         }

      }
   }

   abstract void runInterruptibly();

   abstract boolean wasInterrupted();

   final void interruptTask() {
      Thread currentRunner = this.runner;
      if (currentRunner != null) {
         currentRunner.interrupt();
      }

      this.doneInterrupting = true;
   }
}
