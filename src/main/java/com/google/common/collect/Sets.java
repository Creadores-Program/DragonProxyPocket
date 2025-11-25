package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@GwtCompatible(
   emulated = true
)
public final class Sets {
   private Sets() {
   }

   @GwtCompatible(
      serializable = true
   )
   public static <E extends Enum<E>> ImmutableSet<E> immutableEnumSet(E anElement, E... otherElements) {
      return ImmutableEnumSet.asImmutable(EnumSet.of(anElement, otherElements));
   }

   @GwtCompatible(
      serializable = true
   )
   public static <E extends Enum<E>> ImmutableSet<E> immutableEnumSet(Iterable<E> elements) {
      if (elements instanceof ImmutableEnumSet) {
         return (ImmutableEnumSet)elements;
      } else if (elements instanceof Collection) {
         Collection<E> collection = (Collection)elements;
         return collection.isEmpty() ? ImmutableSet.of() : ImmutableEnumSet.asImmutable(EnumSet.copyOf(collection));
      } else {
         Iterator<E> itr = elements.iterator();
         if (itr.hasNext()) {
            EnumSet<E> enumSet = EnumSet.of((Enum)itr.next());
            Iterators.addAll(enumSet, itr);
            return ImmutableEnumSet.asImmutable(enumSet);
         } else {
            return ImmutableSet.of();
         }
      }
   }

   public static <E extends Enum<E>> EnumSet<E> newEnumSet(Iterable<E> iterable, Class<E> elementType) {
      EnumSet<E> set = EnumSet.noneOf(elementType);
      Iterables.addAll(set, iterable);
      return set;
   }

   public static <E> HashSet<E> newHashSet() {
      return new HashSet();
   }

   public static <E> HashSet<E> newHashSet(E... elements) {
      HashSet<E> set = newHashSetWithExpectedSize(elements.length);
      Collections.addAll(set, elements);
      return set;
   }

   public static <E> HashSet<E> newHashSetWithExpectedSize(int expectedSize) {
      return new HashSet(Maps.capacity(expectedSize));
   }

   public static <E> HashSet<E> newHashSet(Iterable<? extends E> elements) {
      return elements instanceof Collection ? new HashSet(Collections2.cast(elements)) : newHashSet(elements.iterator());
   }

   public static <E> HashSet<E> newHashSet(Iterator<? extends E> elements) {
      HashSet<E> set = newHashSet();
      Iterators.addAll(set, elements);
      return set;
   }

   public static <E> Set<E> newConcurrentHashSet() {
      return newSetFromMap(new ConcurrentHashMap());
   }

   public static <E> Set<E> newConcurrentHashSet(Iterable<? extends E> elements) {
      Set<E> set = newConcurrentHashSet();
      Iterables.addAll(set, elements);
      return set;
   }

   public static <E> LinkedHashSet<E> newLinkedHashSet() {
      return new LinkedHashSet();
   }

   public static <E> LinkedHashSet<E> newLinkedHashSetWithExpectedSize(int expectedSize) {
      return new LinkedHashSet(Maps.capacity(expectedSize));
   }

   public static <E> LinkedHashSet<E> newLinkedHashSet(Iterable<? extends E> elements) {
      if (elements instanceof Collection) {
         return new LinkedHashSet(Collections2.cast(elements));
      } else {
         LinkedHashSet<E> set = newLinkedHashSet();
         Iterables.addAll(set, elements);
         return set;
      }
   }

   public static <E extends Comparable> TreeSet<E> newTreeSet() {
      return new TreeSet();
   }

   public static <E extends Comparable> TreeSet<E> newTreeSet(Iterable<? extends E> elements) {
      TreeSet<E> set = newTreeSet();
      Iterables.addAll(set, elements);
      return set;
   }

   public static <E> TreeSet<E> newTreeSet(Comparator<? super E> comparator) {
      return new TreeSet((Comparator)Preconditions.checkNotNull(comparator));
   }

   public static <E> Set<E> newIdentityHashSet() {
      return newSetFromMap(Maps.newIdentityHashMap());
   }

