package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import sun.misc.Unsafe;

@GwtCompatible(
   emulated = true
)
public abstract class AbstractFuture<V> implements ListenableFuture<V> {
   private static final boolean GENERATE_CANCELLATION_CAUSES = Boolean.parseBoolean(System.getProperty("guava.concurrent.generate_cancellation_cause", "false"));
   private static final Logger log = Logger.getLogger(AbstractFuture.class.getName());
   private static final long SPIN_THRESHOLD_NANOS = 1000L;
   private static final AbstractFuture.AtomicHelper ATOMIC_HELPER;
   private static final Object NULL;
   private volatile Object value;
   private volatile AbstractFuture.Listener listeners;
   private volatile AbstractFuture.Waiter waiters;

   private void removeWaiter(AbstractFuture.Waiter node) {
      node.thread = null;

      label28:
      while(true) {
         AbstractFuture.Waiter pred = null;
         AbstractFuture.Waiter curr = this.waiters;
         if (curr == AbstractFuture.Waiter.TOMBSTONE) {
            return;
         }

         AbstractFuture.Waiter succ;
         for(; curr != null; curr = succ) {
            succ = curr.next;
            if (curr.thread != null) {
               pred = curr;
            } else if (pred != null) {
               pred.next = succ;
               if (pred.thread == null) {
                  continue label28;
               }
            } else if (!ATOMIC_HELPER.casWaiters(this, curr, succ)) {
               continue label28;
            }
         }

         return;
      }
   }

   protected AbstractFuture() {
   }

   public V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException, ExecutionException {
      long remainingNanos = unit.toNanos(timeout);
      if (Thread.interrupted()) {
         throw new InterruptedException();
      } else {
         Object localValue = this.value;
         if (localValue != null & !(localValue instanceof AbstractFuture.SetFuture)) {
            return this.getDoneValue(localValue);
         } else {
            long endNanos = remainingNanos > 0L ? System.nanoTime() + remainingNanos : 0L;
            if (remainingNanos >= 1000L) {
               label117: {
                  AbstractFuture.Waiter oldHead = this.waiters;
                  if (oldHead != AbstractFuture.Waiter.TOMBSTONE) {
                     AbstractFuture.Waiter node = new AbstractFuture.Waiter();

                     do {
                        node.setNext(oldHead);
                        if (ATOMIC_HELPER.casWaiters(this, oldHead, node)) {
                           do {
                              LockSupport.parkNanos(this, remainingNanos);
                              if (Thread.interrupted()) {
                                 this.removeWaiter(node);
                                 throw new InterruptedException();
                              }

                              localValue = this.value;
                              if (localValue != null & !(localValue instanceof AbstractFuture.SetFuture)) {
                                 return this.getDoneValue(localValue);
                              }

                              remainingNanos = endNanos - System.nanoTime();
                           } while(remainingNanos >= 1000L);

                           this.removeWaiter(node);
                           break label117;
                        }

                        oldHead = this.waiters;
                     } while(oldHead != AbstractFuture.Waiter.TOMBSTONE);
                  }

                  return this.getDoneValue(this.value);
               }
            }

            while(remainingNanos > 0L) {
               localValue = this.value;
               if (localValue != null & !(localValue instanceof AbstractFuture.SetFuture)) {
                  return this.getDoneValue(localValue);
               }

               if (Thread.interrupted()) {
                  throw new InterruptedException();
               }

               remainingNanos = endNanos - System.nanoTime();
            }

            throw new TimeoutException();
         }
      }
   }

