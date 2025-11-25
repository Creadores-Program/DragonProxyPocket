package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@Beta
@GwtIncompatible("uses NavigableMap")
public class TreeRangeSet<C extends Comparable<?>> extends AbstractRangeSet<C> {
   @VisibleForTesting
   final NavigableMap<Cut<C>, Range<C>> rangesByLowerBound;
   private transient Set<Range<C>> asRanges;
   private transient Set<Range<C>> asDescendingSetOfRanges;
   private transient RangeSet<C> complement;

   public static <C extends Comparable<?>> TreeRangeSet<C> create() {
      return new TreeRangeSet(new TreeMap());
   }

   public static <C extends Comparable<?>> TreeRangeSet<C> create(RangeSet<C> rangeSet) {
      TreeRangeSet<C> result = create();
      result.addAll(rangeSet);
      return result;
   }

   private TreeRangeSet(NavigableMap<Cut<C>, Range<C>> rangesByLowerCut) {
      this.rangesByLowerBound = rangesByLowerCut;
   }

   public Set<Range<C>> asRanges() {
      Set<Range<C>> result = this.asRanges;
      return result == null ? (this.asRanges = new TreeRangeSet.AsRanges(this.rangesByLowerBound.values())) : result;
   }

   public Set<Range<C>> asDescendingSetOfRanges() {
      Set<Range<C>> result = this.asDescendingSetOfRanges;
      return result == null ? (this.asDescendingSetOfRanges = new TreeRangeSet.AsRanges(this.rangesByLowerBound.descendingMap().values())) : result;
   }

   @Nullable
   public Range<C> rangeContaining(C value) {
      Preconditions.checkNotNull(value);
      Entry<Cut<C>, Range<C>> floorEntry = this.rangesByLowerBound.floorEntry(Cut.belowValue(value));
      return floorEntry != null && ((Range)floorEntry.getValue()).contains(value) ? (Range)floorEntry.getValue() : null;
   }

   public boolean encloses(Range<C> range) {
      Preconditions.checkNotNull(range);
      Entry<Cut<C>, Range<C>> floorEntry = this.rangesByLowerBound.floorEntry(range.lowerBound);
      return floorEntry != null && ((Range)floorEntry.getValue()).encloses(range);
   }

   @Nullable
   private Range<C> rangeEnclosing(Range<C> range) {
      Preconditions.checkNotNull(range);
      Entry<Cut<C>, Range<C>> floorEntry = this.rangesByLowerBound.floorEntry(range.lowerBound);
      return floorEntry != null && ((Range)floorEntry.getValue()).encloses(range) ? (Range)floorEntry.getValue() : null;
   }

   public Range<C> span() {
      Entry<Cut<C>, Range<C>> firstEntry = this.rangesByLowerBound.firstEntry();
      Entry<Cut<C>, Range<C>> lastEntry = this.rangesByLowerBound.lastEntry();
      if (firstEntry == null) {
         throw new NoSuchElementException();
      } else {
         return Range.create(((Range)firstEntry.getValue()).lowerBound, ((Range)lastEntry.getValue()).upperBound);
      }
   }

   public void add(Range<C> rangeToAdd) {
      Preconditions.checkNotNull(rangeToAdd);
      if (!rangeToAdd.isEmpty()) {
         Cut<C> lbToAdd = rangeToAdd.lowerBound;
         Cut<C> ubToAdd = rangeToAdd.upperBound;
         Entry<Cut<C>, Range<C>> entryBelowLB = this.rangesByLowerBound.lowerEntry(lbToAdd);
         if (entryBelowLB != null) {
            Range<C> rangeBelowLB = (Range)entryBelowLB.getValue();
            if (rangeBelowLB.upperBound.compareTo(lbToAdd) >= 0) {
               if (rangeBelowLB.upperBound.compareTo(ubToAdd) >= 0) {
                  ubToAdd = rangeBelowLB.upperBound;
               }

               lbToAdd = rangeBelowLB.lowerBound;
            }
         }

         Entry<Cut<C>, Range<C>> entryBelowUB = this.rangesByLowerBound.floorEntry(ubToAdd);
         if (entryBelowUB != null) {
            Range<C> rangeBelowUB = (Range)entryBelowUB.getValue();
            if (rangeBelowUB.upperBound.compareTo(ubToAdd) >= 0) {
               ubToAdd = rangeBelowUB.upperBound;
            }
         }

         this.rangesByLowerBound.subMap(lbToAdd, ubToAdd).clear();
         this.replaceRangeWithSameLowerBound(Range.create(lbToAdd, ubToAdd));
      }
   }

