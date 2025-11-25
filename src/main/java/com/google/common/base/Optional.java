package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@CheckReturnValue
@GwtCompatible(
   serializable = true
)
public abstract class Optional<T> implements Serializable {
   private static final long serialVersionUID = 0L;

   public static <T> Optional<T> absent() {
      return Absent.withType();
   }

   public static <T> Optional<T> of(T reference) {
      return new Present(Preconditions.checkNotNull(reference));
   }

   public static <T> Optional<T> fromNullable(@Nullable T nullableReference) {
      return (Optional)(nullableReference == null ? absent() : new Present(nullableReference));
   }

   Optional() {
   }

   public abstract boolean isPresent();

   public abstract T get();

   public abstract T or(T var1);

   public abstract Optional<T> or(Optional<? extends T> var1);

   @Beta
   public abstract T or(Supplier<? extends T> var1);

   @Nullable
   public abstract T orNull();

   public abstract Set<T> asSet();

   public abstract <V> Optional<V> transform(Function<? super T, V> var1);

   public abstract boolean equals(@Nullable Object var1);

   public abstract int hashCode();

   public abstract String toString();

   @Beta
   public static <T> Iterable<T> presentInstances(final Iterable<? extends Optional<? extends T>> optionals) {
      Preconditions.checkNotNull(optionals);
      return new Iterable<T>() {
         public Iterator<T> iterator() {
            return new AbstractIterator<T>() {
               private final Iterator<? extends Optional<? extends T>> iterator = (Iterator)Preconditions.checkNotNull(optionals.iterator());

               protected T computeNext() {
                  while(true) {
                     if (this.iterator.hasNext()) {
                        Optional<? extends T> optional = (Optional)this.iterator.next();
                        if (!optional.isPresent()) {
                           continue;
                        }

                        return optional.get();
                     }

                     return this.endOfData();
                  }
               }
            };
         }
      };
   }
}
