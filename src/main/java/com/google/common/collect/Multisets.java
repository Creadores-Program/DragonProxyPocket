package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.primitives.Ints;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@GwtCompatible
public final class Multisets {
   private static final Ordering<Multiset.Entry<?>> DECREASING_COUNT_ORDERING = new Ordering<Multiset.Entry<?>>() {
      public int compare(Multiset.Entry<?> entry1, Multiset.Entry<?> entry2) {
         return Ints.compare(entry2.getCount(), entry1.getCount());
      }
   };

   private Multisets() {
   }

   public static <E> Multiset<E> unmodifiableMultiset(Multiset<? extends E> multiset) {
      return (Multiset)(!(multiset instanceof Multisets.UnmodifiableMultiset) && !(multiset instanceof ImmutableMultiset) ? new Multisets.UnmodifiableMultiset((Multiset)Preconditions.checkNotNull(multiset)) : multiset);
   }

   /** @deprecated */
   @Deprecated
   public static <E> Multiset<E> unmodifiableMultiset(ImmutableMultiset<E> multiset) {
      return (Multiset)Preconditions.checkNotNull(multiset);
   }

   @Beta
   public static <E> SortedMultiset<E> unmodifiableSortedMultiset(SortedMultiset<E> sortedMultiset) {
      return new UnmodifiableSortedMultiset((SortedMultiset)Preconditions.checkNotNull(sortedMultiset));
   }

   public static <E> Multiset.Entry<E> immutableEntry(@Nullable E e, int n) {
      return new Multisets.ImmutableEntry(e, n);
   }

   @CheckReturnValue
   @Beta
   public static <E> Multiset<E> filter(Multiset<E> unfiltered, Predicate<? super E> predicate) {
      if (unfiltered instanceof Multisets.FilteredMultiset) {
         Multisets.FilteredMultiset<E> filtered = (Multisets.FilteredMultiset)unfiltered;
         Predicate<E> combinedPredicate = Predicates.and(filtered.predicate, predicate);
         return new Multisets.FilteredMultiset(filtered.unfiltered, combinedPredicate);
      } else {
         return new Multisets.FilteredMultiset(unfiltered, predicate);
      }
   }

   static int inferDistinctElements(Iterable<?> elements) {
      return elements instanceof Multiset ? ((Multiset)elements).elementSet().size() : 11;
   }

   @Beta
   public static <E> Multiset<E> union(final Multiset<? extends E> multiset1, final Multiset<? extends E> multiset2) {
      Preconditions.checkNotNull(multiset1);
      Preconditions.checkNotNull(multiset2);
      return new AbstractMultiset<E>() {
         public boolean contains(@Nullable Object element) {
            return multiset1.contains(element) || multiset2.contains(element);
         }

         public boolean isEmpty() {
            return multiset1.isEmpty() && multiset2.isEmpty();
         }

         public int count(Object element) {
            return Math.max(multiset1.count(element), multiset2.count(element));
         }

         Set<E> createElementSet() {
            return Sets.union(multiset1.elementSet(), multiset2.elementSet());
         }

         Iterator<Multiset.Entry<E>> entryIterator() {
            final Iterator<? extends Multiset.Entry<? extends E>> iterator1 = multiset1.entrySet().iterator();
            final Iterator<? extends Multiset.Entry<? extends E>> iterator2 = multiset2.entrySet().iterator();
            return new AbstractIterator<Multiset.Entry<E>>() {
               protected Multiset.Entry<E> computeNext() {
                  Multiset.Entry entry2;
                  Object element;
                  if (iterator1.hasNext()) {
                     entry2 = (Multiset.Entry)iterator1.next();
                     element = entry2.getElement();
                     int count = Math.max(entry2.getCount(), multiset2.count(element));
                     return Multisets.immutableEntry(element, count);
                  } else {
                     do {
                        if (!iterator2.hasNext()) {
                           return (Multiset.Entry)this.endOfData();
                        }

                        entry2 = (Multiset.Entry)iterator2.next();
                        element = entry2.getElement();
                     } while(multiset1.contains(element));

                     return Multisets.immutableEntry(element, entry2.getCount());
                  }
               }
            };
         }

         int distinctElements() {
            return this.elementSet().size();
         }
      };
   }

