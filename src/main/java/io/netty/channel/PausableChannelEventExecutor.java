package io.netty.channel;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.PausableEventExecutor;
import io.netty.util.concurrent.ProgressivePromise;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.CallableEventExecutorAdapter;
import io.netty.util.internal.RunnableEventExecutorAdapter;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

abstract class PausableChannelEventExecutor implements PausableEventExecutor, ChannelHandlerInvoker {
   abstract Channel channel();

   abstract ChannelHandlerInvoker unwrapInvoker();

   public void invokeFlush(ChannelHandlerContext ctx) {
      this.unwrapInvoker().invokeFlush(ctx);
   }

   public EventExecutor executor() {
      return this;
   }

   public void invokeChannelRegistered(ChannelHandlerContext ctx) {
      this.unwrapInvoker().invokeChannelRegistered(ctx);
   }

   public void invokeChannelUnregistered(ChannelHandlerContext ctx) {
      this.unwrapInvoker().invokeChannelUnregistered(ctx);
   }

   public void invokeChannelActive(ChannelHandlerContext ctx) {
      this.unwrapInvoker().invokeChannelActive(ctx);
   }

   public void invokeChannelInactive(ChannelHandlerContext ctx) {
      this.unwrapInvoker().invokeChannelInactive(ctx);
   }