   public void remove(Range<C> rangeToRemove) {
      Preconditions.checkNotNull(rangeToRemove);
      if (!rangeToRemove.isEmpty()) {
         Entry<Cut<C>, Range<C>> entryBelowLB = this.rangesByLowerBound.lowerEntry(rangeToRemove.lowerBound);
         if (entryBelowLB != null) {
            Range<C> rangeBelowLB = (Range)entryBelowLB.getValue();
            if (rangeBelowLB.upperBound.compareTo(rangeToRemove.lowerBound) >= 0) {
               if (rangeToRemove.hasUpperBound() && rangeBelowLB.upperBound.compareTo(rangeToRemove.upperBound) >= 0) {
                  this.replaceRangeWithSameLowerBound(Range.create(rangeToRemove.upperBound, rangeBelowLB.upperBound));
               }

               this.replaceRangeWithSameLowerBound(Range.create(rangeBelowLB.lowerBound, rangeToRemove.lowerBound));
            }
         }

         Entry<Cut<C>, Range<C>> entryBelowUB = this.rangesByLowerBound.floorEntry(rangeToRemove.upperBound);
         if (entryBelowUB != null) {
            Range<C> rangeBelowUB = (Range)entryBelowUB.getValue();
            if (rangeToRemove.hasUpperBound() && rangeBelowUB.upperBound.compareTo(rangeToRemove.upperBound) >= 0) {
               this.replaceRangeWithSameLowerBound(Range.create(rangeToRemove.upperBound, rangeBelowUB.upperBound));
            }
         }

         this.rangesByLowerBound.subMap(rangeToRemove.lowerBound, rangeToRemove.upperBound).clear();
      }
   }

   private void replaceRangeWithSameLowerBound(Range<C> range) {
      if (range.isEmpty()) {
         this.rangesByLowerBound.remove(range.lowerBound);
      } else {
         this.rangesByLowerBound.put(range.lowerBound, range);
      }

   }

   public RangeSet<C> complement() {
      RangeSet<C> result = this.complement;
      return result == null ? (this.complement = new TreeRangeSet.Complement()) : result;
   }

   public RangeSet<C> subRangeSet(Range<C> view) {
      return (RangeSet)(view.equals(Range.all()) ? this : new TreeRangeSet.SubRangeSet(view));
   }

   // $FF: synthetic method
   TreeRangeSet(NavigableMap x0, Object x1) {
      this(x0);
   }

   private final class SubRangeSet extends TreeRangeSet<C> {
      private final Range<C> restriction;

      SubRangeSet(Range<C> restriction) {
         super(new TreeRangeSet.SubRangeSetRangesByLowerBound(Range.all(), restriction, TreeRangeSet.this.rangesByLowerBound), null);
         this.restriction = restriction;
      }

      public boolean encloses(Range<C> range) {
         if (!this.restriction.isEmpty() && this.restriction.encloses(range)) {
            Range<C> enclosing = TreeRangeSet.this.rangeEnclosing(range);
            return enclosing != null && !enclosing.intersection(this.restriction).isEmpty();
         } else {
            return false;
         }
      }

      @Nullable
      public Range<C> rangeContaining(C value) {
         if (!this.restriction.contains(value)) {
            return null;
         } else {
            Range<C> result = TreeRangeSet.this.rangeContaining(value);
            return result == null ? null : result.intersection(this.restriction);
         }
      }

      public void add(Range<C> rangeToAdd) {
         Preconditions.checkArgument(this.restriction.encloses(rangeToAdd), "Cannot add range %s to subRangeSet(%s)", rangeToAdd, this.restriction);
         super.add(rangeToAdd);
      }

      public void remove(Range<C> rangeToRemove) {
         if (rangeToRemove.isConnected(this.restriction)) {
            TreeRangeSet.this.remove(rangeToRemove.intersection(this.restriction));
         }

      }

      public boolean contains(C value) {
         return this.restriction.contains(value) && TreeRangeSet.this.contains(value);
      }

      public void clear() {
         TreeRangeSet.this.remove(this.restriction);
      }