   public V get() throws InterruptedException, ExecutionException {
      if (Thread.interrupted()) {
         throw new InterruptedException();
      } else {
         Object localValue = this.value;
         if (localValue != null & !(localValue instanceof AbstractFuture.SetFuture)) {
            return this.getDoneValue(localValue);
         } else {
            AbstractFuture.Waiter oldHead = this.waiters;
            if (oldHead != AbstractFuture.Waiter.TOMBSTONE) {
               AbstractFuture.Waiter node = new AbstractFuture.Waiter();

               do {
                  node.setNext(oldHead);
                  if (ATOMIC_HELPER.casWaiters(this, oldHead, node)) {
                     do {
                        LockSupport.park(this);
                        if (Thread.interrupted()) {
                           this.removeWaiter(node);
                           throw new InterruptedException();
                        }

                        localValue = this.value;
                     } while(!(localValue != null & !(localValue instanceof AbstractFuture.SetFuture)));

                     return this.getDoneValue(localValue);
                  }

                  oldHead = this.waiters;
               } while(oldHead != AbstractFuture.Waiter.TOMBSTONE);
            }

            return this.getDoneValue(this.value);
         }
      }
   }

   private V getDoneValue(Object obj) throws ExecutionException {
      if (obj instanceof AbstractFuture.Cancellation) {
         throw cancellationExceptionWithCause("Task was cancelled.", ((AbstractFuture.Cancellation)obj).cause);
      } else if (obj instanceof AbstractFuture.Failure) {
         throw new ExecutionException(((AbstractFuture.Failure)obj).exception);
      } else {
         return obj == NULL ? null : obj;
      }
   }

   public boolean isDone() {
      Object localValue = this.value;
      return localValue != null & !(localValue instanceof AbstractFuture.SetFuture);
   }

   public boolean isCancelled() {
      Object localValue = this.value;
      return localValue instanceof AbstractFuture.Cancellation;
   }

   public boolean cancel(boolean mayInterruptIfRunning) {
      Object localValue = this.value;
      if (localValue == null | localValue instanceof AbstractFuture.SetFuture) {
         Throwable cause = GENERATE_CANCELLATION_CAUSES ? this.newCancellationCause() : null;
         AbstractFuture.Cancellation valueToSet = new AbstractFuture.Cancellation(mayInterruptIfRunning, cause);

         do {
            if (ATOMIC_HELPER.casValue(this, localValue, valueToSet)) {
               if (mayInterruptIfRunning) {
                  this.interruptTask();
               }

               this.complete();
               if (localValue instanceof AbstractFuture.SetFuture) {
                  ((AbstractFuture.SetFuture)localValue).future.cancel(mayInterruptIfRunning);
               }

               return true;
            }

            localValue = this.value;
         } while(localValue instanceof AbstractFuture.SetFuture);
      }

      return false;
   }

   private Throwable newCancellationCause() {
      return new CancellationException("Future.cancel() was called.");
   }

   protected void interruptTask() {
   }

   protected final boolean wasInterrupted() {
      Object localValue = this.value;
      return localValue instanceof AbstractFuture.Cancellation && ((AbstractFuture.Cancellation)localValue).wasInterrupted;
   }

   public void addListener(Runnable listener, Executor executor) {
      Preconditions.checkNotNull(listener, "Runnable was null.");
      Preconditions.checkNotNull(executor, "Executor was null.");
      AbstractFuture.Listener oldHead = this.listeners;
      if (oldHead != AbstractFuture.Listener.TOMBSTONE) {
         AbstractFuture.Listener newNode = new AbstractFuture.Listener(listener, executor);

         do {
            newNode.next = oldHead;
            if (ATOMIC_HELPER.casListeners(this, oldHead, newNode)) {
               return;
            }

            oldHead = this.listeners;
         } while(oldHead != AbstractFuture.Listener.TOMBSTONE);
      }

      executeListener(listener, executor);
   }

   protected boolean set(@Nullable V value) {
      Object valueToSet = value == null ? NULL : value;
      if (ATOMIC_HELPER.casValue(this, (Object)null, valueToSet)) {
         this.complete();
         return true;
      } else {
         return false;
      }
   }

   protected boolean setException(Throwable throwable) {
      Object valueToSet = new AbstractFuture.Failure((Throwable)Preconditions.checkNotNull(throwable));
      if (ATOMIC_HELPER.casValue(this, (Object)null, valueToSet)) {
         this.complete();
         return true;
      } else {
         return false;
      }
   }

