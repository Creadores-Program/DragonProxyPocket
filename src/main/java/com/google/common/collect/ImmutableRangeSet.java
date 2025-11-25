package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;

@Beta
public final class ImmutableRangeSet<C extends Comparable> extends AbstractRangeSet<C> implements Serializable {
   private static final ImmutableRangeSet<Comparable<?>> EMPTY = new ImmutableRangeSet(ImmutableList.of());
   private static final ImmutableRangeSet<Comparable<?>> ALL = new ImmutableRangeSet(ImmutableList.of(Range.all()));
   private final transient ImmutableList<Range<C>> ranges;
   private transient ImmutableRangeSet<C> complement;

   public static <C extends Comparable> ImmutableRangeSet<C> of() {
      return EMPTY;
   }

   static <C extends Comparable> ImmutableRangeSet<C> all() {
      return ALL;
   }

   public static <C extends Comparable> ImmutableRangeSet<C> of(Range<C> range) {
      Preconditions.checkNotNull(range);
      if (range.isEmpty()) {
         return of();
      } else {
         return range.equals(Range.all()) ? all() : new ImmutableRangeSet(ImmutableList.of(range));
      }
   }

   public static <C extends Comparable> ImmutableRangeSet<C> copyOf(RangeSet<C> rangeSet) {
      Preconditions.checkNotNull(rangeSet);
      if (rangeSet.isEmpty()) {
         return of();
      } else if (rangeSet.encloses(Range.all())) {
         return all();
      } else {
         if (rangeSet instanceof ImmutableRangeSet) {
            ImmutableRangeSet<C> immutableRangeSet = (ImmutableRangeSet)rangeSet;
            if (!immutableRangeSet.isPartialView()) {
               return immutableRangeSet;
            }
         }

         return new ImmutableRangeSet(ImmutableList.copyOf((Collection)rangeSet.asRanges()));
      }
   }

   ImmutableRangeSet(ImmutableList<Range<C>> ranges) {
      this.ranges = ranges;
   }

   private ImmutableRangeSet(ImmutableList<Range<C>> ranges, ImmutableRangeSet<C> complement) {
      this.ranges = ranges;
      this.complement = complement;
   }

   public boolean encloses(Range<C> otherRange) {
      int index = SortedLists.binarySearch(this.ranges, Range.lowerBoundFn(), otherRange.lowerBound, Ordering.natural(), SortedLists.KeyPresentBehavior.ANY_PRESENT, SortedLists.KeyAbsentBehavior.NEXT_LOWER);
      return index != -1 && ((Range)this.ranges.get(index)).encloses(otherRange);
   }

   public Range<C> rangeContaining(C value) {
      int index = SortedLists.binarySearch(this.ranges, Range.lowerBoundFn(), Cut.belowValue(value), Ordering.natural(), SortedLists.KeyPresentBehavior.ANY_PRESENT, SortedLists.KeyAbsentBehavior.NEXT_LOWER);
      if (index != -1) {
         Range<C> range = (Range)this.ranges.get(index);
         return range.contains(value) ? range : null;
      } else {
         return null;
      }
   }

   public Range<C> span() {
      if (this.ranges.isEmpty()) {
         throw new NoSuchElementException();
      } else {
         return Range.create(((Range)this.ranges.get(0)).lowerBound, ((Range)this.ranges.get(this.ranges.size() - 1)).upperBound);
      }
   }

   public boolean isEmpty() {
      return this.ranges.isEmpty();
   }

   public void add(Range<C> range) {
      throw new UnsupportedOperationException();
   }

   public void addAll(RangeSet<C> other) {
      throw new UnsupportedOperationException();
   }

   public void remove(Range<C> range) {
      throw new UnsupportedOperationException();
   }

   public void removeAll(RangeSet<C> other) {
      throw new UnsupportedOperationException();
   }

   public ImmutableSet<Range<C>> asRanges() {
      return (ImmutableSet)(this.ranges.isEmpty() ? ImmutableSet.of() : new RegularImmutableSortedSet(this.ranges, Range.RANGE_LEX_ORDERING));
   }

