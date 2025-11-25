package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import javax.annotation.Nullable;

@GwtCompatible(
   serializable = true
)
final class NullsFirstOrdering<T> extends Ordering<T> implements Serializable {
   final Ordering<? super T> ordering;
   private static final long serialVersionUID = 0L;

   NullsFirstOrdering(Ordering<? super T> ordering) {
      this.ordering = ordering;
   }

   public int compare(@Nullable T left, @Nullable T right) {
      if (left == right) {
         return 0;
      } else if (left == null) {
         return -1;
      } else {
         return right == null ? 1 : this.ordering.compare(left, right);
      }
   }

   public <S extends T> Ordering<S> reverse() {
      return this.ordering.reverse().nullsLast();
   }

   public <S extends T> Ordering<S> nullsFirst() {
      return this;
   }

   public <S extends T> Ordering<S> nullsLast() {
      return this.ordering.nullsLast();
   }

   public boolean equals(@Nullable Object object) {
      if (object == this) {
         return true;
      } else if (object instanceof NullsFirstOrdering) {
         NullsFirstOrdering<?> that = (NullsFirstOrdering)object;
         return this.ordering.equals(that.ordering);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.ordering.hashCode() ^ 957692532;
   }

   public String toString() {
      return this.ordering + ".nullsFirst()";
   }
}
