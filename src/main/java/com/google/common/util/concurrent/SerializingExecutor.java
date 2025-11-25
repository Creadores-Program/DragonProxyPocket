package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.GuardedBy;

final class SerializingExecutor implements Executor {
   private static final Logger log = Logger.getLogger(SerializingExecutor.class.getName());
   private final Executor executor;
   @GuardedBy("internalLock")
   private final Deque<Runnable> queue = new ArrayDeque();
   @GuardedBy("internalLock")
   private boolean isWorkerRunning = false;
   @GuardedBy("internalLock")
   private int suspensions = 0;
   private final Object internalLock = new Object();

   public SerializingExecutor(Executor executor) {
      this.executor = (Executor)Preconditions.checkNotNull(executor);
   }

   public void execute(Runnable task) {
      synchronized(this.internalLock) {
         this.queue.add(task);
      }

      this.startQueueWorker();
   }

   public void executeFirst(Runnable task) {
      synchronized(this.internalLock) {
         this.queue.addFirst(task);
      }

      this.startQueueWorker();
   }

   public void suspend() {
      synchronized(this.internalLock) {
         ++this.suspensions;
      }
   }

   public void resume() {
      synchronized(this.internalLock) {
         Preconditions.checkState(this.suspensions > 0);
         --this.suspensions;
      }

      this.startQueueWorker();
   }

   private void startQueueWorker() {
      synchronized(this.internalLock) {
         if (this.queue.peek() == null) {
            return;
         }

         if (this.suspensions > 0) {
            return;
         }

         if (this.isWorkerRunning) {
            return;
         }

         this.isWorkerRunning = true;
      }

      boolean executionRejected = true;
      boolean var11 = false;

      try {
         var11 = true;
         this.executor.execute(new SerializingExecutor.QueueWorker());
         executionRejected = false;
         var11 = false;
      } finally {
         if (var11) {
            if (executionRejected) {
               synchronized(this.internalLock) {
                  this.isWorkerRunning = false;
               }
            }

         }
      }

      if (executionRejected) {
         synchronized(this.internalLock) {
            this.isWorkerRunning = false;
         }
      }

   }

   private final class QueueWorker implements Runnable {
      private QueueWorker() {
      }

      public void run() {
         try {
            this.workOnQueue();
         } catch (Error var5) {
            synchronized(SerializingExecutor.this.internalLock) {
               SerializingExecutor.this.isWorkerRunning = false;
            }

            throw var5;
         }
      }

      private void workOnQueue() {
         while(true) {
            Runnable task = null;
            synchronized(SerializingExecutor.this.internalLock) {
               if (SerializingExecutor.this.suspensions == 0) {
                  task = (Runnable)SerializingExecutor.this.queue.poll();
               }

               if (task == null) {
                  SerializingExecutor.this.isWorkerRunning = false;
                  return;
               }
            }

            try {
               task.run();
            } catch (RuntimeException var4) {
               SerializingExecutor.log.log(Level.SEVERE, "Exception while executing runnable " + task, var4);
            }
         }
      }

      // $FF: synthetic method
      QueueWorker(Object x1) {
         this();
      }
   }
}
