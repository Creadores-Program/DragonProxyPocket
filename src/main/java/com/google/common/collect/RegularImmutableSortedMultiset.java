package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import java.util.Comparator;
import javax.annotation.Nullable;

final class RegularImmutableSortedMultiset<E> extends ImmutableSortedMultiset<E> {
   private static final long[] ZERO_CUMULATIVE_COUNTS = new long[]{0L};
   private final transient RegularImmutableSortedSet<E> elementSet;
   private final transient long[] cumulativeCounts;
   private final transient int offset;
   private final transient int length;

   RegularImmutableSortedMultiset(Comparator<? super E> comparator) {
      this.elementSet = ImmutableSortedSet.emptySet(comparator);
      this.cumulativeCounts = ZERO_CUMULATIVE_COUNTS;
      this.offset = 0;
      this.length = 0;
   }

   RegularImmutableSortedMultiset(RegularImmutableSortedSet<E> elementSet, long[] cumulativeCounts, int offset, int length) {
      this.elementSet = elementSet;
      this.cumulativeCounts = cumulativeCounts;
      this.offset = offset;
      this.length = length;
   }

   private int getCount(int index) {
      return (int)(this.cumulativeCounts[this.offset + index + 1] - this.cumulativeCounts[this.offset + index]);
   }

   Multiset.Entry<E> getEntry(int index) {
      return Multisets.immutableEntry(this.elementSet.asList().get(index), this.getCount(index));
   }

   public Multiset.Entry<E> firstEntry() {
      return this.isEmpty() ? null : this.getEntry(0);
   }

   public Multiset.Entry<E> lastEntry() {
      return this.isEmpty() ? null : this.getEntry(this.length - 1);
   }

   public int count(@Nullable Object element) {
      int index = this.elementSet.indexOf(element);
      return index >= 0 ? this.getCount(index) : 0;
   }

   public int size() {
      long size = this.cumulativeCounts[this.offset + this.length] - this.cumulativeCounts[this.offset];
      return Ints.saturatedCast(size);
   }

   public ImmutableSortedSet<E> elementSet() {
      return this.elementSet;
   }

   public ImmutableSortedMultiset<E> headMultiset(E upperBound, BoundType boundType) {
      return this.getSubMultiset(0, this.elementSet.headIndex(upperBound, Preconditions.checkNotNull(boundType) == BoundType.CLOSED));
   }

   public ImmutableSortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType) {
      return this.getSubMultiset(this.elementSet.tailIndex(lowerBound, Preconditions.checkNotNull(boundType) == BoundType.CLOSED), this.length);
   }

   ImmutableSortedMultiset<E> getSubMultiset(int from, int to) {
      Preconditions.checkPositionIndexes(from, to, this.length);
      if (from == to) {
         return emptyMultiset(this.comparator());
      } else if (from == 0 && to == this.length) {
         return this;
      } else {
         RegularImmutableSortedSet<E> subElementSet = this.elementSet.getSubSet(from, to);
         return new RegularImmutableSortedMultiset(subElementSet, this.cumulativeCounts, this.offset + from, to - from);
      }
   }

   boolean isPartialView() {
      return this.offset > 0 || this.length < this.cumulativeCounts.length - 1;
   }
}
