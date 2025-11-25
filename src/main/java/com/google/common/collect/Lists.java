package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import com.google.common.primitives.Ints;
import java.io.Serializable;
import java.math.RoundingMode;
import java.util.AbstractList;
import java.util.AbstractSequentialList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@GwtCompatible(
   emulated = true
)
public final class Lists {
   private Lists() {
   }

   @GwtCompatible(
      serializable = true
   )
   public static <E> ArrayList<E> newArrayList() {
      return new ArrayList();
   }

   @GwtCompatible(
      serializable = true
   )
   public static <E> ArrayList<E> newArrayList(E... elements) {
      Preconditions.checkNotNull(elements);
      int capacity = computeArrayListCapacity(elements.length);
      ArrayList<E> list = new ArrayList(capacity);
      Collections.addAll(list, elements);
      return list;
   }

   @VisibleForTesting
   static int computeArrayListCapacity(int arraySize) {
      CollectPreconditions.checkNonnegative(arraySize, "arraySize");
      return Ints.saturatedCast(5L + (long)arraySize + (long)(arraySize / 10));
   }

   @GwtCompatible(
      serializable = true
   )
   public static <E> ArrayList<E> newArrayList(Iterable<? extends E> elements) {
      Preconditions.checkNotNull(elements);
      return elements instanceof Collection ? new ArrayList(Collections2.cast(elements)) : newArrayList(elements.iterator());
   }

   @GwtCompatible(
      serializable = true
   )
   public static <E> ArrayList<E> newArrayList(Iterator<? extends E> elements) {
      ArrayList<E> list = newArrayList();
      Iterators.addAll(list, elements);
      return list;
   }

   @GwtCompatible(
      serializable = true
   )
   public static <E> ArrayList<E> newArrayListWithCapacity(int initialArraySize) {
      CollectPreconditions.checkNonnegative(initialArraySize, "initialArraySize");
      return new ArrayList(initialArraySize);
   }

   @GwtCompatible(
      serializable = true
   )
   public static <E> ArrayList<E> newArrayListWithExpectedSize(int estimatedSize) {
      return new ArrayList(computeArrayListCapacity(estimatedSize));
   }

   @GwtCompatible(
      serializable = true
   )
   public static <E> LinkedList<E> newLinkedList() {
      return new LinkedList();
   }

   @GwtCompatible(
      serializable = true
   )
   public static <E> LinkedList<E> newLinkedList(Iterable<? extends E> elements) {
      LinkedList<E> list = newLinkedList();
      Iterables.addAll(list, elements);
      return list;
   }

   @GwtIncompatible("CopyOnWriteArrayList")
   public static <E> CopyOnWriteArrayList<E> newCopyOnWriteArrayList() {
      return new CopyOnWriteArrayList();
   }

   @GwtIncompatible("CopyOnWriteArrayList")
   public static <E> CopyOnWriteArrayList<E> newCopyOnWriteArrayList(Iterable<? extends E> elements) {
      Collection<? extends E> elementsCollection = elements instanceof Collection ? Collections2.cast(elements) : newArrayList(elements);
      return new CopyOnWriteArrayList((Collection)elementsCollection);
   }

   public static <E> List<E> asList(@Nullable E first, E[] rest) {
      return new Lists.OnePlusArrayList(first, rest);
   }

   public static <E> List<E> asList(@Nullable E first, @Nullable E second, E[] rest) {
      return new Lists.TwoPlusArrayList(first, second, rest);
   }

   public static <B> List<List<B>> cartesianProduct(List<? extends List<? extends B>> lists) {
      return CartesianList.create(lists);
   }

   public static <B> List<List<B>> cartesianProduct(List<? extends B>... lists) {
      return cartesianProduct(Arrays.asList(lists));
   }

   @CheckReturnValue
   public static <F, T> List<T> transform(List<F> fromList, Function<? super F, ? extends T> function) {
      return (List)(fromList instanceof RandomAccess ? new Lists.TransformingRandomAccessList(fromList, function) : new Lists.TransformingSequentialList(fromList, function));
   }

   public static <T> List<List<T>> partition(List<T> list, int size) {
      Preconditions.checkNotNull(list);
      Preconditions.checkArgument(size > 0);
      return (List)(list instanceof RandomAccess ? new Lists.RandomAccessPartition(list, size) : new Lists.Partition(list, size));
   }

   @Beta
   public static ImmutableList<Character> charactersOf(String string) {
      return new Lists.StringAsImmutableList((String)Preconditions.checkNotNull(string));
   }

   @Beta
   public static List<Character> charactersOf(CharSequence sequence) {
      return new Lists.CharSequenceAsList((CharSequence)Preconditions.checkNotNull(sequence));
   }

