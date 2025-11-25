package com.google.common.primitives;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Converter;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;
import javax.annotation.CheckForNull;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@CheckReturnValue
@GwtCompatible(
   emulated = true
)
public final class Ints {
   public static final int BYTES = 4;
   public static final int MAX_POWER_OF_TWO = 1073741824;

   private Ints() {
   }

   public static int hashCode(int value) {
      return value;
   }

   public static int checkedCast(long value) {
      int result = (int)value;
      if ((long)result != value) {
         throw new IllegalArgumentException("Out of range: " + value);
      } else {
         return result;
      }
   }

   public static int saturatedCast(long value) {
      if (value > 2147483647L) {
         return Integer.MAX_VALUE;
      } else {
         return value < -2147483648L ? Integer.MIN_VALUE : (int)value;
      }
   }

   public static int compare(int a, int b) {
      return a < b ? -1 : (a > b ? 1 : 0);
   }

   public static boolean contains(int[] array, int target) {
      int[] arr$ = array;
      int len$ = array.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         int value = arr$[i$];
         if (value == target) {
            return true;
         }
      }

      return false;
   }

   public static int indexOf(int[] array, int target) {
      return indexOf(array, target, 0, array.length);
   }

   private static int indexOf(int[] array, int target, int start, int end) {
      for(int i = start; i < end; ++i) {
         if (array[i] == target) {
            return i;
         }
      }

      return -1;
   }

   public static int indexOf(int[] array, int[] target) {
      Preconditions.checkNotNull(array, "array");
      Preconditions.checkNotNull(target, "target");
      if (target.length == 0) {
         return 0;
      } else {
         label28:
         for(int i = 0; i < array.length - target.length + 1; ++i) {
            for(int j = 0; j < target.length; ++j) {
               if (array[i + j] != target[j]) {
                  continue label28;
               }
            }

            return i;
         }

         return -1;
      }
   }

   public static int lastIndexOf(int[] array, int target) {
      return lastIndexOf(array, target, 0, array.length);
   }

   private static int lastIndexOf(int[] array, int target, int start, int end) {
      for(int i = end - 1; i >= start; --i) {
         if (array[i] == target) {
            return i;
         }
      }

      return -1;
   }

   public static int min(int... array) {
      Preconditions.checkArgument(array.length > 0);
      int min = array[0];

      for(int i = 1; i < array.length; ++i) {
         if (array[i] < min) {
            min = array[i];
         }
      }

      return min;
   }

   public static int max(int... array) {
      Preconditions.checkArgument(array.length > 0);
      int max = array[0];

      for(int i = 1; i < array.length; ++i) {
         if (array[i] > max) {
            max = array[i];
         }
      }

      return max;
   }

   public static int[] concat(int[]... arrays) {
      int length = 0;
      int[][] arr$ = arrays;
      int pos = arrays.length;

      for(int i$ = 0; i$ < pos; ++i$) {
         int[] array = arr$[i$];
         length += array.length;
      }

      int[] result = new int[length];
      pos = 0;
      int[][] arr$ = arrays;
      int len$ = arrays.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         int[] array = arr$[i$];
         System.arraycopy(array, 0, result, pos, array.length);
         pos += array.length;
      }

      return result;
   }

   @GwtIncompatible("doesn't work")
   public static byte[] toByteArray(int value) {
      return new byte[]{(byte)(value >> 24), (byte)(value >> 16), (byte)(value >> 8), (byte)value};
   }

   @GwtIncompatible("doesn't work")
   public static int fromByteArray(byte[] bytes) {
      Preconditions.checkArgument(bytes.length >= 4, "array too small: %s < %s", bytes.length, 4);
      return fromBytes(bytes[0], bytes[1], bytes[2], bytes[3]);
   }

   @GwtIncompatible("doesn't work")
   public static int fromBytes(byte b1, byte b2, byte b3, byte b4) {
      return b1 << 24 | (b2 & 255) << 16 | (b3 & 255) << 8 | b4 & 255;
   }

   @Beta
   public static Converter<String, Integer> stringConverter() {
      return Ints.IntConverter.INSTANCE;
   }

   public static int[] ensureCapacity(int[] array, int minLength, int padding) {
      Preconditions.checkArgument(minLength >= 0, "Invalid minLength: %s", minLength);
      Preconditions.checkArgument(padding >= 0, "Invalid padding: %s", padding);
      return array.length < minLength ? copyOf(array, minLength + padding) : array;
   }

   private static int[] copyOf(int[] original, int length) {
      int[] copy = new int[length];
      System.arraycopy(original, 0, copy, 0, Math.min(original.length, length));
      return copy;
   }

   public static String join(String separator, int... array) {
      Preconditions.checkNotNull(separator);
      if (array.length == 0) {
         return "";
      } else {
         StringBuilder builder = new StringBuilder(array.length * 5);
         builder.append(array[0]);

         for(int i = 1; i < array.length; ++i) {
            builder.append(separator).append(array[i]);
         }

         return builder.toString();
      }
   }

   public static Comparator<int[]> lexicographicalComparator() {
      return Ints.LexicographicalComparator.INSTANCE;
   }

   public static int[] toArray(Collection<? extends Number> collection) {
      if (collection instanceof Ints.IntArrayAsList) {
         return ((Ints.IntArrayAsList)collection).toIntArray();
      } else {
         Object[] boxedArray = collection.toArray();
         int len = boxedArray.length;
         int[] array = new int[len];

         for(int i = 0; i < len; ++i) {
            array[i] = ((Number)Preconditions.checkNotNull(boxedArray[i])).intValue();
         }

         return array;
      }
   }

   public static List<Integer> asList(int... backingArray) {
      return (List)(backingArray.length == 0 ? Collections.emptyList() : new Ints.IntArrayAsList(backingArray));
   }

   @Nullable
   @CheckForNull
   @Beta
   public static Integer tryParse(String string) {
      return tryParse(string, 10);
   }

   @Nullable
   @CheckForNull
   @Beta
   public static Integer tryParse(String string, int radix) {
      Long result = Longs.tryParse(string, radix);
      return result != null && result == (long)result.intValue() ? result.intValue() : null;
   }

   @GwtCompatible
   private static class IntArrayAsList extends AbstractList<Integer> implements RandomAccess, Serializable {
      final int[] array;
      final int start;
      final int end;
      private static final long serialVersionUID = 0L;

      IntArrayAsList(int[] array) {
         this(array, 0, array.length);
      }

      IntArrayAsList(int[] array, int start, int end) {
         this.array = array;
         this.start = start;
         this.end = end;
      }

      public int size() {
         return this.end - this.start;
      }

      public boolean isEmpty() {
         return false;
      }

      public Integer get(int index) {
         Preconditions.checkElementIndex(index, this.size());
         return this.array[this.start + index];
      }

      public boolean contains(Object target) {
         return target instanceof Integer && Ints.indexOf(this.array, (Integer)target, this.start, this.end) != -1;
      }

      public int indexOf(Object target) {
         if (target instanceof Integer) {
            int i = Ints.indexOf(this.array, (Integer)target, this.start, this.end);
            if (i >= 0) {
               return i - this.start;
            }
         }

         return -1;
      }

      public int lastIndexOf(Object target) {
         if (target instanceof Integer) {
            int i = Ints.lastIndexOf(this.array, (Integer)target, this.start, this.end);
            if (i >= 0) {
               return i - this.start;
            }
         }

         return -1;
      }

      public Integer set(int index, Integer element) {
         Preconditions.checkElementIndex(index, this.size());
         int oldValue = this.array[this.start + index];
         this.array[this.start + index] = (Integer)Preconditions.checkNotNull(element);
         return oldValue;
      }

      public List<Integer> subList(int fromIndex, int toIndex) {
         int size = this.size();
         Preconditions.checkPositionIndexes(fromIndex, toIndex, size);
         return (List)(fromIndex == toIndex ? Collections.emptyList() : new Ints.IntArrayAsList(this.array, this.start + fromIndex, this.start + toIndex));
      }

      public boolean equals(@Nullable Object object) {
         if (object == this) {
            return true;
         } else if (object instanceof Ints.IntArrayAsList) {
            Ints.IntArrayAsList that = (Ints.IntArrayAsList)object;
            int size = this.size();
            if (that.size() != size) {
               return false;
            } else {
               for(int i = 0; i < size; ++i) {
                  if (this.array[this.start + i] != that.array[that.start + i]) {
                     return false;
                  }
               }

               return true;
            }
         } else {
            return super.equals(object);
         }
      }

      public int hashCode() {
         int result = 1;

         for(int i = this.start; i < this.end; ++i) {
            result = 31 * result + Ints.hashCode(this.array[i]);
         }

         return result;
      }

      public String toString() {
         StringBuilder builder = new StringBuilder(this.size() * 5);
         builder.append('[').append(this.array[this.start]);

         for(int i = this.start + 1; i < this.end; ++i) {
            builder.append(", ").append(this.array[i]);
         }

         return builder.append(']').toString();
      }

      int[] toIntArray() {
         int size = this.size();
         int[] result = new int[size];
         System.arraycopy(this.array, this.start, result, 0, size);
         return result;
      }
   }

   private static enum LexicographicalComparator implements Comparator<int[]> {
      INSTANCE;

      public int compare(int[] left, int[] right) {
         int minLength = Math.min(left.length, right.length);

         for(int i = 0; i < minLength; ++i) {
            int result = Ints.compare(left[i], right[i]);
            if (result != 0) {
               return result;
            }
         }

         return left.length - right.length;
      }
   }

   private static final class IntConverter extends Converter<String, Integer> implements Serializable {
      static final Ints.IntConverter INSTANCE = new Ints.IntConverter();
      private static final long serialVersionUID = 1L;

      protected Integer doForward(String value) {
         return Integer.decode(value);
      }

      protected String doBackward(Integer value) {
         return value.toString();
      }

      public String toString() {
         return "Ints.stringConverter()";
      }

      private Object readResolve() {
         return INSTANCE;
      }
   }
}