   @GwtIncompatible("CopyOnWriteArraySet")
   public static <E> CopyOnWriteArraySet<E> newCopyOnWriteArraySet() {
      return new CopyOnWriteArraySet();
   }

   @GwtIncompatible("CopyOnWriteArraySet")
   public static <E> CopyOnWriteArraySet<E> newCopyOnWriteArraySet(Iterable<? extends E> elements) {
      Collection<? extends E> elementsCollection = elements instanceof Collection ? Collections2.cast(elements) : Lists.newArrayList(elements);
      return new CopyOnWriteArraySet((Collection)elementsCollection);
   }

   public static <E extends Enum<E>> EnumSet<E> complementOf(Collection<E> collection) {
      if (collection instanceof EnumSet) {
         return EnumSet.complementOf((EnumSet)collection);
      } else {
         Preconditions.checkArgument(!collection.isEmpty(), "collection is empty; use the other version of this method");
         Class<E> type = ((Enum)collection.iterator().next()).getDeclaringClass();
         return makeComplementByHand(collection, type);
      }
   }

   public static <E extends Enum<E>> EnumSet<E> complementOf(Collection<E> collection, Class<E> type) {
      Preconditions.checkNotNull(collection);
      return collection instanceof EnumSet ? EnumSet.complementOf((EnumSet)collection) : makeComplementByHand(collection, type);
   }

   private static <E extends Enum<E>> EnumSet<E> makeComplementByHand(Collection<E> collection, Class<E> type) {
      EnumSet<E> result = EnumSet.allOf(type);
      result.removeAll(collection);
      return result;
   }

   /** @deprecated */
   @Deprecated
   public static <E> Set<E> newSetFromMap(Map<E, Boolean> map) {
      return Platform.newSetFromMap(map);
   }

   public static <E> Sets.SetView<E> union(final Set<? extends E> set1, final Set<? extends E> set2) {
      Preconditions.checkNotNull(set1, "set1");
      Preconditions.checkNotNull(set2, "set2");
      final Set<? extends E> set2minus1 = difference(set2, set1);
      return new Sets.SetView<E>() {
         public int size() {
            return set1.size() + set2minus1.size();
         }

         public boolean isEmpty() {
            return set1.isEmpty() && set2.isEmpty();
         }

         public Iterator<E> iterator() {
            return Iterators.unmodifiableIterator(Iterators.concat(set1.iterator(), set2minus1.iterator()));
         }

         public boolean contains(Object object) {
            return set1.contains(object) || set2.contains(object);
         }

         public <S extends Set<E>> S copyInto(S set) {
            set.addAll(set1);
            set.addAll(set2);
            return set;
         }

         public ImmutableSet<E> immutableCopy() {
            return (new ImmutableSet.Builder()).addAll((Iterable)set1).addAll((Iterable)set2).build();
         }
      };
   }

   public static <E> Sets.SetView<E> intersection(final Set<E> set1, final Set<?> set2) {
      Preconditions.checkNotNull(set1, "set1");
      Preconditions.checkNotNull(set2, "set2");
      final Predicate<Object> inSet2 = Predicates.in(set2);
      return new Sets.SetView<E>() {
         public Iterator<E> iterator() {
            return Iterators.filter(set1.iterator(), inSet2);
         }

         public int size() {
            return Iterators.size(this.iterator());
         }

         public boolean isEmpty() {
            return !this.iterator().hasNext();
         }

         public boolean contains(Object object) {
            return set1.contains(object) && set2.contains(object);
         }

         public boolean containsAll(Collection<?> collection) {
            return set1.containsAll(collection) && set2.containsAll(collection);
         }
      };
   }

