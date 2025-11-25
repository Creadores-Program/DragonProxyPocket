package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@Beta
@GwtCompatible(
   emulated = true
)
public final class Futures extends GwtFuturesCatchingSpecialization {
   private static final AsyncFunction<ListenableFuture<Object>, Object> DEREFERENCER = new AsyncFunction<ListenableFuture<Object>, Object>() {
      public ListenableFuture<Object> apply(ListenableFuture<Object> input) {
         return input;
      }
   };

   private Futures() {
   }

   @CheckReturnValue
   @GwtIncompatible("TODO")
   public static <V, X extends Exception> CheckedFuture<V, X> makeChecked(ListenableFuture<V> future, Function<? super Exception, X> mapper) {
      return new Futures.MappingCheckedFuture((ListenableFuture)Preconditions.checkNotNull(future), mapper);
   }

   @CheckReturnValue
   public static <V> ListenableFuture<V> immediateFuture(@Nullable V value) {
      if (value == null) {
         ListenableFuture<V> typedNull = Futures.ImmediateSuccessfulFuture.NULL;
         return typedNull;
      } else {
         return new Futures.ImmediateSuccessfulFuture(value);
      }
   }

   @CheckReturnValue
   @GwtIncompatible("TODO")
   public static <V, X extends Exception> CheckedFuture<V, X> immediateCheckedFuture(@Nullable V value) {
      return new Futures.ImmediateSuccessfulCheckedFuture(value);
   }

   @CheckReturnValue
   public static <V> ListenableFuture<V> immediateFailedFuture(Throwable throwable) {
      Preconditions.checkNotNull(throwable);
      return new Futures.ImmediateFailedFuture(throwable);
   }

   @CheckReturnValue
   @GwtIncompatible("TODO")
   public static <V> ListenableFuture<V> immediateCancelledFuture() {
      return new Futures.ImmediateCancelledFuture();
   }

   @CheckReturnValue
   @GwtIncompatible("TODO")
   public static <V, X extends Exception> CheckedFuture<V, X> immediateFailedCheckedFuture(X exception) {
      Preconditions.checkNotNull(exception);
      return new Futures.ImmediateFailedCheckedFuture(exception);
   }

   /** @deprecated */
   @Deprecated
   @CheckReturnValue
   public static <V> ListenableFuture<V> withFallback(ListenableFuture<? extends V> input, FutureFallback<? extends V> fallback) {
      return withFallback(input, fallback, MoreExecutors.directExecutor());
   }

   /** @deprecated */
   @Deprecated
   @CheckReturnValue
   public static <V> ListenableFuture<V> withFallback(ListenableFuture<? extends V> input, FutureFallback<? extends V> fallback, Executor executor) {
      return catchingAsync(input, Throwable.class, asAsyncFunction(fallback), executor);
   }

   @CheckReturnValue
   @GwtIncompatible("AVAILABLE but requires exceptionType to be Throwable.class")
   public static <V, X extends Throwable> ListenableFuture<V> catching(ListenableFuture<? extends V> input, Class<X> exceptionType, Function<? super X, ? extends V> fallback) {
      Futures.CatchingFuture<V, X> future = new Futures.CatchingFuture(input, exceptionType, fallback);
      input.addListener(future, MoreExecutors.directExecutor());
      return future;
   }

   @CheckReturnValue
   @GwtIncompatible("AVAILABLE but requires exceptionType to be Throwable.class")
   public static <V, X extends Throwable> ListenableFuture<V> catching(ListenableFuture<? extends V> input, Class<X> exceptionType, Function<? super X, ? extends V> fallback, Executor executor) {
      Futures.CatchingFuture<V, X> future = new Futures.CatchingFuture(input, exceptionType, fallback);
      input.addListener(future, rejectionPropagatingExecutor(executor, future));
      return future;
   }

   @GwtIncompatible("AVAILABLE but requires exceptionType to be Throwable.class")
   public static <V, X extends Throwable> ListenableFuture<V> catchingAsync(ListenableFuture<? extends V> input, Class<X> exceptionType, AsyncFunction<? super X, ? extends V> fallback) {
      Futures.AsyncCatchingFuture<V, X> future = new Futures.AsyncCatchingFuture(input, exceptionType, fallback);
      input.addListener(future, MoreExecutors.directExecutor());
      return future;
   }

