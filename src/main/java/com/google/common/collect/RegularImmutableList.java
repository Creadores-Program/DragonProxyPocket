package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;

@GwtCompatible(
   serializable = true,
   emulated = true
)
class RegularImmutableList<E> extends ImmutableList<E> {
   static final ImmutableList<Object> EMPTY;
   private final transient int offset;
   private final transient int size;
   private final transient Object[] array;

   RegularImmutableList(Object[] array, int offset, int size) {
      this.offset = offset;
      this.size = size;
      this.array = array;
   }

   RegularImmutableList(Object[] array) {
      this(array, 0, array.length);
   }

   public int size() {
      return this.size;
   }

   boolean isPartialView() {
      return this.size != this.array.length;
   }

   int copyIntoArray(Object[] dst, int dstOff) {
      System.arraycopy(this.array, this.offset, dst, dstOff, this.size);
      return dstOff + this.size;
   }

   public E get(int index) {
      Preconditions.checkElementIndex(index, this.size);
      return this.array[index + this.offset];
   }

   ImmutableList<E> subListUnchecked(int fromIndex, int toIndex) {
      return new RegularImmutableList(this.array, this.offset + fromIndex, toIndex - fromIndex);
   }

   public UnmodifiableListIterator<E> listIterator(int index) {
      return Iterators.forArray(this.array, this.offset, this.size, index);
   }

   static {
      EMPTY = new RegularImmutableList(ObjectArrays.EMPTY_ARRAY);
   }
}
