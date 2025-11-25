package io.netty.util.concurrent;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GlobalEventExecutor extends AbstractScheduledEventExecutor {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(GlobalEventExecutor.class);
   private static final long SCHEDULE_PURGE_INTERVAL;
   public static final GlobalEventExecutor INSTANCE;
   final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue();
   final ScheduledFutureTask<Void> purgeTask;
   private final ThreadFactory threadFactory;
   private final GlobalEventExecutor.TaskRunner taskRunner;
   private final AtomicBoolean started;
   volatile Thread thread;
   private final Future<?> terminationFuture;

   private GlobalEventExecutor() {
      this.purgeTask = new ScheduledFutureTask(this, Executors.callable(new GlobalEventExecutor.PurgeTask(), (Object)null), ScheduledFutureTask.deadlineNanos(SCHEDULE_PURGE_INTERVAL), -SCHEDULE_PURGE_INTERVAL);
      this.threadFactory = new DefaultThreadFactory(this.getClass());
      this.taskRunner = new GlobalEventExecutor.TaskRunner();
      this.started = new AtomicBoolean();
      this.terminationFuture = new FailedFuture(this, new UnsupportedOperationException());
      this.scheduledTaskQueue().add(this.purgeTask);
   }

   Runnable takeTask() {
      BlockingQueue taskQueue = this.taskQueue;

      Runnable task;
      do {
         ScheduledFutureTask<?> scheduledTask = this.peekScheduledTask();
         if (scheduledTask == null) {
            Runnable task = null;

            try {
               task = (Runnable)taskQueue.take();
            } catch (InterruptedException var7) {
            }

            return task;
         }

         long delayNanos = scheduledTask.delayNanos();
         if (delayNanos > 0L) {
            try {
               task = (Runnable)taskQueue.poll(delayNanos, TimeUnit.NANOSECONDS);
            } catch (InterruptedException var8) {
               return null;
            }
         } else {
            task = (Runnable)taskQueue.poll();
         }

         if (task == null) {
            this.fetchFromScheduledTaskQueue();
            task = (Runnable)taskQueue.poll();
         }
      } while(task == null);

      return task;
   }

   private void fetchFromScheduledTaskQueue() {
      if (this.hasScheduledTasks()) {
         long nanoTime = AbstractScheduledEventExecutor.nanoTime();

         while(true) {
            Runnable scheduledTask = this.pollScheduledTask(nanoTime);
            if (scheduledTask == null) {
               break;
            }

            this.taskQueue.add(scheduledTask);
         }
      }

   }

   public int pendingTasks() {
      return this.taskQueue.size();
   }

   private void addTask(Runnable task) {
      if (task == null) {
         throw new NullPointerException("task");
      } else {
         this.taskQueue.add(task);
      }
   }

   public boolean inEventLoop(Thread thread) {
      return thread == this.thread;
   }

   public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
      return this.terminationFuture();
   }

   public Future<?> terminationFuture() {
      return this.terminationFuture;
   }

   /** @deprecated */
   @Deprecated
   public void shutdown() {
      throw new UnsupportedOperationException();
   }

   public boolean isShuttingDown() {
      return false;
   }

   public boolean isShutdown() {
      return false;
   }

   public boolean isTerminated() {
      return false;
   }

   public boolean awaitTermination(long timeout, TimeUnit unit) {
      return false;
   }

   public boolean awaitInactivity(long timeout, TimeUnit unit) throws InterruptedException {
      if (unit == null) {
         throw new NullPointerException("unit");
      } else {
         Thread thread = this.thread;
         if (thread == null) {
            throw new IllegalStateException("thread was not started");
         } else {
            thread.join(unit.toMillis(timeout));
            return !thread.isAlive();
         }
      }
   }

   public void execute(Runnable task) {
      if (task == null) {
         throw new NullPointerException("task");
      } else {
         this.addTask(task);
         if (!this.inEventLoop()) {
            this.startThread();
         }

      }
   }

   private void startThread() {
      if (this.started.compareAndSet(false, true)) {
         Thread t = this.threadFactory.newThread(this.taskRunner);
         t.start();
         this.thread = t;
      }

   }

   static {
      SCHEDULE_PURGE_INTERVAL = TimeUnit.SECONDS.toNanos(1L);
      INSTANCE = new GlobalEventExecutor();
   }

   private final class PurgeTask implements Runnable {
      private PurgeTask() {
      }

      public void run() {
         GlobalEventExecutor.this.purgeCancelledScheduledTasks();
      }

      // $FF: synthetic method
      PurgeTask(Object x1) {
         this();
      }
   }

   final class TaskRunner implements Runnable {
      public void run() {
         while(true) {
            Runnable task = GlobalEventExecutor.this.takeTask();
            if (task != null) {
               try {
                  task.run();
               } catch (Throwable var4) {
                  GlobalEventExecutor.logger.warn("Unexpected exception from the global event executor: ", var4);
               }

               if (task != GlobalEventExecutor.this.purgeTask) {
                  continue;
               }
            }

            Queue<ScheduledFutureTask<?>> scheduledTaskQueue = GlobalEventExecutor.this.scheduledTaskQueue;
            if (GlobalEventExecutor.this.taskQueue.isEmpty() && (scheduledTaskQueue == null || scheduledTaskQueue.size() == 1)) {
               boolean stopped = GlobalEventExecutor.this.started.compareAndSet(true, false);

               assert stopped;

               if (GlobalEventExecutor.this.taskQueue.isEmpty() && (scheduledTaskQueue == null || scheduledTaskQueue.size() == 1) || !GlobalEventExecutor.this.started.compareAndSet(false, true)) {
                  return;
               }
            }
         }
      }
   }
}
