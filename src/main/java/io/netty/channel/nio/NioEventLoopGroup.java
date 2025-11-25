package io.netty.channel.nio;

import io.netty.channel.EventLoop;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.ExecutorServiceFactory;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.concurrent.Executor;

public class NioEventLoopGroup extends MultithreadEventLoopGroup {
   public NioEventLoopGroup() {
      this(0);
   }

   public NioEventLoopGroup(int nEventLoops) {
      this(nEventLoops, (Executor)null);
   }

   public NioEventLoopGroup(int nEventLoops, Executor executor) {
      this(nEventLoops, executor, SelectorProvider.provider());
   }

   public NioEventLoopGroup(int nEventLoops, ExecutorServiceFactory executorServiceFactory) {
      this(nEventLoops, executorServiceFactory, SelectorProvider.provider());
   }

   public NioEventLoopGroup(int nEventLoops, Executor executor, SelectorProvider selectorProvider) {
      super(nEventLoops, executor, selectorProvider);
   }

   public NioEventLoopGroup(int nEventLoops, ExecutorServiceFactory executorServiceFactory, SelectorProvider selectorProvider) {
      super(nEventLoops, executorServiceFactory, selectorProvider);
   }

   public void setIoRatio(int ioRatio) {
      Iterator i$ = this.children().iterator();

      while(i$.hasNext()) {
         EventExecutor e = (EventExecutor)i$.next();
         ((NioEventLoop)e).setIoRatio(ioRatio);
      }

   }

   public void rebuildSelectors() {
      Iterator i$ = this.children().iterator();

      while(i$.hasNext()) {
         EventExecutor e = (EventExecutor)i$.next();
         ((NioEventLoop)e).rebuildSelector();
      }

   }

   protected EventLoop newChild(Executor executor, Object... args) throws Exception {
      return new NioEventLoop(this, executor, (SelectorProvider)args[0]);
   }
}