   @Beta
   protected boolean setFuture(ListenableFuture<? extends V> future) {
      Preconditions.checkNotNull(future);
      Object localValue = this.value;
      if (localValue == null) {
         if (future.isDone()) {
            return this.completeWithFuture(future, (Object)null);
         }

         AbstractFuture<V>.SetFuture valueToSet = new AbstractFuture.SetFuture(future);
         if (ATOMIC_HELPER.casValue(this, (Object)null, valueToSet)) {
            try {
               future.addListener(valueToSet, MoreExecutors.directExecutor());
            } catch (Throwable var8) {
               Throwable t = var8;

               AbstractFuture.Failure failure;
               try {
                  failure = new AbstractFuture.Failure(t);
               } catch (Throwable var7) {
                  failure = AbstractFuture.Failure.FALLBACK_INSTANCE;
               }

               ATOMIC_HELPER.casValue(this, valueToSet, failure);
            }

            return true;
         }

         localValue = this.value;
      }

      if (localValue instanceof AbstractFuture.Cancellation) {
         future.cancel(((AbstractFuture.Cancellation)localValue).wasInterrupted);
      }

      return false;
   }

   private boolean completeWithFuture(ListenableFuture<? extends V> future, Object expected) {
      Object valueToSet;
      if (future instanceof AbstractFuture.TrustedFuture) {
         valueToSet = ((AbstractFuture)future).value;
      } else {
         try {
            V v = Uninterruptibles.getUninterruptibly(future);
            valueToSet = v == null ? NULL : v;
         } catch (ExecutionException var5) {
            valueToSet = new AbstractFuture.Failure(var5.getCause());
         } catch (CancellationException var6) {
            valueToSet = new AbstractFuture.Cancellation(false, var6);
         } catch (Throwable var7) {
            valueToSet = new AbstractFuture.Failure(var7);
         }
      }

      if (ATOMIC_HELPER.casValue(this, expected, valueToSet)) {
         this.complete();
         return true;
      } else {
         return false;
      }
   }

   private void complete() {
      for(AbstractFuture.Waiter currentWaiter = this.clearWaiters(); currentWaiter != null; currentWaiter = currentWaiter.next) {
         currentWaiter.unpark();
      }

      AbstractFuture.Listener currentListener = this.clearListeners();

      AbstractFuture.Listener reversedList;
      AbstractFuture.Listener tmp;
      for(reversedList = null; currentListener != null; reversedList = tmp) {
         tmp = currentListener;
         currentListener = currentListener.next;
         tmp.next = reversedList;
      }

      while(reversedList != null) {
         executeListener(reversedList.task, reversedList.executor);
         reversedList = reversedList.next;
      }

      this.done();
   }

   void done() {
   }

   final Throwable trustedGetException() {
      return ((AbstractFuture.Failure)this.value).exception;
   }

   final void maybePropagateCancellation(@Nullable Future<?> related) {
      if (related != null & this.isCancelled()) {
         related.cancel(this.wasInterrupted());
      }

   }

   private AbstractFuture.Waiter clearWaiters() {
      AbstractFuture.Waiter head;
      do {
         head = this.waiters;
      } while(!ATOMIC_HELPER.casWaiters(this, head, AbstractFuture.Waiter.TOMBSTONE));

      return head;
   }

   private AbstractFuture.Listener clearListeners() {
      AbstractFuture.Listener head;
      do {
         head = this.listeners;
      } while(!ATOMIC_HELPER.casListeners(this, head, AbstractFuture.Listener.TOMBSTONE));

      return head;
   }

   private static void executeListener(Runnable runnable, Executor executor) {
      try {
         executor.execute(runnable);
      } catch (RuntimeException var3) {
         log.log(Level.SEVERE, "RuntimeException while executing runnable " + runnable + " with executor " + executor, var3);
      }

   }

