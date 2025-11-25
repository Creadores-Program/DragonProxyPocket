package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import javax.annotation.Nullable;

@GwtCompatible(
   serializable = true,
   emulated = true
)
final class RegularImmutableSet<E> extends ImmutableSet<E> {
   static final RegularImmutableSet<Object> EMPTY;
   private final transient Object[] elements;
   @VisibleForTesting
   final transient Object[] table;
   private final transient int mask;
   private final transient int hashCode;

   RegularImmutableSet(Object[] elements, int hashCode, Object[] table, int mask) {
      this.elements = elements;
      this.table = table;
      this.mask = mask;
      this.hashCode = hashCode;
   }

   public boolean contains(@Nullable Object target) {
      Object[] table = this.table;
      if (target != null && table != null) {
         int i = Hashing.smearedHash(target);

         while(true) {
            i &= this.mask;
            Object candidate = table[i];
            if (candidate == null) {
               return false;
            }

            if (candidate.equals(target)) {
               return true;
            }

            ++i;
         }
      } else {
         return false;
      }
   }

   public int size() {
      return this.elements.length;
   }

   public UnmodifiableIterator<E> iterator() {
      return Iterators.forArray(this.elements);
   }

   int copyIntoArray(Object[] dst, int offset) {
      System.arraycopy(this.elements, 0, dst, offset, this.elements.length);
      return offset + this.elements.length;
   }

   ImmutableList<E> createAsList() {
      return (ImmutableList)(this.table == null ? ImmutableList.of() : new RegularImmutableAsList(this, this.elements));
   }

   boolean isPartialView() {
      return false;
   }

   public int hashCode() {
      return this.hashCode;
   }

   boolean isHashCodeFast() {
      return true;
   }

   static {
      EMPTY = new RegularImmutableSet(ObjectArrays.EMPTY_ARRAY, 0, (Object[])null, 0);
   }
}
