package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@CheckReturnValue
@GwtCompatible(
   emulated = true
)
public final class Predicates {
   private static final Joiner COMMA_JOINER = Joiner.on(',');

   private Predicates() {
   }

   @GwtCompatible(
      serializable = true
   )
   public static <T> Predicate<T> alwaysTrue() {
      return Predicates.ObjectPredicate.ALWAYS_TRUE.withNarrowedType();
   }

   @GwtCompatible(
      serializable = true
   )
   public static <T> Predicate<T> alwaysFalse() {
      return Predicates.ObjectPredicate.ALWAYS_FALSE.withNarrowedType();
   }

   @GwtCompatible(
      serializable = true
   )
   public static <T> Predicate<T> isNull() {
      return Predicates.ObjectPredicate.IS_NULL.withNarrowedType();
   }

   @GwtCompatible(
      serializable = true
   )
   public static <T> Predicate<T> notNull() {
      return Predicates.ObjectPredicate.NOT_NULL.withNarrowedType();
   }

   public static <T> Predicate<T> not(Predicate<T> predicate) {
      return new Predicates.NotPredicate(predicate);
   }

   public static <T> Predicate<T> and(Iterable<? extends Predicate<? super T>> components) {
      return new Predicates.AndPredicate(defensiveCopy(components));
   }

   public static <T> Predicate<T> and(Predicate<? super T>... components) {
      return new Predicates.AndPredicate(defensiveCopy((Object[])components));
   }

   public static <T> Predicate<T> and(Predicate<? super T> first, Predicate<? super T> second) {
      return new Predicates.AndPredicate(asList((Predicate)Preconditions.checkNotNull(first), (Predicate)Preconditions.checkNotNull(second)));
   }

   public static <T> Predicate<T> or(Iterable<? extends Predicate<? super T>> components) {
      return new Predicates.OrPredicate(defensiveCopy(components));
   }

   public static <T> Predicate<T> or(Predicate<? super T>... components) {
      return new Predicates.OrPredicate(defensiveCopy((Object[])components));
   }

   public static <T> Predicate<T> or(Predicate<? super T> first, Predicate<? super T> second) {
      return new Predicates.OrPredicate(asList((Predicate)Preconditions.checkNotNull(first), (Predicate)Preconditions.checkNotNull(second)));
   }

   public static <T> Predicate<T> equalTo(@Nullable T target) {
      return (Predicate)(target == null ? isNull() : new Predicates.IsEqualToPredicate(target));
   }

   @GwtIncompatible("Class.isInstance")
   public static Predicate<Object> instanceOf(Class<?> clazz) {
      return new Predicates.InstanceOfPredicate(clazz);
   }

   @GwtIncompatible("Class.isAssignableFrom")
   @Beta
   public static Predicate<Class<?>> assignableFrom(Class<?> clazz) {
      return new Predicates.AssignableFromPredicate(clazz);
   }

   public static <T> Predicate<T> in(Collection<? extends T> target) {
      return new Predicates.InPredicate(target);
   }

   public static <A, B> Predicate<A> compose(Predicate<B> predicate, Function<A, ? extends B> function) {
      return new Predicates.CompositionPredicate(predicate, function);
   }

   @GwtIncompatible("java.util.regex.Pattern")
   public static Predicate<CharSequence> containsPattern(String pattern) {
      return new Predicates.ContainsPatternFromStringPredicate(pattern);
   }

   @GwtIncompatible("java.util.regex.Pattern")
   public static Predicate<CharSequence> contains(Pattern pattern) {
      return new Predicates.ContainsPatternPredicate(pattern);
   }

   private static <T> List<Predicate<? super T>> asList(Predicate<? super T> first, Predicate<? super T> second) {
      return Arrays.asList(first, second);
   }

   private static <T> List<T> defensiveCopy(T... array) {
      return defensiveCopy((Iterable)Arrays.asList(array));
   }

   static <T> List<T> defensiveCopy(Iterable<T> iterable) {
      ArrayList<T> list = new ArrayList();
      Iterator i$ = iterable.iterator();

      while(i$.hasNext()) {
         T element = i$.next();
         list.add(Preconditions.checkNotNull(element));
      }

      return list;
   }

   @GwtIncompatible("Only used by other GWT-incompatible code.")
   private static class ContainsPatternFromStringPredicate extends Predicates.ContainsPatternPredicate {
      private static final long serialVersionUID = 0L;

      ContainsPatternFromStringPredicate(String string) {
         super(Pattern.compile(string));
      }

      public String toString() {
         return "Predicates.containsPattern(" + this.pattern.pattern() + ")";
      }
   }

   @GwtIncompatible("Only used by other GWT-incompatible code.")
   private static class ContainsPatternPredicate implements Predicate<CharSequence>, Serializable {
      final Pattern pattern;
      private static final long serialVersionUID = 0L;

      ContainsPatternPredicate(Pattern pattern) {
         this.pattern = (Pattern)Preconditions.checkNotNull(pattern);
      }