   @GwtIncompatible("AVAILABLE but requires exceptionType to be Throwable.class")
   public static <V, X extends Throwable> ListenableFuture<V> catchingAsync(ListenableFuture<? extends V> input, Class<X> exceptionType, AsyncFunction<? super X, ? extends V> fallback, Executor executor) {
      Futures.AsyncCatchingFuture<V, X> future = new Futures.AsyncCatchingFuture(input, exceptionType, fallback);
      input.addListener(future, rejectionPropagatingExecutor(executor, future));
      return future;
   }

   /** @deprecated */
   @Deprecated
   static <V> AsyncFunction<Throwable, V> asAsyncFunction(final FutureFallback<V> fallback) {
      Preconditions.checkNotNull(fallback);
      return new AsyncFunction<Throwable, V>() {
         public ListenableFuture<V> apply(Throwable t) throws Exception {
            return (ListenableFuture)Preconditions.checkNotNull(fallback.create(t), "FutureFallback.create returned null instead of a Future. Did you mean to return immediateFuture(null)?");
         }
      };
   }

   @CheckReturnValue
   @GwtIncompatible("java.util.concurrent.ScheduledExecutorService")
   public static <V> ListenableFuture<V> withTimeout(ListenableFuture<V> delegate, long time, TimeUnit unit, ScheduledExecutorService scheduledExecutor) {
      Futures.TimeoutFuture<V> result = new Futures.TimeoutFuture(delegate);
      Futures.TimeoutFuture.Fire<V> fire = new Futures.TimeoutFuture.Fire(result);
      result.timer = scheduledExecutor.schedule(fire, time, unit);
      delegate.addListener(fire, MoreExecutors.directExecutor());
      return result;
   }