   public void invokeExceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      this.unwrapInvoker().invokeExceptionCaught(ctx, cause);
   }

   public void invokeUserEventTriggered(ChannelHandlerContext ctx, Object event) {
      this.unwrapInvoker().invokeUserEventTriggered(ctx, event);
   }

   public void invokeChannelRead(ChannelHandlerContext ctx, Object msg) {
      this.unwrapInvoker().invokeChannelRead(ctx, msg);
   }

   public void invokeChannelReadComplete(ChannelHandlerContext ctx) {
      this.unwrapInvoker().invokeChannelReadComplete(ctx);
   }

   public void invokeChannelWritabilityChanged(ChannelHandlerContext ctx) {
      this.unwrapInvoker().invokeChannelWritabilityChanged(ctx);
   }

   public void invokeBind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) {
      this.unwrapInvoker().invokeBind(ctx, localAddress, promise);
   }

   public void invokeConnect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
      this.unwrapInvoker().invokeConnect(ctx, remoteAddress, localAddress, promise);
   }

   public void invokeDisconnect(ChannelHandlerContext ctx, ChannelPromise promise) {
      this.unwrapInvoker().invokeDisconnect(ctx, promise);
   }

   public void invokeClose(ChannelHandlerContext ctx, ChannelPromise promise) {
      this.unwrapInvoker().invokeClose(ctx, promise);
   }

   public void invokeDeregister(ChannelHandlerContext ctx, ChannelPromise promise) {
      this.unwrapInvoker().invokeDeregister(ctx, promise);
   }

   public void invokeRead(ChannelHandlerContext ctx) {
      this.unwrapInvoker().invokeRead(ctx);
   }

   public void invokeWrite(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
      this.unwrapInvoker().invokeWrite(ctx, msg, promise);
   }

   public EventExecutor next() {
      return this.unwrap().next();
   }

   public <E extends EventExecutor> Set<E> children() {
      return this.unwrap().children();
   }

   public EventExecutorGroup parent() {
      return this.unwrap().parent();
   }

   public boolean inEventLoop() {
      return this.unwrap().inEventLoop();
   }

   public boolean inEventLoop(Thread thread) {
      return this.unwrap().inEventLoop(thread);
   }

   public <V> Promise<V> newPromise() {
      return this.unwrap().newPromise();
   }

   public <V> ProgressivePromise<V> newProgressivePromise() {
      return this.unwrap().newProgressivePromise();
   }

   public <V> Future<V> newSucceededFuture(V result) {
      return this.unwrap().newSucceededFuture(result);
   }

   public <V> Future<V> newFailedFuture(Throwable cause) {
      return this.unwrap().newFailedFuture(cause);
   }

   public boolean isShuttingDown() {
      return this.unwrap().isShuttingDown();
   }

   public Future<?> shutdownGracefully() {
      return this.unwrap().shutdownGracefully();
   }

   public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
      return this.unwrap().shutdownGracefully(quietPeriod, timeout, unit);
   }

   public Future<?> terminationFuture() {
      return this.unwrap().terminationFuture();
   }

   /** @deprecated */
   @Deprecated
   public void shutdown() {
      this.unwrap().shutdown();
   }

   /** @deprecated */
   @Deprecated
   public List<Runnable> shutdownNow() {
      return this.unwrap().shutdownNow();
   }

   public Future<?> submit(Runnable task) {
      if (!this.isAcceptingNewTasks()) {
         throw new RejectedExecutionException();
      } else {
         return this.unwrap().submit(task);
      }
   }

   public <T> Future<T> submit(Runnable task, T result) {
      if (!this.isAcceptingNewTasks()) {
         throw new RejectedExecutionException();
      } else {
         return this.unwrap().submit(task, result);
      }
   }

   public <T> Future<T> submit(Callable<T> task) {
      if (!this.isAcceptingNewTasks()) {
         throw new RejectedExecutionException();
      } else {
         return this.unwrap().submit(task);
      }
   }

   public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
      if (!this.isAcceptingNewTasks()) {
         throw new RejectedExecutionException();
      } else {
         return this.unwrap().schedule(new PausableChannelEventExecutor.ChannelRunnableEventExecutor(this.channel(), command), delay, unit);
      }
   }

   public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
      if (!this.isAcceptingNewTasks()) {
         throw new RejectedExecutionException();
      } else {
         return this.unwrap().schedule(new PausableChannelEventExecutor.ChannelCallableEventExecutor(this.channel(), callable), delay, unit);
      }
   }

   public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
      if (!this.isAcceptingNewTasks()) {
         throw new RejectedExecutionException();
      } else {
         return this.unwrap().scheduleAtFixedRate(new PausableChannelEventExecutor.ChannelRunnableEventExecutor(this.channel(), command), initialDelay, period, unit);
      }
   }

   public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
      if (!this.isAcceptingNewTasks()) {
         throw new RejectedExecutionException();
      } else {
         return this.unwrap().scheduleWithFixedDelay(new PausableChannelEventExecutor.ChannelRunnableEventExecutor(this.channel(), command), initialDelay, delay, unit);
      }
   }

   public boolean isShutdown() {
      return this.unwrap().isShutdown();
   }

   public boolean isTerminated() {
      return this.unwrap().isTerminated();
   }

   public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      return this.unwrap().awaitTermination(timeout, unit);
   }

   public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
      if (!this.isAcceptingNewTasks()) {
         throw new RejectedExecutionException();
      } else {
         return this.unwrap().invokeAll(tasks);
      }
   }

   public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
      if (!this.isAcceptingNewTasks()) {
         throw new RejectedExecutionException();
      } else {
         return this.unwrap().invokeAll(tasks, timeout, unit);
      }
   }

   public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
      if (!this.isAcceptingNewTasks()) {
         throw new RejectedExecutionException();
      } else {
         return this.unwrap().invokeAny(tasks);
      }
   }

   public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      if (!this.isAcceptingNewTasks()) {
         throw new RejectedExecutionException();
      } else {
         return this.unwrap().invokeAny(tasks, timeout, unit);
      }
   }

   public void execute(Runnable command) {
      if (!this.isAcceptingNewTasks()) {
         throw new RejectedExecutionException();
      } else {
         this.unwrap().execute(command);
      }
   }

   public void close() throws Exception {
      this.unwrap().close();
   }

   private static final class ChannelRunnableEventExecutor implements RunnableEventExecutorAdapter {
      final Channel channel;
      final Runnable runnable;

      ChannelRunnableEventExecutor(Channel channel, Runnable runnable) {
         this.channel = channel;
         this.runnable = runnable;
      }

      public EventExecutor executor() {
         return this.channel.eventLoop();
      }

      public Runnable unwrap() {
         return this.runnable;
      }

      public void run() {
         this.runnable.run();
      }
   }

   private static final class ChannelCallableEventExecutor<V> implements CallableEventExecutorAdapter<V> {
      final Channel channel;
      final Callable<V> callable;

      ChannelCallableEventExecutor(Channel channel, Callable<V> callable) {
         this.channel = channel;
         this.callable = callable;
      }

      public EventExecutor executor() {
         return this.channel.eventLoop();
      }

      public Callable unwrap() {
         return this.callable;
      }

      public V call() throws Exception {
         return this.callable.call();
      }
   }
}
