package io.netty.util.concurrent;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class PromiseAggregator<V, F extends Future<V>> implements GenericFutureListener<F> {
   private final Promise<?> aggregatePromise;
   private final boolean failPending;
   private Set<Promise<V>> pendingPromises;

   public PromiseAggregator(Promise<Void> aggregatePromise, boolean failPending) {
      if (aggregatePromise == null) {
         throw new NullPointerException("aggregatePromise");
      } else {
         this.aggregatePromise = aggregatePromise;
         this.failPending = failPending;
      }
   }

   public PromiseAggregator(Promise<Void> aggregatePromise) {
      this(aggregatePromise, true);
   }

   @SafeVarargs
   public final PromiseAggregator<V, F> add(Promise<V>... promises) {
      if (promises == null) {
         throw new NullPointerException("promises");
      } else if (promises.length == 0) {
         return this;
      } else {
         synchronized(this) {
            if (this.pendingPromises == null) {
               int size;
               if (promises.length > 1) {
                  size = promises.length;
               } else {
                  size = 2;
               }

               this.pendingPromises = new LinkedHashSet(size);
            }

            Promise[] arr$ = promises;
            int len$ = promises.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               Promise<V> p = arr$[i$];
               if (p != null) {
                  this.pendingPromises.add(p);
                  p.addListener(this);
               }
            }

            return this;
         }
      }
   }

   public synchronized void operationComplete(F future) throws Exception {
      if (this.pendingPromises == null) {
         this.aggregatePromise.setSuccess((Object)null);
      } else {
         this.pendingPromises.remove(future);
         if (!future.isSuccess()) {
            Throwable cause = future.cause();
            this.aggregatePromise.setFailure(cause);
            if (this.failPending) {
               Iterator i$ = this.pendingPromises.iterator();

               while(i$.hasNext()) {
                  Promise<V> pendingFuture = (Promise)i$.next();
                  pendingFuture.setFailure(cause);
               }
            }
         } else if (this.pendingPromises.isEmpty()) {
            this.aggregatePromise.setSuccess((Object)null);
         }
      }

   }
}
