package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import java.util.Map;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@CheckReturnValue
@GwtCompatible
public final class Functions {
   private Functions() {
   }

   public static Function<Object, String> toStringFunction() {
      return Functions.ToStringFunction.INSTANCE;
   }

   public static <E> Function<E, E> identity() {
      return Functions.IdentityFunction.INSTANCE;
   }

   public static <K, V> Function<K, V> forMap(Map<K, V> map) {
      return new Functions.FunctionForMapNoDefault(map);
   }

   public static <K, V> Function<K, V> forMap(Map<K, ? extends V> map, @Nullable V defaultValue) {
      return new Functions.ForMapWithDefault(map, defaultValue);
   }

   public static <A, B, C> Function<A, C> compose(Function<B, C> g, Function<A, ? extends B> f) {
      return new Functions.FunctionComposition(g, f);
   }

   public static <T> Function<T, Boolean> forPredicate(Predicate<T> predicate) {
      return new Functions.PredicateFunction(predicate);
   }

   public static <E> Function<Object, E> constant(@Nullable E value) {
      return new Functions.ConstantFunction(value);
   }

   @Beta
   public static <T> Function<Object, T> forSupplier(Supplier<T> supplier) {
      return new Functions.SupplierFunction(supplier);
   }

   private static class SupplierFunction<T> implements Function<Object, T>, Serializable {
      private final Supplier<T> supplier;
      private static final long serialVersionUID = 0L;

      private SupplierFunction(Supplier<T> supplier) {
         this.supplier = (Supplier)Preconditions.checkNotNull(supplier);
      }

      public T apply(@Nullable Object input) {
         return this.supplier.get();
      }

      public boolean equals(@Nullable Object obj) {
         if (obj instanceof Functions.SupplierFunction) {
            Functions.SupplierFunction<?> that = (Functions.SupplierFunction)obj;
            return this.supplier.equals(that.supplier);
         } else {
            return false;
         }
      }

      public int hashCode() {
         return this.supplier.hashCode();
      }

      public String toString() {
         return "Functions.forSupplier(" + this.supplier + ")";
      }

      // $FF: synthetic method
      SupplierFunction(Supplier x0, Object x1) {
         this(x0);
      }
   }

   private static class ConstantFunction<E> implements Function<Object, E>, Serializable {
      private final E value;
      private static final long serialVersionUID = 0L;

      public ConstantFunction(@Nullable E value) {
         this.value = value;
      }

      public E apply(@Nullable Object from) {
         return this.value;
      }

      public boolean equals(@Nullable Object obj) {
         if (obj instanceof Functions.ConstantFunction) {
            Functions.ConstantFunction<?> that = (Functions.ConstantFunction)obj;
            return Objects.equal(this.value, that.value);
         } else {
            return false;
         }
      }

      public int hashCode() {
         return this.value == null ? 0 : this.value.hashCode();
      }

      public String toString() {
         return "Functions.constant(" + this.value + ")";
      }
   }

   private static class PredicateFunction<T> implements Function<T, Boolean>, Serializable {
      private final Predicate<T> predicate;
      private static final long serialVersionUID = 0L;

      private PredicateFunction(Predicate<T> predicate) {
         this.predicate = (Predicate)Preconditions.checkNotNull(predicate);
      }

      public Boolean apply(@Nullable T t) {
         return this.predicate.apply(t);
      }

      public boolean equals(@Nullable Object obj) {
         if (obj instanceof Functions.PredicateFunction) {
            Functions.PredicateFunction<?> that = (Functions.PredicateFunction)obj;
            return this.predicate.equals(that.predicate);
         } else {
            return false;
         }
      }

      public int hashCode() {
         return this.predicate.hashCode();
      }

      public String toString() {
         return "Functions.forPredicate(" + this.predicate + ")";
      }

      // $FF: synthetic method
      PredicateFunction(Predicate x0, Object x1) {
         this(x0);
      }
   }

   private static class FunctionComposition<A, B, C> implements Function<A, C>, Serializable {
      private final Function<B, C> g;
      private final Function<A, ? extends B> f;
      private static final long serialVersionUID = 0L;

      public FunctionComposition(Function<B, C> g, Function<A, ? extends B> f) {
         this.g = (Function)Preconditions.checkNotNull(g);
         this.f = (Function)Preconditions.checkNotNull(f);
      }

      public C apply(@Nullable A a) {
         return this.g.apply(this.f.apply(a));
      }

      public boolean equals(@Nullable Object obj) {
         if (!(obj instanceof Functions.FunctionComposition)) {
            return false;
         } else {
            Functions.FunctionComposition<?, ?, ?> that = (Functions.FunctionComposition)obj;
            return this.f.equals(that.f) && this.g.equals(that.g);
         }
      }

      public int hashCode() {
         return this.f.hashCode() ^ this.g.hashCode();
      }

      public String toString() {
         return this.g + "(" + this.f + ")";
      }
   }

   private static class ForMapWithDefault<K, V> implements Function<K, V>, Serializable {
      final Map<K, ? extends V> map;
      final V defaultValue;
      private static final long serialVersionUID = 0L;

      ForMapWithDefault(Map<K, ? extends V> map, @Nullable V defaultValue) {
         this.map = (Map)Preconditions.checkNotNull(map);
         this.defaultValue = defaultValue;
      }

      public V apply(@Nullable K key) {
         V result = this.map.get(key);
         return result == null && !this.map.containsKey(key) ? this.defaultValue : result;
      }

      public boolean equals(@Nullable Object o) {
         if (!(o instanceof Functions.ForMapWithDefault)) {
            return false;
         } else {
            Functions.ForMapWithDefault<?, ?> that = (Functions.ForMapWithDefault)o;
            return this.map.equals(that.map) && Objects.equal(this.defaultValue, that.defaultValue);
         }
      }

      public int hashCode() {
         return Objects.hashCode(this.map, this.defaultValue);
      }

      public String toString() {
         return "Functions.forMap(" + this.map + ", defaultValue=" + this.defaultValue + ")";
      }
   }

   private static class FunctionForMapNoDefault<K, V> implements Function<K, V>, Serializable {
      final Map<K, V> map;
      private static final long serialVersionUID = 0L;

      FunctionForMapNoDefault(Map<K, V> map) {
         this.map = (Map)Preconditions.checkNotNull(map);
      }

      public V apply(@Nullable K key) {
         V result = this.map.get(key);
         Preconditions.checkArgument(result != null || this.map.containsKey(key), "Key '%s' not present in map", key);
         return result;
      }

      public boolean equals(@Nullable Object o) {
         if (o instanceof Functions.FunctionForMapNoDefault) {
            Functions.FunctionForMapNoDefault<?, ?> that = (Functions.FunctionForMapNoDefault)o;
            return this.map.equals(that.map);
         } else {
            return false;
         }
      }

      public int hashCode() {
         return this.map.hashCode();
      }

      public String toString() {
         return "Functions.forMap(" + this.map + ")";
      }
   }

   private static enum IdentityFunction implements Function<Object, Object> {
      INSTANCE;

      @Nullable
      public Object apply(@Nullable Object o) {
         return o;
      }

      public String toString() {
         return "Functions.identity()";
      }
   }

   private static enum ToStringFunction implements Function<Object, String> {
      INSTANCE;

      public String apply(Object o) {
         Preconditions.checkNotNull(o);
         return o.toString();
      }

      public String toString() {
         return "Functions.toStringFunction()";
      }
   }
}
