package io.netty.util.concurrent;

import io.netty.util.internal.CallableEventExecutorAdapter;
import io.netty.util.internal.OneTimeTask;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

final class ScheduledFutureTask<V> extends PromiseTask<V> implements ScheduledFuture<V> {
   private static final AtomicLong nextTaskId = new AtomicLong();
   private static final long START_TIME = System.nanoTime();
   private final long id;
   private long deadlineNanos;
   private final long periodNanos;

   static long nanoTime() {
      return System.nanoTime() - START_TIME;
   }

   static long deadlineNanos(long delay) {
      return nanoTime() + delay;
   }

   ScheduledFutureTask(EventExecutor executor, Callable<V> callable, long nanoTime, long period) {
      super(executor.unwrap(), callable);
      this.id = nextTaskId.getAndIncrement();
      if (period == 0L) {
         throw new IllegalArgumentException("period: 0 (expected: != 0)");
      } else {
         this.deadlineNanos = nanoTime;
         this.periodNanos = period;
      }
   }

   ScheduledFutureTask(EventExecutor executor, Callable<V> callable, long nanoTime) {
      super(executor.unwrap(), callable);
      this.id = nextTaskId.getAndIncrement();
      this.deadlineNanos = nanoTime;
      this.periodNanos = 0L;
   }

   public long deadlineNanos() {
      return this.deadlineNanos;
   }

   public long delayNanos() {
      return Math.max(0L, this.deadlineNanos() - nanoTime());
   }

   public long delayNanos(long currentTimeNanos) {
      return Math.max(0L, this.deadlineNanos() - (currentTimeNanos - START_TIME));
   }

   public long getDelay(TimeUnit unit) {
      return unit.convert(this.delayNanos(), TimeUnit.NANOSECONDS);
   }

   public int compareTo(Delayed o) {
      if (this == o) {
         return 0;
      } else {
         ScheduledFutureTask<?> that = (ScheduledFutureTask)o;
         long d = this.deadlineNanos() - that.deadlineNanos();
         if (d < 0L) {
            return -1;
         } else if (d > 0L) {
            return 1;
         } else if (this.id < that.id) {
            return -1;
         } else if (this.id == that.id) {
            throw new Error();
         } else {
            return 1;
         }
      }
   }

   public void run() {
      assert this.executor().inEventLoop();

      try {
         if (this.isMigrationPending()) {
            this.scheduleWithNewExecutor();
         } else if (this.needsLaterExecution()) {
            if (!this.executor().isShutdown()) {
               this.deadlineNanos = nanoTime() + TimeUnit.MICROSECONDS.toNanos(10L);
               if (!this.isCancelled()) {
                  Queue<ScheduledFutureTask<?>> scheduledTaskQueue = ((AbstractScheduledEventExecutor)this.executor()).scheduledTaskQueue;

                  assert scheduledTaskQueue != null;

                  scheduledTaskQueue.add(this);
               }
            }
         } else if (this.periodNanos == 0L) {
            if (this.setUncancellableInternal()) {
               V result = this.task.call();
               this.setSuccessInternal(result);
            }
         } else if (!this.isCancelled()) {
            this.task.call();
            if (!this.executor().isShutdown()) {
               long p = this.periodNanos;
               if (p > 0L) {
                  this.deadlineNanos += p;
               } else {
                  this.deadlineNanos = nanoTime() - p;
               }

               if (!this.isCancelled()) {
                  Queue<ScheduledFutureTask<?>> scheduledTaskQueue = ((AbstractScheduledEventExecutor)this.executor()).scheduledTaskQueue;

                  assert scheduledTaskQueue != null;

                  scheduledTaskQueue.add(this);
               }
            }
         }
      } catch (Throwable var4) {
         this.setFailureInternal(var4);
      }

   }

   protected StringBuilder toStringBuilder() {
      StringBuilder buf = super.toStringBuilder();
      buf.setCharAt(buf.length() - 1, ',');
      return buf.append(" id: ").append(this.id).append(", deadline: ").append(this.deadlineNanos).append(", period: ").append(this.periodNanos).append(')');
   }

   private boolean needsLaterExecution() {
      return this.task instanceof CallableEventExecutorAdapter && ((CallableEventExecutorAdapter)this.task).executor() instanceof PausableEventExecutor && !((PausableEventExecutor)((CallableEventExecutorAdapter)this.task).executor()).isAcceptingNewTasks();
   }

   private boolean isMigrationPending() {
      return !this.isCancelled() && this.task instanceof CallableEventExecutorAdapter && this.executor() != ((CallableEventExecutorAdapter)this.task).executor().unwrap();
   }

   private void scheduleWithNewExecutor() {
      EventExecutor newExecutor = ((CallableEventExecutorAdapter)this.task).executor().unwrap();
      if (newExecutor instanceof SingleThreadEventExecutor) {
         if (!newExecutor.isShutdown()) {
            this.executor = newExecutor;
            final Queue<ScheduledFutureTask<?>> scheduledTaskQueue = ((SingleThreadEventExecutor)newExecutor).scheduledTaskQueue();
            this.executor.execute(new OneTimeTask() {
               public void run() {
                  ScheduledFutureTask.this.deadlineNanos = ScheduledFutureTask.nanoTime();
                  if (!ScheduledFutureTask.this.isCancelled()) {
                     scheduledTaskQueue.add(ScheduledFutureTask.this);
                  }

               }
            });
         }

      } else {
         throw new UnsupportedOperationException("task migration unsupported");
      }
   }
}
