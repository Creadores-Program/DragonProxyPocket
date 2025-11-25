package com.google.common.hash;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.primitives.SignedBytes;
import com.google.common.primitives.UnsignedBytes;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@Beta
public final class BloomFilter<T> implements Predicate<T>, Serializable {
   private final BloomFilterStrategies.BitArray bits;
   private final int numHashFunctions;
   private final Funnel<? super T> funnel;
   private final BloomFilter.Strategy strategy;

   private BloomFilter(BloomFilterStrategies.BitArray bits, int numHashFunctions, Funnel<? super T> funnel, BloomFilter.Strategy strategy) {
      Preconditions.checkArgument(numHashFunctions > 0, "numHashFunctions (%s) must be > 0", numHashFunctions);
      Preconditions.checkArgument(numHashFunctions <= 255, "numHashFunctions (%s) must be <= 255", numHashFunctions);
      this.bits = (BloomFilterStrategies.BitArray)Preconditions.checkNotNull(bits);
      this.numHashFunctions = numHashFunctions;
      this.funnel = (Funnel)Preconditions.checkNotNull(funnel);
      this.strategy = (BloomFilter.Strategy)Preconditions.checkNotNull(strategy);
   }

   @CheckReturnValue
   public BloomFilter<T> copy() {
      return new BloomFilter(this.bits.copy(), this.numHashFunctions, this.funnel, this.strategy);
   }

   @CheckReturnValue
   public boolean mightContain(T object) {
      return this.strategy.mightContain(object, this.funnel, this.numHashFunctions, this.bits);
   }

   /** @deprecated */
   @Deprecated
   @CheckReturnValue
   public boolean apply(T input) {
      return this.mightContain(input);
   }

   public boolean put(T object) {
      return this.strategy.put(object, this.funnel, this.numHashFunctions, this.bits);
   }

   @CheckReturnValue
   public double expectedFpp() {
      return Math.pow((double)this.bits.bitCount() / (double)this.bitSize(), (double)this.numHashFunctions);
   }

   @VisibleForTesting
   long bitSize() {
      return this.bits.bitSize();
   }

   @CheckReturnValue
   public boolean isCompatible(BloomFilter<T> that) {
      Preconditions.checkNotNull(that);
      return this != that && this.numHashFunctions == that.numHashFunctions && this.bitSize() == that.bitSize() && this.strategy.equals(that.strategy) && this.funnel.equals(that.funnel);
   }

   public void putAll(BloomFilter<T> that) {
      Preconditions.checkNotNull(that);
      Preconditions.checkArgument(this != that, "Cannot combine a BloomFilter with itself.");
      Preconditions.checkArgument(this.numHashFunctions == that.numHashFunctions, "BloomFilters must have the same number of hash functions (%s != %s)", this.numHashFunctions, that.numHashFunctions);
      Preconditions.checkArgument(this.bitSize() == that.bitSize(), "BloomFilters must have the same size underlying bit arrays (%s != %s)", this.bitSize(), that.bitSize());
      Preconditions.checkArgument(this.strategy.equals(that.strategy), "BloomFilters must have equal strategies (%s != %s)", this.strategy, that.strategy);
      Preconditions.checkArgument(this.funnel.equals(that.funnel), "BloomFilters must have equal funnels (%s != %s)", this.funnel, that.funnel);
      this.bits.putAll(that.bits);
   }

   public boolean equals(@Nullable Object object) {
      if (object == this) {
         return true;
      } else if (!(object instanceof BloomFilter)) {
         return false;
      } else {
         BloomFilter<?> that = (BloomFilter)object;
         return this.numHashFunctions == that.numHashFunctions && this.funnel.equals(that.funnel) && this.bits.equals(that.bits) && this.strategy.equals(that.strategy);
      }
   }

   public int hashCode() {
      return Objects.hashCode(this.numHashFunctions, this.funnel, this.strategy, this.bits);
   }

   @CheckReturnValue
   public static <T> BloomFilter<T> create(Funnel<? super T> funnel, int expectedInsertions, double fpp) {
      return create(funnel, (long)expectedInsertions, fpp);
   }

   @CheckReturnValue
   public static <T> BloomFilter<T> create(Funnel<? super T> funnel, long expectedInsertions, double fpp) {
      return create(funnel, expectedInsertions, fpp, BloomFilterStrategies.MURMUR128_MITZ_64);
   }

