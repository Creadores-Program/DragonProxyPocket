package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;

@GwtCompatible
final class Constraints {
   private Constraints() {
   }

   public static <E> Collection<E> constrainedCollection(Collection<E> collection, Constraint<? super E> constraint) {
      return new Constraints.ConstrainedCollection(collection, constraint);
   }

   public static <E> Set<E> constrainedSet(Set<E> set, Constraint<? super E> constraint) {
      return new Constraints.ConstrainedSet(set, constraint);
   }

   public static <E> SortedSet<E> constrainedSortedSet(SortedSet<E> sortedSet, Constraint<? super E> constraint) {
      return new Constraints.ConstrainedSortedSet(sortedSet, constraint);
   }

   public static <E> List<E> constrainedList(List<E> list, Constraint<? super E> constraint) {
      return (List)(list instanceof RandomAccess ? new Constraints.ConstrainedRandomAccessList(list, constraint) : new Constraints.ConstrainedList(list, constraint));
   }

   private static <E> ListIterator<E> constrainedListIterator(ListIterator<E> listIterator, Constraint<? super E> constraint) {
      return new Constraints.ConstrainedListIterator(listIterator, constraint);
   }

   static <E> Collection<E> constrainedTypePreservingCollection(Collection<E> collection, Constraint<E> constraint) {
      if (collection instanceof SortedSet) {
         return constrainedSortedSet((SortedSet)collection, constraint);
      } else if (collection instanceof Set) {
         return constrainedSet((Set)collection, constraint);
      } else {
         return (Collection)(collection instanceof List ? constrainedList((List)collection, constraint) : constrainedCollection(collection, constraint));
      }
   }

   private static <E> Collection<E> checkElements(Collection<E> elements, Constraint<? super E> constraint) {
      Collection<E> copy = Lists.newArrayList((Iterable)elements);
      Iterator i$ = copy.iterator();

      while(i$.hasNext()) {
         E element = i$.next();
         constraint.checkElement(element);
      }

      return copy;
   }

   static class ConstrainedListIterator<E> extends ForwardingListIterator<E> {
      private final ListIterator<E> delegate;
      private final Constraint<? super E> constraint;

      public ConstrainedListIterator(ListIterator<E> delegate, Constraint<? super E> constraint) {
         this.delegate = delegate;
         this.constraint = constraint;
      }

      protected ListIterator<E> delegate() {
         return this.delegate;
      }

      public void add(E element) {
         this.constraint.checkElement(element);
         this.delegate.add(element);
      }

      public void set(E element) {
         this.constraint.checkElement(element);
         this.delegate.set(element);
      }
   }

   static class ConstrainedRandomAccessList<E> extends Constraints.ConstrainedList<E> implements RandomAccess {
      ConstrainedRandomAccessList(List<E> delegate, Constraint<? super E> constraint) {
         super(delegate, constraint);
      }
   }

   @GwtCompatible
   private static class ConstrainedList<E> extends ForwardingList<E> {
      final List<E> delegate;
      final Constraint<? super E> constraint;

      ConstrainedList(List<E> delegate, Constraint<? super E> constraint) {
         this.delegate = (List)Preconditions.checkNotNull(delegate);
         this.constraint = (Constraint)Preconditions.checkNotNull(constraint);
      }

      protected List<E> delegate() {
         return this.delegate;
      }

      public boolean add(E element) {
         this.constraint.checkElement(element);
         return this.delegate.add(element);
      }

      public void add(int index, E element) {
         this.constraint.checkElement(element);
         this.delegate.add(index, element);
      }

      public boolean addAll(Collection<? extends E> elements) {
         return this.delegate.addAll(Constraints.checkElements(elements, this.constraint));
      }

      public boolean addAll(int index, Collection<? extends E> elements) {
         return this.delegate.addAll(index, Constraints.checkElements(elements, this.constraint));
      }

      public ListIterator<E> listIterator() {
         return Constraints.constrainedListIterator(this.delegate.listIterator(), this.constraint);
      }

      public ListIterator<E> listIterator(int index) {
         return Constraints.constrainedListIterator(this.delegate.listIterator(index), this.constraint);
      }

      public E set(int index, E element) {
         this.constraint.checkElement(element);
         return this.delegate.set(index, element);
      }

      public List<E> subList(int fromIndex, int toIndex) {
         return Constraints.constrainedList(this.delegate.subList(fromIndex, toIndex), this.constraint);
      }
   }

   private static class ConstrainedSortedSet<E> extends ForwardingSortedSet<E> {
      final SortedSet<E> delegate;
      final Constraint<? super E> constraint;

      ConstrainedSortedSet(SortedSet<E> delegate, Constraint<? super E> constraint) {
         this.delegate = (SortedSet)Preconditions.checkNotNull(delegate);
         this.constraint = (Constraint)Preconditions.checkNotNull(constraint);
      }

      protected SortedSet<E> delegate() {
         return this.delegate;
      }

      public SortedSet<E> headSet(E toElement) {
         return Constraints.constrainedSortedSet(this.delegate.headSet(toElement), this.constraint);
      }

      public SortedSet<E> subSet(E fromElement, E toElement) {
         return Constraints.constrainedSortedSet(this.delegate.subSet(fromElement, toElement), this.constraint);
      }

      public SortedSet<E> tailSet(E fromElement) {
         return Constraints.constrainedSortedSet(this.delegate.tailSet(fromElement), this.constraint);
      }

      public boolean add(E element) {
         this.constraint.checkElement(element);
         return this.delegate.add(element);
      }

      public boolean addAll(Collection<? extends E> elements) {
         return this.delegate.addAll(Constraints.checkElements(elements, this.constraint));
      }
   }

   static class ConstrainedSet<E> extends ForwardingSet<E> {
      private final Set<E> delegate;
      private final Constraint<? super E> constraint;

      public ConstrainedSet(Set<E> delegate, Constraint<? super E> constraint) {
         this.delegate = (Set)Preconditions.checkNotNull(delegate);
         this.constraint = (Constraint)Preconditions.checkNotNull(constraint);
      }

      protected Set<E> delegate() {
         return this.delegate;
      }

      public boolean add(E element) {
         this.constraint.checkElement(element);
         return this.delegate.add(element);
      }

      public boolean addAll(Collection<? extends E> elements) {
         return this.delegate.addAll(Constraints.checkElements(elements, this.constraint));
      }
   }

   static class ConstrainedCollection<E> extends ForwardingCollection<E> {
      private final Collection<E> delegate;
      private final Constraint<? super E> constraint;

      public ConstrainedCollection(Collection<E> delegate, Constraint<? super E> constraint) {
         this.delegate = (Collection)Preconditions.checkNotNull(delegate);
         this.constraint = (Constraint)Preconditions.checkNotNull(constraint);
      }

      protected Collection<E> delegate() {
         return this.delegate;
      }

      public boolean add(E element) {
         this.constraint.checkElement(element);
         return this.delegate.add(element);
      }

      public boolean addAll(Collection<? extends E> elements) {
         return this.delegate.addAll(Constraints.checkElements(elements, this.constraint));
      }
   }
}