   public static <E> Multiset<E> intersection(final Multiset<E> multiset1, final Multiset<?> multiset2) {
      Preconditions.checkNotNull(multiset1);
      Preconditions.checkNotNull(multiset2);
      return new AbstractMultiset<E>() {
         public int count(Object element) {
            int count1 = multiset1.count(element);
            return count1 == 0 ? 0 : Math.min(count1, multiset2.count(element));
         }

         Set<E> createElementSet() {
            return Sets.intersection(multiset1.elementSet(), multiset2.elementSet());
         }

         Iterator<Multiset.Entry<E>> entryIterator() {
            final Iterator<Multiset.Entry<E>> iterator1 = multiset1.entrySet().iterator();
            return new AbstractIterator<Multiset.Entry<E>>() {
               protected Multiset.Entry<E> computeNext() {
                  while(true) {
                     if (iterator1.hasNext()) {
                        Multiset.Entry<E> entry1 = (Multiset.Entry)iterator1.next();
                        E element = entry1.getElement();
                        int count = Math.min(entry1.getCount(), multiset2.count(element));
                        if (count <= 0) {
                           continue;
                        }

                        return Multisets.immutableEntry(element, count);
                     }

                     return (Multiset.Entry)this.endOfData();
                  }
               }
            };
         }

         int distinctElements() {
            return this.elementSet().size();
         }
      };
   }

   @Beta
   public static <E> Multiset<E> sum(final Multiset<? extends E> multiset1, final Multiset<? extends E> multiset2) {
      Preconditions.checkNotNull(multiset1);
      Preconditions.checkNotNull(multiset2);
      return new AbstractMultiset<E>() {
         public boolean contains(@Nullable Object element) {
            return multiset1.contains(element) || multiset2.contains(element);
         }

         public boolean isEmpty() {
            return multiset1.isEmpty() && multiset2.isEmpty();
         }

         public int size() {
            return multiset1.size() + multiset2.size();
         }

         public int count(Object element) {
            return multiset1.count(element) + multiset2.count(element);
         }

         Set<E> createElementSet() {
            return Sets.union(multiset1.elementSet(), multiset2.elementSet());
         }

         Iterator<Multiset.Entry<E>> entryIterator() {
            final Iterator<? extends Multiset.Entry<? extends E>> iterator1 = multiset1.entrySet().iterator();
            final Iterator<? extends Multiset.Entry<? extends E>> iterator2 = multiset2.entrySet().iterator();
            return new AbstractIterator<Multiset.Entry<E>>() {
               protected Multiset.Entry<E> computeNext() {
                  Multiset.Entry entry2;
                  Object element;
                  if (iterator1.hasNext()) {
                     entry2 = (Multiset.Entry)iterator1.next();
                     element = entry2.getElement();
                     int count = entry2.getCount() + multiset2.count(element);
                     return Multisets.immutableEntry(element, count);
                  } else {
                     do {
                        if (!iterator2.hasNext()) {
                           return (Multiset.Entry)this.endOfData();
                        }

                        entry2 = (Multiset.Entry)iterator2.next();
                        element = entry2.getElement();
                     } while(multiset1.contains(element));

                     return Multisets.immutableEntry(element, entry2.getCount());
                  }
               }
            };
         }

         int distinctElements() {
            return this.elementSet().size();
         }
      };
   }

