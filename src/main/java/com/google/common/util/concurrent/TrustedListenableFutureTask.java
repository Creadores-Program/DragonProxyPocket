package com.google.common.util.concurrent;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.RunnableFuture;
import javax.annotation.Nullable;

@GwtCompatible
class TrustedListenableFutureTask<V> extends AbstractFuture.TrustedFuture<V> implements RunnableFuture<V> {
   private TrustedListenableFutureTask<V>.TrustedFutureInterruptibleTask task;

   static <V> TrustedListenableFutureTask<V> create(Callable<V> callable) {
      return new TrustedListenableFutureTask(callable);
   }

   static <V> TrustedListenableFutureTask<V> create(Runnable runnable, @Nullable V result) {
      return new TrustedListenableFutureTask(Executors.callable(runnable, result));
   }

   TrustedListenableFutureTask(Callable<V> callable) {
      this.task = new TrustedListenableFutureTask.TrustedFutureInterruptibleTask(callable);
   }

   public void run() {
      TrustedListenableFutureTask<V>.TrustedFutureInterruptibleTask localTask = this.task;
      if (localTask != null) {
         localTask.run();
      }

   }

   final void done() {
      super.done();
      this.task = null;
   }

   @GwtIncompatible("Interruption not supported")
   protected final void interruptTask() {
      TrustedListenableFutureTask<V>.TrustedFutureInterruptibleTask localTask = this.task;
      if (localTask != null) {
         localTask.interruptTask();
      }

   }

   private final class TrustedFutureInterruptibleTask extends InterruptibleTask {
      private final Callable<V> callable;

      TrustedFutureInterruptibleTask(Callable<V> callable) {
         this.callable = (Callable)Preconditions.checkNotNull(callable);
      }

      void runInterruptibly() {
         if (!TrustedListenableFutureTask.this.isDone()) {
            try {
               TrustedListenableFutureTask.this.set(this.callable.call());
            } catch (Throwable var2) {
               TrustedListenableFutureTask.this.setException(var2);
            }
         }

      }

      boolean wasInterrupted() {
         return TrustedListenableFutureTask.this.wasInterrupted();
      }
   }
}
