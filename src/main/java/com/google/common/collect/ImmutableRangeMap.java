package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@Beta
@GwtIncompatible("NavigableMap")
public class ImmutableRangeMap<K extends Comparable<?>, V> implements RangeMap<K, V>, Serializable {
   private static final ImmutableRangeMap<Comparable<?>, Object> EMPTY = new ImmutableRangeMap(ImmutableList.of(), ImmutableList.of());
   private final transient ImmutableList<Range<K>> ranges;
   private final transient ImmutableList<V> values;
   private static final long serialVersionUID = 0L;

   public static <K extends Comparable<?>, V> ImmutableRangeMap<K, V> of() {
      return EMPTY;
   }

   public static <K extends Comparable<?>, V> ImmutableRangeMap<K, V> of(Range<K> range, V value) {
      return new ImmutableRangeMap(ImmutableList.of(range), ImmutableList.of(value));
   }

   public static <K extends Comparable<?>, V> ImmutableRangeMap<K, V> copyOf(RangeMap<K, ? extends V> rangeMap) {
      if (rangeMap instanceof ImmutableRangeMap) {
         return (ImmutableRangeMap)rangeMap;
      } else {
         Map<Range<K>, ? extends V> map = rangeMap.asMapOfRanges();
         ImmutableList.Builder<Range<K>> rangesBuilder = new ImmutableList.Builder(map.size());
         ImmutableList.Builder<V> valuesBuilder = new ImmutableList.Builder(map.size());
         Iterator i$ = map.entrySet().iterator();

         while(i$.hasNext()) {
            Entry<Range<K>, ? extends V> entry = (Entry)i$.next();
            rangesBuilder.add(entry.getKey());
            valuesBuilder.add(entry.getValue());
         }

         return new ImmutableRangeMap(rangesBuilder.build(), valuesBuilder.build());
      }
   }

   public static <K extends Comparable<?>, V> ImmutableRangeMap.Builder<K, V> builder() {
      return new ImmutableRangeMap.Builder();
   }

   ImmutableRangeMap(ImmutableList<Range<K>> ranges, ImmutableList<V> values) {
      this.ranges = ranges;
      this.values = values;
   }

   @Nullable
   public V get(K key) {
      int index = SortedLists.binarySearch(this.ranges, (Function)Range.lowerBoundFn(), (Comparable)Cut.belowValue(key), SortedLists.KeyPresentBehavior.ANY_PRESENT, SortedLists.KeyAbsentBehavior.NEXT_LOWER);
      if (index == -1) {
         return null;
      } else {
         Range<K> range = (Range)this.ranges.get(index);
         return range.contains(key) ? this.values.get(index) : null;
      }
   }

   @Nullable
   public Entry<Range<K>, V> getEntry(K key) {
      int index = SortedLists.binarySearch(this.ranges, (Function)Range.lowerBoundFn(), (Comparable)Cut.belowValue(key), SortedLists.KeyPresentBehavior.ANY_PRESENT, SortedLists.KeyAbsentBehavior.NEXT_LOWER);
      if (index == -1) {
         return null;
      } else {
         Range<K> range = (Range)this.ranges.get(index);
         return range.contains(key) ? Maps.immutableEntry(range, this.values.get(index)) : null;
      }
   }

   public Range<K> span() {
      if (this.ranges.isEmpty()) {
         throw new NoSuchElementException();
      } else {
         Range<K> firstRange = (Range)this.ranges.get(0);
         Range<K> lastRange = (Range)this.ranges.get(this.ranges.size() - 1);
         return Range.create(firstRange.lowerBound, lastRange.upperBound);
      }
   }

   public void put(Range<K> range, V value) {
      throw new UnsupportedOperationException();
   }

   public void putAll(RangeMap<K, V> rangeMap) {
      throw new UnsupportedOperationException();
   }

   public void clear() {
      throw new UnsupportedOperationException();
   }

   public void remove(Range<K> range) {
      throw new UnsupportedOperationException();
   }

   public ImmutableMap<Range<K>, V> asMapOfRanges() {
      if (this.ranges.isEmpty()) {
         return ImmutableMap.of();
      } else {
         RegularImmutableSortedSet<Range<K>> rangeSet = new RegularImmutableSortedSet(this.ranges, Range.RANGE_LEX_ORDERING);
         return new ImmutableSortedMap(rangeSet, this.values);
      }
   }

   public ImmutableMap<Range<K>, V> asDescendingMapOfRanges() {
      if (this.ranges.isEmpty()) {
         return ImmutableMap.of();
      } else {
         RegularImmutableSortedSet<Range<K>> rangeSet = new RegularImmutableSortedSet(this.ranges.reverse(), Range.RANGE_LEX_ORDERING.reverse());
         return new ImmutableSortedMap(rangeSet, this.values.reverse());
      }
   }

