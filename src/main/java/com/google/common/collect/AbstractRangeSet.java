package com.google.common.collect;

import java.util.Iterator;
import javax.annotation.Nullable;

abstract class AbstractRangeSet<C extends Comparable> implements RangeSet<C> {
   public boolean contains(C value) {
      return this.rangeContaining(value) != null;
   }

   public abstract Range<C> rangeContaining(C var1);

   public boolean isEmpty() {
      return this.asRanges().isEmpty();
   }

   public void add(Range<C> range) {
      throw new UnsupportedOperationException();
   }

   public void remove(Range<C> range) {
      throw new UnsupportedOperationException();
   }

   public void clear() {
      this.remove(Range.all());
   }

   public boolean enclosesAll(RangeSet<C> other) {
      Iterator i$ = other.asRanges().iterator();

      Range range;
      do {
         if (!i$.hasNext()) {
            return true;
         }

         range = (Range)i$.next();
      } while(this.encloses(range));

      return false;
   }

   public void addAll(RangeSet<C> other) {
      Iterator i$ = other.asRanges().iterator();

      while(i$.hasNext()) {
         Range<C> range = (Range)i$.next();
         this.add(range);
      }

   }

   public void removeAll(RangeSet<C> other) {
      Iterator i$ = other.asRanges().iterator();

      while(i$.hasNext()) {
         Range<C> range = (Range)i$.next();
         this.remove(range);
      }

   }

   public abstract boolean encloses(Range<C> var1);

   public boolean equals(@Nullable Object obj) {
      if (obj == this) {
         return true;
      } else if (obj instanceof RangeSet) {
         RangeSet<?> other = (RangeSet)obj;
         return this.asRanges().equals(other.asRanges());
      } else {
         return false;
      }
   }

   public final int hashCode() {
      return this.asRanges().hashCode();
   }

   public final String toString() {
      return this.asRanges().toString();
   }
}
