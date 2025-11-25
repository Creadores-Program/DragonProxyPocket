package io.netty.util.concurrent;

import java.util.Set;

public interface EventExecutor extends EventExecutorGroup {
   EventExecutor next();

   <E extends EventExecutor> Set<E> children();

   EventExecutorGroup parent();

   boolean inEventLoop();

   boolean inEventLoop(Thread var1);

   EventExecutor unwrap();

   <V> Promise<V> newPromise();

   <V> ProgressivePromise<V> newProgressivePromise();

   <V> Future<V> newSucceededFuture(V var1);

   <V> Future<V> newFailedFuture(Throwable var1);
}
