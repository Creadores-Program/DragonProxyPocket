package io.netty.channel;

import io.netty.util.concurrent.AbstractEventExecutorGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ThreadPerTaskExecutor;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.PlatformDependent;
import java.util.Collections;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ThreadPerChannelEventLoopGroup extends AbstractEventExecutorGroup implements EventLoopGroup {
   private final Object[] childArgs;
   private final int maxChannels;
   final Executor executor;
   final Set<EventLoop> activeChildren;
   private final Set<EventLoop> readOnlyActiveChildren;
   final Queue<EventLoop> idleChildren;
   private final ChannelException tooManyChannels;
   private volatile boolean shuttingDown;
   private final Promise<?> terminationFuture;
   private final FutureListener<Object> childTerminationListener;

   protected ThreadPerChannelEventLoopGroup() {
      this(0);
   }

   protected ThreadPerChannelEventLoopGroup(int maxChannels) {
      this(maxChannels, Executors.defaultThreadFactory());
   }

   protected ThreadPerChannelEventLoopGroup(int maxChannels, ThreadFactory threadFactory, Object... args) {
      this(maxChannels, (Executor)(new ThreadPerTaskExecutor(threadFactory)), args);
   }

   protected ThreadPerChannelEventLoopGroup(int maxChannels, Executor executor, Object... args) {
      this.activeChildren = Collections.newSetFromMap(PlatformDependent.newConcurrentHashMap());
      this.readOnlyActiveChildren = Collections.unmodifiableSet(this.activeChildren);
      this.idleChildren = new ConcurrentLinkedQueue();
      this.terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);
      this.childTerminationListener = new FutureListener<Object>() {
         public void operationComplete(Future<Object> future) throws Exception {
            if (ThreadPerChannelEventLoopGroup.this.isTerminated()) {
               ThreadPerChannelEventLoopGroup.this.terminationFuture.trySuccess((Object)null);
            }

         }
      };
      if (maxChannels < 0) {
         throw new IllegalArgumentException(String.format("maxChannels: %d (expected: >= 0)", maxChannels));
      } else if (executor == null) {
         throw new NullPointerException("executor");
      } else {
         if (args == null) {
            this.childArgs = EmptyArrays.EMPTY_OBJECTS;
         } else {
            this.childArgs = (Object[])args.clone();
         }

         this.maxChannels = maxChannels;
         this.executor = executor;
         this.tooManyChannels = new ChannelException("too many channels (max: " + maxChannels + ')');
         this.tooManyChannels.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
      }
   }

   protected EventLoop newChild(Object... args) throws Exception {
      return new ThreadPerChannelEventLoop(this);
   }

   public <E extends EventExecutor> Set<E> children() {
      return this.readOnlyActiveChildren;
   }

   public EventLoop next() {
      throw new UnsupportedOperationException();
   }

   public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
      this.shuttingDown = true;
      Iterator i$ = this.activeChildren.iterator();

      EventLoop l;
      while(i$.hasNext()) {
         l = (EventLoop)i$.next();
         l.shutdownGracefully(quietPeriod, timeout, unit);
      }

      i$ = this.idleChildren.iterator();

      while(i$.hasNext()) {
         l = (EventLoop)i$.next();
         l.shutdownGracefully(quietPeriod, timeout, unit);
      }

      if (this.isTerminated()) {
         this.terminationFuture.trySuccess((Object)null);
      }

      return this.terminationFuture();
   }

   public Future<?> terminationFuture() {
      return this.terminationFuture;
   }

   /** @deprecated */
   @Deprecated
   public void shutdown() {
      this.shuttingDown = true;
      Iterator i$ = this.activeChildren.iterator();

      EventLoop l;
      while(i$.hasNext()) {
         l = (EventLoop)i$.next();
         l.shutdown();
      }

      i$ = this.idleChildren.iterator();

      while(i$.hasNext()) {
         l = (EventLoop)i$.next();
         l.shutdown();
      }

      if (this.isTerminated()) {
         this.terminationFuture.trySuccess((Object)null);
      }

   }

   public boolean isShuttingDown() {
      Iterator i$ = this.activeChildren.iterator();

      EventLoop l;
      do {
         if (!i$.hasNext()) {
            i$ = this.idleChildren.iterator();

            do {
               if (!i$.hasNext()) {
                  return true;
               }

               l = (EventLoop)i$.next();
            } while(l.isShuttingDown());

            return false;
         }

         l = (EventLoop)i$.next();
      } while(l.isShuttingDown());

      return false;
   }

   public boolean isShutdown() {
      Iterator i$ = this.activeChildren.iterator();

      EventLoop l;
      do {
         if (!i$.hasNext()) {
            i$ = this.idleChildren.iterator();

            do {
               if (!i$.hasNext()) {
                  return true;
               }

               l = (EventLoop)i$.next();
            } while(l.isShutdown());

            return false;
         }

         l = (EventLoop)i$.next();
      } while(l.isShutdown());

      return false;
   }

   public boolean isTerminated() {
      Iterator i$ = this.activeChildren.iterator();

      EventLoop l;
      do {
         if (!i$.hasNext()) {
            i$ = this.idleChildren.iterator();

            do {
               if (!i$.hasNext()) {
                  return true;
               }

               l = (EventLoop)i$.next();
            } while(l.isTerminated());

            return false;
         }

         l = (EventLoop)i$.next();
      } while(l.isTerminated());

      return false;
   }

   public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      long deadline = System.nanoTime() + unit.toNanos(timeout);
      Iterator i$ = this.activeChildren.iterator();

      EventLoop l;
      long timeLeft;
      while(i$.hasNext()) {
         l = (EventLoop)i$.next();

         while(true) {
            timeLeft = deadline - System.nanoTime();
            if (timeLeft <= 0L) {
               return this.isTerminated();
            }

            if (l.awaitTermination(timeLeft, TimeUnit.NANOSECONDS)) {
               break;
            }
         }
      }

      i$ = this.idleChildren.iterator();

      while(i$.hasNext()) {
         l = (EventLoop)i$.next();

         while(true) {
            timeLeft = deadline - System.nanoTime();
            if (timeLeft <= 0L) {
               return this.isTerminated();
            }

            if (l.awaitTermination(timeLeft, TimeUnit.NANOSECONDS)) {
               break;
            }
         }
      }

      return this.isTerminated();
   }

   public ChannelFuture register(Channel channel) {
      if (channel == null) {
         throw new NullPointerException("channel");
      } else {
         try {
            EventLoop l = this.nextChild();
            return l.register(channel, new DefaultChannelPromise(channel, l));
         } catch (Throwable var3) {
            return new FailedChannelFuture(channel, GlobalEventExecutor.INSTANCE, var3);
         }
      }
   }

   public ChannelFuture register(Channel channel, ChannelPromise promise) {
      if (channel == null) {
         throw new NullPointerException("channel");
      } else {
         try {
            return this.nextChild().register(channel, promise);
         } catch (Throwable var4) {
            promise.setFailure(var4);
            return promise;
         }
      }
   }

   private EventLoop nextChild() throws Exception {
      if (this.shuttingDown) {
         throw new RejectedExecutionException("shutting down");
      } else {
         EventLoop loop = (EventLoop)this.idleChildren.poll();
         if (loop == null) {
            if (this.maxChannels > 0 && this.activeChildren.size() >= this.maxChannels) {
               throw this.tooManyChannels;
            }

            loop = this.newChild(this.childArgs);
            loop.terminationFuture().addListener(this.childTerminationListener);
         }

         this.activeChildren.add(loop);
         return loop;
      }
   }
}
