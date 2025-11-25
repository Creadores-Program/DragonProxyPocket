package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@Beta
@GwtIncompatible("hasn't been tested yet")
public abstract class ImmutableSortedMultiset<E> extends ImmutableSortedMultisetFauxverideShim<E> implements SortedMultiset<E> {
   private static final Comparator<Comparable> NATURAL_ORDER = Ordering.natural();
   private static final ImmutableSortedMultiset<Comparable> NATURAL_EMPTY_MULTISET;
   transient ImmutableSortedMultiset<E> descendingMultiset;

   public static <E> ImmutableSortedMultiset<E> of() {
      return NATURAL_EMPTY_MULTISET;
   }

   public static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(E element) {
      RegularImmutableSortedSet<E> elementSet = (RegularImmutableSortedSet)ImmutableSortedSet.of(element);
      long[] cumulativeCounts = new long[]{0L, 1L};
      return new RegularImmutableSortedMultiset(elementSet, cumulativeCounts, 0, 1);
   }

   public static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(E e1, E e2) {
      return copyOf(Ordering.natural(), (Iterable)Arrays.asList(e1, e2));
   }

   public static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(E e1, E e2, E e3) {
      return copyOf(Ordering.natural(), (Iterable)Arrays.asList(e1, e2, e3));
   }

   public static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(E e1, E e2, E e3, E e4) {
      return copyOf(Ordering.natural(), (Iterable)Arrays.asList(e1, e2, e3, e4));
   }

   public static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(E e1, E e2, E e3, E e4, E e5) {
      return copyOf(Ordering.natural(), (Iterable)Arrays.asList(e1, e2, e3, e4, e5));
   }

