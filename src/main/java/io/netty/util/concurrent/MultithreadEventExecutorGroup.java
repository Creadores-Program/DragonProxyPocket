package io.netty.util.concurrent;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MultithreadEventExecutorGroup extends AbstractEventExecutorGroup {
   private final EventExecutor[] children;
   private final Set<EventExecutor> readonlyChildren;
   private final AtomicInteger childIndex;
   private final AtomicInteger terminatedChildren;
   private final Promise<?> terminationFuture;
   private final MultithreadEventExecutorGroup.EventExecutorChooser chooser;

   protected MultithreadEventExecutorGroup(int nEventExecutors, ExecutorServiceFactory executorServiceFactory, Object... args) {
      this(nEventExecutors, executorServiceFactory != null ? executorServiceFactory.newExecutorService(nEventExecutors) : null, true, args);
   }

   protected MultithreadEventExecutorGroup(int nEventExecutors, Executor executor, Object... args) {
      this(nEventExecutors, executor, false, args);
   }

   private MultithreadEventExecutorGroup(int nEventExecutors, final Executor executor, final boolean shutdownExecutor, Object... args) {
      this.childIndex = new AtomicInteger();
      this.terminatedChildren = new AtomicInteger();
      this.terminationFuture = new DefaultPromise(GlobalEventExecutor.INSTANCE);
      if (nEventExecutors <= 0) {
         throw new IllegalArgumentException(String.format("nEventExecutors: %d (expected: > 0)", nEventExecutors));
      } else {
         if (executor == null) {
            executor = this.newDefaultExecutorService(nEventExecutors);
            shutdownExecutor = true;
         }

         this.children = new EventExecutor[nEventExecutors];
         if (isPowerOfTwo(this.children.length)) {
            this.chooser = new MultithreadEventExecutorGroup.PowerOfTwoEventExecutorChooser();
         } else {
            this.chooser = new MultithreadEventExecutorGroup.GenericEventExecutorChooser();
         }

         for(int i = 0; i < nEventExecutors; ++i) {
            boolean success = false;
            boolean var18 = false;

            try {
               var18 = true;
               this.children[i] = this.newChild((Executor)executor, args);
               success = true;
               var18 = false;
            } catch (Exception var19) {
               throw new IllegalStateException("failed to create a child event loop", var19);
            } finally {
               if (var18) {
                  if (!success) {
                     int j;
                     for(j = 0; j < i; ++j) {
                        this.children[j].shutdownGracefully();
                     }

                     for(j = 0; j < i; ++j) {
                        EventExecutor e = this.children[j];

                        try {
                           while(!e.isTerminated()) {
                              e.awaitTermination(2147483647L, TimeUnit.SECONDS);
                           }
                        } catch (InterruptedException var20) {
                           Thread.currentThread().interrupt();
                           break;
                        }
                     }
                  }

               }
            }

            if (!success) {
               int j;
               for(j = 0; j < i; ++j) {
                  this.children[j].shutdownGracefully();
               }

               for(j = 0; j < i; ++j) {
                  EventExecutor e = this.children[j];

                  try {
                     while(!e.isTerminated()) {
                        e.awaitTermination(2147483647L, TimeUnit.SECONDS);
                     }
                  } catch (InterruptedException var22) {
                     Thread.currentThread().interrupt();
                     break;
                  }
               }
            }
         }

         FutureListener<Object> terminationListener = new FutureListener<Object>() {
            public void operationComplete(Future<Object> future) throws Exception {
               if (MultithreadEventExecutorGroup.this.terminatedChildren.incrementAndGet() == MultithreadEventExecutorGroup.this.children.length) {
                  MultithreadEventExecutorGroup.this.terminationFuture.setSuccess((Object)null);
                  if (shutdownExecutor) {
                     ((ExecutorService)executor).shutdown();
                  }
               }

            }
         };
         EventExecutor[] arr$ = this.children;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            EventExecutor e = arr$[i$];
            e.terminationFuture().addListener(terminationListener);
         }

         Set<EventExecutor> childrenSet = new LinkedHashSet(this.children.length);
         Collections.addAll(childrenSet, this.children);
         this.readonlyChildren = Collections.unmodifiableSet(childrenSet);
      }
   }

   protected ExecutorService newDefaultExecutorService(int nEventExecutors) {
      return (new DefaultExecutorServiceFactory(this.getClass())).newExecutorService(nEventExecutors);
   }

   public EventExecutor next() {
      return this.chooser.next();
   }

   public final int executorCount() {
      return this.children.length;
   }

   public final <E extends EventExecutor> Set<E> children() {
      return this.readonlyChildren;
   }

   protected abstract EventExecutor newChild(Executor var1, Object... var2) throws Exception;

   public Future<?> shutdownGracefully(long quietPeriod, long timeout, TimeUnit unit) {
      EventExecutor[] arr$ = this.children;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         EventExecutor l = arr$[i$];
         l.shutdownGracefully(quietPeriod, timeout, unit);
      }

      return this.terminationFuture();
   }

   public Future<?> terminationFuture() {
      return this.terminationFuture;
   }

   /** @deprecated */
   @Deprecated
   public void shutdown() {
      EventExecutor[] arr$ = this.children;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         EventExecutor l = arr$[i$];
         l.shutdown();
      }

   }

   public boolean isShuttingDown() {
      EventExecutor[] arr$ = this.children;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         EventExecutor l = arr$[i$];
         if (!l.isShuttingDown()) {
            return false;
         }
      }

      return true;
   }

   public boolean isShutdown() {
      EventExecutor[] arr$ = this.children;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         EventExecutor l = arr$[i$];
         if (!l.isShutdown()) {
            return false;
         }
      }

      return true;
   }

   public boolean isTerminated() {
      EventExecutor[] arr$ = this.children;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         EventExecutor l = arr$[i$];
         if (!l.isTerminated()) {
            return false;
         }
      }

      return true;
   }

   public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      long deadline = System.nanoTime() + unit.toNanos(timeout);
      EventExecutor[] arr$ = this.children;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         EventExecutor l = arr$[i$];

         long timeLeft;
         do {
            timeLeft = deadline - System.nanoTime();
            if (timeLeft <= 0L) {
               return this.isTerminated();
            }
         } while(!l.awaitTermination(timeLeft, TimeUnit.NANOSECONDS));
      }

      return this.isTerminated();
   }

   private static boolean isPowerOfTwo(int val) {
      return (val & -val) == val;
   }

   private final class GenericEventExecutorChooser implements MultithreadEventExecutorGroup.EventExecutorChooser {
      private GenericEventExecutorChooser() {
      }

      public EventExecutor next() {
         return MultithreadEventExecutorGroup.this.children[Math.abs(MultithreadEventExecutorGroup.this.childIndex.getAndIncrement() % MultithreadEventExecutorGroup.this.children.length)];
      }

      // $FF: synthetic method
      GenericEventExecutorChooser(Object x1) {
         this();
      }
   }

   private final class PowerOfTwoEventExecutorChooser implements MultithreadEventExecutorGroup.EventExecutorChooser {
      private PowerOfTwoEventExecutorChooser() {
      }

      public EventExecutor next() {
         return MultithreadEventExecutorGroup.this.children[MultithreadEventExecutorGroup.this.childIndex.getAndIncrement() & MultithreadEventExecutorGroup.this.children.length - 1];
      }

      // $FF: synthetic method
      PowerOfTwoEventExecutorChooser(Object x1) {
         this();
      }
   }

   private interface EventExecutorChooser {
      EventExecutor next();
   }
}
