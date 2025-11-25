package io.netty.channel.epoll;

import io.netty.channel.EventLoop;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ExecutorServiceFactory;
import java.util.Iterator;
import java.util.concurrent.Executor;

public final class EpollEventLoopGroup extends MultithreadEventLoopGroup {
   public EpollEventLoopGroup() {
      this(0);
   }

   public EpollEventLoopGroup(int nEventLoops) {
      this(nEventLoops, (Executor)null);
   }

   public EpollEventLoopGroup(int nEventLoops, Executor executor) {
      this(nEventLoops, (Executor)executor, 0);
   }

   public EpollEventLoopGroup(int nEventLoops, ExecutorServiceFactory executorServiceFactory) {
      this(nEventLoops, (ExecutorServiceFactory)executorServiceFactory, 0);
   }

   /** @deprecated */
   @Deprecated
   public EpollEventLoopGroup(int nEventLoops, Executor executor, int maxEventsAtOnce) {
      super(nEventLoops, executor, maxEventsAtOnce);
   }

   /** @deprecated */
   @Deprecated
   public EpollEventLoopGroup(int nEventLoops, ExecutorServiceFactory executorServiceFactory, int maxEventsAtOnce) {
      super(nEventLoops, executorServiceFactory, maxEventsAtOnce);
   }

   public void setIoRatio(int ioRatio) {
      Iterator i$ = this.children().iterator();

      while(i$.hasNext()) {
         EventExecutor e = (EventExecutor)i$.next();
         ((EpollEventLoop)e).setIoRatio(ioRatio);
      }

   }

   protected EventLoop newChild(Executor executor, Object... args) throws Exception {
      return new EpollEventLoop(this, executor, (Integer)args[0]);
   }
}
