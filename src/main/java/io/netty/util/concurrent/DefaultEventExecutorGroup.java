package io.netty.util.concurrent;

import java.util.concurrent.Executor;

public class DefaultEventExecutorGroup extends MultithreadEventExecutorGroup {
   public DefaultEventExecutorGroup(int nEventExecutors) {
      this(nEventExecutors, (Executor)null);
   }

   public DefaultEventExecutorGroup(int nEventExecutors, Executor executor) {
      super(nEventExecutors, executor);
   }

   public DefaultEventExecutorGroup(int nEventExecutors, ExecutorServiceFactory executorServiceFactory) {
      super(nEventExecutors, executorServiceFactory);
   }

   protected EventExecutor newChild(Executor executor, Object... args) throws Exception {
      return new DefaultEventExecutor(this, executor);
   }
}