      public RangeSet<C> subRangeSet(Range<C> view) {
         if (view.encloses(this.restriction)) {
            return this;
         } else {
            return (RangeSet)(view.isConnected(this.restriction) ? new TreeRangeSet.SubRangeSet(this.restriction.intersection(view)) : ImmutableRangeSet.of());
         }
      }
   }

   private static final class SubRangeSetRangesByLowerBound<C extends Comparable<?>> extends AbstractNavigableMap<Cut<C>, Range<C>> {
      private final Range<Cut<C>> lowerBoundWindow;
      private final Range<C> restriction;
      private final NavigableMap<Cut<C>, Range<C>> rangesByLowerBound;
      private final NavigableMap<Cut<C>, Range<C>> rangesByUpperBound;

      private SubRangeSetRangesByLowerBound(Range<Cut<C>> lowerBoundWindow, Range<C> restriction, NavigableMap<Cut<C>, Range<C>> rangesByLowerBound) {
         this.lowerBoundWindow = (Range)Preconditions.checkNotNull(lowerBoundWindow);
         this.restriction = (Range)Preconditions.checkNotNull(restriction);
         this.rangesByLowerBound = (NavigableMap)Preconditions.checkNotNull(rangesByLowerBound);
         this.rangesByUpperBound = new TreeRangeSet.RangesByUpperBound(rangesByLowerBound);
      }

      private NavigableMap<Cut<C>, Range<C>> subMap(Range<Cut<C>> window) {
         return (NavigableMap)(!window.isConnected(this.lowerBoundWindow) ? ImmutableSortedMap.of() : new TreeRangeSet.SubRangeSetRangesByLowerBound(this.lowerBoundWindow.intersection(window), this.restriction, this.rangesByLowerBound));
      }

      public NavigableMap<Cut<C>, Range<C>> subMap(Cut<C> fromKey, boolean fromInclusive, Cut<C> toKey, boolean toInclusive) {
         return this.subMap(Range.range(fromKey, BoundType.forBoolean(fromInclusive), toKey, BoundType.forBoolean(toInclusive)));
      }

      public NavigableMap<Cut<C>, Range<C>> headMap(Cut<C> toKey, boolean inclusive) {
         return this.subMap(Range.upTo(toKey, BoundType.forBoolean(inclusive)));
      }

      public NavigableMap<Cut<C>, Range<C>> tailMap(Cut<C> fromKey, boolean inclusive) {
         return this.subMap(Range.downTo(fromKey, BoundType.forBoolean(inclusive)));
      }

      public Comparator<? super Cut<C>> comparator() {
         return Ordering.natural();
      }

      public boolean containsKey(@Nullable Object key) {
         return this.get(key) != null;
      }

      @Nullable
      public Range<C> get(@Nullable Object key) {
         if (key instanceof Cut) {
            try {
               Cut<C> cut = (Cut)key;
               if (!this.lowerBoundWindow.contains(cut) || cut.compareTo(this.restriction.lowerBound) < 0 || cut.compareTo(this.restriction.upperBound) >= 0) {
                  return null;
               }

               Range candidate;
               if (cut.equals(this.restriction.lowerBound)) {
                  candidate = (Range)Maps.valueOrNull(this.rangesByLowerBound.floorEntry(cut));
                  if (candidate != null && candidate.upperBound.compareTo(this.restriction.lowerBound) > 0) {
                     return candidate.intersection(this.restriction);
                  }
               } else {
                  candidate = (Range)this.rangesByLowerBound.get(cut);
                  if (candidate != null) {
                     return candidate.intersection(this.restriction);
                  }
               }
            } catch (ClassCastException var4) {
               return null;
            }
         }

         return null;
      }

