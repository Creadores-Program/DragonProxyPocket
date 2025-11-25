package com.google.common.cache;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Supplier;
import java.util.concurrent.atomic.AtomicLong;

@GwtCompatible(
   emulated = true
)
final class LongAddables {
   private static final Supplier<LongAddable> SUPPLIER;

   public static LongAddable create() {
      return (LongAddable)SUPPLIER.get();
   }

   static {
      Supplier supplier;
      try {
         new LongAdder();
         supplier = new Supplier<LongAddable>() {
            public LongAddable get() {
               return new LongAdder();
            }
         };
      } catch (Throwable var2) {
         supplier = new Supplier<LongAddable>() {
            public LongAddable get() {
               return new LongAddables.PureJavaLongAddable();
            }
         };
      }

      SUPPLIER = supplier;
   }

   private static final class PureJavaLongAddable extends AtomicLong implements LongAddable {
      private PureJavaLongAddable() {
      }

      public void increment() {
         this.getAndIncrement();
      }

      public void add(long x) {
         this.getAndAdd(x);
      }

      public long sum() {
         return this.get();
      }

      // $FF: synthetic method
      PureJavaLongAddable(Object x0) {
         this();
      }
   }
}