   @Beta
   public static <E> Multiset<E> difference(final Multiset<E> multiset1, final Multiset<?> multiset2) {
      Preconditions.checkNotNull(multiset1);
      Preconditions.checkNotNull(multiset2);
      return new AbstractMultiset<E>() {
         public int count(@Nullable Object element) {
            int count1 = multiset1.count(element);
            return count1 == 0 ? 0 : Math.max(0, count1 - multiset2.count(element));
         }

         Iterator<Multiset.Entry<E>> entryIterator() {
            final Iterator<Multiset.Entry<E>> iterator1 = multiset1.entrySet().iterator();
            return new AbstractIterator<Multiset.Entry<E>>() {
               protected Multiset.Entry<E> computeNext() {
                  while(true) {
                     if (iterator1.hasNext()) {
                        Multiset.Entry<E> entry1 = (Multiset.Entry)iterator1.next();
                        E element = entry1.getElement();
                        int count = entry1.getCount() - multiset2.count(element);
                        if (count <= 0) {
                           continue;
                        }

                        return Multisets.immutableEntry(element, count);
                     }

                     return (Multiset.Entry)this.endOfData();
                  }
               }
            };
         }

         int distinctElements() {
            return Iterators.size(this.entryIterator());
         }
      };
   }

   public static boolean containsOccurrences(Multiset<?> superMultiset, Multiset<?> subMultiset) {
      Preconditions.checkNotNull(superMultiset);
      Preconditions.checkNotNull(subMultiset);
      Iterator i$ = subMultiset.entrySet().iterator();

      Multiset.Entry entry;
      int superCount;
      do {
         if (!i$.hasNext()) {
            return true;
         }

         entry = (Multiset.Entry)i$.next();
         superCount = superMultiset.count(entry.getElement());
      } while(superCount >= entry.getCount());

      return false;
   }

   public static boolean retainOccurrences(Multiset<?> multisetToModify, Multiset<?> multisetToRetain) {
      return retainOccurrencesImpl(multisetToModify, multisetToRetain);
   }

   private static <E> boolean retainOccurrencesImpl(Multiset<E> multisetToModify, Multiset<?> occurrencesToRetain) {
      Preconditions.checkNotNull(multisetToModify);
      Preconditions.checkNotNull(occurrencesToRetain);
      Iterator<Multiset.Entry<E>> entryIterator = multisetToModify.entrySet().iterator();
      boolean changed = false;

      while(entryIterator.hasNext()) {
         Multiset.Entry<E> entry = (Multiset.Entry)entryIterator.next();
         int retainCount = occurrencesToRetain.count(entry.getElement());
         if (retainCount == 0) {
            entryIterator.remove();
            changed = true;
         } else if (retainCount < entry.getCount()) {
            multisetToModify.setCount(entry.getElement(), retainCount);
            changed = true;
         }
      }

      return changed;
   }

   public static boolean removeOccurrences(Multiset<?> multisetToModify, Iterable<?> occurrencesToRemove) {
      if (occurrencesToRemove instanceof Multiset) {
         return removeOccurrences(multisetToModify, (Multiset)occurrencesToRemove);
      } else {
         Preconditions.checkNotNull(multisetToModify);
         Preconditions.checkNotNull(occurrencesToRemove);
         boolean changed = false;

         Object o;
         for(Iterator i$ = occurrencesToRemove.iterator(); i$.hasNext(); changed |= multisetToModify.remove(o)) {
            o = i$.next();
         }

         return changed;
      }
   }

   public static boolean removeOccurrences(Multiset<?> multisetToModify, Multiset<?> occurrencesToRemove) {
      Preconditions.checkNotNull(multisetToModify);
      Preconditions.checkNotNull(occurrencesToRemove);
      boolean changed = false;
      Iterator entryIterator = multisetToModify.entrySet().iterator();

      while(entryIterator.hasNext()) {
         Multiset.Entry<?> entry = (Multiset.Entry)entryIterator.next();
         int removeCount = occurrencesToRemove.count(entry.getElement());
         if (removeCount >= entry.getCount()) {
            entryIterator.remove();
            changed = true;
         } else if (removeCount > 0) {
            multisetToModify.remove(entry.getElement(), removeCount);
            changed = true;
         }
      }

      return changed;
   }