   public ImmutableSet<Range<C>> asDescendingSetOfRanges() {
      return (ImmutableSet)(this.ranges.isEmpty() ? ImmutableSet.of() : new RegularImmutableSortedSet(this.ranges.reverse(), Range.RANGE_LEX_ORDERING.reverse()));
   }

   public ImmutableRangeSet<C> complement() {
      ImmutableRangeSet<C> result = this.complement;
      if (result != null) {
         return result;
      } else if (this.ranges.isEmpty()) {
         return this.complement = all();
      } else if (this.ranges.size() == 1 && ((Range)this.ranges.get(0)).equals(Range.all())) {
         return this.complement = of();
      } else {
         ImmutableList<Range<C>> complementRanges = new ImmutableRangeSet.ComplementRanges();
         result = this.complement = new ImmutableRangeSet(complementRanges, this);
         return result;
      }
   }

   private ImmutableList<Range<C>> intersectRanges(final Range<C> range) {
      if (!this.ranges.isEmpty() && !range.isEmpty()) {
         if (range.encloses(this.span())) {
            return this.ranges;
         } else {
            final int fromIndex;
            if (range.hasLowerBound()) {
               fromIndex = SortedLists.binarySearch(this.ranges, (Function)Range.upperBoundFn(), (Comparable)range.lowerBound, SortedLists.KeyPresentBehavior.FIRST_AFTER, SortedLists.KeyAbsentBehavior.NEXT_HIGHER);
            } else {
               fromIndex = 0;
            }

            int toIndex;
            if (range.hasUpperBound()) {
               toIndex = SortedLists.binarySearch(this.ranges, (Function)Range.lowerBoundFn(), (Comparable)range.upperBound, SortedLists.KeyPresentBehavior.FIRST_PRESENT, SortedLists.KeyAbsentBehavior.NEXT_HIGHER);
            } else {
               toIndex = this.ranges.size();
            }

            final int length = toIndex - fromIndex;
            return length == 0 ? ImmutableList.of() : new ImmutableList<Range<C>>() {
               public int size() {
                  return length;
               }

               public Range<C> get(int index) {
                  Preconditions.checkElementIndex(index, length);
                  return index != 0 && index != length - 1 ? (Range)ImmutableRangeSet.this.ranges.get(index + fromIndex) : ((Range)ImmutableRangeSet.this.ranges.get(index + fromIndex)).intersection(range);
               }

               boolean isPartialView() {
                  return true;
               }
            };
         }
      } else {
         return ImmutableList.of();
      }
   }

   public ImmutableRangeSet<C> subRangeSet(Range<C> range) {
      if (!this.isEmpty()) {
         Range<C> span = this.span();
         if (range.encloses(span)) {
            return this;
         }

         if (range.isConnected(span)) {
            return new ImmutableRangeSet(this.intersectRanges(range));
         }
      }

      return of();
   }

   public ImmutableSortedSet<C> asSet(DiscreteDomain<C> domain) {
      Preconditions.checkNotNull(domain);
      if (this.isEmpty()) {
         return ImmutableSortedSet.of();
      } else {
         Range<C> span = this.span().canonical(domain);
         if (!span.hasLowerBound()) {
            throw new IllegalArgumentException("Neither the DiscreteDomain nor this range set are bounded below");
         } else {
            if (!span.hasUpperBound()) {
               try {
                  domain.maxValue();
               } catch (NoSuchElementException var4) {
                  throw new IllegalArgumentException("Neither the DiscreteDomain nor this range set are bounded above");
               }
            }

            return new ImmutableRangeSet.AsSet(domain);
         }
      }
   }

   boolean isPartialView() {
      return this.ranges.isPartialView();
   }

   public static <C extends Comparable<?>> ImmutableRangeSet.Builder<C> builder() {
      return new ImmutableRangeSet.Builder();
   }

   Object writeReplace() {
      return new ImmutableRangeSet.SerializedForm(this.ranges);
   }

   private static final class SerializedForm<C extends Comparable> implements Serializable {
      private final ImmutableList<Range<C>> ranges;

