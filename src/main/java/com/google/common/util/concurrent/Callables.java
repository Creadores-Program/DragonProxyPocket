package com.google.common.util.concurrent;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;

@GwtCompatible(
   emulated = true
)
public final class Callables {
   private Callables() {
   }

   public static <T> Callable<T> returning(@Nullable final T value) {
      return new Callable<T>() {
         public T call() {
            return value;
         }
      };
   }

   @GwtIncompatible("threads")
   static <T> Callable<T> threadRenaming(final Callable<T> callable, final Supplier<String> nameSupplier) {
      Preconditions.checkNotNull(nameSupplier);
      Preconditions.checkNotNull(callable);
      return new Callable<T>() {
         public T call() throws Exception {
            Thread currentThread = Thread.currentThread();
            String oldName = currentThread.getName();
            boolean restoreName = Callables.trySetName((String)nameSupplier.get(), currentThread);

            Object var4;
            try {
               var4 = callable.call();
            } finally {
               if (restoreName) {
                  Callables.trySetName(oldName, currentThread);
               }

            }

            return var4;
         }
      };
   }

   @GwtIncompatible("threads")
   static Runnable threadRenaming(final Runnable task, final Supplier<String> nameSupplier) {
      Preconditions.checkNotNull(nameSupplier);
      Preconditions.checkNotNull(task);
      return new Runnable() {
         public void run() {
            Thread currentThread = Thread.currentThread();
            String oldName = currentThread.getName();
            boolean restoreName = Callables.trySetName((String)nameSupplier.get(), currentThread);

            try {
               task.run();
            } finally {
               if (restoreName) {
                  Callables.trySetName(oldName, currentThread);
               }

            }

         }
      };
   }

   @GwtIncompatible("threads")
   private static boolean trySetName(String threadName, Thread currentThread) {
      try {
         currentThread.setName(threadName);
         return true;
      } catch (SecurityException var3) {
         return false;
      }
   }
}