      public boolean apply(CharSequence t) {
         return this.pattern.matcher(t).find();
      }

      public int hashCode() {
         return Objects.hashCode(this.pattern.pattern(), this.pattern.flags());
      }

      public boolean equals(@Nullable Object obj) {
         if (!(obj instanceof Predicates.ContainsPatternPredicate)) {
            return false;
         } else {
            Predicates.ContainsPatternPredicate that = (Predicates.ContainsPatternPredicate)obj;
            return Objects.equal(this.pattern.pattern(), that.pattern.pattern()) && Objects.equal(this.pattern.flags(), that.pattern.flags());
         }
      }

      public String toString() {
         String patternString = Objects.toStringHelper((Object)this.pattern).add("pattern", this.pattern.pattern()).add("pattern.flags", this.pattern.flags()).toString();
         return "Predicates.contains(" + patternString + ")";
      }
   }

   private static class CompositionPredicate<A, B> implements Predicate<A>, Serializable {
      final Predicate<B> p;
      final Function<A, ? extends B> f;
      private static final long serialVersionUID = 0L;

      private CompositionPredicate(Predicate<B> p, Function<A, ? extends B> f) {
         this.p = (Predicate)Preconditions.checkNotNull(p);
         this.f = (Function)Preconditions.checkNotNull(f);
      }

      public boolean apply(@Nullable A a) {
         return this.p.apply(this.f.apply(a));
      }

      public boolean equals(@Nullable Object obj) {
         if (!(obj instanceof Predicates.CompositionPredicate)) {
            return false;
         } else {
            Predicates.CompositionPredicate<?, ?> that = (Predicates.CompositionPredicate)obj;
            return this.f.equals(that.f) && this.p.equals(that.p);
         }
      }

      public int hashCode() {
         return this.f.hashCode() ^ this.p.hashCode();
      }

      public String toString() {
         return this.p + "(" + this.f + ")";
      }

      // $FF: synthetic method
      CompositionPredicate(Predicate x0, Function x1, Object x2) {
         this(x0, x1);
      }
   }

   private static class InPredicate<T> implements Predicate<T>, Serializable {
      private final Collection<?> target;
      private static final long serialVersionUID = 0L;

      private InPredicate(Collection<?> target) {
         this.target = (Collection)Preconditions.checkNotNull(target);
      }

      public boolean apply(@Nullable T t) {
         try {
            return this.target.contains(t);
         } catch (NullPointerException var3) {
            return false;
         } catch (ClassCastException var4) {
            return false;
         }
      }

      public boolean equals(@Nullable Object obj) {
         if (obj instanceof Predicates.InPredicate) {
            Predicates.InPredicate<?> that = (Predicates.InPredicate)obj;
            return this.target.equals(that.target);
         } else {
            return false;
         }
      }

      public int hashCode() {
         return this.target.hashCode();
      }

      public String toString() {
         return "Predicates.in(" + this.target + ")";
      }

      // $FF: synthetic method
      InPredicate(Collection x0, Object x1) {
         this(x0);
      }
   }

   @GwtIncompatible("Class.isAssignableFrom")
   private static class AssignableFromPredicate implements Predicate<Class<?>>, Serializable {
      private final Class<?> clazz;
      private static final long serialVersionUID = 0L;

      private AssignableFromPredicate(Class<?> clazz) {
         this.clazz = (Class)Preconditions.checkNotNull(clazz);
      }

      public boolean apply(Class<?> input) {
         return this.clazz.isAssignableFrom(input);
      }

      public int hashCode() {
         return this.clazz.hashCode();
      }

      public boolean equals(@Nullable Object obj) {
         if (obj instanceof Predicates.AssignableFromPredicate) {
            Predicates.AssignableFromPredicate that = (Predicates.AssignableFromPredicate)obj;
            return this.clazz == that.clazz;
         } else {
            return false;
         }
      }

      public String toString() {
         return "Predicates.assignableFrom(" + this.clazz.getName() + ")";
      }

      // $FF: synthetic method
      AssignableFromPredicate(Class x0, Object x1) {
         this(x0);
      }
   }

   @GwtIncompatible("Class.isInstance")
   private static class InstanceOfPredicate implements Predicate<Object>, Serializable {
      private final Class<?> clazz;
      private static final long serialVersionUID = 0L;

      private InstanceOfPredicate(Class<?> clazz) {
         this.clazz = (Class)Preconditions.checkNotNull(clazz);
      }

      public boolean apply(@Nullable Object o) {
         return this.clazz.isInstance(o);
      }

      public int hashCode() {
         return this.clazz.hashCode();
      }

      public boolean equals(@Nullable Object obj) {
         if (obj instanceof Predicates.InstanceOfPredicate) {
            Predicates.InstanceOfPredicate that = (Predicates.InstanceOfPredicate)obj;
            return this.clazz == that.clazz;
         } else {
            return false;
         }
      }

      public String toString() {
         return "Predicates.instanceOf(" + this.clazz.getName() + ")";
      }

