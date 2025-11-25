package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import java.util.Iterator;
import javax.annotation.Nullable;

@Beta
@GwtCompatible
public abstract class Converter<A, B> implements Function<A, B> {
   private final boolean handleNullAutomatically;
   private transient Converter<B, A> reverse;

   protected Converter() {
      this(true);
   }

   Converter(boolean handleNullAutomatically) {
      this.handleNullAutomatically = handleNullAutomatically;
   }

   protected abstract B doForward(A var1);

   protected abstract A doBackward(B var1);

   @Nullable
   public final B convert(@Nullable A a) {
      return this.correctedDoForward(a);
   }

   @Nullable
   B correctedDoForward(@Nullable A a) {
      if (this.handleNullAutomatically) {
         return a == null ? null : Preconditions.checkNotNull(this.doForward(a));
      } else {
         return this.doForward(a);
      }
   }

   @Nullable
   A correctedDoBackward(@Nullable B b) {
      if (this.handleNullAutomatically) {
         return b == null ? null : Preconditions.checkNotNull(this.doBackward(b));
      } else {
         return this.doBackward(b);
      }
   }

   public Iterable<B> convertAll(final Iterable<? extends A> fromIterable) {
      Preconditions.checkNotNull(fromIterable, "fromIterable");
      return new Iterable<B>() {
         public Iterator<B> iterator() {
            return new Iterator<B>() {
               private final Iterator<? extends A> fromIterator = fromIterable.iterator();

               public boolean hasNext() {
                  return this.fromIterator.hasNext();
               }

               public B next() {
                  return Converter.this.convert(this.fromIterator.next());
               }

               public void remove() {
                  this.fromIterator.remove();
               }
            };
         }
      };
   }

   public Converter<B, A> reverse() {
      Converter<B, A> result = this.reverse;
      return result == null ? (this.reverse = new Converter.ReverseConverter(this)) : result;
   }

   public final <C> Converter<A, C> andThen(Converter<B, C> secondConverter) {
      return this.doAndThen(secondConverter);
   }

   <C> Converter<A, C> doAndThen(Converter<B, C> secondConverter) {
      return new Converter.ConverterComposition(this, (Converter)Preconditions.checkNotNull(secondConverter));
   }

   /** @deprecated */
   @Deprecated
   @Nullable
   public final B apply(@Nullable A a) {
      return this.convert(a);
   }

   public boolean equals(@Nullable Object object) {
      return super.equals(object);
   }

   public static <A, B> Converter<A, B> from(Function<? super A, ? extends B> forwardFunction, Function<? super B, ? extends A> backwardFunction) {
      return new Converter.FunctionBasedConverter(forwardFunction, backwardFunction);
   }

   public static <T> Converter<T, T> identity() {
      return Converter.IdentityConverter.INSTANCE;
   }

   private static final class IdentityConverter<T> extends Converter<T, T> implements Serializable {
      static final Converter.IdentityConverter INSTANCE = new Converter.IdentityConverter();
      private static final long serialVersionUID = 0L;

      protected T doForward(T t) {
         return t;
      }

      protected T doBackward(T t) {
         return t;
      }

      public Converter.IdentityConverter<T> reverse() {
         return this;
      }

      <S> Converter<T, S> doAndThen(Converter<T, S> otherConverter) {
         return (Converter)Preconditions.checkNotNull(otherConverter, "otherConverter");
      }

      public String toString() {
         return "Converter.identity()";
      }

      private Object readResolve() {
         return INSTANCE;
      }
   }

   private static final class FunctionBasedConverter<A, B> extends Converter<A, B> implements Serializable {
      private final Function<? super A, ? extends B> forwardFunction;
      private final Function<? super B, ? extends A> backwardFunction;

      private FunctionBasedConverter(Function<? super A, ? extends B> forwardFunction, Function<? super B, ? extends A> backwardFunction) {
         this.forwardFunction = (Function)Preconditions.checkNotNull(forwardFunction);
         this.backwardFunction = (Function)Preconditions.checkNotNull(backwardFunction);
      }

      protected B doForward(A a) {
         return this.forwardFunction.apply(a);
      }

      protected A doBackward(B b) {
         return this.backwardFunction.apply(b);
      }

      public boolean equals(@Nullable Object object) {
         if (!(object instanceof Converter.FunctionBasedConverter)) {
            return false;
         } else {
            Converter.FunctionBasedConverter<?, ?> that = (Converter.FunctionBasedConverter)object;
            return this.forwardFunction.equals(that.forwardFunction) && this.backwardFunction.equals(that.backwardFunction);
         }
      }

      public int hashCode() {
         return this.forwardFunction.hashCode() * 31 + this.backwardFunction.hashCode();
      }

      public String toString() {
         return "Converter.from(" + this.forwardFunction + ", " + this.backwardFunction + ")";
      }

      // $FF: synthetic method
      FunctionBasedConverter(Function x0, Function x1, Object x2) {
         this(x0, x1);
      }
   }

   private static final class ConverterComposition<A, B, C> extends Converter<A, C> implements Serializable {
      final Converter<A, B> first;
      final Converter<B, C> second;
      private static final long serialVersionUID = 0L;

      ConverterComposition(Converter<A, B> first, Converter<B, C> second) {
         this.first = first;
         this.second = second;
      }

      protected C doForward(A a) {
         throw new AssertionError();
      }

      protected A doBackward(C c) {
         throw new AssertionError();
      }

      @Nullable
      C correctedDoForward(@Nullable A a) {
         return this.second.correctedDoForward(this.first.correctedDoForward(a));
      }

      @Nullable
      A correctedDoBackward(@Nullable C c) {
         return this.first.correctedDoBackward(this.second.correctedDoBackward(c));
      }

      public boolean equals(@Nullable Object object) {
         if (!(object instanceof Converter.ConverterComposition)) {
            return false;
         } else {
            Converter.ConverterComposition<?, ?, ?> that = (Converter.ConverterComposition)object;
            return this.first.equals(that.first) && this.second.equals(that.second);
         }
      }

      public int hashCode() {
         return 31 * this.first.hashCode() + this.second.hashCode();
      }

      public String toString() {
         return this.first + ".andThen(" + this.second + ")";
      }
   }

   private static final class ReverseConverter<A, B> extends Converter<B, A> implements Serializable {
      final Converter<A, B> original;
      private static final long serialVersionUID = 0L;

      ReverseConverter(Converter<A, B> original) {
         this.original = original;
      }

      protected A doForward(B b) {
         throw new AssertionError();
      }

      protected B doBackward(A a) {
         throw new AssertionError();
      }

      @Nullable
      A correctedDoForward(@Nullable B b) {
         return this.original.correctedDoBackward(b);
      }

      @Nullable
      B correctedDoBackward(@Nullable A a) {
         return this.original.correctedDoForward(a);
      }

      public Converter<A, B> reverse() {
         return this.original;
      }

      public boolean equals(@Nullable Object object) {
         if (object instanceof Converter.ReverseConverter) {
            Converter.ReverseConverter<?, ?> that = (Converter.ReverseConverter)object;
            return this.original.equals(that.original);
         } else {
            return false;
         }
      }

      public int hashCode() {
         return ~this.original.hashCode();
      }

      public String toString() {
         return this.original + ".reverse()";
      }
   }
}