      SerializedForm(ImmutableList<Range<C>> ranges) {
         this.ranges = ranges;
      }

      Object readResolve() {
         if (this.ranges.isEmpty()) {
            return ImmutableRangeSet.of();
         } else {
            return this.ranges.equals(ImmutableList.of(Range.all())) ? ImmutableRangeSet.all() : new ImmutableRangeSet(this.ranges);
         }
      }
   }

   public static class Builder<C extends Comparable<?>> {
      private final RangeSet<C> rangeSet = TreeRangeSet.create();

      public ImmutableRangeSet.Builder<C> add(Range<C> range) {
         if (range.isEmpty()) {
            throw new IllegalArgumentException("range must not be empty, but was " + range);
         } else if (this.rangeSet.complement().encloses(range)) {
            this.rangeSet.add(range);
            return this;
         } else {
            Iterator i$ = this.rangeSet.asRanges().iterator();

            while(i$.hasNext()) {
               Range<C> currentRange = (Range)i$.next();
               Preconditions.checkArgument(!currentRange.isConnected(range) || currentRange.intersection(range).isEmpty(), "Ranges may not overlap, but received %s and %s", currentRange, range);
            }

            throw new AssertionError("should have thrown an IAE above");
         }
      }

      public ImmutableRangeSet.Builder<C> addAll(RangeSet<C> ranges) {
         Iterator i$ = ranges.asRanges().iterator();

         while(i$.hasNext()) {
            Range<C> range = (Range)i$.next();
            this.add(range);
         }

         return this;
      }

      public ImmutableRangeSet<C> build() {
         return ImmutableRangeSet.copyOf(this.rangeSet);
      }
   }

   private static class AsSetSerializedForm<C extends Comparable> implements Serializable {
      private final ImmutableList<Range<C>> ranges;
      private final DiscreteDomain<C> domain;

      AsSetSerializedForm(ImmutableList<Range<C>> ranges, DiscreteDomain<C> domain) {
         this.ranges = ranges;
         this.domain = domain;
      }

      Object readResolve() {
         return (new ImmutableRangeSet(this.ranges)).asSet(this.domain);
      }
   }

   private final class AsSet extends ImmutableSortedSet<C> {
      private final DiscreteDomain<C> domain;
      private transient Integer size;

      AsSet(DiscreteDomain<C> domain) {
         super(Ordering.natural());
         this.domain = domain;
      }

      public int size() {
         Integer result = this.size;
         if (result == null) {
            long total = 0L;
            Iterator i$ = ImmutableRangeSet.this.ranges.iterator();

            while(i$.hasNext()) {
               Range<C> range = (Range)i$.next();
               total += (long)ContiguousSet.create(range, this.domain).size();
               if (total >= 2147483647L) {
                  break;
               }
            }

            result = this.size = Ints.saturatedCast(total);
         }

         return result;
      }

      public UnmodifiableIterator<C> iterator() {
         return new AbstractIterator<C>() {
            final Iterator<Range<C>> rangeItr;
            Iterator<C> elemItr;

            {
               this.rangeItr = ImmutableRangeSet.this.ranges.iterator();
               this.elemItr = Iterators.emptyIterator();
            }

            protected C computeNext() {
               while(true) {
                  if (!this.elemItr.hasNext()) {
                     if (this.rangeItr.hasNext()) {
                        this.elemItr = ContiguousSet.create((Range)this.rangeItr.next(), AsSet.this.domain).iterator();
                        continue;
                     }

                     return (Comparable)this.endOfData();
                  }

                  return (Comparable)this.elemItr.next();
               }
            }
         };
      }

      @GwtIncompatible("NavigableSet")
      public UnmodifiableIterator<C> descendingIterator() {
         return new AbstractIterator<C>() {
            final Iterator<Range<C>> rangeItr;
            Iterator<C> elemItr;

            {
               this.rangeItr = ImmutableRangeSet.this.ranges.reverse().iterator();
               this.elemItr = Iterators.emptyIterator();
            }

            protected C computeNext() {
               while(true) {
                  if (!this.elemItr.hasNext()) {
                     if (this.rangeItr.hasNext()) {
                        this.elemItr = ContiguousSet.create((Range)this.rangeItr.next(), AsSet.this.domain).descendingIterator();
                        continue;
                     }

                     return (Comparable)this.endOfData();
                  }

                  return (Comparable)this.elemItr.next();
               }
            }
         };
      }