   public static <E> Sets.SetView<E> difference(final Set<E> set1, final Set<?> set2) {
      Preconditions.checkNotNull(set1, "set1");
      Preconditions.checkNotNull(set2, "set2");
      final Predicate<Object> notInSet2 = Predicates.not(Predicates.in(set2));
      return new Sets.SetView<E>() {
         public Iterator<E> iterator() {
            return Iterators.filter(set1.iterator(), notInSet2);
         }

         public int size() {
            return Iterators.size(this.iterator());
         }

         public boolean isEmpty() {
            return set2.containsAll(set1);
         }

         public boolean contains(Object element) {
            return set1.contains(element) && !set2.contains(element);
         }
      };
   }

   public static <E> Sets.SetView<E> symmetricDifference(final Set<? extends E> set1, final Set<? extends E> set2) {
      Preconditions.checkNotNull(set1, "set1");
      Preconditions.checkNotNull(set2, "set2");
      return new Sets.SetView<E>() {
         public Iterator<E> iterator() {
            final Iterator<? extends E> itr1 = set1.iterator();
            final Iterator<? extends E> itr2 = set2.iterator();
            return new AbstractIterator<E>() {
               public E computeNext() {
                  while(true) {
                     Object elem2;
                     if (itr1.hasNext()) {
                        elem2 = itr1.next();
                        if (set2.contains(elem2)) {
                           continue;
                        }

                        return elem2;
                     }

                     do {
                        if (!itr2.hasNext()) {
                           return this.endOfData();
                        }

                        elem2 = itr2.next();
                     } while(set1.contains(elem2));

                     return elem2;
                  }
               }
            };
         }

         public int size() {
            return Iterators.size(this.iterator());
         }

         public boolean isEmpty() {
            return set1.equals(set2);
         }

         public boolean contains(Object element) {
            return set1.contains(element) ^ set2.contains(element);
         }
      };
   }

   @CheckReturnValue
   public static <E> Set<E> filter(Set<E> unfiltered, Predicate<? super E> predicate) {
      if (unfiltered instanceof SortedSet) {
         return filter((SortedSet)unfiltered, predicate);
      } else if (unfiltered instanceof Sets.FilteredSet) {
         Sets.FilteredSet<E> filtered = (Sets.FilteredSet)unfiltered;
         Predicate<E> combinedPredicate = Predicates.and(filtered.predicate, predicate);
         return new Sets.FilteredSet((Set)filtered.unfiltered, combinedPredicate);
      } else {
         return new Sets.FilteredSet((Set)Preconditions.checkNotNull(unfiltered), (Predicate)Preconditions.checkNotNull(predicate));
      }
   }

   @CheckReturnValue
   public static <E> SortedSet<E> filter(SortedSet<E> unfiltered, Predicate<? super E> predicate) {
      return Platform.setsFilterSortedSet(unfiltered, predicate);
   }

   static <E> SortedSet<E> filterSortedIgnoreNavigable(SortedSet<E> unfiltered, Predicate<? super E> predicate) {
      if (unfiltered instanceof Sets.FilteredSet) {
         Sets.FilteredSet<E> filtered = (Sets.FilteredSet)unfiltered;
         Predicate<E> combinedPredicate = Predicates.and(filtered.predicate, predicate);
         return new Sets.FilteredSortedSet((SortedSet)filtered.unfiltered, combinedPredicate);
      } else {
         return new Sets.FilteredSortedSet((SortedSet)Preconditions.checkNotNull(unfiltered), (Predicate)Preconditions.checkNotNull(predicate));
      }
   }

   @CheckReturnValue
   @GwtIncompatible("NavigableSet")
   public static <E> NavigableSet<E> filter(NavigableSet<E> unfiltered, Predicate<? super E> predicate) {
      if (unfiltered instanceof Sets.FilteredSet) {
         Sets.FilteredSet<E> filtered = (Sets.FilteredSet)unfiltered;
         Predicate<E> combinedPredicate = Predicates.and(filtered.predicate, predicate);
         return new Sets.FilteredNavigableSet((NavigableSet)filtered.unfiltered, combinedPredicate);
      } else {
         return new Sets.FilteredNavigableSet((NavigableSet)Preconditions.checkNotNull(unfiltered), (Predicate)Preconditions.checkNotNull(predicate));
      }
   }