      Iterator<Entry<Cut<C>, Range<C>>> entryIterator() {
         if (this.restriction.isEmpty()) {
            return Iterators.emptyIterator();
         } else if (this.lowerBoundWindow.upperBound.isLessThan(this.restriction.lowerBound)) {
            return Iterators.emptyIterator();
         } else {
            final Iterator completeRangeItr;
            if (this.lowerBoundWindow.lowerBound.isLessThan(this.restriction.lowerBound)) {
               completeRangeItr = this.rangesByUpperBound.tailMap(this.restriction.lowerBound, false).values().iterator();
            } else {
               completeRangeItr = this.rangesByLowerBound.tailMap(this.lowerBoundWindow.lowerBound.endpoint(), this.lowerBoundWindow.lowerBoundType() == BoundType.CLOSED).values().iterator();
            }

            final Cut<Cut<C>> upperBoundOnLowerBounds = (Cut)Ordering.natural().min(this.lowerBoundWindow.upperBound, Cut.belowValue(this.restriction.upperBound));
            return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
               protected Entry<Cut<C>, Range<C>> computeNext() {
                  if (!completeRangeItr.hasNext()) {
                     return (Entry)this.endOfData();
                  } else {
                     Range<C> nextRange = (Range)completeRangeItr.next();
                     if (upperBoundOnLowerBounds.isLessThan(nextRange.lowerBound)) {
                        return (Entry)this.endOfData();
                     } else {
                        nextRange = nextRange.intersection(SubRangeSetRangesByLowerBound.this.restriction);
                        return Maps.immutableEntry(nextRange.lowerBound, nextRange);
                     }
                  }
               }
            };
         }
      }