   @CheckReturnValue
   public static <T> List<T> reverse(List<T> list) {
      if (list instanceof ImmutableList) {
         return ((ImmutableList)list).reverse();
      } else if (list instanceof Lists.ReverseList) {
         return ((Lists.ReverseList)list).getForwardList();
      } else {
         return (List)(list instanceof RandomAccess ? new Lists.RandomAccessReverseList(list) : new Lists.ReverseList(list));
      }
   }

   static int hashCodeImpl(List<?> list) {
      int hashCode = 1;

      for(Iterator i$ = list.iterator(); i$.hasNext(); hashCode = ~(~hashCode)) {
         Object o = i$.next();
         hashCode = 31 * hashCode + (o == null ? 0 : o.hashCode());
      }

      return hashCode;
   }

   static boolean equalsImpl(List<?> thisList, @Nullable Object other) {
      if (other == Preconditions.checkNotNull(thisList)) {
         return true;
      } else if (!(other instanceof List)) {
         return false;
      } else {
         List<?> otherList = (List)other;
         int size = thisList.size();
         if (size != otherList.size()) {
            return false;
         } else if (thisList instanceof RandomAccess && otherList instanceof RandomAccess) {
            for(int i = 0; i < size; ++i) {
               if (!Objects.equal(thisList.get(i), otherList.get(i))) {
                  return false;
               }
            }

            return true;
         } else {
            return Iterators.elementsEqual(thisList.iterator(), otherList.iterator());
         }
      }
   }

   static <E> boolean addAllImpl(List<E> list, int index, Iterable<? extends E> elements) {
      boolean changed = false;
      ListIterator<E> listIterator = list.listIterator(index);

      for(Iterator i$ = elements.iterator(); i$.hasNext(); changed = true) {
         E e = i$.next();
         listIterator.add(e);
      }

      return changed;
   }

   static int indexOfImpl(List<?> list, @Nullable Object element) {
      if (list instanceof RandomAccess) {
         return indexOfRandomAccess(list, element);
      } else {
         ListIterator listIterator = list.listIterator();

         do {
            if (!listIterator.hasNext()) {
               return -1;
            }
         } while(!Objects.equal(element, listIterator.next()));

         return listIterator.previousIndex();
      }
   }

   private static int indexOfRandomAccess(List<?> list, @Nullable Object element) {
      int size = list.size();
      int i;
      if (element == null) {
         for(i = 0; i < size; ++i) {
            if (list.get(i) == null) {
               return i;
            }
         }
      } else {
         for(i = 0; i < size; ++i) {
            if (element.equals(list.get(i))) {
               return i;
            }
         }
      }

      return -1;
   }

   static int lastIndexOfImpl(List<?> list, @Nullable Object element) {
      if (list instanceof RandomAccess) {
         return lastIndexOfRandomAccess(list, element);
      } else {
         ListIterator listIterator = list.listIterator(list.size());

         do {
            if (!listIterator.hasPrevious()) {
               return -1;
            }
         } while(!Objects.equal(element, listIterator.previous()));

         return listIterator.nextIndex();
      }
   }

   private static int lastIndexOfRandomAccess(List<?> list, @Nullable Object element) {
      int i;
      if (element == null) {
         for(i = list.size() - 1; i >= 0; --i) {
            if (list.get(i) == null) {
               return i;
            }
         }
      } else {
         for(i = list.size() - 1; i >= 0; --i) {
            if (element.equals(list.get(i))) {
               return i;
            }
         }
      }

      return -1;
   }

   static <E> ListIterator<E> listIteratorImpl(List<E> list, int index) {
      return (new Lists.AbstractListWrapper(list)).listIterator(index);
   }

   static <E> List<E> subListImpl(List<E> list, int fromIndex, int toIndex) {
      Object wrapper;
      if (list instanceof RandomAccess) {
         wrapper = new Lists.RandomAccessListWrapper<E>(list) {
            private static final long serialVersionUID = 0L;

            public ListIterator<E> listIterator(int index) {
               return this.backingList.listIterator(index);
            }
         };
      } else {
         wrapper = new Lists.AbstractListWrapper<E>(list) {
            private static final long serialVersionUID = 0L;

            public ListIterator<E> listIterator(int index) {
               return this.backingList.listIterator(index);
            }
         };
      }

      return ((List)wrapper).subList(fromIndex, toIndex);
   }

   static <T> List<T> cast(Iterable<T> iterable) {
      return (List)iterable;
   }

   private static class RandomAccessListWrapper<E> extends Lists.AbstractListWrapper<E> implements RandomAccess {
      RandomAccessListWrapper(List<E> backingList) {
         super(backingList);
      }
   }