   public static <B> Set<List<B>> cartesianProduct(List<? extends Set<? extends B>> sets) {
      return Sets.CartesianSet.create(sets);
   }

   public static <B> Set<List<B>> cartesianProduct(Set<? extends B>... sets) {
      return cartesianProduct(Arrays.asList(sets));
   }

   @GwtCompatible(
      serializable = false
   )
   public static <E> Set<Set<E>> powerSet(Set<E> set) {
      return new Sets.PowerSet(set);
   }

   static int hashCodeImpl(Set<?> s) {
      int hashCode = 0;

      for(Iterator i$ = s.iterator(); i$.hasNext(); hashCode = ~(~hashCode)) {
         Object o = i$.next();
         hashCode += o != null ? o.hashCode() : 0;
      }

      return hashCode;
   }

   static boolean equalsImpl(Set<?> s, @Nullable Object object) {
      if (s == object) {
         return true;
      } else if (object instanceof Set) {
         Set o = (Set)object;

         try {
            return s.size() == o.size() && s.containsAll(o);
         } catch (NullPointerException var4) {
            return false;
         } catch (ClassCastException var5) {
            return false;
         }
      } else {
         return false;
      }
   }

   @GwtIncompatible("NavigableSet")
   public static <E> NavigableSet<E> unmodifiableNavigableSet(NavigableSet<E> set) {
      return (NavigableSet)(!(set instanceof ImmutableSortedSet) && !(set instanceof Sets.UnmodifiableNavigableSet) ? new Sets.UnmodifiableNavigableSet(set) : set);
   }

   @GwtIncompatible("NavigableSet")
   public static <E> NavigableSet<E> synchronizedNavigableSet(NavigableSet<E> navigableSet) {
      return Synchronized.navigableSet(navigableSet);
   }

   static boolean removeAllImpl(Set<?> set, Iterator<?> iterator) {
      boolean changed;
      for(changed = false; iterator.hasNext(); changed |= set.remove(iterator.next())) {
      }

      return changed;
   }

   static boolean removeAllImpl(Set<?> set, Collection<?> collection) {
      Preconditions.checkNotNull(collection);
      if (collection instanceof Multiset) {
         collection = ((Multiset)collection).elementSet();
      }

      return collection instanceof Set && ((Collection)collection).size() > set.size() ? Iterators.removeAll(set.iterator(), (Collection)collection) : removeAllImpl(set, ((Collection)collection).iterator());
   }

   @GwtIncompatible("NavigableSet")
   static class DescendingSet<E> extends ForwardingNavigableSet<E> {
      private final NavigableSet<E> forward;

      DescendingSet(NavigableSet<E> forward) {
         this.forward = forward;
      }

      protected NavigableSet<E> delegate() {
         return this.forward;
      }

      public E lower(E e) {
         return this.forward.higher(e);
      }

      public E floor(E e) {
         return this.forward.ceiling(e);
      }

      public E ceiling(E e) {
         return this.forward.floor(e);
      }

      public E higher(E e) {
         return this.forward.lower(e);
      }

      public E pollFirst() {
         return this.forward.pollLast();
      }

      public E pollLast() {
         return this.forward.pollFirst();
      }

      public NavigableSet<E> descendingSet() {
         return this.forward;
      }

      public Iterator<E> descendingIterator() {
         return this.forward.iterator();
      }

      public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
         return this.forward.subSet(toElement, toInclusive, fromElement, fromInclusive).descendingSet();
      }

      public NavigableSet<E> headSet(E toElement, boolean inclusive) {
         return this.forward.tailSet(toElement, inclusive).descendingSet();
      }

