package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
@Beta
public abstract class RateLimiter {
   private final RateLimiter.SleepingStopwatch stopwatch;
   private volatile Object mutexDoNotUseDirectly;

   public static RateLimiter create(double permitsPerSecond) {
      return create(RateLimiter.SleepingStopwatch.createFromSystemTimer(), permitsPerSecond);
   }

   @VisibleForTesting
   static RateLimiter create(RateLimiter.SleepingStopwatch stopwatch, double permitsPerSecond) {
      RateLimiter rateLimiter = new SmoothRateLimiter.SmoothBursty(stopwatch, 1.0D);
      rateLimiter.setRate(permitsPerSecond);
      return rateLimiter;
   }

   public static RateLimiter create(double permitsPerSecond, long warmupPeriod, TimeUnit unit) {
      Preconditions.checkArgument(warmupPeriod >= 0L, "warmupPeriod must not be negative: %s", warmupPeriod);
      return create(RateLimiter.SleepingStopwatch.createFromSystemTimer(), permitsPerSecond, warmupPeriod, unit, 3.0D);
   }

   @VisibleForTesting
   static RateLimiter create(RateLimiter.SleepingStopwatch stopwatch, double permitsPerSecond, long warmupPeriod, TimeUnit unit, double coldFactor) {
      RateLimiter rateLimiter = new SmoothRateLimiter.SmoothWarmingUp(stopwatch, warmupPeriod, unit, coldFactor);
      rateLimiter.setRate(permitsPerSecond);
      return rateLimiter;
   }

   private Object mutex() {
      Object mutex = this.mutexDoNotUseDirectly;
      if (mutex == null) {
         synchronized(this) {
            mutex = this.mutexDoNotUseDirectly;
            if (mutex == null) {
               this.mutexDoNotUseDirectly = mutex = new Object();
            }
         }
      }

      return mutex;
   }

   RateLimiter(RateLimiter.SleepingStopwatch stopwatch) {
      this.stopwatch = (RateLimiter.SleepingStopwatch)Preconditions.checkNotNull(stopwatch);
   }

   public final void setRate(double permitsPerSecond) {
      Preconditions.checkArgument(permitsPerSecond > 0.0D && !Double.isNaN(permitsPerSecond), "rate must be positive");
      synchronized(this.mutex()) {
         this.doSetRate(permitsPerSecond, this.stopwatch.readMicros());
      }
   }

   abstract void doSetRate(double var1, long var3);

   public final double getRate() {
      synchronized(this.mutex()) {
         return this.doGetRate();
      }
   }

   abstract double doGetRate();

   public double acquire() {
      return this.acquire(1);
   }

   public double acquire(int permits) {
      long microsToWait = this.reserve(permits);
      this.stopwatch.sleepMicrosUninterruptibly(microsToWait);
      return 1.0D * (double)microsToWait / (double)TimeUnit.SECONDS.toMicros(1L);
   }

   final long reserve(int permits) {
      checkPermits(permits);
      synchronized(this.mutex()) {
         return this.reserveAndGetWaitLength(permits, this.stopwatch.readMicros());
      }
   }

   public boolean tryAcquire(long timeout, TimeUnit unit) {
      return this.tryAcquire(1, timeout, unit);
   }

   public boolean tryAcquire(int permits) {
      return this.tryAcquire(permits, 0L, TimeUnit.MICROSECONDS);
   }

   public boolean tryAcquire() {
      return this.tryAcquire(1, 0L, TimeUnit.MICROSECONDS);
   }

   public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
      long timeoutMicros = Math.max(unit.toMicros(timeout), 0L);
      checkPermits(permits);
      long microsToWait;
      synchronized(this.mutex()) {
         long nowMicros = this.stopwatch.readMicros();
         if (!this.canAcquire(nowMicros, timeoutMicros)) {
            return false;
         }

         microsToWait = this.reserveAndGetWaitLength(permits, nowMicros);
      }

      this.stopwatch.sleepMicrosUninterruptibly(microsToWait);
      return true;
   }

   private boolean canAcquire(long nowMicros, long timeoutMicros) {
      return this.queryEarliestAvailable(nowMicros) - timeoutMicros <= nowMicros;
   }

   final long reserveAndGetWaitLength(int permits, long nowMicros) {
      long momentAvailable = this.reserveEarliestAvailable(permits, nowMicros);
      return Math.max(momentAvailable - nowMicros, 0L);
   }

   abstract long queryEarliestAvailable(long var1);

   abstract long reserveEarliestAvailable(int var1, long var2);

   public String toString() {
      return String.format(Locale.ROOT, "RateLimiter[stableRate=%3.1fqps]", this.getRate());
   }

   private static int checkPermits(int permits) {
      Preconditions.checkArgument(permits > 0, "Requested permits (%s) must be positive", permits);
      return permits;
   }

   @VisibleForTesting
   abstract static class SleepingStopwatch {
      abstract long readMicros();

      abstract void sleepMicrosUninterruptibly(long var1);

      static final RateLimiter.SleepingStopwatch createFromSystemTimer() {
         return new RateLimiter.SleepingStopwatch() {
            final Stopwatch stopwatch = Stopwatch.createStarted();

            long readMicros() {
               return this.stopwatch.elapsed(TimeUnit.MICROSECONDS);
            }

            void sleepMicrosUninterruptibly(long micros) {
               if (micros > 0L) {
                  Uninterruptibles.sleepUninterruptibly(micros, TimeUnit.MICROSECONDS);
               }

            }
         };
      }
   }
}
