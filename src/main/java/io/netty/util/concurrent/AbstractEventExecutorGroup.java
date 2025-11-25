package io.netty.util.concurrent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class AbstractEventExecutorGroup implements EventExecutorGroup {
   public Future<?> submit(Runnable task) {
      return this.next().submit(task);
   }

   public <T> Future<T> submit(Runnable task, T result) {
      return this.next().submit(task, result);
   }

   public <T> Future<T> submit(Callable<T> task) {
      return this.next().submit(task);
   }

   public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
      return this.next().schedule(command, delay, unit);
   }

   public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
      return this.next().schedule(callable, delay, unit);
   }

   public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
      return this.next().scheduleAtFixedRate(command, initialDelay, period, unit);
   }

   public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
      return this.next().scheduleWithFixedDelay(command, initialDelay, delay, unit);
   }

   public Future<?> shutdownGracefully() {
      return this.shutdownGracefully(2L, 15L, TimeUnit.SECONDS);
   }

   /** @deprecated */
   @Deprecated
   public abstract void shutdown();

   /** @deprecated */
   @Deprecated
   public List<Runnable> shutdownNow() {
      this.shutdown();
      return Collections.emptyList();
   }

   public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
      return this.next().invokeAll(tasks);
   }

   public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
      return this.next().invokeAll(tasks, timeout, unit);
   }

   public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
      return this.next().invokeAny(tasks);
   }

   public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
      return this.next().invokeAny(tasks, timeout, unit);
   }

   public void execute(Runnable command) {
      this.next().execute(command);
   }

   public void close() throws Exception {
      this.shutdownGracefully().syncUninterruptibly();
   }
}
