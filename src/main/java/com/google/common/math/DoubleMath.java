package com.google.common.math;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Booleans;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Iterator;

@GwtCompatible(
   emulated = true
)
public final class DoubleMath {
   private static final double MIN_INT_AS_DOUBLE = -2.147483648E9D;
   private static final double MAX_INT_AS_DOUBLE = 2.147483647E9D;
   private static final double MIN_LONG_AS_DOUBLE = -9.223372036854776E18D;
   private static final double MAX_LONG_AS_DOUBLE_PLUS_ONE = 9.223372036854776E18D;
   private static final double LN_2 = Math.log(2.0D);
   @VisibleForTesting
   static final int MAX_FACTORIAL = 170;
   @VisibleForTesting
   static final double[] everySixteenthFactorial = new double[]{1.0D, 2.0922789888E13D, 2.631308369336935E35D, 1.2413915592536073E61D, 1.2688693218588417E89D, 7.156945704626381E118D, 9.916779348709496E149D, 1.974506857221074E182D, 3.856204823625804E215D, 5.5502938327393044E249D, 4.7147236359920616E284D};

   @GwtIncompatible("#isMathematicalInteger, com.google.common.math.DoubleUtils")
   static double roundIntermediate(double x, RoundingMode mode) {
      if (!DoubleUtils.isFinite(x)) {
         throw new ArithmeticException("input is infinite or NaN");
      } else {
         double z;
         switch(mode) {
         case UNNECESSARY:
            MathPreconditions.checkRoundingUnnecessary(isMathematicalInteger(x));
            return x;
         case FLOOR:
            if (!(x >= 0.0D) && !isMathematicalInteger(x)) {
               return x - 1.0D;
            }

            return x;
         case CEILING:
            if (!(x <= 0.0D) && !isMathematicalInteger(x)) {
               return x + 1.0D;
            }

            return x;
         case DOWN:
            return x;
         case UP:
            if (isMathematicalInteger(x)) {
               return x;
            }

            return x + Math.copySign(1.0D, x);
         case HALF_EVEN:
            return Math.rint(x);
         case HALF_UP:
            z = Math.rint(x);
            if (Math.abs(x - z) == 0.5D) {
               return x + Math.copySign(0.5D, x);
            }

            return z;
         case HALF_DOWN:
            z = Math.rint(x);
            if (Math.abs(x - z) == 0.5D) {
               return x;
            }

            return z;
         default:
            throw new AssertionError();
         }
      }
   }

   @GwtIncompatible("#roundIntermediate")
   public static int roundToInt(double x, RoundingMode mode) {
      double z = roundIntermediate(x, mode);
      MathPreconditions.checkInRange(z > -2.147483649E9D & z < 2.147483648E9D);
      return (int)z;
   }

   @GwtIncompatible("#roundIntermediate")
   public static long roundToLong(double x, RoundingMode mode) {
      double z = roundIntermediate(x, mode);
      MathPreconditions.checkInRange(-9.223372036854776E18D - z < 1.0D & z < 9.223372036854776E18D);
      return (long)z;
   }

   @GwtIncompatible("#roundIntermediate, java.lang.Math.getExponent, com.google.common.math.DoubleUtils")
   public static BigInteger roundToBigInteger(double x, RoundingMode mode) {
      x = roundIntermediate(x, mode);
      if (-9.223372036854776E18D - x < 1.0D & x < 9.223372036854776E18D) {
         return BigInteger.valueOf((long)x);
      } else {
         int exponent = Math.getExponent(x);
         long significand = DoubleUtils.getSignificand(x);
         BigInteger result = BigInteger.valueOf(significand).shiftLeft(exponent - 52);
         return x < 0.0D ? result.negate() : result;
      }
   }

   @GwtIncompatible("com.google.common.math.DoubleUtils")
   public static boolean isPowerOfTwo(double x) {
      return x > 0.0D && DoubleUtils.isFinite(x) && LongMath.isPowerOfTwo(DoubleUtils.getSignificand(x));
   }

   public static double log2(double x) {
      return Math.log(x) / LN_2;
   }

