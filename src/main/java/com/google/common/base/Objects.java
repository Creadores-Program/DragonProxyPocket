package com.google.common.base;

import com.google.common.annotations.GwtCompatible;
import java.util.Arrays;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@GwtCompatible
public final class Objects {
   private Objects() {
   }

   @CheckReturnValue
   public static boolean equal(@Nullable Object a, @Nullable Object b) {
      return a == b || a != null && a.equals(b);
   }

   @CheckReturnValue
   public static int hashCode(@Nullable Object... objects) {
      return Arrays.hashCode(objects);
   }

   /** @deprecated */
   @CheckReturnValue
   @Deprecated
   public static Objects.ToStringHelper toStringHelper(Object self) {
      return new Objects.ToStringHelper(self.getClass().getSimpleName());
   }

   /** @deprecated */
   @CheckReturnValue
   @Deprecated
   public static Objects.ToStringHelper toStringHelper(Class<?> clazz) {
      return new Objects.ToStringHelper(clazz.getSimpleName());
   }

   /** @deprecated */
   @CheckReturnValue
   @Deprecated
   public static Objects.ToStringHelper toStringHelper(String className) {
      return new Objects.ToStringHelper(className);
   }

   /** @deprecated */
   @CheckReturnValue
   @Deprecated
   public static <T> T firstNonNull(@Nullable T first, @Nullable T second) {
      return MoreObjects.firstNonNull(first, second);
   }

   /** @deprecated */
   @Deprecated
   public static final class ToStringHelper {
      private final String className;
      private Objects.ToStringHelper.ValueHolder holderHead;
      private Objects.ToStringHelper.ValueHolder holderTail;
      private boolean omitNullValues;

      private ToStringHelper(String className) {
         this.holderHead = new Objects.ToStringHelper.ValueHolder();
         this.holderTail = this.holderHead;
         this.omitNullValues = false;
         this.className = (String)Preconditions.checkNotNull(className);
      }

      public Objects.ToStringHelper omitNullValues() {
         this.omitNullValues = true;
         return this;
      }

      public Objects.ToStringHelper add(String name, @Nullable Object value) {
         return this.addHolder(name, value);
      }

      public Objects.ToStringHelper add(String name, boolean value) {
         return this.addHolder(name, String.valueOf(value));
      }

      public Objects.ToStringHelper add(String name, char value) {
         return this.addHolder(name, String.valueOf(value));
      }

      public Objects.ToStringHelper add(String name, double value) {
         return this.addHolder(name, String.valueOf(value));
      }

      public Objects.ToStringHelper add(String name, float value) {
         return this.addHolder(name, String.valueOf(value));
      }

      public Objects.ToStringHelper add(String name, int value) {
         return this.addHolder(name, String.valueOf(value));
      }

      public Objects.ToStringHelper add(String name, long value) {
         return this.addHolder(name, String.valueOf(value));
      }

      public Objects.ToStringHelper addValue(@Nullable Object value) {
         return this.addHolder(value);
      }

      public Objects.ToStringHelper addValue(boolean value) {
         return this.addHolder(String.valueOf(value));
      }

      public Objects.ToStringHelper addValue(char value) {
         return this.addHolder(String.valueOf(value));
      }

      public Objects.ToStringHelper addValue(double value) {
         return this.addHolder(String.valueOf(value));
      }

      public Objects.ToStringHelper addValue(float value) {
         return this.addHolder(String.valueOf(value));
      }

      public Objects.ToStringHelper addValue(int value) {
         return this.addHolder(String.valueOf(value));
      }

      public Objects.ToStringHelper addValue(long value) {
         return this.addHolder(String.valueOf(value));
      }

      public String toString() {
         boolean omitNullValuesSnapshot = this.omitNullValues;
         String nextSeparator = "";
         StringBuilder builder = (new StringBuilder(32)).append(this.className).append('{');

         for(Objects.ToStringHelper.ValueHolder valueHolder = this.holderHead.next; valueHolder != null; valueHolder = valueHolder.next) {
            if (!omitNullValuesSnapshot || valueHolder.value != null) {
               builder.append(nextSeparator);
               nextSeparator = ", ";
               if (valueHolder.name != null) {
                  builder.append(valueHolder.name).append('=');
               }

               builder.append(valueHolder.value);
            }
         }

         return builder.append('}').toString();
      }

      private Objects.ToStringHelper.ValueHolder addHolder() {
         Objects.ToStringHelper.ValueHolder valueHolder = new Objects.ToStringHelper.ValueHolder();
         this.holderTail = this.holderTail.next = valueHolder;
         return valueHolder;
      }

      private Objects.ToStringHelper addHolder(@Nullable Object value) {
         Objects.ToStringHelper.ValueHolder valueHolder = this.addHolder();
         valueHolder.value = value;
         return this;
      }

      private Objects.ToStringHelper addHolder(String name, @Nullable Object value) {
         Objects.ToStringHelper.ValueHolder valueHolder = this.addHolder();
         valueHolder.value = value;
         valueHolder.name = (String)Preconditions.checkNotNull(name);
         return this;
      }

      // $FF: synthetic method
      ToStringHelper(String x0, Object x1) {
         this(x0);
      }

      private static final class ValueHolder {
         String name;
         Object value;
         Objects.ToStringHelper.ValueHolder next;

         private ValueHolder() {
         }

         // $FF: synthetic method
         ValueHolder(Object x0) {
            this();
         }
      }
   }
}