   /** @deprecated */
   @Deprecated
   public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input, AsyncFunction<? super I, ? extends O> function) {
      return transformAsync(input, function);
   }

   /** @deprecated */
   @Deprecated
   public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input, AsyncFunction<? super I, ? extends O> function, Executor executor) {
      return transformAsync(input, function, executor);
   }

   public static <I, O> ListenableFuture<O> transformAsync(ListenableFuture<I> input, AsyncFunction<? super I, ? extends O> function) {
      Futures.AsyncChainingFuture<I, O> output = new Futures.AsyncChainingFuture(input, function);
      input.addListener(output, MoreExecutors.directExecutor());
      return output;
   }

   public static <I, O> ListenableFuture<O> transformAsync(ListenableFuture<I> input, AsyncFunction<? super I, ? extends O> function, Executor executor) {
      Preconditions.checkNotNull(executor);
      Futures.AsyncChainingFuture<I, O> output = new Futures.AsyncChainingFuture(input, function);
      input.addListener(output, rejectionPropagatingExecutor(executor, output));
      return output;
   }

   private static Executor rejectionPropagatingExecutor(final Executor delegate, final AbstractFuture<?> future) {
      Preconditions.checkNotNull(delegate);
      return delegate == MoreExecutors.directExecutor() ? delegate : new Executor() {
         volatile boolean thrownFromDelegate = true;

         public void execute(final Runnable command) {
            try {
               delegate.execute(new Runnable() {
                  public void run() {
                     thrownFromDelegate = false;
                     command.run();
                  }
               });
            } catch (RejectedExecutionException var3) {
               if (this.thrownFromDelegate) {
                  future.setException(var3);
               }
            }

         }
      };
   }

   public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input, Function<? super I, ? extends O> function) {
      Preconditions.checkNotNull(function);
      Futures.ChainingFuture<I, O> output = new Futures.ChainingFuture(input, function);
      input.addListener(output, MoreExecutors.directExecutor());
      return output;
   }

   public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input, Function<? super I, ? extends O> function, Executor executor) {
      Preconditions.checkNotNull(function);
      Futures.ChainingFuture<I, O> output = new Futures.ChainingFuture(input, function);
      input.addListener(output, rejectionPropagatingExecutor(executor, output));
      return output;
   }

   @CheckReturnValue
   @GwtIncompatible("TODO")
   public static <I, O> Future<O> lazyTransform(final Future<I> input, final Function<? super I, ? extends O> function) {
      Preconditions.checkNotNull(input);
      Preconditions.checkNotNull(function);
      return new Future<O>() {
         public boolean cancel(boolean mayInterruptIfRunning) {
            return input.cancel(mayInterruptIfRunning);
         }

         public boolean isCancelled() {
            return input.isCancelled();
         }

         public boolean isDone() {
            return input.isDone();
         }

         public O get() throws InterruptedException, ExecutionException {
            return this.applyTransformation(input.get());
         }

         public O get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return this.applyTransformation(input.get(timeout, unit));
         }

         private O applyTransformation(I inputx) throws ExecutionException {
            try {
               return function.apply(inputx);
            } catch (Throwable var3) {
               throw new ExecutionException(var3);
            }
         }
      };
   }

   @CheckReturnValue
   public static <V> ListenableFuture<V> dereference(ListenableFuture<? extends ListenableFuture<? extends V>> nested) {
      return transformAsync(nested, DEREFERENCER);
   }

   @SafeVarargs
   @CheckReturnValue
   @Beta
   public static <V> ListenableFuture<List<V>> allAsList(ListenableFuture<? extends V>... futures) {
      return new Futures.ListFuture(ImmutableList.copyOf((Object[])futures), true);
   }

   @CheckReturnValue
   @Beta
   public static <V> ListenableFuture<List<V>> allAsList(Iterable<? extends ListenableFuture<? extends V>> futures) {
      return new Futures.ListFuture(ImmutableList.copyOf(futures), true);
   }

   @CheckReturnValue
   @GwtIncompatible("TODO")
   public static <V> ListenableFuture<V> nonCancellationPropagating(ListenableFuture<V> future) {
      return new Futures.NonCancellationPropagatingFuture(future);
   }

   @SafeVarargs
   @CheckReturnValue
   @Beta
   public static <V> ListenableFuture<List<V>> successfulAsList(ListenableFuture<? extends V>... futures) {
      return new Futures.ListFuture(ImmutableList.copyOf((Object[])futures), false);
   }

   @CheckReturnValue
   @Beta
   public static <V> ListenableFuture<List<V>> successfulAsList(Iterable<? extends ListenableFuture<? extends V>> futures) {
      return new Futures.ListFuture(ImmutableList.copyOf(futures), false);
   }

   @CheckReturnValue
   @Beta
   @GwtIncompatible("TODO")
   public static <T> ImmutableList<ListenableFuture<T>> inCompletionOrder(Iterable<? extends ListenableFuture<? extends T>> futures) {
      final ConcurrentLinkedQueue<SettableFuture<T>> delegates = Queues.newConcurrentLinkedQueue();
      ImmutableList.Builder<ListenableFuture<T>> listBuilder = ImmutableList.builder();
      SerializingExecutor executor = new SerializingExecutor(MoreExecutors.directExecutor());
      Iterator i$ = futures.iterator();

      while(i$.hasNext()) {
         final ListenableFuture<? extends T> future = (ListenableFuture)i$.next();
         SettableFuture<T> delegate = SettableFuture.create();
         delegates.add(delegate);
         future.addListener(new Runnable() {
            public void run() {
               ((SettableFuture)delegates.remove()).setFuture(future);
            }
         }, executor);
         listBuilder.add((Object)delegate);
      }

      return listBuilder.build();
   }

   public static <V> void addCallback(ListenableFuture<V> future, FutureCallback<? super V> callback) {
      addCallback(future, callback, MoreExecutors.directExecutor());
   }

   public static <V> void addCallback(final ListenableFuture<V> future, final FutureCallback<? super V> callback, Executor executor) {
      Preconditions.checkNotNull(callback);
      Runnable callbackListener = new Runnable() {
         public void run() {
            Object value;
            try {
               value = Uninterruptibles.getUninterruptibly(future);
            } catch (ExecutionException var3) {
               callback.onFailure(var3.getCause());
               return;
            } catch (RuntimeException var4) {
               callback.onFailure(var4);
               return;
            } catch (Error var5) {
               callback.onFailure(var5);
               return;
            }

            callback.onSuccess(value);
         }
      };
      future.addListener(callbackListener, executor);
   }

   /** @deprecated */
   @Deprecated
   @GwtIncompatible("reflection")
   public static <V, X extends Exception> V get(Future<V> future, Class<X> exceptionClass) throws X {
      return getChecked(future, exceptionClass);
   }

   /** @deprecated */
   @Deprecated
   @GwtIncompatible("reflection")
   public static <V, X extends Exception> V get(Future<V> future, long timeout, TimeUnit unit, Class<X> exceptionClass) throws X {
      return getChecked(future, exceptionClass, timeout, unit);
   }

   @GwtIncompatible("reflection")
   public static <V, X extends Exception> V getChecked(Future<V> future, Class<X> exceptionClass) throws X {
      return FuturesGetChecked.getChecked(future, exceptionClass);
   }

   @GwtIncompatible("reflection")
   public static <V, X extends Exception> V getChecked(Future<V> future, Class<X> exceptionClass, long timeout, TimeUnit unit) throws X {
      return FuturesGetChecked.getChecked(future, exceptionClass, timeout, unit);
   }

   @GwtIncompatible("TODO")
   public static <V> V getUnchecked(Future<V> future) {
      Preconditions.checkNotNull(future);

      try {
         return Uninterruptibles.getUninterruptibly(future);
      } catch (ExecutionException var2) {
         wrapAndThrowUnchecked(var2.getCause());
         throw new AssertionError();
      }
   }

   @GwtIncompatible("TODO")
   private static void wrapAndThrowUnchecked(Throwable cause) {
      if (cause instanceof Error) {
         throw new ExecutionError((Error)cause);
      } else {
         throw new UncheckedExecutionException(cause);
      }
   }

   @GwtIncompatible("TODO")
   private static class MappingCheckedFuture<V, X extends Exception> extends AbstractCheckedFuture<V, X> {
      final Function<? super Exception, X> mapper;

      MappingCheckedFuture(ListenableFuture<V> delegate, Function<? super Exception, X> mapper) {
         super(delegate);
         this.mapper = (Function)Preconditions.checkNotNull(mapper);
      }

      protected X mapException(Exception e) {
         return (Exception)this.mapper.apply(e);
      }
   }

   private static final class ListFuture<V> extends CollectionFuture<V, List<V>> {
      ListFuture(ImmutableCollection<? extends ListenableFuture<? extends V>> futures, boolean allMustSucceed) {
         this.init(new Futures.ListFuture.ListFutureRunningState(futures, allMustSucceed));
      }

      private final class ListFutureRunningState extends CollectionFuture<V, List<V>>.CollectionFutureRunningState {
         ListFutureRunningState(ImmutableCollection<? extends ListenableFuture<? extends V>> futures, boolean allMustSucceed) {
            super(futures, allMustSucceed);
         }

         public List<V> combine(List<Optional<V>> values) {
            List<V> result = Lists.newArrayList();
            Iterator i$ = values.iterator();

            while(i$.hasNext()) {
               Optional<V> element = (Optional)i$.next();
               result.add(element != null ? element.orNull() : null);
            }

            return Collections.unmodifiableList(result);
         }
      }
   }

   @GwtIncompatible("TODO")
   private static final class NonCancellationPropagatingFuture<V> extends AbstractFuture.TrustedFuture<V> {
      NonCancellationPropagatingFuture(final ListenableFuture<V> delegate) {
         delegate.addListener(new Runnable() {
            public void run() {
               NonCancellationPropagatingFuture.this.setFuture(delegate);
            }
         }, MoreExecutors.directExecutor());
      }
   }

   private static final class ChainingFuture<I, O> extends Futures.AbstractChainingFuture<I, O, Function<? super I, ? extends O>> {
      ChainingFuture(ListenableFuture<? extends I> inputFuture, Function<? super I, ? extends O> function) {
         super(inputFuture, function);
      }

      void doTransform(Function<? super I, ? extends O> function, I input) {
         this.set(function.apply(input));
      }
   }

   private static final class AsyncChainingFuture<I, O> extends Futures.AbstractChainingFuture<I, O, AsyncFunction<? super I, ? extends O>> {
      AsyncChainingFuture(ListenableFuture<? extends I> inputFuture, AsyncFunction<? super I, ? extends O> function) {
         super(inputFuture, function);
      }

      void doTransform(AsyncFunction<? super I, ? extends O> function, I input) throws Exception {
         ListenableFuture<? extends O> outputFuture = function.apply(input);
         Preconditions.checkNotNull(outputFuture, "AsyncFunction.apply returned null instead of a Future. Did you mean to return immediateFuture(null)?");
         this.setFuture(outputFuture);
      }
   }

   private abstract static class AbstractChainingFuture<I, O, F> extends AbstractFuture.TrustedFuture<O> implements Runnable {
      @Nullable
      ListenableFuture<? extends I> inputFuture;
      @Nullable
      F function;

      AbstractChainingFuture(ListenableFuture<? extends I> inputFuture, F function) {
         this.inputFuture = (ListenableFuture)Preconditions.checkNotNull(inputFuture);
         this.function = Preconditions.checkNotNull(function);
      }

      public final void run() {
         try {
            ListenableFuture<? extends I> localInputFuture = this.inputFuture;
            F localFunction = this.function;
            if (this.isCancelled() | localInputFuture == null | localFunction == null) {
               return;
            }

            this.inputFuture = null;
            this.function = null;

            Object sourceResult;
            try {
               sourceResult = Uninterruptibles.getUninterruptibly(localInputFuture);
            } catch (CancellationException var5) {
               this.cancel(false);
               return;
            } catch (ExecutionException var6) {
               this.setException(var6.getCause());
               return;
            }

            this.doTransform(localFunction, sourceResult);
         } catch (UndeclaredThrowableException var7) {
            this.setException(var7.getCause());
         } catch (Throwable var8) {
            this.setException(var8);
         }

      }

      abstract void doTransform(F var1, I var2) throws Exception;

      final void done() {
         this.maybePropagateCancellation(this.inputFuture);
         this.inputFuture = null;
         this.function = null;
      }
   }

   private static final class TimeoutFuture<V> extends AbstractFuture.TrustedFuture<V> {
      @Nullable
      ListenableFuture<V> delegateRef;
      @Nullable
      Future<?> timer;

      TimeoutFuture(ListenableFuture<V> delegate) {
         this.delegateRef = (ListenableFuture)Preconditions.checkNotNull(delegate);
      }

      void done() {
         this.maybePropagateCancellation(this.delegateRef);
         Future<?> localTimer = this.timer;
         if (localTimer != null) {
            localTimer.cancel(false);
         }

         this.delegateRef = null;
         this.timer = null;
      }

      private static final class Fire<V> implements Runnable {
         @Nullable
         Futures.TimeoutFuture<V> timeoutFutureRef;

         Fire(Futures.TimeoutFuture<V> timeoutFuture) {
            this.timeoutFutureRef = timeoutFuture;
         }

         public void run() {
            Futures.TimeoutFuture<V> timeoutFuture = this.timeoutFutureRef;
            if (timeoutFuture != null) {
               ListenableFuture<V> delegate = timeoutFuture.delegateRef;
               if (delegate != null) {
                  this.timeoutFutureRef = null;
                  if (delegate.isDone()) {
                     timeoutFuture.setFuture(delegate);
                  } else {
                     try {
                        timeoutFuture.setException(new TimeoutException("Future timed out: " + delegate));
                     } finally {
                        delegate.cancel(true);
                     }
                  }

               }
            }
         }
      }
   }

   static final class CatchingFuture<V, X extends Throwable> extends Futures.AbstractCatchingFuture<V, X, Function<? super X, ? extends V>> {
      CatchingFuture(ListenableFuture<? extends V> input, Class<X> exceptionType, Function<? super X, ? extends V> fallback) {
         super(input, exceptionType, fallback);
      }

      void doFallback(Function<? super X, ? extends V> fallback, X cause) throws Exception {
         V replacement = fallback.apply(cause);
         this.set(replacement);
      }
   }

   static final class AsyncCatchingFuture<V, X extends Throwable> extends Futures.AbstractCatchingFuture<V, X, AsyncFunction<? super X, ? extends V>> {
      AsyncCatchingFuture(ListenableFuture<? extends V> input, Class<X> exceptionType, AsyncFunction<? super X, ? extends V> fallback) {
         super(input, exceptionType, fallback);
      }

      void doFallback(AsyncFunction<? super X, ? extends V> fallback, X cause) throws Exception {
         ListenableFuture<? extends V> replacement = fallback.apply(cause);
         Preconditions.checkNotNull(replacement, "AsyncFunction.apply returned null instead of a Future. Did you mean to return immediateFuture(null)?");
         this.setFuture(replacement);
      }
   }

   private abstract static class AbstractCatchingFuture<V, X extends Throwable, F> extends AbstractFuture.TrustedFuture<V> implements Runnable {
      @Nullable
      ListenableFuture<? extends V> inputFuture;
      @Nullable
      Class<X> exceptionType;
      @Nullable
      F fallback;

      AbstractCatchingFuture(ListenableFuture<? extends V> inputFuture, Class<X> exceptionType, F fallback) {
         this.inputFuture = (ListenableFuture)Preconditions.checkNotNull(inputFuture);
         this.exceptionType = (Class)Preconditions.checkNotNull(exceptionType);
         this.fallback = Preconditions.checkNotNull(fallback);
      }

      public final void run() {
         ListenableFuture<? extends V> localInputFuture = this.inputFuture;
         Class<X> localExceptionType = this.exceptionType;
         F localFallback = this.fallback;
         if (!(localInputFuture == null | localExceptionType == null | localFallback == null | this.isCancelled())) {
            this.inputFuture = null;
            this.exceptionType = null;
            this.fallback = null;

            Throwable throwable;
            try {
               this.set(Uninterruptibles.getUninterruptibly(localInputFuture));
               return;
            } catch (ExecutionException var7) {
               throwable = var7.getCause();
            } catch (Throwable var8) {
               throwable = var8;
            }

            try {
               if (Platform.isInstanceOfThrowableClass(throwable, localExceptionType)) {
                  this.doFallback(localFallback, throwable);
               } else {
                  this.setException(throwable);
               }
            } catch (Throwable var6) {
               this.setException(var6);
            }

         }
      }

      abstract void doFallback(F var1, X var2) throws Exception;

      final void done() {
         this.maybePropagateCancellation(this.inputFuture);
         this.inputFuture = null;
         this.exceptionType = null;
         this.fallback = null;
      }
   }

   @GwtIncompatible("TODO")
   private static class ImmediateFailedCheckedFuture<V, X extends Exception> extends Futures.ImmediateFuture<V> implements CheckedFuture<V, X> {
      private final X thrown;

      ImmediateFailedCheckedFuture(X thrown) {
         super(null);
         this.thrown = thrown;
      }

      public V get() throws ExecutionException {
         throw new ExecutionException(this.thrown);
      }

      public V checkedGet() throws X {
         throw this.thrown;
      }

      public V checkedGet(long timeout, TimeUnit unit) throws X {
         Preconditions.checkNotNull(unit);
         throw this.thrown;
      }
   }

   @GwtIncompatible("TODO")
   private static class ImmediateCancelledFuture<V> extends Futures.ImmediateFuture<V> {
      private final CancellationException thrown = new CancellationException("Immediate cancelled future.");

      ImmediateCancelledFuture() {
         super(null);
      }

      public boolean isCancelled() {
         return true;
      }

      public V get() {
         throw AbstractFuture.cancellationExceptionWithCause("Task was cancelled.", this.thrown);
      }
   }

   private static class ImmediateFailedFuture<V> extends Futures.ImmediateFuture<V> {
      private final Throwable thrown;

      ImmediateFailedFuture(Throwable thrown) {
         super(null);
         this.thrown = thrown;
      }

      public V get() throws ExecutionException {
         throw new ExecutionException(this.thrown);
      }
   }

   @GwtIncompatible("TODO")
   private static class ImmediateSuccessfulCheckedFuture<V, X extends Exception> extends Futures.ImmediateFuture<V> implements CheckedFuture<V, X> {
      @Nullable
      private final V value;

      ImmediateSuccessfulCheckedFuture(@Nullable V value) {
         super(null);
         this.value = value;
      }

      public V get() {
         return this.value;
      }

      public V checkedGet() {
         return this.value;
      }

      public V checkedGet(long timeout, TimeUnit unit) {
         Preconditions.checkNotNull(unit);
         return this.value;
      }
   }

   private static class ImmediateSuccessfulFuture<V> extends Futures.ImmediateFuture<V> {
      static final Futures.ImmediateSuccessfulFuture<Object> NULL = new Futures.ImmediateSuccessfulFuture((Object)null);
      @Nullable
      private final V value;

      ImmediateSuccessfulFuture(@Nullable V value) {
         super(null);
         this.value = value;
      }

      public V get() {
         return this.value;
      }
   }

   private abstract static class ImmediateFuture<V> implements ListenableFuture<V> {
      private static final Logger log = Logger.getLogger(Futures.ImmediateFuture.class.getName());

      private ImmediateFuture() {
      }

      public void addListener(Runnable listener, Executor executor) {
         Preconditions.checkNotNull(listener, "Runnable was null.");
         Preconditions.checkNotNull(executor, "Executor was null.");

         try {
            executor.execute(listener);
         } catch (RuntimeException var4) {
            log.log(Level.SEVERE, "RuntimeException while executing runnable " + listener + " with executor " + executor, var4);
         }

      }

      public boolean cancel(boolean mayInterruptIfRunning) {
         return false;
      }

      public abstract V get() throws ExecutionException;

      public V get(long timeout, TimeUnit unit) throws ExecutionException {
         Preconditions.checkNotNull(unit);
         return this.get();
      }

      public boolean isCancelled() {
         return false;
      }

      public boolean isDone() {
         return true;
      }

      // $FF: synthetic method
      ImmediateFuture(Object x0) {
         this();
      }
   }
}