   static final CancellationException cancellationExceptionWithCause(@Nullable String message, @Nullable Throwable cause) {
      CancellationException exception = new CancellationException(message);
      exception.initCause(cause);
      return exception;
   }

   static {
      Object helper;
      try {
         helper = new AbstractFuture.UnsafeAtomicHelper();
      } catch (Throwable var4) {
         try {
            helper = new AbstractFuture.SafeAtomicHelper(AtomicReferenceFieldUpdater.newUpdater(AbstractFuture.Waiter.class, Thread.class, "thread"), AtomicReferenceFieldUpdater.newUpdater(AbstractFuture.Waiter.class, AbstractFuture.Waiter.class, "next"), AtomicReferenceFieldUpdater.newUpdater(AbstractFuture.class, AbstractFuture.Waiter.class, "waiters"), AtomicReferenceFieldUpdater.newUpdater(AbstractFuture.class, AbstractFuture.Listener.class, "listeners"), AtomicReferenceFieldUpdater.newUpdater(AbstractFuture.class, Object.class, "value"));
         } catch (Throwable var3) {
            log.log(Level.SEVERE, "UnsafeAtomicHelper is broken!", var4);
            log.log(Level.SEVERE, "SafeAtomicHelper is broken!", var3);
            helper = new AbstractFuture.SynchronizedHelper();
         }
      }

      ATOMIC_HELPER = (AbstractFuture.AtomicHelper)helper;
      Class var1 = LockSupport.class;
      NULL = new Object();
   }

   private static final class SynchronizedHelper extends AbstractFuture.AtomicHelper {
      private SynchronizedHelper() {
         super(null);
      }

      void putThread(AbstractFuture.Waiter waiter, Thread newValue) {
         waiter.thread = newValue;
      }

      void putNext(AbstractFuture.Waiter waiter, AbstractFuture.Waiter newValue) {
         waiter.next = newValue;
      }

      boolean casWaiters(AbstractFuture<?> future, AbstractFuture.Waiter expect, AbstractFuture.Waiter update) {
         synchronized(future) {
            if (future.waiters == expect) {
               future.waiters = update;
               return true;
            } else {
               return false;
            }
         }
      }

      boolean casListeners(AbstractFuture<?> future, AbstractFuture.Listener expect, AbstractFuture.Listener update) {
         synchronized(future) {
            if (future.listeners == expect) {
               future.listeners = update;
               return true;
            } else {
               return false;
            }
         }
      }

      boolean casValue(AbstractFuture<?> future, Object expect, Object update) {
         synchronized(future) {
            if (future.value == expect) {
               future.value = update;
               return true;
            } else {
               return false;
            }
         }
      }

      // $FF: synthetic method
      SynchronizedHelper(Object x0) {
         this();
      }
   }

   private static final class SafeAtomicHelper extends AbstractFuture.AtomicHelper {
      final AtomicReferenceFieldUpdater<AbstractFuture.Waiter, Thread> waiterThreadUpdater;
      final AtomicReferenceFieldUpdater<AbstractFuture.Waiter, AbstractFuture.Waiter> waiterNextUpdater;
      final AtomicReferenceFieldUpdater<AbstractFuture, AbstractFuture.Waiter> waitersUpdater;
      final AtomicReferenceFieldUpdater<AbstractFuture, AbstractFuture.Listener> listenersUpdater;
      final AtomicReferenceFieldUpdater<AbstractFuture, Object> valueUpdater;

      SafeAtomicHelper(AtomicReferenceFieldUpdater<AbstractFuture.Waiter, Thread> waiterThreadUpdater, AtomicReferenceFieldUpdater<AbstractFuture.Waiter, AbstractFuture.Waiter> waiterNextUpdater, AtomicReferenceFieldUpdater<AbstractFuture, AbstractFuture.Waiter> waitersUpdater, AtomicReferenceFieldUpdater<AbstractFuture, AbstractFuture.Listener> listenersUpdater, AtomicReferenceFieldUpdater<AbstractFuture, Object> valueUpdater) {
         super(null);
         this.waiterThreadUpdater = waiterThreadUpdater;
         this.waiterNextUpdater = waiterNextUpdater;
         this.waitersUpdater = waitersUpdater;
         this.listenersUpdater = listenersUpdater;
         this.valueUpdater = valueUpdater;
      }

