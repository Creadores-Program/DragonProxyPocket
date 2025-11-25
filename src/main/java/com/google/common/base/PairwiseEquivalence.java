package com.google.common.base;

import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import java.util.Iterator;
import javax.annotation.Nullable;

@GwtCompatible(
   serializable = true
)
final class PairwiseEquivalence<T> extends Equivalence<Iterable<T>> implements Serializable {
   final Equivalence<? super T> elementEquivalence;
   private static final long serialVersionUID = 1L;

   PairwiseEquivalence(Equivalence<? super T> elementEquivalence) {
      this.elementEquivalence = (Equivalence)Preconditions.checkNotNull(elementEquivalence);
   }

   protected boolean doEquivalent(Iterable<T> iterableA, Iterable<T> iterableB) {
      Iterator<T> iteratorA = iterableA.iterator();
      Iterator iteratorB = iterableB.iterator();

      while(iteratorA.hasNext() && iteratorB.hasNext()) {
         if (!this.elementEquivalence.equivalent(iteratorA.next(), iteratorB.next())) {
            return false;
         }
      }

      return !iteratorA.hasNext() && !iteratorB.hasNext();
   }

   protected int doHash(Iterable<T> iterable) {
      int hash = 78721;

      Object element;
      for(Iterator i$ = iterable.iterator(); i$.hasNext(); hash = hash * 24943 + this.elementEquivalence.hash(element)) {
         element = i$.next();
      }

      return hash;
   }

   public boolean equals(@Nullable Object object) {
      if (object instanceof PairwiseEquivalence) {
         PairwiseEquivalence<?> that = (PairwiseEquivalence)object;
         return this.elementEquivalence.equals(that.elementEquivalence);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.elementEquivalence.hashCode() ^ 1185147655;
   }

   public String toString() {
      return this.elementEquivalence + ".pairwise()";
   }
}
