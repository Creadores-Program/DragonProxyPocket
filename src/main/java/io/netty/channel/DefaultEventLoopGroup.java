package io.netty.channel;

import io.netty.util.concurrent.ExecutorServiceFactory;
import java.util.concurrent.Executor;

public class DefaultEventLoopGroup extends MultithreadEventLoopGroup {
   public DefaultEventLoopGroup() {
      this(0);
   }

   public DefaultEventLoopGroup(int nEventLoops) {
      this(nEventLoops, (Executor)null);
   }

   public DefaultEventLoopGroup(int nEventLoops, Executor executor) {
      super(nEventLoops, executor);
   }

   public DefaultEventLoopGroup(int nEventLoops, ExecutorServiceFactory executorServiceFactory) {
      super(nEventLoops, executorServiceFactory);
   }

   protected EventLoop newChild(Executor executor, Object... args) throws Exception {
      return new DefaultEventLoop(this, executor);
   }
}