   @GwtIncompatible("java.lang.Math.getExponent, com.google.common.math.DoubleUtils")
   public static int log2(double x, RoundingMode mode) {
      Preconditions.checkArgument(x > 0.0D && DoubleUtils.isFinite(x), "x must be positive and finite");
      int exponent = Math.getExponent(x);
      if (!DoubleUtils.isNormal(x)) {
         return log2(x * 4.503599627370496E15D, mode) - 52;
      } else {
         boolean increment;
         switch(mode) {
         case UNNECESSARY:
            MathPreconditions.checkRoundingUnnecessary(isPowerOfTwo(x));
         case FLOOR:
            increment = false;
            break;
         case CEILING:
            increment = !isPowerOfTwo(x);
            break;
         case DOWN:
            increment = exponent < 0 & !isPowerOfTwo(x);
            break;
         case UP:
            increment = exponent >= 0 & !isPowerOfTwo(x);
            break;
         case HALF_EVEN:
         case HALF_UP:
         case HALF_DOWN:
            double xScaled = DoubleUtils.scaleNormalize(x);
            increment = xScaled * xScaled > 2.0D;
            break;
         default:
            throw new AssertionError();
         }

         return increment ? exponent + 1 : exponent;
      }
   }

   @GwtIncompatible("java.lang.Math.getExponent, com.google.common.math.DoubleUtils")
   public static boolean isMathematicalInteger(double x) {
      return DoubleUtils.isFinite(x) && (x == 0.0D || 52 - Long.numberOfTrailingZeros(DoubleUtils.getSignificand(x)) <= Math.getExponent(x));
   }

   public static double factorial(int n) {
      MathPreconditions.checkNonNegative("n", n);
      if (n > 170) {
         return Double.POSITIVE_INFINITY;
      } else {
         double accum = 1.0D;

         for(int i = 1 + (n & -16); i <= n; ++i) {
            accum *= (double)i;
         }

         return accum * everySixteenthFactorial[n >> 4];
      }
   }

   public static boolean fuzzyEquals(double a, double b, double tolerance) {
      MathPreconditions.checkNonNegative("tolerance", tolerance);
      return Math.copySign(a - b, 1.0D) <= tolerance || a == b || Double.isNaN(a) && Double.isNaN(b);
   }

   public static int fuzzyCompare(double a, double b, double tolerance) {
      if (fuzzyEquals(a, b, tolerance)) {
         return 0;
      } else if (a < b) {
         return -1;
      } else {
         return a > b ? 1 : Booleans.compare(Double.isNaN(a), Double.isNaN(b));
      }
   }

   @GwtIncompatible("com.google.common.math.DoubleUtils")
   public static double mean(double... values) {
      Preconditions.checkArgument(values.length > 0, "Cannot take mean of 0 values");
      long count = 1L;
      double mean = checkFinite(values[0]);

      for(int index = 1; index < values.length; ++index) {
         checkFinite(values[index]);
         ++count;
         mean += (values[index] - mean) / (double)count;
      }

      return mean;
   }

   public static double mean(int... values) {
      Preconditions.checkArgument(values.length > 0, "Cannot take mean of 0 values");
      long sum = 0L;

      for(int index = 0; index < values.length; ++index) {
         sum += (long)values[index];
      }

      return (double)sum / (double)values.length;
   }

   public static double mean(long... values) {
      Preconditions.checkArgument(values.length > 0, "Cannot take mean of 0 values");
      long count = 1L;
      double mean = (double)values[0];

      for(int index = 1; index < values.length; ++index) {
         ++count;
         mean += ((double)values[index] - mean) / (double)count;
      }

      return mean;
   }

   @GwtIncompatible("com.google.common.math.DoubleUtils")
   public static double mean(Iterable<? extends Number> values) {
      return mean(values.iterator());
   }

   @GwtIncompatible("com.google.common.math.DoubleUtils")
   public static double mean(Iterator<? extends Number> values) {
      Preconditions.checkArgument(values.hasNext(), "Cannot take mean of 0 values");
      long count = 1L;

      double mean;
      double value;
      for(mean = checkFinite(((Number)values.next()).doubleValue()); values.hasNext(); mean += (value - mean) / (double)count) {
         value = checkFinite(((Number)values.next()).doubleValue());
         ++count;
      }

      return mean;
   }

   @GwtIncompatible("com.google.common.math.DoubleUtils")
   private static double checkFinite(double argument) {
      Preconditions.checkArgument(DoubleUtils.isFinite(argument));
      return argument;
   }

   private DoubleMath() {
   }
}