   public ImmutableRangeMap<K, V> subRangeMap(final Range<K> range) {
      if (((Range)Preconditions.checkNotNull(range)).isEmpty()) {
         return of();
      } else if (!this.ranges.isEmpty() && !range.encloses(this.span())) {
         final int lowerIndex = SortedLists.binarySearch(this.ranges, (Function)Range.upperBoundFn(), (Comparable)range.lowerBound, SortedLists.KeyPresentBehavior.FIRST_AFTER, SortedLists.KeyAbsentBehavior.NEXT_HIGHER);
         int upperIndex = SortedLists.binarySearch(this.ranges, (Function)Range.lowerBoundFn(), (Comparable)range.upperBound, SortedLists.KeyPresentBehavior.ANY_PRESENT, SortedLists.KeyAbsentBehavior.NEXT_HIGHER);
         if (lowerIndex >= upperIndex) {
            return of();
         } else {
            final int len = upperIndex - lowerIndex;
            ImmutableList<Range<K>> subRanges = new ImmutableList<Range<K>>() {
               public int size() {
                  return len;
               }

               public Range<K> get(int index) {
                  Preconditions.checkElementIndex(index, len);
                  return index != 0 && index != len - 1 ? (Range)ImmutableRangeMap.this.ranges.get(index + lowerIndex) : ((Range)ImmutableRangeMap.this.ranges.get(index + lowerIndex)).intersection(range);
               }

               boolean isPartialView() {
                  return true;
               }
            };
            return new ImmutableRangeMap<K, V>(subRanges, this.values.subList(lowerIndex, upperIndex)) {
               public ImmutableRangeMap<K, V> subRangeMap(Range<K> subRange) {
                  return range.isConnected(subRange) ? ImmutableRangeMap.this.subRangeMap(subRange.intersection(range)) : ImmutableRangeMap.of();
               }
            };
         }
      } else {
         return this;
      }
   }

   public int hashCode() {
      return this.asMapOfRanges().hashCode();
   }

   public boolean equals(@Nullable Object o) {
      if (o instanceof RangeMap) {
         RangeMap<?, ?> rangeMap = (RangeMap)o;
         return this.asMapOfRanges().equals(rangeMap.asMapOfRanges());
      } else {
         return false;
      }
   }

   public String toString() {
      return this.asMapOfRanges().toString();
   }

   Object writeReplace() {
      return new ImmutableRangeMap.SerializedForm(this.asMapOfRanges());
   }

   private static class SerializedForm<K extends Comparable<?>, V> implements Serializable {
      private final ImmutableMap<Range<K>, V> mapOfRanges;
      private static final long serialVersionUID = 0L;

      SerializedForm(ImmutableMap<Range<K>, V> mapOfRanges) {
         this.mapOfRanges = mapOfRanges;
      }

      Object readResolve() {
         return this.mapOfRanges.isEmpty() ? ImmutableRangeMap.of() : this.createRangeMap();
      }

      Object createRangeMap() {
         ImmutableRangeMap.Builder<K, V> builder = new ImmutableRangeMap.Builder();
         Iterator i$ = this.mapOfRanges.entrySet().iterator();

         while(i$.hasNext()) {
            Entry<Range<K>, V> entry = (Entry)i$.next();
            builder.put((Range)entry.getKey(), entry.getValue());
         }

         return builder.build();
      }
   }

   public static final class Builder<K extends Comparable<?>, V> {
      private final RangeSet<K> keyRanges = TreeRangeSet.create();
      private final RangeMap<K, V> rangeMap = TreeRangeMap.create();

      public ImmutableRangeMap.Builder<K, V> put(Range<K> range, V value) {
         Preconditions.checkNotNull(range);
         Preconditions.checkNotNull(value);
         Preconditions.checkArgument(!range.isEmpty(), "Range must not be empty, but was %s", range);
         if (!this.keyRanges.complement().encloses(range)) {
            Iterator i$ = this.rangeMap.asMapOfRanges().entrySet().iterator();

            while(i$.hasNext()) {
               Entry<Range<K>, V> entry = (Entry)i$.next();
               Range<K> key = (Range)entry.getKey();
               if (key.isConnected(range) && !key.intersection(range).isEmpty()) {
                  throw new IllegalArgumentException("Overlapping ranges: range " + range + " overlaps with entry " + entry);
               }
            }
         }

         this.keyRanges.add(range);
         this.rangeMap.put(range, value);
         return this;
      }

      public ImmutableRangeMap.Builder<K, V> putAll(RangeMap<K, ? extends V> rangeMap) {
         Iterator i$ = rangeMap.asMapOfRanges().entrySet().iterator();

         while(i$.hasNext()) {
            Entry<Range<K>, ? extends V> entry = (Entry)i$.next();
            this.put((Range)entry.getKey(), entry.getValue());
         }

         return this;
      }

      public ImmutableRangeMap<K, V> build() {
         Map<Range<K>, V> map = this.rangeMap.asMapOfRanges();
         ImmutableList.Builder<Range<K>> rangesBuilder = new ImmutableList.Builder(map.size());
         ImmutableList.Builder<V> valuesBuilder = new ImmutableList.Builder(map.size());
         Iterator i$ = map.entrySet().iterator();

         while(i$.hasNext()) {
            Entry<Range<K>, V> entry = (Entry)i$.next();
            rangesBuilder.add(entry.getKey());
            valuesBuilder.add(entry.getValue());
         }

         return new ImmutableRangeMap(rangesBuilder.build(), valuesBuilder.build());
      }
   }
}
