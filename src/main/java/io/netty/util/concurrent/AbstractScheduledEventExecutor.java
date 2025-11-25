package io.netty.util.concurrent;

import io.netty.util.internal.CallableEventExecutorAdapter;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.RunnableEventExecutorAdapter;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class AbstractScheduledEventExecutor extends AbstractEventExecutor {
   Queue<ScheduledFutureTask<?>> scheduledTaskQueue;

   protected AbstractScheduledEventExecutor() {
   }

   protected AbstractScheduledEventExecutor(EventExecutorGroup parent) {
      super(parent);
   }

   protected static long nanoTime() {
      return ScheduledFutureTask.nanoTime();
   }

   Queue<ScheduledFutureTask<?>> scheduledTaskQueue() {
      if (this.scheduledTaskQueue == null) {
         this.scheduledTaskQueue = new PriorityQueue();
      }

      return this.scheduledTaskQueue;
   }

   private static boolean isNullOrEmpty(Queue<ScheduledFutureTask<?>> queue) {
      return queue == null || queue.isEmpty();
   }

   protected void cancelScheduledTasks() {
      assert this.inEventLoop();

      Queue<ScheduledFutureTask<?>> scheduledTaskQueue = this.scheduledTaskQueue;
      if (!isNullOrEmpty(scheduledTaskQueue)) {
         ScheduledFutureTask<?>[] scheduledTasks = (ScheduledFutureTask[])scheduledTaskQueue.toArray(new ScheduledFutureTask[scheduledTaskQueue.size()]);
         ScheduledFutureTask[] arr$ = scheduledTasks;
         int len$ = scheduledTasks.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            ScheduledFutureTask<?> task = arr$[i$];
            task.cancel(false);
         }

         scheduledTaskQueue.clear();
      }
   }

   protected final Runnable pollScheduledTask() {
      return this.pollScheduledTask(nanoTime());
   }

   protected final Runnable pollScheduledTask(long nanoTime) {
      assert this.inEventLoop();

      Queue<ScheduledFutureTask<?>> scheduledTaskQueue = this.scheduledTaskQueue;
      ScheduledFutureTask<?> scheduledTask = scheduledTaskQueue == null ? null : (ScheduledFutureTask)scheduledTaskQueue.peek();
      if (scheduledTask == null) {
         return null;
      } else if (scheduledTask.deadlineNanos() <= nanoTime) {
         scheduledTaskQueue.remove();
         return scheduledTask;
      } else {
         return null;
      }
   }

   protected final long nextScheduledTaskNano() {
      Queue<ScheduledFutureTask<?>> scheduledTaskQueue = this.scheduledTaskQueue;
      ScheduledFutureTask<?> scheduledTask = scheduledTaskQueue == null ? null : (ScheduledFutureTask)scheduledTaskQueue.peek();
      return scheduledTask == null ? -1L : Math.max(0L, scheduledTask.deadlineNanos() - nanoTime());
   }

   final ScheduledFutureTask<?> peekScheduledTask() {
      Queue<ScheduledFutureTask<?>> scheduledTaskQueue = this.scheduledTaskQueue;
      return scheduledTaskQueue == null ? null : (ScheduledFutureTask)scheduledTaskQueue.peek();
   }

   protected final boolean hasScheduledTasks() {
      Queue<ScheduledFutureTask<?>> scheduledTaskQueue = this.scheduledTaskQueue;
      ScheduledFutureTask<?> scheduledTask = scheduledTaskQueue == null ? null : (ScheduledFutureTask)scheduledTaskQueue.peek();
      return scheduledTask != null && scheduledTask.deadlineNanos() <= nanoTime();
   }

   public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
      ObjectUtil.checkNotNull(command, "command");
      ObjectUtil.checkNotNull(unit, "unit");
      if (delay < 0L) {
         throw new IllegalArgumentException(String.format("delay: %d (expected: >= 0)", delay));
      } else {
         return this.schedule(new ScheduledFutureTask(this, toCallable(command), ScheduledFutureTask.deadlineNanos(unit.toNanos(delay))));
      }
   }

   public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
      ObjectUtil.checkNotNull(callable, "callable");
      ObjectUtil.checkNotNull(unit, "unit");
      if (delay < 0L) {
         throw new IllegalArgumentException(String.format("delay: %d (expected: >= 0)", delay));
      } else {
         return this.schedule(new ScheduledFutureTask(this, callable, ScheduledFutureTask.deadlineNanos(unit.toNanos(delay))));
      }
   }

   public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
      ObjectUtil.checkNotNull(command, "command");
      ObjectUtil.checkNotNull(unit, "unit");
      if (initialDelay < 0L) {
         throw new IllegalArgumentException(String.format("initialDelay: %d (expected: >= 0)", initialDelay));
      } else if (period <= 0L) {
         throw new IllegalArgumentException(String.format("period: %d (expected: > 0)", period));
      } else {
         return this.schedule(new ScheduledFutureTask(this, toCallable(command), ScheduledFutureTask.deadlineNanos(unit.toNanos(initialDelay)), unit.toNanos(period)));
      }
   }

   public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
      ObjectUtil.checkNotNull(command, "command");
      ObjectUtil.checkNotNull(unit, "unit");
      if (initialDelay < 0L) {
         throw new IllegalArgumentException(String.format("initialDelay: %d (expected: >= 0)", initialDelay));
      } else if (delay <= 0L) {
         throw new IllegalArgumentException(String.format("delay: %d (expected: > 0)", delay));
      } else {
         return this.schedule(new ScheduledFutureTask(this, toCallable(command), ScheduledFutureTask.deadlineNanos(unit.toNanos(initialDelay)), -unit.toNanos(delay)));
      }
   }

   <V> ScheduledFuture<V> schedule(final ScheduledFutureTask<V> task) {
      if (this.inEventLoop()) {
         this.scheduledTaskQueue().add(task);
      } else {
         this.execute(new Runnable() {
            public void run() {
               AbstractScheduledEventExecutor.this.scheduledTaskQueue().add(task);
            }
         });
      }

      return task;
   }

   void purgeCancelledScheduledTasks() {
      Queue<ScheduledFutureTask<?>> scheduledTaskQueue = this.scheduledTaskQueue;
      if (!isNullOrEmpty(scheduledTaskQueue)) {
         Iterator i = scheduledTaskQueue.iterator();

         while(i.hasNext()) {
            ScheduledFutureTask<?> task = (ScheduledFutureTask)i.next();
            if (task.isCancelled()) {
               i.remove();
            }
         }

      }
   }

   private static Callable<Void> toCallable(Runnable command) {
      return (Callable)(command instanceof RunnableEventExecutorAdapter ? new AbstractScheduledEventExecutor.RunnableToCallableAdapter((RunnableEventExecutorAdapter)command) : Executors.callable(command, (Object)null));
   }

   private static class RunnableToCallableAdapter implements CallableEventExecutorAdapter<Void> {
      final RunnableEventExecutorAdapter runnable;

      RunnableToCallableAdapter(RunnableEventExecutorAdapter runnable) {
         this.runnable = runnable;
      }

      public EventExecutor executor() {
         return this.runnable.executor();
      }

      public Callable<Void> unwrap() {
         return null;
      }

      public Void call() throws Exception {
         this.runnable.run();
         return null;
      }
   }
}
