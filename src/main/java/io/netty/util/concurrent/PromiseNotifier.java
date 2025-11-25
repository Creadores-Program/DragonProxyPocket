package io.netty.util.concurrent;

public class PromiseNotifier<V, F extends Future<V>> implements GenericFutureListener<F> {
   private final Promise<? super V>[] promises;

   @SafeVarargs
   public PromiseNotifier(Promise<? super V>... promises) {
      if (promises == null) {
         throw new NullPointerException("promises");
      } else {
         Promise[] arr$ = promises;
         int len$ = promises.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            Promise<? super V> promise = arr$[i$];
            if (promise == null) {
               throw new IllegalArgumentException("promises contains null Promise");
            }
         }

         this.promises = (Promise[])promises.clone();
      }
   }

   public void operationComplete(F future) throws Exception {
      Promise[] arr$;
      int len$;
      int i$;
      Promise p;
      if (future.isSuccess()) {
         V result = future.get();
         arr$ = this.promises;
         len$ = arr$.length;

         for(i$ = 0; i$ < len$; ++i$) {
            p = arr$[i$];
            p.setSuccess(result);
         }

      } else {
         Throwable cause = future.cause();
         arr$ = this.promises;
         len$ = arr$.length;

         for(i$ = 0; i$ < len$; ++i$) {
            p = arr$[i$];
            p.setFailure(cause);
         }

      }
   }
}