      // $FF: synthetic method
      InstanceOfPredicate(Class x0, Object x1) {
         this(x0);
      }
   }

   private static class IsEqualToPredicate<T> implements Predicate<T>, Serializable {
      private final T target;
      private static final long serialVersionUID = 0L;

      private IsEqualToPredicate(T target) {
         this.target = target;
      }

      public boolean apply(T t) {
         return this.target.equals(t);
      }

      public int hashCode() {
         return this.target.hashCode();
      }

      public boolean equals(@Nullable Object obj) {
         if (obj instanceof Predicates.IsEqualToPredicate) {
            Predicates.IsEqualToPredicate<?> that = (Predicates.IsEqualToPredicate)obj;
            return this.target.equals(that.target);
         } else {
            return false;
         }
      }

      public String toString() {
         return "Predicates.equalTo(" + this.target + ")";
      }

      // $FF: synthetic method
      IsEqualToPredicate(Object x0, Object x1) {
         this(x0);
      }
   }

   private static class OrPredicate<T> implements Predicate<T>, Serializable {
      private final List<? extends Predicate<? super T>> components;
      private static final long serialVersionUID = 0L;

      private OrPredicate(List<? extends Predicate<? super T>> components) {
         this.components = components;
      }

      public boolean apply(@Nullable T t) {
         for(int i = 0; i < this.components.size(); ++i) {
            if (((Predicate)this.components.get(i)).apply(t)) {
               return true;
            }
         }

         return false;
      }

      public int hashCode() {
         return this.components.hashCode() + 87855567;
      }

      public boolean equals(@Nullable Object obj) {
         if (obj instanceof Predicates.OrPredicate) {
            Predicates.OrPredicate<?> that = (Predicates.OrPredicate)obj;
            return this.components.equals(that.components);
         } else {
            return false;
         }
      }

      public String toString() {
         return "Predicates.or(" + Predicates.COMMA_JOINER.join((Iterable)this.components) + ")";
      }

      // $FF: synthetic method
      OrPredicate(List x0, Object x1) {
         this(x0);
      }
   }

   private static class AndPredicate<T> implements Predicate<T>, Serializable {
      private final List<? extends Predicate<? super T>> components;
      private static final long serialVersionUID = 0L;

      private AndPredicate(List<? extends Predicate<? super T>> components) {
         this.components = components;
      }

      public boolean apply(@Nullable T t) {
         for(int i = 0; i < this.components.size(); ++i) {
            if (!((Predicate)this.components.get(i)).apply(t)) {
               return false;
            }
         }

         return true;
      }

      public int hashCode() {
         return this.components.hashCode() + 306654252;
      }

      public boolean equals(@Nullable Object obj) {
         if (obj instanceof Predicates.AndPredicate) {
            Predicates.AndPredicate<?> that = (Predicates.AndPredicate)obj;
            return this.components.equals(that.components);
         } else {
            return false;
         }
      }

      public String toString() {
         return "Predicates.and(" + Predicates.COMMA_JOINER.join((Iterable)this.components) + ")";
      }

      // $FF: synthetic method
      AndPredicate(List x0, Object x1) {
         this(x0);
      }
   }

   private static class NotPredicate<T> implements Predicate<T>, Serializable {
      final Predicate<T> predicate;
      private static final long serialVersionUID = 0L;

      NotPredicate(Predicate<T> predicate) {
         this.predicate = (Predicate)Preconditions.checkNotNull(predicate);
      }

      public boolean apply(@Nullable T t) {
         return !this.predicate.apply(t);
      }

      public int hashCode() {
         return ~this.predicate.hashCode();
      }

      public boolean equals(@Nullable Object obj) {
         if (obj instanceof Predicates.NotPredicate) {
            Predicates.NotPredicate<?> that = (Predicates.NotPredicate)obj;
            return this.predicate.equals(that.predicate);
         } else {
            return false;
         }
      }

      public String toString() {
         return "Predicates.not(" + this.predicate + ")";
      }
   }

   static enum ObjectPredicate implements Predicate<Object> {
      ALWAYS_TRUE {
         public boolean apply(@Nullable Object o) {
            return true;
         }

         public String toString() {
            return "Predicates.alwaysTrue()";
         }
      },
      ALWAYS_FALSE {
         public boolean apply(@Nullable Object o) {
            return false;
         }

         public String toString() {
            return "Predicates.alwaysFalse()";
         }
      },
      IS_NULL {
         public boolean apply(@Nullable Object o) {
            return o == null;
         }

         public String toString() {
            return "Predicates.isNull()";
         }
      },
      NOT_NULL {
         public boolean apply(@Nullable Object o) {
            return o != null;
         }

         public String toString() {
            return "Predicates.notNull()";
         }
      };

      private ObjectPredicate() {
      }

      <T> Predicate<T> withNarrowedType() {
         return this;
      }

      // $FF: synthetic method
      ObjectPredicate(Object x2) {
         this();
      }
   }
}