      void putThread(AbstractFuture.Waiter waiter, Thread newValue) {
         this.waiterThreadUpdater.lazySet(waiter, newValue);
      }

      void putNext(AbstractFuture.Waiter waiter, AbstractFuture.Waiter newValue) {
         this.waiterNextUpdater.lazySet(waiter, newValue);
      }

      boolean casWaiters(AbstractFuture<?> future, AbstractFuture.Waiter expect, AbstractFuture.Waiter update) {
         return this.waitersUpdater.compareAndSet(future, expect, update);
      }

      boolean casListeners(AbstractFuture<?> future, AbstractFuture.Listener expect, AbstractFuture.Listener update) {
         return this.listenersUpdater.compareAndSet(future, expect, update);
      }

      boolean casValue(AbstractFuture<?> future, Object expect, Object update) {
         return this.valueUpdater.compareAndSet(future, expect, update);
      }
   }

   private static final class UnsafeAtomicHelper extends AbstractFuture.AtomicHelper {
      static final Unsafe UNSAFE;
      static final long LISTENERS_OFFSET;
      static final long WAITERS_OFFSET;
      static final long VALUE_OFFSET;
      static final long WAITER_THREAD_OFFSET;
      static final long WAITER_NEXT_OFFSET;

      private UnsafeAtomicHelper() {
         super(null);
      }

      void putThread(AbstractFuture.Waiter waiter, Thread newValue) {
         UNSAFE.putObject(waiter, WAITER_THREAD_OFFSET, newValue);
      }

      void putNext(AbstractFuture.Waiter waiter, AbstractFuture.Waiter newValue) {
         UNSAFE.putObject(waiter, WAITER_NEXT_OFFSET, newValue);
      }

      boolean casWaiters(AbstractFuture<?> future, AbstractFuture.Waiter expect, AbstractFuture.Waiter update) {
         return UNSAFE.compareAndSwapObject(future, WAITERS_OFFSET, expect, update);
      }

      boolean casListeners(AbstractFuture<?> future, AbstractFuture.Listener expect, AbstractFuture.Listener update) {
         return UNSAFE.compareAndSwapObject(future, LISTENERS_OFFSET, expect, update);
      }

      boolean casValue(AbstractFuture<?> future, Object expect, Object update) {
         return UNSAFE.compareAndSwapObject(future, VALUE_OFFSET, expect, update);
      }

      // $FF: synthetic method
      UnsafeAtomicHelper(Object x0) {
         this();
      }

      static {
         Unsafe unsafe = null;

         try {
            unsafe = Unsafe.getUnsafe();
         } catch (SecurityException var5) {
            try {
               unsafe = (Unsafe)AccessController.doPrivileged(new PrivilegedExceptionAction<Unsafe>() {
                  public Unsafe run() throws Exception {
                     Class<Unsafe> k = Unsafe.class;
                     Field[] arr$ = k.getDeclaredFields();
                     int len$ = arr$.length;

                     for(int i$ = 0; i$ < len$; ++i$) {
                        Field f = arr$[i$];
                        f.setAccessible(true);
                        Object x = f.get((Object)null);
                        if (k.isInstance(x)) {
                           return (Unsafe)k.cast(x);
                        }
                     }

                     throw new NoSuchFieldError("the Unsafe");
                  }
               });
            } catch (PrivilegedActionException var4) {
               throw new RuntimeException("Could not initialize intrinsics", var4.getCause());
            }
         }

         try {
            Class<?> abstractFuture = AbstractFuture.class;
            WAITERS_OFFSET = unsafe.objectFieldOffset(abstractFuture.getDeclaredField("waiters"));
            LISTENERS_OFFSET = unsafe.objectFieldOffset(abstractFuture.getDeclaredField("listeners"));
            VALUE_OFFSET = unsafe.objectFieldOffset(abstractFuture.getDeclaredField("value"));
            WAITER_THREAD_OFFSET = unsafe.objectFieldOffset(AbstractFuture.Waiter.class.getDeclaredField("thread"));
            WAITER_NEXT_OFFSET = unsafe.objectFieldOffset(AbstractFuture.Waiter.class.getDeclaredField("next"));
            UNSAFE = unsafe;
         } catch (Exception var3) {
            throw Throwables.propagate(var3);
         }
      }
   }

