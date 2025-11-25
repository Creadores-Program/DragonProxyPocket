package io.netty.channel;

import io.netty.util.concurrent.ExecutorServiceFactory;
import io.netty.util.concurrent.MultithreadEventExecutorGroup;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.concurrent.Executor;

public abstract class MultithreadEventLoopGroup extends MultithreadEventExecutorGroup implements EventLoopGroup {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(MultithreadEventLoopGroup.class);
   private static final int DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads", Runtime.getRuntime().availableProcessors() * 2));

   protected MultithreadEventLoopGroup(int nEventLoops, Executor executor, Object... args) {
      super(nEventLoops == 0 ? DEFAULT_EVENT_LOOP_THREADS : nEventLoops, executor, args);
   }

   protected MultithreadEventLoopGroup(int nEventLoops, ExecutorServiceFactory executorServiceFactory, Object... args) {
      super(nEventLoops == 0 ? DEFAULT_EVENT_LOOP_THREADS : nEventLoops, executorServiceFactory, args);
   }

   public EventLoop next() {
      return (EventLoop)super.next();
   }

   protected abstract EventLoop newChild(Executor var1, Object... var2) throws Exception;

   public ChannelFuture register(Channel channel) {
      return this.next().register(channel);
   }

   public ChannelFuture register(Channel channel, ChannelPromise promise) {
      return this.next().register(channel, promise);
   }

   static {
      if (logger.isDebugEnabled()) {
         logger.debug("-Dio.netty.eventLoopThreads: {}", (Object)DEFAULT_EVENT_LOOP_THREADS);
      }

   }
}