   @VisibleForTesting
   static <T> BloomFilter<T> create(Funnel<? super T> funnel, long expectedInsertions, double fpp, BloomFilter.Strategy strategy) {
      Preconditions.checkNotNull(funnel);
      Preconditions.checkArgument(expectedInsertions >= 0L, "Expected insertions (%s) must be >= 0", expectedInsertions);
      Preconditions.checkArgument(fpp > 0.0D, "False positive probability (%s) must be > 0.0", fpp);
      Preconditions.checkArgument(fpp < 1.0D, "False positive probability (%s) must be < 1.0", fpp);
      Preconditions.checkNotNull(strategy);
      if (expectedInsertions == 0L) {
         expectedInsertions = 1L;
      }

      long numBits = optimalNumOfBits(expectedInsertions, fpp);
      int numHashFunctions = optimalNumOfHashFunctions(expectedInsertions, numBits);

      try {
         return new BloomFilter(new BloomFilterStrategies.BitArray(numBits), numHashFunctions, funnel, strategy);
      } catch (IllegalArgumentException var10) {
         throw new IllegalArgumentException("Could not create BloomFilter of " + numBits + " bits", var10);
      }
   }

   @CheckReturnValue
   public static <T> BloomFilter<T> create(Funnel<? super T> funnel, int expectedInsertions) {
      return create(funnel, (long)expectedInsertions);
   }

   @CheckReturnValue
   public static <T> BloomFilter<T> create(Funnel<? super T> funnel, long expectedInsertions) {
      return create(funnel, expectedInsertions, 0.03D);
   }

   @VisibleForTesting
   static int optimalNumOfHashFunctions(long n, long m) {
      return Math.max(1, (int)Math.round((double)m / (double)n * Math.log(2.0D)));
   }

   @VisibleForTesting
   static long optimalNumOfBits(long n, double p) {
      if (p == 0.0D) {
         p = Double.MIN_VALUE;
      }

      return (long)((double)(-n) * Math.log(p) / (Math.log(2.0D) * Math.log(2.0D)));
   }

   private Object writeReplace() {
      return new BloomFilter.SerialForm(this);
   }

   public void writeTo(OutputStream out) throws IOException {
      DataOutputStream dout = new DataOutputStream(out);
      dout.writeByte(SignedBytes.checkedCast((long)this.strategy.ordinal()));
      dout.writeByte(UnsignedBytes.checkedCast((long)this.numHashFunctions));
      dout.writeInt(this.bits.data.length);
      long[] arr$ = this.bits.data;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         long value = arr$[i$];
         dout.writeLong(value);
      }

   }

   @CheckReturnValue
   public static <T> BloomFilter<T> readFrom(InputStream in, Funnel<T> funnel) throws IOException {
      Preconditions.checkNotNull(in, "InputStream");
      Preconditions.checkNotNull(funnel, "Funnel");
      int strategyOrdinal = -1;
      int numHashFunctions = -1;
      byte dataLength = -1;

      try {
         DataInputStream din = new DataInputStream(in);
         strategyOrdinal = din.readByte();
         int numHashFunctions = UnsignedBytes.toInt(din.readByte());
         int dataLength = din.readInt();
         BloomFilter.Strategy strategy = BloomFilterStrategies.values()[strategyOrdinal];
         long[] data = new long[dataLength];

         for(int i = 0; i < data.length; ++i) {
            data[i] = din.readLong();
         }

         return new BloomFilter(new BloomFilterStrategies.BitArray(data), numHashFunctions, funnel, strategy);
      } catch (RuntimeException var9) {
         IOException ioException = new IOException("Unable to deserialize BloomFilter from InputStream. strategyOrdinal: " + strategyOrdinal + " numHashFunctions: " + numHashFunctions + " dataLength: " + dataLength);
         ioException.initCause(var9);
         throw ioException;
      }
   }

   // $FF: synthetic method
   BloomFilter(BloomFilterStrategies.BitArray x0, int x1, Funnel x2, BloomFilter.Strategy x3, Object x4) {
      this(x0, x1, x2, x3);
   }

   private static class SerialForm<T> implements Serializable {
      final long[] data;
      final int numHashFunctions;
      final Funnel<? super T> funnel;
      final BloomFilter.Strategy strategy;
      private static final long serialVersionUID = 1L;

      SerialForm(BloomFilter<T> bf) {
         this.data = bf.bits.data;
         this.numHashFunctions = bf.numHashFunctions;
         this.funnel = bf.funnel;
         this.strategy = bf.strategy;
      }

      Object readResolve() {
         return new BloomFilter(new BloomFilterStrategies.BitArray(this.data), this.numHashFunctions, this.funnel, this.strategy);
      }
   }

   interface Strategy extends Serializable {
      <T> boolean put(T var1, Funnel<? super T> var2, int var3, BloomFilterStrategies.BitArray var4);

      <T> boolean mightContain(T var1, Funnel<? super T> var2, int var3, BloomFilterStrategies.BitArray var4);

      int ordinal();
   }
}