      Iterator<Entry<Cut<C>, Range<C>>> descendingEntryIterator() {
         if (this.restriction.isEmpty()) {
            return Iterators.emptyIterator();
         } else {
            Cut<Cut<C>> upperBoundOnLowerBounds = (Cut)Ordering.natural().min(this.lowerBoundWindow.upperBound, Cut.belowValue(this.restriction.upperBound));
            final Iterator<Range<C>> completeRangeItr = this.rangesByLowerBound.headMap(upperBoundOnLowerBounds.endpoint(), upperBoundOnLowerBounds.typeAsUpperBound() == BoundType.CLOSED).descendingMap().values().iterator();
            return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
               protected Entry<Cut<C>, Range<C>> computeNext() {
                  if (!completeRangeItr.hasNext()) {
                     return (Entry)this.endOfData();
                  } else {
                     Range<C> nextRange = (Range)completeRangeItr.next();
                     if (SubRangeSetRangesByLowerBound.this.restriction.lowerBound.compareTo(nextRange.upperBound) >= 0) {
                        return (Entry)this.endOfData();
                     } else {
                        nextRange = nextRange.intersection(SubRangeSetRangesByLowerBound.this.restriction);
                        return SubRangeSetRangesByLowerBound.this.lowerBoundWindow.contains(nextRange.lowerBound) ? Maps.immutableEntry(nextRange.lowerBound, nextRange) : (Entry)this.endOfData();
                     }
                  }
               }
            };
         }
      }

      public int size() {
         return Iterators.size(this.entryIterator());
      }

      // $FF: synthetic method
      SubRangeSetRangesByLowerBound(Range x0, Range x1, NavigableMap x2, Object x3) {
         this(x0, x1, x2);
      }
   }

   private final class Complement extends TreeRangeSet<C> {
      Complement() {
         super(new TreeRangeSet.ComplementRangesByLowerBound(TreeRangeSet.this.rangesByLowerBound), null);
      }

      public void add(Range<C> rangeToAdd) {
         TreeRangeSet.this.remove(rangeToAdd);
      }

      public void remove(Range<C> rangeToRemove) {
         TreeRangeSet.this.add(rangeToRemove);
      }

      public boolean contains(C value) {
         return !TreeRangeSet.this.contains(value);
      }

      public RangeSet<C> complement() {
         return TreeRangeSet.this;
      }
   }

   private static final class ComplementRangesByLowerBound<C extends Comparable<?>> extends AbstractNavigableMap<Cut<C>, Range<C>> {
      private final NavigableMap<Cut<C>, Range<C>> positiveRangesByLowerBound;
      private final NavigableMap<Cut<C>, Range<C>> positiveRangesByUpperBound;
      private final Range<Cut<C>> complementLowerBoundWindow;

      ComplementRangesByLowerBound(NavigableMap<Cut<C>, Range<C>> positiveRangesByLowerBound) {
         this(positiveRangesByLowerBound, Range.all());
      }

      private ComplementRangesByLowerBound(NavigableMap<Cut<C>, Range<C>> positiveRangesByLowerBound, Range<Cut<C>> window) {
         this.positiveRangesByLowerBound = positiveRangesByLowerBound;
         this.positiveRangesByUpperBound = new TreeRangeSet.RangesByUpperBound(positiveRangesByLowerBound);
         this.complementLowerBoundWindow = window;
      }

      private NavigableMap<Cut<C>, Range<C>> subMap(Range<Cut<C>> subWindow) {
         if (!this.complementLowerBoundWindow.isConnected(subWindow)) {
            return ImmutableSortedMap.of();
         } else {
            subWindow = subWindow.intersection(this.complementLowerBoundWindow);
            return new TreeRangeSet.ComplementRangesByLowerBound(this.positiveRangesByLowerBound, subWindow);
         }
      }

      public NavigableMap<Cut<C>, Range<C>> subMap(Cut<C> fromKey, boolean fromInclusive, Cut<C> toKey, boolean toInclusive) {
         return this.subMap(Range.range(fromKey, BoundType.forBoolean(fromInclusive), toKey, BoundType.forBoolean(toInclusive)));
      }

      public NavigableMap<Cut<C>, Range<C>> headMap(Cut<C> toKey, boolean inclusive) {
         return this.subMap(Range.upTo(toKey, BoundType.forBoolean(inclusive)));
      }

      public NavigableMap<Cut<C>, Range<C>> tailMap(Cut<C> fromKey, boolean inclusive) {
         return this.subMap(Range.downTo(fromKey, BoundType.forBoolean(inclusive)));
      }

      public Comparator<? super Cut<C>> comparator() {
         return Ordering.natural();
      }

      Iterator<Entry<Cut<C>, Range<C>>> entryIterator() {
         Collection positiveRanges;
         if (this.complementLowerBoundWindow.hasLowerBound()) {
            positiveRanges = this.positiveRangesByUpperBound.tailMap(this.complementLowerBoundWindow.lowerEndpoint(), this.complementLowerBoundWindow.lowerBoundType() == BoundType.CLOSED).values();
         } else {
            positiveRanges = this.positiveRangesByUpperBound.values();
         }

         final PeekingIterator<Range<C>> positiveItr = Iterators.peekingIterator(positiveRanges.iterator());
         final Cut firstComplementRangeLowerBound;
         if (this.complementLowerBoundWindow.contains(Cut.belowAll()) && (!positiveItr.hasNext() || ((Range)positiveItr.peek()).lowerBound != Cut.belowAll())) {
            firstComplementRangeLowerBound = Cut.belowAll();
         } else {
            if (!positiveItr.hasNext()) {
               return Iterators.emptyIterator();
            }

            firstComplementRangeLowerBound = ((Range)positiveItr.next()).upperBound;
         }

         return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
            Cut<C> nextComplementRangeLowerBound = firstComplementRangeLowerBound;

            protected Entry<Cut<C>, Range<C>> computeNext() {
               if (!ComplementRangesByLowerBound.this.complementLowerBoundWindow.upperBound.isLessThan(this.nextComplementRangeLowerBound) && this.nextComplementRangeLowerBound != Cut.aboveAll()) {
                  Range negativeRange;
                  if (positiveItr.hasNext()) {
                     Range<C> positiveRange = (Range)positiveItr.next();
                     negativeRange = Range.create(this.nextComplementRangeLowerBound, positiveRange.lowerBound);
                     this.nextComplementRangeLowerBound = positiveRange.upperBound;
                  } else {
                     negativeRange = Range.create(this.nextComplementRangeLowerBound, Cut.aboveAll());
                     this.nextComplementRangeLowerBound = Cut.aboveAll();
                  }

                  return Maps.immutableEntry(negativeRange.lowerBound, negativeRange);
               } else {
                  return (Entry)this.endOfData();
               }
            }
         };
      }

      Iterator<Entry<Cut<C>, Range<C>>> descendingEntryIterator() {
         Cut<C> startingPoint = this.complementLowerBoundWindow.hasUpperBound() ? (Cut)this.complementLowerBoundWindow.upperEndpoint() : Cut.aboveAll();
         boolean inclusive = this.complementLowerBoundWindow.hasUpperBound() && this.complementLowerBoundWindow.upperBoundType() == BoundType.CLOSED;
         final PeekingIterator<Range<C>> positiveItr = Iterators.peekingIterator(this.positiveRangesByUpperBound.headMap(startingPoint, inclusive).descendingMap().values().iterator());
         Cut cut;
         if (positiveItr.hasNext()) {
            cut = ((Range)positiveItr.peek()).upperBound == Cut.aboveAll() ? ((Range)positiveItr.next()).lowerBound : (Cut)this.positiveRangesByLowerBound.higherKey(((Range)positiveItr.peek()).upperBound);
         } else {
            if (!this.complementLowerBoundWindow.contains(Cut.belowAll()) || this.positiveRangesByLowerBound.containsKey(Cut.belowAll())) {
               return Iterators.emptyIterator();
            }

            cut = (Cut)this.positiveRangesByLowerBound.higherKey(Cut.belowAll());
         }

         final Cut<C> firstComplementRangeUpperBound = (Cut)MoreObjects.firstNonNull(cut, Cut.aboveAll());
         return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
            Cut<C> nextComplementRangeUpperBound = firstComplementRangeUpperBound;

            protected Entry<Cut<C>, Range<C>> computeNext() {
               if (this.nextComplementRangeUpperBound == Cut.belowAll()) {
                  return (Entry)this.endOfData();
               } else {
                  Range negativeRange;
                  if (positiveItr.hasNext()) {
                     negativeRange = (Range)positiveItr.next();
                     Range<C> negativeRangex = Range.create(negativeRange.upperBound, this.nextComplementRangeUpperBound);
                     this.nextComplementRangeUpperBound = negativeRange.lowerBound;
                     if (ComplementRangesByLowerBound.this.complementLowerBoundWindow.lowerBound.isLessThan(negativeRangex.lowerBound)) {
                        return Maps.immutableEntry(negativeRangex.lowerBound, negativeRangex);
                     }
                  } else if (ComplementRangesByLowerBound.this.complementLowerBoundWindow.lowerBound.isLessThan(Cut.belowAll())) {
                     negativeRange = Range.create(Cut.belowAll(), this.nextComplementRangeUpperBound);
                     this.nextComplementRangeUpperBound = Cut.belowAll();
                     return Maps.immutableEntry(Cut.belowAll(), negativeRange);
                  }

                  return (Entry)this.endOfData();
               }
            }
         };
      }

      public int size() {
         return Iterators.size(this.entryIterator());
      }

      @Nullable
      public Range<C> get(Object key) {
         if (key instanceof Cut) {
            try {
               Cut<C> cut = (Cut)key;
               Entry<Cut<C>, Range<C>> firstEntry = this.tailMap(cut, true).firstEntry();
               if (firstEntry != null && ((Cut)firstEntry.getKey()).equals(cut)) {
                  return (Range)firstEntry.getValue();
               }
            } catch (ClassCastException var4) {
               return null;
            }
         }

         return null;
      }

      public boolean containsKey(Object key) {
         return this.get(key) != null;
      }
   }

   @VisibleForTesting
   static final class RangesByUpperBound<C extends Comparable<?>> extends AbstractNavigableMap<Cut<C>, Range<C>> {
      private final NavigableMap<Cut<C>, Range<C>> rangesByLowerBound;
      private final Range<Cut<C>> upperBoundWindow;

      RangesByUpperBound(NavigableMap<Cut<C>, Range<C>> rangesByLowerBound) {
         this.rangesByLowerBound = rangesByLowerBound;
         this.upperBoundWindow = Range.all();
      }

      private RangesByUpperBound(NavigableMap<Cut<C>, Range<C>> rangesByLowerBound, Range<Cut<C>> upperBoundWindow) {
         this.rangesByLowerBound = rangesByLowerBound;
         this.upperBoundWindow = upperBoundWindow;
      }

      private NavigableMap<Cut<C>, Range<C>> subMap(Range<Cut<C>> window) {
         return (NavigableMap)(window.isConnected(this.upperBoundWindow) ? new TreeRangeSet.RangesByUpperBound(this.rangesByLowerBound, window.intersection(this.upperBoundWindow)) : ImmutableSortedMap.of());
      }

      public NavigableMap<Cut<C>, Range<C>> subMap(Cut<C> fromKey, boolean fromInclusive, Cut<C> toKey, boolean toInclusive) {
         return this.subMap(Range.range(fromKey, BoundType.forBoolean(fromInclusive), toKey, BoundType.forBoolean(toInclusive)));
      }

      public NavigableMap<Cut<C>, Range<C>> headMap(Cut<C> toKey, boolean inclusive) {
         return this.subMap(Range.upTo(toKey, BoundType.forBoolean(inclusive)));
      }

      public NavigableMap<Cut<C>, Range<C>> tailMap(Cut<C> fromKey, boolean inclusive) {
         return this.subMap(Range.downTo(fromKey, BoundType.forBoolean(inclusive)));
      }

      public Comparator<? super Cut<C>> comparator() {
         return Ordering.natural();
      }

      public boolean containsKey(@Nullable Object key) {
         return this.get(key) != null;
      }

      public Range<C> get(@Nullable Object key) {
         if (key instanceof Cut) {
            try {
               Cut<C> cut = (Cut)key;
               if (!this.upperBoundWindow.contains(cut)) {
                  return null;
               }

               Entry<Cut<C>, Range<C>> candidate = this.rangesByLowerBound.lowerEntry(cut);
               if (candidate != null && ((Range)candidate.getValue()).upperBound.equals(cut)) {
                  return (Range)candidate.getValue();
               }
            } catch (ClassCastException var4) {
               return null;
            }
         }

         return null;
      }

      Iterator<Entry<Cut<C>, Range<C>>> entryIterator() {
         final Iterator backingItr;
         if (!this.upperBoundWindow.hasLowerBound()) {
            backingItr = this.rangesByLowerBound.values().iterator();
         } else {
            Entry<Cut<C>, Range<C>> lowerEntry = this.rangesByLowerBound.lowerEntry(this.upperBoundWindow.lowerEndpoint());
            if (lowerEntry == null) {
               backingItr = this.rangesByLowerBound.values().iterator();
            } else if (this.upperBoundWindow.lowerBound.isLessThan(((Range)lowerEntry.getValue()).upperBound)) {
               backingItr = this.rangesByLowerBound.tailMap(lowerEntry.getKey(), true).values().iterator();
            } else {
               backingItr = this.rangesByLowerBound.tailMap(this.upperBoundWindow.lowerEndpoint(), true).values().iterator();
            }
         }

         return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
            protected Entry<Cut<C>, Range<C>> computeNext() {
               if (!backingItr.hasNext()) {
                  return (Entry)this.endOfData();
               } else {
                  Range<C> range = (Range)backingItr.next();
                  return RangesByUpperBound.this.upperBoundWindow.upperBound.isLessThan(range.upperBound) ? (Entry)this.endOfData() : Maps.immutableEntry(range.upperBound, range);
               }
            }
         };
      }

      Iterator<Entry<Cut<C>, Range<C>>> descendingEntryIterator() {
         Collection candidates;
         if (this.upperBoundWindow.hasUpperBound()) {
            candidates = this.rangesByLowerBound.headMap(this.upperBoundWindow.upperEndpoint(), false).descendingMap().values();
         } else {
            candidates = this.rangesByLowerBound.descendingMap().values();
         }

         final PeekingIterator<Range<C>> backingItr = Iterators.peekingIterator(candidates.iterator());
         if (backingItr.hasNext() && this.upperBoundWindow.upperBound.isLessThan(((Range)backingItr.peek()).upperBound)) {
            backingItr.next();
         }

         return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
            protected Entry<Cut<C>, Range<C>> computeNext() {
               if (!backingItr.hasNext()) {
                  return (Entry)this.endOfData();
               } else {
                  Range<C> range = (Range)backingItr.next();
                  return RangesByUpperBound.this.upperBoundWindow.lowerBound.isLessThan(range.upperBound) ? Maps.immutableEntry(range.upperBound, range) : (Entry)this.endOfData();
               }
            }
         };
      }

      public int size() {
         return this.upperBoundWindow.equals(Range.all()) ? this.rangesByLowerBound.size() : Iterators.size(this.entryIterator());
      }

      public boolean isEmpty() {
         return this.upperBoundWindow.equals(Range.all()) ? this.rangesByLowerBound.isEmpty() : !this.entryIterator().hasNext();
      }
   }

   final class AsRanges extends ForwardingCollection<Range<C>> implements Set<Range<C>> {
      final Collection<Range<C>> delegate;

      AsRanges(Collection<Range<C>> delegate) {
         this.delegate = delegate;
      }

      protected Collection<Range<C>> delegate() {
         return this.delegate;
      }

      public int hashCode() {
         return Sets.hashCodeImpl(this);
      }

      public boolean equals(@Nullable Object o) {
         return Sets.equalsImpl(this, o);
      }
   }
}