      ImmutableSortedSet<C> subSet(Range<C> range) {
         return ImmutableRangeSet.this.subRangeSet(range).asSet(this.domain);
      }

      ImmutableSortedSet<C> headSetImpl(C toElement, boolean inclusive) {
         return this.subSet(Range.upTo(toElement, BoundType.forBoolean(inclusive)));
      }

      ImmutableSortedSet<C> subSetImpl(C fromElement, boolean fromInclusive, C toElement, boolean toInclusive) {
         return !fromInclusive && !toInclusive && Range.compareOrThrow(fromElement, toElement) == 0 ? ImmutableSortedSet.of() : this.subSet(Range.range(fromElement, BoundType.forBoolean(fromInclusive), toElement, BoundType.forBoolean(toInclusive)));
      }

      ImmutableSortedSet<C> tailSetImpl(C fromElement, boolean inclusive) {
         return this.subSet(Range.downTo(fromElement, BoundType.forBoolean(inclusive)));
      }

      public boolean contains(@Nullable Object o) {
         if (o == null) {
            return false;
         } else {
            try {
               C c = (Comparable)o;
               return ImmutableRangeSet.this.contains(c);
            } catch (ClassCastException var3) {
               return false;
            }
         }
      }

      int indexOf(Object target) {
         if (this.contains(target)) {
            C c = (Comparable)target;
            long total = 0L;

            Range range;
            for(Iterator i$ = ImmutableRangeSet.this.ranges.iterator(); i$.hasNext(); total += (long)ContiguousSet.create(range, this.domain).size()) {
               range = (Range)i$.next();
               if (range.contains(c)) {
                  return Ints.saturatedCast(total + (long)ContiguousSet.create(range, this.domain).indexOf(c));
               }
            }

            throw new AssertionError("impossible");
         } else {
            return -1;
         }
      }

      boolean isPartialView() {
         return ImmutableRangeSet.this.ranges.isPartialView();
      }

      public String toString() {
         return ImmutableRangeSet.this.ranges.toString();
      }

      Object writeReplace() {
         return new ImmutableRangeSet.AsSetSerializedForm(ImmutableRangeSet.this.ranges, this.domain);
      }
   }

   private final class ComplementRanges extends ImmutableList<Range<C>> {
      private final boolean positiveBoundedBelow;
      private final boolean positiveBoundedAbove;
      private final int size;

      ComplementRanges() {
         this.positiveBoundedBelow = ((Range)ImmutableRangeSet.this.ranges.get(0)).hasLowerBound();
         this.positiveBoundedAbove = ((Range)Iterables.getLast(ImmutableRangeSet.this.ranges)).hasUpperBound();
         int size = ImmutableRangeSet.this.ranges.size() - 1;
         if (this.positiveBoundedBelow) {
            ++size;
         }

         if (this.positiveBoundedAbove) {
            ++size;
         }

         this.size = size;
      }

      public int size() {
         return this.size;
      }

      public Range<C> get(int index) {
         Preconditions.checkElementIndex(index, this.size);
         Cut lowerBound;
         if (this.positiveBoundedBelow) {
            lowerBound = index == 0 ? Cut.belowAll() : ((Range)ImmutableRangeSet.this.ranges.get(index - 1)).upperBound;
         } else {
            lowerBound = ((Range)ImmutableRangeSet.this.ranges.get(index)).upperBound;
         }

         Cut upperBound;
         if (this.positiveBoundedAbove && index == this.size - 1) {
            upperBound = Cut.aboveAll();
         } else {
            upperBound = ((Range)ImmutableRangeSet.this.ranges.get(index + (this.positiveBoundedBelow ? 0 : 1))).lowerBound;
         }

         return Range.create(lowerBound, upperBound);
      }

      boolean isPartialView() {
         return true;
      }
   }
}