      public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
         return this.forward.headSet(fromElement, inclusive).descendingSet();
      }

      public Comparator<? super E> comparator() {
         Comparator<? super E> forwardComparator = this.forward.comparator();
         return forwardComparator == null ? Ordering.natural().reverse() : reverse(forwardComparator);
      }

      private static <T> Ordering<T> reverse(Comparator<T> forward) {
         return Ordering.from(forward).reverse();
      }

      public E first() {
         return this.forward.last();
      }

      public SortedSet<E> headSet(E toElement) {
         return this.standardHeadSet(toElement);
      }

      public E last() {
         return this.forward.first();
      }

      public SortedSet<E> subSet(E fromElement, E toElement) {
         return this.standardSubSet(fromElement, toElement);
      }

      public SortedSet<E> tailSet(E fromElement) {
         return this.standardTailSet(fromElement);
      }

      public Iterator<E> iterator() {
         return this.forward.descendingIterator();
      }

      public Object[] toArray() {
         return this.standardToArray();
      }

      public <T> T[] toArray(T[] array) {
         return this.standardToArray(array);
      }

      public String toString() {
         return this.standardToString();
      }
   }

   @GwtIncompatible("NavigableSet")
   static final class UnmodifiableNavigableSet<E> extends ForwardingSortedSet<E> implements NavigableSet<E>, Serializable {
      private final NavigableSet<E> delegate;
      private transient Sets.UnmodifiableNavigableSet<E> descendingSet;
      private static final long serialVersionUID = 0L;

      UnmodifiableNavigableSet(NavigableSet<E> delegate) {
         this.delegate = (NavigableSet)Preconditions.checkNotNull(delegate);
      }

      protected SortedSet<E> delegate() {
         return Collections.unmodifiableSortedSet(this.delegate);
      }

      public E lower(E e) {
         return this.delegate.lower(e);
      }

      public E floor(E e) {
         return this.delegate.floor(e);
      }

      public E ceiling(E e) {
         return this.delegate.ceiling(e);
      }

      public E higher(E e) {
         return this.delegate.higher(e);
      }

      public E pollFirst() {
         throw new UnsupportedOperationException();
      }

      public E pollLast() {
         throw new UnsupportedOperationException();
      }

      public NavigableSet<E> descendingSet() {
         Sets.UnmodifiableNavigableSet<E> result = this.descendingSet;
         if (result == null) {
            result = this.descendingSet = new Sets.UnmodifiableNavigableSet(this.delegate.descendingSet());
            result.descendingSet = this;
         }

         return result;
      }

      public Iterator<E> descendingIterator() {
         return Iterators.unmodifiableIterator(this.delegate.descendingIterator());
      }

      public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
         return Sets.unmodifiableNavigableSet(this.delegate.subSet(fromElement, fromInclusive, toElement, toInclusive));
      }

      public NavigableSet<E> headSet(E toElement, boolean inclusive) {
         return Sets.unmodifiableNavigableSet(this.delegate.headSet(toElement, inclusive));
      }

      public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
         return Sets.unmodifiableNavigableSet(this.delegate.tailSet(fromElement, inclusive));
      }
   }

   private static final class PowerSet<E> extends AbstractSet<Set<E>> {
      final ImmutableMap<E, Integer> inputSet;

      PowerSet(Set<E> input) {
         this.inputSet = Maps.indexMap(input);
         Preconditions.checkArgument(this.inputSet.size() <= 30, "Too many elements to create power set: %s > 30", this.inputSet.size());
      }

      public int size() {
         return 1 << this.inputSet.size();
      }

      public boolean isEmpty() {
         return false;
      }

      public Iterator<Set<E>> iterator() {
         return new AbstractIndexedListIterator<Set<E>>(this.size()) {
            protected Set<E> get(int setBits) {
               return new Sets.SubSet(PowerSet.this.inputSet, setBits);
            }
         };
      }

      public boolean contains(@Nullable Object obj) {
         if (obj instanceof Set) {
            Set<?> set = (Set)obj;
            return this.inputSet.keySet().containsAll(set);
         } else {
            return false;
         }
      }

      public boolean equals(@Nullable Object obj) {
         if (obj instanceof Sets.PowerSet) {
            Sets.PowerSet<?> that = (Sets.PowerSet)obj;
            return this.inputSet.equals(that.inputSet);
         } else {
            return super.equals(obj);
         }
      }

      public int hashCode() {
         return this.inputSet.keySet().hashCode() << this.inputSet.size() - 1;
      }

      public String toString() {
         return "powerSet(" + this.inputSet + ")";
      }
   }

   private static final class SubSet<E> extends AbstractSet<E> {
      private final ImmutableMap<E, Integer> inputSet;
      private final int mask;

      SubSet(ImmutableMap<E, Integer> inputSet, int mask) {
         this.inputSet = inputSet;
         this.mask = mask;
      }

      public Iterator<E> iterator() {
         return new UnmodifiableIterator<E>() {
            final ImmutableList<E> elements;
            int remainingSetBits;

            {
               this.elements = SubSet.this.inputSet.keySet().asList();
               this.remainingSetBits = SubSet.this.mask;
            }

            public boolean hasNext() {
               return this.remainingSetBits != 0;
            }

            public E next() {
               int index = Integer.numberOfTrailingZeros(this.remainingSetBits);
               if (index == 32) {
                  throw new NoSuchElementException();
               } else {
                  this.remainingSetBits &= ~(1 << index);
                  return this.elements.get(index);
               }
            }
         };
      }

      public int size() {
         return Integer.bitCount(this.mask);
      }

      public boolean contains(@Nullable Object o) {
         Integer index = (Integer)this.inputSet.get(o);
         return index != null && (this.mask & 1 << index) != 0;
      }
   }

   private static final class CartesianSet<E> extends ForwardingCollection<List<E>> implements Set<List<E>> {
      private final transient ImmutableList<ImmutableSet<E>> axes;
      private final transient CartesianList<E> delegate;

      static <E> Set<List<E>> create(List<? extends Set<? extends E>> sets) {
         ImmutableList.Builder<ImmutableSet<E>> axesBuilder = new ImmutableList.Builder(sets.size());
         Iterator i$ = sets.iterator();

         while(i$.hasNext()) {
            Set<? extends E> set = (Set)i$.next();
            ImmutableSet<E> copy = ImmutableSet.copyOf((Collection)set);
            if (copy.isEmpty()) {
               return ImmutableSet.of();
            }

            axesBuilder.add((Object)copy);
         }

         final ImmutableList<ImmutableSet<E>> axes = axesBuilder.build();
         ImmutableList<List<E>> listAxes = new ImmutableList<List<E>>() {
            public int size() {
               return axes.size();
            }

            public List<E> get(int index) {
               return ((ImmutableSet)axes.get(index)).asList();
            }

            boolean isPartialView() {
               return true;
            }
         };
         return new Sets.CartesianSet(axes, new CartesianList(listAxes));
      }

      private CartesianSet(ImmutableList<ImmutableSet<E>> axes, CartesianList<E> delegate) {
         this.axes = axes;
         this.delegate = delegate;
      }

      protected Collection<List<E>> delegate() {
         return this.delegate;
      }

      public boolean equals(@Nullable Object object) {
         if (object instanceof Sets.CartesianSet) {
            Sets.CartesianSet<?> that = (Sets.CartesianSet)object;
            return this.axes.equals(that.axes);
         } else {
            return super.equals(object);
         }
      }

      public int hashCode() {
         int adjust = this.size() - 1;

         int hash;
         for(hash = 0; hash < this.axes.size(); ++hash) {
            adjust *= 31;
            adjust = ~(~adjust);
         }

         hash = 1;

         for(Iterator i$ = this.axes.iterator(); i$.hasNext(); hash = ~(~hash)) {
            Set<E> axis = (Set)i$.next();
            hash = 31 * hash + this.size() / axis.size() * axis.hashCode();
         }

         hash += adjust;
         return ~(~hash);
      }
   }

   @GwtIncompatible("NavigableSet")
   private static class FilteredNavigableSet<E> extends Sets.FilteredSortedSet<E> implements NavigableSet<E> {
      FilteredNavigableSet(NavigableSet<E> unfiltered, Predicate<? super E> predicate) {
         super(unfiltered, predicate);
      }

      NavigableSet<E> unfiltered() {
         return (NavigableSet)this.unfiltered;
      }

      @Nullable
      public E lower(E e) {
         return Iterators.getNext(this.headSet(e, false).descendingIterator(), (Object)null);
      }

      @Nullable
      public E floor(E e) {
         return Iterators.getNext(this.headSet(e, true).descendingIterator(), (Object)null);
      }

      public E ceiling(E e) {
         return Iterables.getFirst(this.tailSet(e, true), (Object)null);
      }

      public E higher(E e) {
         return Iterables.getFirst(this.tailSet(e, false), (Object)null);
      }

      public E pollFirst() {
         return Iterables.removeFirstMatching(this.unfiltered(), this.predicate);
      }

      public E pollLast() {
         return Iterables.removeFirstMatching(this.unfiltered().descendingSet(), this.predicate);
      }

      public NavigableSet<E> descendingSet() {
         return Sets.filter(this.unfiltered().descendingSet(), this.predicate);
      }

      public Iterator<E> descendingIterator() {
         return Iterators.filter(this.unfiltered().descendingIterator(), this.predicate);
      }

      public E last() {
         return this.descendingIterator().next();
      }

      public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
         return Sets.filter(this.unfiltered().subSet(fromElement, fromInclusive, toElement, toInclusive), this.predicate);
      }

      public NavigableSet<E> headSet(E toElement, boolean inclusive) {
         return Sets.filter(this.unfiltered().headSet(toElement, inclusive), this.predicate);
      }

      public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
         return Sets.filter(this.unfiltered().tailSet(fromElement, inclusive), this.predicate);
      }
   }

   private static class FilteredSortedSet<E> extends Sets.FilteredSet<E> implements SortedSet<E> {
      FilteredSortedSet(SortedSet<E> unfiltered, Predicate<? super E> predicate) {
         super(unfiltered, predicate);
      }

      public Comparator<? super E> comparator() {
         return ((SortedSet)this.unfiltered).comparator();
      }

      public SortedSet<E> subSet(E fromElement, E toElement) {
         return new Sets.FilteredSortedSet(((SortedSet)this.unfiltered).subSet(fromElement, toElement), this.predicate);
      }

      public SortedSet<E> headSet(E toElement) {
         return new Sets.FilteredSortedSet(((SortedSet)this.unfiltered).headSet(toElement), this.predicate);
      }

      public SortedSet<E> tailSet(E fromElement) {
         return new Sets.FilteredSortedSet(((SortedSet)this.unfiltered).tailSet(fromElement), this.predicate);
      }

      public E first() {
         return this.iterator().next();
      }

      public E last() {
         SortedSet sortedUnfiltered = (SortedSet)this.unfiltered;

         while(true) {
            E element = sortedUnfiltered.last();
            if (this.predicate.apply(element)) {
               return element;
            }

            sortedUnfiltered = sortedUnfiltered.headSet(element);
         }
      }
   }

   private static class FilteredSet<E> extends Collections2.FilteredCollection<E> implements Set<E> {
      FilteredSet(Set<E> unfiltered, Predicate<? super E> predicate) {
         super(unfiltered, predicate);
      }

      public boolean equals(@Nullable Object object) {
         return Sets.equalsImpl(this, object);
      }

      public int hashCode() {
         return Sets.hashCodeImpl(this);
      }
   }

   public abstract static class SetView<E> extends AbstractSet<E> {
      private SetView() {
      }

      public ImmutableSet<E> immutableCopy() {
         return ImmutableSet.copyOf((Collection)this);
      }

      public <S extends Set<E>> S copyInto(S set) {
         set.addAll(this);
         return set;
      }

      // $FF: synthetic method
      SetView(Object x0) {
         this();
      }
   }

   abstract static class ImprovedAbstractSet<E> extends AbstractSet<E> {
      public boolean removeAll(Collection<?> c) {
         return Sets.removeAllImpl(this, (Collection)c);
      }

      public boolean retainAll(Collection<?> c) {
         return super.retainAll((Collection)Preconditions.checkNotNull(c));
      }
   }
}