   private static class AbstractListWrapper<E> extends AbstractList<E> {
      final List<E> backingList;

      AbstractListWrapper(List<E> backingList) {
         this.backingList = (List)Preconditions.checkNotNull(backingList);
      }

      public void add(int index, E element) {
         this.backingList.add(index, element);
      }

      public boolean addAll(int index, Collection<? extends E> c) {
         return this.backingList.addAll(index, c);
      }

      public E get(int index) {
         return this.backingList.get(index);
      }

      public E remove(int index) {
         return this.backingList.remove(index);
      }

      public E set(int index, E element) {
         return this.backingList.set(index, element);
      }

      public boolean contains(Object o) {
         return this.backingList.contains(o);
      }

      public int size() {
         return this.backingList.size();
      }
   }

   private static class RandomAccessReverseList<T> extends Lists.ReverseList<T> implements RandomAccess {
      RandomAccessReverseList(List<T> forwardList) {
         super(forwardList);
      }
   }

   private static class ReverseList<T> extends AbstractList<T> {
      private final List<T> forwardList;

      ReverseList(List<T> forwardList) {
         this.forwardList = (List)Preconditions.checkNotNull(forwardList);
      }

      List<T> getForwardList() {
         return this.forwardList;
      }

      private int reverseIndex(int index) {
         int size = this.size();
         Preconditions.checkElementIndex(index, size);
         return size - 1 - index;
      }

      private int reversePosition(int index) {
         int size = this.size();
         Preconditions.checkPositionIndex(index, size);
         return size - index;
      }

      public void add(int index, @Nullable T element) {
         this.forwardList.add(this.reversePosition(index), element);
      }

      public void clear() {
         this.forwardList.clear();
      }

      public T remove(int index) {
         return this.forwardList.remove(this.reverseIndex(index));
      }

      protected void removeRange(int fromIndex, int toIndex) {
         this.subList(fromIndex, toIndex).clear();
      }

      public T set(int index, @Nullable T element) {
         return this.forwardList.set(this.reverseIndex(index), element);
      }

      public T get(int index) {
         return this.forwardList.get(this.reverseIndex(index));
      }

      public int size() {
         return this.forwardList.size();
      }

      public List<T> subList(int fromIndex, int toIndex) {
         Preconditions.checkPositionIndexes(fromIndex, toIndex, this.size());
         return Lists.reverse(this.forwardList.subList(this.reversePosition(toIndex), this.reversePosition(fromIndex)));
      }

      public Iterator<T> iterator() {
         return this.listIterator();
      }

      public ListIterator<T> listIterator(int index) {
         int start = this.reversePosition(index);
         final ListIterator<T> forwardIterator = this.forwardList.listIterator(start);
         return new ListIterator<T>() {
            boolean canRemoveOrSet;

            public void add(T e) {
               forwardIterator.add(e);
               forwardIterator.previous();
               this.canRemoveOrSet = false;
            }

            public boolean hasNext() {
               return forwardIterator.hasPrevious();
            }

            public boolean hasPrevious() {
               return forwardIterator.hasNext();
            }

            public T next() {
               if (!this.hasNext()) {
                  throw new NoSuchElementException();
               } else {
                  this.canRemoveOrSet = true;
                  return forwardIterator.previous();
               }
            }

            public int nextIndex() {
               return ReverseList.this.reversePosition(forwardIterator.nextIndex());
            }

            public T previous() {
               if (!this.hasPrevious()) {
                  throw new NoSuchElementException();
               } else {
                  this.canRemoveOrSet = true;
                  return forwardIterator.next();
               }
            }

            public int previousIndex() {
               return this.nextIndex() - 1;
            }

            public void remove() {
               CollectPreconditions.checkRemove(this.canRemoveOrSet);
               forwardIterator.remove();
               this.canRemoveOrSet = false;
            }

            public void set(T e) {
               Preconditions.checkState(this.canRemoveOrSet);
               forwardIterator.set(e);
            }
         };
      }
   }

   private static final class CharSequenceAsList extends AbstractList<Character> {
      private final CharSequence sequence;

      CharSequenceAsList(CharSequence sequence) {
         this.sequence = sequence;
      }

      public Character get(int index) {
         Preconditions.checkElementIndex(index, this.size());
         return this.sequence.charAt(index);
      }

      public int size() {
         return this.sequence.length();
      }
   }

   private static final class StringAsImmutableList extends ImmutableList<Character> {
      private final String string;

      StringAsImmutableList(String string) {
         this.string = string;
      }

      public int indexOf(@Nullable Object object) {
         return object instanceof Character ? this.string.indexOf((Character)object) : -1;
      }

