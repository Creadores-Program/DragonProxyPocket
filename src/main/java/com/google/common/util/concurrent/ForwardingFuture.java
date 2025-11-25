package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.collect.ForwardingObject;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class ForwardingFuture<V> extends ForwardingObject implements Future<V> {
   protected ForwardingFuture() {
   }

   protected abstract Future<V> delegate();

   public boolean cancel(boolean mayInterruptIfRunning) {
      return this.delegate().cancel(mayInterruptIfRunning);
   }

   public boolean isCancelled() {
      return this.delegate().isCancelled();
   }

   public boolean isDone() {
      return this.delegate().isDone();
   }

   public V get() throws InterruptedException, ExecutionException {
      return this.delegate().get();
   }

   public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return this.delegate().get(timeout, unit);
   }

   public abstract static class SimpleForwardingFuture<V> extends ForwardingFuture<V> {
      private final Future<V> delegate;

      protected SimpleForwardingFuture(Future<V> delegate) {
         this.delegate = (Future)Preconditions.checkNotNull(delegate);
      }

      protected final Future<V> delegate() {
         return this.delegate;
      }
   }
}