   private abstract static class AtomicHelper {
      private AtomicHelper() {
      }

      abstract void putThread(AbstractFuture.Waiter var1, Thread var2);

      abstract void putNext(AbstractFuture.Waiter var1, AbstractFuture.Waiter var2);

      abstract boolean casWaiters(AbstractFuture<?> var1, AbstractFuture.Waiter var2, AbstractFuture.Waiter var3);

      abstract boolean casListeners(AbstractFuture<?> var1, AbstractFuture.Listener var2, AbstractFuture.Listener var3);

      abstract boolean casValue(AbstractFuture<?> var1, Object var2, Object var3);

      // $FF: synthetic method
      AtomicHelper(Object x0) {
         this();
      }
   }

   private final class SetFuture implements Runnable {
      final ListenableFuture<? extends V> future;

      SetFuture(ListenableFuture<? extends V> future) {
         this.future = future;
      }

      public void run() {
         if (AbstractFuture.this.value == this) {
            AbstractFuture.this.completeWithFuture(this.future, this);
         }
      }
   }

   private static final class Cancellation {
      final boolean wasInterrupted;
      @Nullable
      final Throwable cause;

      Cancellation(boolean wasInterrupted, @Nullable Throwable cause) {
         this.wasInterrupted = wasInterrupted;
         this.cause = cause;
      }
   }

   private static final class Failure {
      static final AbstractFuture.Failure FALLBACK_INSTANCE = new AbstractFuture.Failure(new Throwable("Failure occurred while trying to finish a future.") {
         public synchronized Throwable fillInStackTrace() {
            return this;
         }
      });
      final Throwable exception;

      Failure(Throwable exception) {
         this.exception = (Throwable)Preconditions.checkNotNull(exception);
      }
   }

   private static final class Listener {
      static final AbstractFuture.Listener TOMBSTONE = new AbstractFuture.Listener((Runnable)null, (Executor)null);
      final Runnable task;
      final Executor executor;
      @Nullable
      AbstractFuture.Listener next;

      Listener(Runnable task, Executor executor) {
         this.task = task;
         this.executor = executor;
      }
   }

   private static final class Waiter {
      static final AbstractFuture.Waiter TOMBSTONE = new AbstractFuture.Waiter(false);
      @Nullable
      volatile Thread thread;
      @Nullable
      volatile AbstractFuture.Waiter next;

      Waiter(boolean unused) {
      }

      Waiter() {
         AbstractFuture.ATOMIC_HELPER.putThread(this, Thread.currentThread());
      }

      void setNext(AbstractFuture.Waiter next) {
         AbstractFuture.ATOMIC_HELPER.putNext(this, next);
      }

      void unpark() {
         Thread w = this.thread;
         if (w != null) {
            this.thread = null;
            LockSupport.unpark(w);
         }

      }
   }

   abstract static class TrustedFuture<V> extends AbstractFuture<V> {
      public final V get() throws InterruptedException, ExecutionException {
         return super.get();
      }

      public final V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
         return super.get(timeout, unit);
      }

      public final boolean isDone() {
         return super.isDone();
      }

      public final boolean isCancelled() {
         return super.isCancelled();
      }

      public final void addListener(Runnable listener, Executor executor) {
         super.addListener(listener, executor);
      }
   }
}
