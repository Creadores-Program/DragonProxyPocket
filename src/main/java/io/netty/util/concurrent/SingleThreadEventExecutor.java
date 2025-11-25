package io.netty.util.concurrent;

import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public abstract class SingleThreadEventExecutor extends AbstractScheduledEventExecutor {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(SingleThreadEventExecutor.class);
   private static final int ST_NOT_STARTED = 1;
   private static final int ST_STARTED = 2;
   private static final int ST_SHUTTING_DOWN = 3;
   private static final int ST_SHUTDOWN = 4;
   private static final int ST_TERMINATED = 5;
   private static final Runnable WAKEUP_TASK = new Runnable() {
      public void run() {
      }
   };
   private static final AtomicIntegerFieldUpdater<SingleThreadEventExecutor> STATE_UPDATER;
   private static final AtomicReferenceFieldUpdater<SingleThreadEventExecutor, Thread> THREAD_UPDATER;
   private final Queue<Runnable> taskQueue;
   private volatile Thread thread;
   private final Executor executor;
   private final Semaphore threadLock = new Semaphore(0);
   private final Set<Runnable> shutdownHooks = new LinkedHashSet();
   private final boolean addTaskWakesUp;
   private long lastExecutionTime;
   private volatile int state = 1;
   private volatile long gracefulShutdownQuietPeriod;
   private volatile long gracefulShutdownTimeout;
   private long gracefulShutdownStartTime;
   private final Promise<?> terminationFuture;
   private boolean firstRun;
   private final Runnable asRunnable;
   private static final long SCHEDULE_PURGE_INTERVAL;

   protected SingleThreadEventExecutor(EventExecutorGroup parent, Executor executor, boolean addTaskWakesUp) {
      super(parent);
      this.terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);
      this.firstRun = true;
      this.asRunnable = new Runnable() {
         public void run() {
            SingleThreadEventExecutor.this.updateThread(Thread.currentThread());
            if (SingleThreadEventExecutor.this.firstRun) {
               SingleThreadEventExecutor.this.firstRun = false;
               SingleThreadEventExecutor.this.updateLastExecutionTime();
            }

            try {
               SingleThreadEventExecutor.this.run();
            } catch (Throwable var2) {
               SingleThreadEventExecutor.logger.warn("Unexpected exception from an event executor: ", var2);
               SingleThreadEventExecutor.this.cleanupAndTerminate(false);
            }

         }
      };
      if (executor == null) {
         throw new NullPointerException("executor");
      } else {
         this.addTaskWakesUp = addTaskWakesUp;
         this.executor = executor;
         this.taskQueue = this.newTaskQueue();
      }
   }

   protected Queue<Runnable> newTaskQueue() {
      return new LinkedBlockingQueue();
   }

   protected Runnable pollTask() {
      assert this.inEventLoop();

      Runnable task;
      do {
         task = (Runnable)this.taskQueue.poll();
      } while(task == WAKEUP_TASK);

      return task;
   }

   protected Runnable takeTask() {
      assert this.inEventLoop();

      if (!(this.taskQueue instanceof BlockingQueue)) {
         throw new UnsupportedOperationException();
      } else {
         BlockingQueue taskQueue = (BlockingQueue)this.taskQueue;

         Runnable task;
         do {
            ScheduledFutureTask<?> scheduledTask = this.peekScheduledTask();
            if (scheduledTask == null) {
               Runnable task = null;

               try {
                  task = (Runnable)taskQueue.take();
                  if (task == WAKEUP_TASK) {
                     task = null;
                  }
               } catch (InterruptedException var7) {
               }

               return task;
            }

            long delayNanos = scheduledTask.delayNanos();
            task = null;
            if (delayNanos > 0L) {
               try {
                  task = (Runnable)taskQueue.poll(delayNanos, TimeUnit.NANOSECONDS);
               } catch (InterruptedException var8) {
                  return null;
               }
            }

            if (task == null) {
               this.fetchFromScheduledTaskQueue();
               task = (Runnable)taskQueue.poll();
            }
         } while(task == null);

         return task;
      }
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

   protected Runnable peekTask() {
      assert this.inEventLoop();

      return (Runnable)this.taskQueue.peek();
   }

   protected boolean hasTasks() {
      assert this.inEventLoop();

      return !this.taskQueue.isEmpty();
   }

   public final int pendingTasks() {
      return this.taskQueue.size();
   }

   protected void addTask(Runnable task) {
      if (task == null) {
         throw new NullPointerException("task");
      } else {
         if (this.isShutdown()) {
            reject();
         }

         this.taskQueue.add(task);
      }
   }

   protected boolean removeTask(Runnable task) {
      if (task == null) {
         throw new NullPointerException("task");
      } else {
         return this.taskQueue.remove(task);
      }
   }

   protected boolean runAllTasks() {
      this.fetchFromScheduledTaskQueue();
      Runnable task = this.pollTask();
      if (task == null) {
         return false;
      } else {
         do {
            try {
               task.run();
            } catch (Throwable var3) {
               logger.warn("A task raised an exception.", var3);
            }

            task = this.pollTask();
         } while(task != null);

         this.lastExecutionTime = ScheduledFutureTask.nanoTime();
         return true;
      }
   }

   protected boolean runAllTasks(long timeoutNanos) {
      this.fetchFromScheduledTaskQueue();
      Runnable task = this.pollTask();
      if (task == null) {
         return false;
      } else {
         long deadline = ScheduledFutureTask.nanoTime() + timeoutNanos;
         long runTasks = 0L;

         long lastExecutionTime;
         while(true) {
            try {
               task.run();
            } catch (Throwable var11) {
               logger.warn("A task raised an exception.", var11);
            }

            ++runTasks;
            if ((runTasks & 63L) == 0L) {
               lastExecutionTime = ScheduledFutureTask.nanoTime();
               if (lastExecutionTime >= deadline) {
                  break;
               }
            }

            task = this.pollTask();
            if (task == null) {
               lastExecutionTime = ScheduledFutureTask.nanoTime();
               break;
            }
         }

         this.lastExecutionTime = lastExecutionTime;
         return true;
      }
   }

   protected long delayNanos(long currentTimeNanos) {
      ScheduledFutureTask<?> scheduledTask = this.peekScheduledTask();
      return scheduledTask == null ? SCHEDULE_PURGE_INTERVAL : scheduledTask.delayNanos(currentTimeNanos);
   }

   protected void updateLastExecutionTime() {
      this.lastExecutionTime = ScheduledFutureTask.nanoTime();
   }

   protected abstract void run();

   protected void cleanup() {
   }

   protected void wakeup(boolean inEventLoop) {
      if (!inEventLoop || STATE_UPDATER.get(this) == 3) {
         this.taskQueue.add(WAKEUP_TASK);
      }

   }

   public boolean inEventLoop(Thread thread) {
      return thread == this.thread;
   }

   public void addShutdownHook(final Runnable task) {
      if (this.inEventLoop()) {
         this.shutdownHooks.add(task);
      } else {
         this.execute(new Runnable() {
            public void run() {
               SingleThreadEventExecutor.this.shutdownHooks.add(task);
            }
         });
      }

   }

   public void removeShutdownHook(final Runnable task) {
      if (this.inEventLoop()) {
         this.shutdownHooks.remove(task);
      } else {
         this.execute(new Runnable() {
            public void run() {
               SingleThreadEventExecutor.this.shutdownHooks.remove(task);
            }
         });
      }

   }

   private boolean runShutdownHooks() {
      boolean ran = false;

      while(!this.shutdownHooks.isEmpty()) {
         List<Runnable> copy = new ArrayList(this.shutdownHooks);
         this.shutdownHooks.clear();
         Iterator i$ = copy.iterator();

         while(i$.hasNext()) {
            Runnable task = (Runnable)i$.next();

            try {
               task.run();
            } catch (Throwable var9) {
               logger.warn("Shutdown hook raised an exception.", var9);
            } finally {
               ran = true;
            }
         }
      }

      if (ran) {
         this.lastExecutionTime = ScheduledFutureTask.nanoTime();
      }

      return ran;
   }

   public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
      if (quietPeriod < 0L) {
         throw new IllegalArgumentException("quietPeriod: " + quietPeriod + " (expected >= 0)");
      } else if (timeout < quietPeriod) {
         throw new IllegalArgumentException("timeout: " + timeout + " (expected >= quietPeriod (" + quietPeriod + "))");
      } else if (unit == null) {
         throw new NullPointerException("unit");
      } else if (this.isShuttingDown()) {
         return this.terminationFuture();
      } else {
         boolean inEventLoop = this.inEventLoop();

         boolean wakeup;
         int oldState;
         int newState;
         do {
            if (this.isShuttingDown()) {
               return this.terminationFuture();
            }

            wakeup = true;
            oldState = STATE_UPDATER.get(this);
            if (inEventLoop) {
               newState = 3;
            } else {
               switch(oldState) {
               case 1:
               case 2:
                  newState = 3;
                  break;
               default:
                  newState = oldState;
                  wakeup = false;
               }
            }
         } while(!STATE_UPDATER.compareAndSet(this, oldState, newState));

         this.gracefulShutdownQuietPeriod = unit.toNanos(quietPeriod);
         this.gracefulShutdownTimeout = unit.toNanos(timeout);
         if (oldState == 1) {
            this.scheduleExecution();
         }

         if (wakeup) {
            this.wakeup(inEventLoop);
         }

         return this.terminationFuture();
      }
   }

   public Future<?> terminationFuture() {
      return this.terminationFuture;
   }

   /** @deprecated */
   @Deprecated
   public void shutdown() {
      if (!this.isShutdown()) {
         boolean inEventLoop = this.inEventLoop();

         boolean wakeup;
         int oldState;
         int newState;
         do {
            if (this.isShuttingDown()) {
               return;
            }

            wakeup = true;
            oldState = STATE_UPDATER.get(this);
            if (inEventLoop) {
               newState = 4;
            } else {
               switch(oldState) {
               case 1:
               case 2:
               case 3:
                  newState = 4;
                  break;
               default:
                  newState = oldState;
                  wakeup = false;
               }
            }
         } while(!STATE_UPDATER.compareAndSet(this, oldState, newState));

         if (oldState == 1) {
            this.scheduleExecution();
         }

         if (wakeup) {
            this.wakeup(inEventLoop);
         }

      }
   }

   public boolean isShuttingDown() {
      return STATE_UPDATER.get(this) >= 3;
   }

   public boolean isShutdown() {
      return STATE_UPDATER.get(this) >= 4;
   }

   public boolean isTerminated() {
      return STATE_UPDATER.get(this) == 5;
   }

   protected boolean confirmShutdown() {
      if (!this.isShuttingDown()) {
         return false;
      } else if (!this.inEventLoop()) {
         throw new IllegalStateException("must be invoked from an event loop");
      } else {
         this.cancelScheduledTasks();
         if (this.gracefulShutdownStartTime == 0L) {
            this.gracefulShutdownStartTime = ScheduledFutureTask.nanoTime();
         }

         if (!this.runAllTasks() && !this.runShutdownHooks()) {
            long nanoTime = ScheduledFutureTask.nanoTime();
            if (!this.isShutdown() && nanoTime - this.gracefulShutdownStartTime <= this.gracefulShutdownTimeout) {
               if (nanoTime - this.lastExecutionTime <= this.gracefulShutdownQuietPeriod) {
                  this.wakeup(true);

                  try {
                     Thread.sleep(100L);
                  } catch (InterruptedException var4) {
                  }

                  return false;
               } else {
                  return true;
               }
            } else {
               return true;
            }
         } else if (this.isShutdown()) {
            return true;
         } else {
            this.wakeup(true);
            return false;
         }
      }
   }

   public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      if (unit == null) {
         throw new NullPointerException("unit");
      } else if (this.inEventLoop()) {
         throw new IllegalStateException("cannot await termination of the current thread");
      } else {
         if (this.threadLock.tryAcquire(timeout, unit)) {
            this.threadLock.release();
         }

         return this.isTerminated();
      }
   }

   public void execute(Runnable task) {
      if (task == null) {
         throw new NullPointerException("task");
      } else {
         boolean inEventLoop = this.inEventLoop();
         if (inEventLoop) {
            this.addTask(task);
         } else {
            this.startExecution();
            this.addTask(task);
            if (this.isShutdown() && this.removeTask(task)) {
               reject();
            }
         }

         if (!this.addTaskWakesUp && this.wakesUpForTask(task)) {
            this.wakeup(inEventLoop);
         }

      }
   }

   protected boolean wakesUpForTask(Runnable task) {
      return true;
   }

   protected static void reject() {
      throw new RejectedExecutionException("event executor terminated");
   }

   protected void cleanupAndTerminate(boolean success) {
      int oldState;
      do {
         oldState = STATE_UPDATER.get(this);
      } while(oldState < 3 && !STATE_UPDATER.compareAndSet(this, oldState, 3));

      if (success && this.gracefulShutdownStartTime == 0L) {
         logger.error("Buggy " + EventExecutor.class.getSimpleName() + " implementation; " + SingleThreadEventExecutor.class.getSimpleName() + ".confirmShutdown() must be called " + "before run() implementation terminates.");
      }

      try {
         while(!this.confirmShutdown()) {
         }
      } finally {
         try {
            this.cleanup();
         } finally {
            STATE_UPDATER.set(this, 5);
            this.threadLock.release();
            if (!this.taskQueue.isEmpty()) {
               logger.warn("An event executor terminated with non-empty task queue (" + this.taskQueue.size() + ')');
            }

            this.firstRun = true;
            this.terminationFuture.setSuccess((Object)null);
         }
      }

   }

   private void startExecution() {
      if (STATE_UPDATER.get(this) == 1 && STATE_UPDATER.compareAndSet(this, 1, 2)) {
         this.schedule(new ScheduledFutureTask(this, Executors.callable(new SingleThreadEventExecutor.PurgeTask(), (Object)null), ScheduledFutureTask.deadlineNanos(SCHEDULE_PURGE_INTERVAL), -SCHEDULE_PURGE_INTERVAL));
         this.scheduleExecution();
      }

   }

   protected final void scheduleExecution() {
      this.updateThread((Thread)null);
      this.executor.execute(this.asRunnable);
   }

   private void updateThread(Thread t) {
      THREAD_UPDATER.lazySet(this, t);
   }

   static {
      AtomicIntegerFieldUpdater<SingleThreadEventExecutor> updater = PlatformDependent.newAtomicIntegerFieldUpdater(SingleThreadEventExecutor.class, "state");
      if (updater == null) {
         updater = AtomicIntegerFieldUpdater.newUpdater(SingleThreadEventExecutor.class, "state");
      }

      STATE_UPDATER = updater;
      AtomicReferenceFieldUpdater<SingleThreadEventExecutor, Thread> refUpdater = PlatformDependent.newAtomicReferenceFieldUpdater(SingleThreadEventExecutor.class, "thread");
      if (refUpdater == null) {
         refUpdater = AtomicReferenceFieldUpdater.newUpdater(SingleThreadEventExecutor.class, Thread.class, "thread");
      }

      THREAD_UPDATER = refUpdater;
      SCHEDULE_PURGE_INTERVAL = TimeUnit.SECONDS.toNanos(1L);
   }

   private final class PurgeTask implements Runnable {
      private PurgeTask() {
      }

      public void run() {
         SingleThreadEventExecutor.this.purgeCancelledScheduledTasks();
      }

      // $FF: synthetic method
      PurgeTask(Object x1) {
         this();
      }
   }
}