      public int lastIndexOf(@Nullable Object object) {
         return object instanceof Character ? this.string.lastIndexOf((Character)object) : -1;
      }

      public ImmutableList<Character> subList(int fromIndex, int toIndex) {
         Preconditions.checkPositionIndexes(fromIndex, toIndex, this.size());
         return Lists.charactersOf(this.string.substring(fromIndex, toIndex));
      }

      boolean isPartialView() {
         return false;
      }

      public Character get(int index) {
         Preconditions.checkElementIndex(index, this.size());
         return this.string.charAt(index);
      }

      public int size() {
         return this.string.length();
      }
   }

   private static class RandomAccessPartition<T> extends Lists.Partition<T> implements RandomAccess {
      RandomAccessPartition(List<T> list, int size) {
         super(list, size);
      }
   }

   private static class Partition<T> extends AbstractList<List<T>> {
      final List<T> list;
      final int size;

      Partition(List<T> list, int size) {
         this.list = list;
         this.size = size;
      }

      public List<T> get(int index) {
         Preconditions.checkElementIndex(index, this.size());
         int start = index * this.size;
         int end = Math.min(start + this.size, this.list.size());
         return this.list.subList(start, end);
      }

      public int size() {
         return IntMath.divide(this.list.size(), this.size, RoundingMode.CEILING);
      }

      public boolean isEmpty() {
         return this.list.isEmpty();
      }
   }

   private static class TransformingRandomAccessList<F, T> extends AbstractList<T> implements RandomAccess, Serializable {
      final List<F> fromList;
      final Function<? super F, ? extends T> function;
      private static final long serialVersionUID = 0L;

      TransformingRandomAccessList(List<F> fromList, Function<? super F, ? extends T> function) {
         this.fromList = (List)Preconditions.checkNotNull(fromList);
         this.function = (Function)Preconditions.checkNotNull(function);
      }

      public void clear() {
         this.fromList.clear();
      }

      public T get(int index) {
         return this.function.apply(this.fromList.get(index));
      }

      public Iterator<T> iterator() {
         return this.listIterator();
      }

      public ListIterator<T> listIterator(int index) {
         return new TransformedListIterator<F, T>(this.fromList.listIterator(index)) {
            T transform(F from) {
               return TransformingRandomAccessList.this.function.apply(from);
            }
         };
      }

      public boolean isEmpty() {
         return this.fromList.isEmpty();
      }

      public T remove(int index) {
         return this.function.apply(this.fromList.remove(index));
      }

      public int size() {
         return this.fromList.size();
      }
   }

   private static class TransformingSequentialList<F, T> extends AbstractSequentialList<T> implements Serializable {
      final List<F> fromList;
      final Function<? super F, ? extends T> function;
      private static final long serialVersionUID = 0L;

      TransformingSequentialList(List<F> fromList, Function<? super F, ? extends T> function) {
         this.fromList = (List)Preconditions.checkNotNull(fromList);
         this.function = (Function)Preconditions.checkNotNull(function);
      }

      public void clear() {
         this.fromList.clear();
      }

      public int size() {
         return this.fromList.size();
      }

      public ListIterator<T> listIterator(int index) {
         return new TransformedListIterator<F, T>(this.fromList.listIterator(index)) {
            T transform(F from) {
               return TransformingSequentialList.this.function.apply(from);
            }
         };
      }
   }

   private static class TwoPlusArrayList<E> extends AbstractList<E> implements Serializable, RandomAccess {
      final E first;
      final E second;
      final E[] rest;
      private static final long serialVersionUID = 0L;

      TwoPlusArrayList(@Nullable E first, @Nullable E second, E[] rest) {
         this.first = first;
         this.second = second;
         this.rest = (Object[])Preconditions.checkNotNull(rest);
      }

      public int size() {
         return this.rest.length + 2;
      }

      public E get(int index) {
         switch(index) {
         case 0:
            return this.first;
         case 1:
            return this.second;
         default:
            Preconditions.checkElementIndex(index, this.size());
            return this.rest[index - 2];
         }
      }
   }

   private static class OnePlusArrayList<E> extends AbstractList<E> implements Serializable, RandomAccess {
      final E first;
      final E[] rest;
      private static final long serialVersionUID = 0L;

      OnePlusArrayList(@Nullable E first, E[] rest) {
         this.first = first;
         this.rest = (Object[])Preconditions.checkNotNull(rest);
      }

      public int size() {
         return this.rest.length + 1;
      }

      public E get(int index) {
         Preconditions.checkElementIndex(index, this.size());
         return index == 0 ? this.first : this.rest[index - 1];
      }
   }
}