   public static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E... remaining) {
      int size = remaining.length + 6;
      List<E> all = Lists.newArrayListWithCapacity(size);
      Collections.addAll(all, new Comparable[]{e1, e2, e3, e4, e5, e6});
      Collections.addAll(all, remaining);
      return copyOf(Ordering.natural(), (Iterable)all);
   }

   public static <E extends Comparable<? super E>> ImmutableSortedMultiset<E> copyOf(E[] elements) {
      return copyOf(Ordering.natural(), (Iterable)Arrays.asList(elements));
   }

   public static <E> ImmutableSortedMultiset<E> copyOf(Iterable<? extends E> elements) {
      Ordering<E> naturalOrder = Ordering.natural();
      return copyOf(naturalOrder, (Iterable)elements);
   }

   public static <E> ImmutableSortedMultiset<E> copyOf(Iterator<? extends E> elements) {
      Ordering<E> naturalOrder = Ordering.natural();
      return copyOf(naturalOrder, (Iterator)elements);
   }

   public static <E> ImmutableSortedMultiset<E> copyOf(Comparator<? super E> comparator, Iterator<? extends E> elements) {
      Preconditions.checkNotNull(comparator);
      return (new ImmutableSortedMultiset.Builder(comparator)).addAll(elements).build();
   }

   public static <E> ImmutableSortedMultiset<E> copyOf(Comparator<? super E> comparator, Iterable<? extends E> elements) {
      if (elements instanceof ImmutableSortedMultiset) {
         ImmutableSortedMultiset<E> multiset = (ImmutableSortedMultiset)elements;
         if (comparator.equals(multiset.comparator())) {
            if (multiset.isPartialView()) {
               return copyOfSortedEntries(comparator, multiset.entrySet().asList());
            }

            return multiset;
         }
      }

      Iterable<? extends E> elements = Lists.newArrayList(elements);
      TreeMultiset<E> sortedCopy = TreeMultiset.create((Comparator)Preconditions.checkNotNull(comparator));
      Iterables.addAll(sortedCopy, elements);
      return copyOfSortedEntries(comparator, sortedCopy.entrySet());
   }

   public static <E> ImmutableSortedMultiset<E> copyOfSorted(SortedMultiset<E> sortedMultiset) {
      return copyOfSortedEntries(sortedMultiset.comparator(), Lists.newArrayList((Iterable)sortedMultiset.entrySet()));
   }

   private static <E> ImmutableSortedMultiset<E> copyOfSortedEntries(Comparator<? super E> comparator, Collection<Multiset.Entry<E>> entries) {
      if (entries.isEmpty()) {
         return emptyMultiset(comparator);
      } else {
         ImmutableList.Builder<E> elementsBuilder = new ImmutableList.Builder(entries.size());
         long[] cumulativeCounts = new long[entries.size() + 1];
         int i = 0;

         for(Iterator i$ = entries.iterator(); i$.hasNext(); ++i) {
            Multiset.Entry<E> entry = (Multiset.Entry)i$.next();
            elementsBuilder.add(entry.getElement());
            cumulativeCounts[i + 1] = cumulativeCounts[i] + (long)entry.getCount();
         }

         return new RegularImmutableSortedMultiset(new RegularImmutableSortedSet(elementsBuilder.build(), comparator), cumulativeCounts, 0, entries.size());
      }
   }

   static <E> ImmutableSortedMultiset<E> emptyMultiset(Comparator<? super E> comparator) {
      return (ImmutableSortedMultiset)(NATURAL_ORDER.equals(comparator) ? NATURAL_EMPTY_MULTISET : new RegularImmutableSortedMultiset(comparator));
   }

   ImmutableSortedMultiset() {
   }

   public final Comparator<? super E> comparator() {
      return this.elementSet().comparator();
   }

   public abstract ImmutableSortedSet<E> elementSet();

   public ImmutableSortedMultiset<E> descendingMultiset() {
      ImmutableSortedMultiset<E> result = this.descendingMultiset;
      return result == null ? (this.descendingMultiset = (ImmutableSortedMultiset)(this.isEmpty() ? emptyMultiset(Ordering.from(this.comparator()).reverse()) : new DescendingImmutableSortedMultiset(this))) : result;
   }

   /** @deprecated */
   @Deprecated
   public final Multiset.Entry<E> pollFirstEntry() {
      throw new UnsupportedOperationException();
   }

   /** @deprecated */
   @Deprecated
   public final Multiset.Entry<E> pollLastEntry() {
      throw new UnsupportedOperationException();
   }

   public abstract ImmutableSortedMultiset<E> headMultiset(E var1, BoundType var2);

   public ImmutableSortedMultiset<E> subMultiset(E lowerBound, BoundType lowerBoundType, E upperBound, BoundType upperBoundType) {
      Preconditions.checkArgument(this.comparator().compare(lowerBound, upperBound) <= 0, "Expected lowerBound <= upperBound but %s > %s", lowerBound, upperBound);
      return this.tailMultiset(lowerBound, lowerBoundType).headMultiset(upperBound, upperBoundType);
   }

   public abstract ImmutableSortedMultiset<E> tailMultiset(E var1, BoundType var2);

   public static <E> ImmutableSortedMultiset.Builder<E> orderedBy(Comparator<E> comparator) {
      return new ImmutableSortedMultiset.Builder(comparator);
   }

   public static <E extends Comparable<?>> ImmutableSortedMultiset.Builder<E> reverseOrder() {
      return new ImmutableSortedMultiset.Builder(Ordering.natural().reverse());
   }

   public static <E extends Comparable<?>> ImmutableSortedMultiset.Builder<E> naturalOrder() {
      return new ImmutableSortedMultiset.Builder(Ordering.natural());
   }

   Object writeReplace() {
      return new ImmutableSortedMultiset.SerializedForm(this);
   }

   static {
      NATURAL_EMPTY_MULTISET = new RegularImmutableSortedMultiset(NATURAL_ORDER);
   }

   private static final class SerializedForm<E> implements Serializable {
      Comparator<? super E> comparator;
      E[] elements;
      int[] counts;

      SerializedForm(SortedMultiset<E> multiset) {
         this.comparator = multiset.comparator();
         int n = multiset.entrySet().size();
         this.elements = (Object[])(new Object[n]);
         this.counts = new int[n];
         int i = 0;

         for(Iterator i$ = multiset.entrySet().iterator(); i$.hasNext(); ++i) {
            Multiset.Entry<E> entry = (Multiset.Entry)i$.next();
            this.elements[i] = entry.getElement();
            this.counts[i] = entry.getCount();
         }

      }

      Object readResolve() {
         int n = this.elements.length;
         ImmutableSortedMultiset.Builder<E> builder = new ImmutableSortedMultiset.Builder(this.comparator);

         for(int i = 0; i < n; ++i) {
            builder.addCopies(this.elements[i], this.counts[i]);
         }

         return builder.build();
      }
   }

   public static class Builder<E> extends ImmutableMultiset.Builder<E> {
      public Builder(Comparator<? super E> comparator) {
         super(TreeMultiset.create((Comparator)Preconditions.checkNotNull(comparator)));
      }

      public ImmutableSortedMultiset.Builder<E> add(E element) {
         super.add(element);
         return this;
      }

      public ImmutableSortedMultiset.Builder<E> addCopies(E element, int occurrences) {
         super.addCopies(element, occurrences);
         return this;
      }

      public ImmutableSortedMultiset.Builder<E> setCount(E element, int count) {
         super.setCount(element, count);
         return this;
      }

      public ImmutableSortedMultiset.Builder<E> add(E... elements) {
         super.add(elements);
         return this;
      }

      public ImmutableSortedMultiset.Builder<E> addAll(Iterable<? extends E> elements) {
         super.addAll(elements);
         return this;
      }

      public ImmutableSortedMultiset.Builder<E> addAll(Iterator<? extends E> elements) {
         super.addAll(elements);
         return this;
      }

      public ImmutableSortedMultiset<E> build() {
         return ImmutableSortedMultiset.copyOfSorted((SortedMultiset)this.contents);
      }
   }
}