   static boolean equalsImpl(Multiset<?> multiset, @Nullable Object object) {
      if (object == multiset) {
         return true;
      } else if (object instanceof Multiset) {
         Multiset<?> that = (Multiset)object;
         if (multiset.size() == that.size() && multiset.entrySet().size() == that.entrySet().size()) {
            Iterator i$ = that.entrySet().iterator();

            Multiset.Entry entry;
            do {
               if (!i$.hasNext()) {
                  return true;
               }

               entry = (Multiset.Entry)i$.next();
            } while(multiset.count(entry.getElement()) == entry.getCount());

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   static <E> boolean addAllImpl(Multiset<E> self, Collection<? extends E> elements) {
      if (elements.isEmpty()) {
         return false;
      } else {
         if (elements instanceof Multiset) {
            Multiset<? extends E> that = cast(elements);
            Iterator i$ = that.entrySet().iterator();

            while(i$.hasNext()) {
               Multiset.Entry<? extends E> entry = (Multiset.Entry)i$.next();
               self.add(entry.getElement(), entry.getCount());
            }
         } else {
            Iterators.addAll(self, elements.iterator());
         }

         return true;
      }
   }

   static boolean removeAllImpl(Multiset<?> self, Collection<?> elementsToRemove) {
      Collection<?> collection = elementsToRemove instanceof Multiset ? ((Multiset)elementsToRemove).elementSet() : elementsToRemove;
      return self.elementSet().removeAll((Collection)collection);
   }

   static boolean retainAllImpl(Multiset<?> self, Collection<?> elementsToRetain) {
      Preconditions.checkNotNull(elementsToRetain);
      Collection<?> collection = elementsToRetain instanceof Multiset ? ((Multiset)elementsToRetain).elementSet() : elementsToRetain;
      return self.elementSet().retainAll((Collection)collection);
   }

   static <E> int setCountImpl(Multiset<E> self, E element, int count) {
      CollectPreconditions.checkNonnegative(count, "count");
      int oldCount = self.count(element);
      int delta = count - oldCount;
      if (delta > 0) {
         self.add(element, delta);
      } else if (delta < 0) {
         self.remove(element, -delta);
      }

      return oldCount;
   }

   static <E> boolean setCountImpl(Multiset<E> self, E element, int oldCount, int newCount) {
      CollectPreconditions.checkNonnegative(oldCount, "oldCount");
      CollectPreconditions.checkNonnegative(newCount, "newCount");
      if (self.count(element) == oldCount) {
         self.setCount(element, newCount);
         return true;
      } else {
         return false;
      }
   }

   static <E> Iterator<E> iteratorImpl(Multiset<E> multiset) {
      return new Multisets.MultisetIteratorImpl(multiset, multiset.entrySet().iterator());
   }

   static int sizeImpl(Multiset<?> multiset) {
      long size = 0L;

      Multiset.Entry entry;
      for(Iterator i$ = multiset.entrySet().iterator(); i$.hasNext(); size += (long)entry.getCount()) {
         entry = (Multiset.Entry)i$.next();
      }

      return Ints.saturatedCast(size);
   }

   static <T> Multiset<T> cast(Iterable<T> iterable) {
      return (Multiset)iterable;
   }

   @Beta
   public static <E> ImmutableMultiset<E> copyHighestCountFirst(Multiset<E> multiset) {
      List<Multiset.Entry<E>> sortedEntries = DECREASING_COUNT_ORDERING.immutableSortedCopy(multiset.entrySet());
      return ImmutableMultiset.copyFromEntries(sortedEntries);
   }

   static final class MultisetIteratorImpl<E> implements Iterator<E> {
      private final Multiset<E> multiset;
      private final Iterator<Multiset.Entry<E>> entryIterator;
      private Multiset.Entry<E> currentEntry;
      private int laterCount;
      private int totalCount;
      private boolean canRemove;

      MultisetIteratorImpl(Multiset<E> multiset, Iterator<Multiset.Entry<E>> entryIterator) {
         this.multiset = multiset;
         this.entryIterator = entryIterator;
      }

      public boolean hasNext() {
         return this.laterCount > 0 || this.entryIterator.hasNext();
      }

      public E next() {
         if (!this.hasNext()) {
            throw new NoSuchElementException();
         } else {
            if (this.laterCount == 0) {
               this.currentEntry = (Multiset.Entry)this.entryIterator.next();
               this.totalCount = this.laterCount = this.currentEntry.getCount();
            }

            --this.laterCount;
            this.canRemove = true;
            return this.currentEntry.getElement();
         }
      }

      public void remove() {
         CollectPreconditions.checkRemove(this.canRemove);
         if (this.totalCount == 1) {
            this.entryIterator.remove();
         } else {
            this.multiset.remove(this.currentEntry.getElement());
         }

         --this.totalCount;
         this.canRemove = false;
      }
   }

   abstract static class EntrySet<E> extends Sets.ImprovedAbstractSet<Multiset.Entry<E>> {
      abstract Multiset<E> multiset();

      public boolean contains(@Nullable Object o) {
         if (o instanceof Multiset.Entry) {
            Multiset.Entry<?> entry = (Multiset.Entry)o;
            if (entry.getCount() <= 0) {
               return false;
            } else {
               int count = this.multiset().count(entry.getElement());
               return count == entry.getCount();
            }
         } else {
            return false;
         }
      }

      public boolean remove(Object object) {
         if (object instanceof Multiset.Entry) {
            Multiset.Entry<?> entry = (Multiset.Entry)object;
            Object element = entry.getElement();
            int entryCount = entry.getCount();
            if (entryCount != 0) {
               Multiset<Object> multiset = this.multiset();
               return multiset.setCount(element, entryCount, 0);
            }
         }

         return false;
      }

      public void clear() {
         this.multiset().clear();
      }
   }

   abstract static class ElementSet<E> extends Sets.ImprovedAbstractSet<E> {
      abstract Multiset<E> multiset();

      public void clear() {
         this.multiset().clear();
      }

      public boolean contains(Object o) {
         return this.multiset().contains(o);
      }

      public boolean containsAll(Collection<?> c) {
         return this.multiset().containsAll(c);
      }

      public boolean isEmpty() {
         return this.multiset().isEmpty();
      }

      public Iterator<E> iterator() {
         return new TransformedIterator<Multiset.Entry<E>, E>(this.multiset().entrySet().iterator()) {
            E transform(Multiset.Entry<E> entry) {
               return entry.getElement();
            }
         };
      }

      public boolean remove(Object o) {
         return this.multiset().remove(o, Integer.MAX_VALUE) > 0;
      }

      public int size() {
         return this.multiset().entrySet().size();
      }
   }

   abstract static class AbstractEntry<E> implements Multiset.Entry<E> {
      public boolean equals(@Nullable Object object) {
         if (!(object instanceof Multiset.Entry)) {
            return false;
         } else {
            Multiset.Entry<?> that = (Multiset.Entry)object;
            return this.getCount() == that.getCount() && Objects.equal(this.getElement(), that.getElement());
         }
      }

      public int hashCode() {
         E e = this.getElement();
         return (e == null ? 0 : e.hashCode()) ^ this.getCount();
      }

      public String toString() {
         String text = String.valueOf(this.getElement());
         int n = this.getCount();
         return n == 1 ? text : text + " x " + n;
      }
   }

   private static final class FilteredMultiset<E> extends AbstractMultiset<E> {
      final Multiset<E> unfiltered;
      final Predicate<? super E> predicate;

      FilteredMultiset(Multiset<E> unfiltered, Predicate<? super E> predicate) {
         this.unfiltered = (Multiset)Preconditions.checkNotNull(unfiltered);
         this.predicate = (Predicate)Preconditions.checkNotNull(predicate);
      }

      public UnmodifiableIterator<E> iterator() {
         return Iterators.filter(this.unfiltered.iterator(), this.predicate);
      }

      Set<E> createElementSet() {
         return Sets.filter(this.unfiltered.elementSet(), this.predicate);
      }

      Set<Multiset.Entry<E>> createEntrySet() {
         return Sets.filter(this.unfiltered.entrySet(), new Predicate<Multiset.Entry<E>>() {
            public boolean apply(Multiset.Entry<E> entry) {
               return FilteredMultiset.this.predicate.apply(entry.getElement());
            }
         });
      }

      Iterator<Multiset.Entry<E>> entryIterator() {
         throw new AssertionError("should never be called");
      }

      int distinctElements() {
         return this.elementSet().size();
      }

      public int count(@Nullable Object element) {
         int count = this.unfiltered.count(element);
         if (count > 0) {
            return this.predicate.apply(element) ? count : 0;
         } else {
            return 0;
         }
      }

      public int add(@Nullable E element, int occurrences) {
         Preconditions.checkArgument(this.predicate.apply(element), "Element %s does not match predicate %s", element, this.predicate);
         return this.unfiltered.add(element, occurrences);
      }

      public int remove(@Nullable Object element, int occurrences) {
         CollectPreconditions.checkNonnegative(occurrences, "occurrences");
         if (occurrences == 0) {
            return this.count(element);
         } else {
            return this.contains(element) ? this.unfiltered.remove(element, occurrences) : 0;
         }
      }

      public void clear() {
         this.elementSet().clear();
      }
   }

   static class ImmutableEntry<E> extends Multisets.AbstractEntry<E> implements Serializable {
      @Nullable
      private final E element;
      private final int count;
      private static final long serialVersionUID = 0L;

      ImmutableEntry(@Nullable E element, int count) {
         this.element = element;
         this.count = count;
         CollectPreconditions.checkNonnegative(count, "count");
      }

      @Nullable
      public final E getElement() {
         return this.element;
      }

      public final int getCount() {
         return this.count;
      }

      public Multisets.ImmutableEntry<E> nextInBucket() {
         return null;
      }
   }

   static class UnmodifiableMultiset<E> extends ForwardingMultiset<E> implements Serializable {
      final Multiset<? extends E> delegate;
      transient Set<E> elementSet;
      transient Set<Multiset.Entry<E>> entrySet;
      private static final long serialVersionUID = 0L;

      UnmodifiableMultiset(Multiset<? extends E> delegate) {
         this.delegate = delegate;
      }

      protected Multiset<E> delegate() {
         return this.delegate;
      }

      Set<E> createElementSet() {
         return Collections.unmodifiableSet(this.delegate.elementSet());
      }

      public Set<E> elementSet() {
         Set<E> es = this.elementSet;
         return es == null ? (this.elementSet = this.createElementSet()) : es;
      }

      public Set<Multiset.Entry<E>> entrySet() {
         Set<Multiset.Entry<E>> es = this.entrySet;
         return es == null ? (this.entrySet = Collections.unmodifiableSet(this.delegate.entrySet())) : es;
      }

      public Iterator<E> iterator() {
         return Iterators.unmodifiableIterator(this.delegate.iterator());
      }

      public boolean add(E element) {
         throw new UnsupportedOperationException();
      }

      public int add(E element, int occurences) {
         throw new UnsupportedOperationException();
      }

      public boolean addAll(Collection<? extends E> elementsToAdd) {
         throw new UnsupportedOperationException();
      }

      public boolean remove(Object element) {
         throw new UnsupportedOperationException();
      }

      public int remove(Object element, int occurrences) {
         throw new UnsupportedOperationException();
      }

      public boolean removeAll(Collection<?> elementsToRemove) {
         throw new UnsupportedOperationException();
      }

      public boolean retainAll(Collection<?> elementsToRetain) {
         throw new UnsupportedOperationException();
      }

      public void clear() {
         throw new UnsupportedOperationException();
      }

      public int setCount(E element, int count) {
         throw new UnsupportedOperationException();
      }

      public boolean setCount(E element, int oldCount, int newCount) {
         throw new UnsupportedOperationException();
      }
   }
}
