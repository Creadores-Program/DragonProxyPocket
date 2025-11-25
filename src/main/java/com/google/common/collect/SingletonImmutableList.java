package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;

@GwtCompatible(
   serializable = true,
   emulated = true
)
final class SingletonImmutableList<E> extends ImmutableList<E> {
   final transient E element;

   SingletonImmutableList(E element) {
      this.element = Preconditions.checkNotNull(element);
   }

   public E get(int index) {
      Preconditions.checkElementIndex(index, 1);
      return this.element;
   }

   public UnmodifiableIterator<E> iterator() {
      return Iterators.singletonIterator(this.element);
   }

   public int size() {
      return 1;
   }

   public ImmutableList<E> subList(int fromIndex, int toIndex) {
      Preconditions.checkPositionIndexes(fromIndex, toIndex, 1);
      return (ImmutableList)(fromIndex == toIndex ? ImmutableList.of() : this);
   }

   public String toString() {
      String elementToString = this.element.toString();
      return (new StringBuilder(elementToString.length() + 2)).append('[').append(elementToString).append(']').toString();
   }

   boolean isPartialView() {
      return false;
   }
}
