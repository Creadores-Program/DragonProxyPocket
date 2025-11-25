package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

@GwtCompatible(
   emulated = true
)
public abstract class FluentIterable<E> implements Iterable<E> {
   private final Iterable<E> iterable;

   protected FluentIterable() {
      this.iterable = this;
   }

   FluentIterable(Iterable<E> iterable) {
      this.iterable = (Iterable)Preconditions.checkNotNull(iterable);
   }

   @CheckReturnValue
   public static <E> FluentIterable<E> from(final Iterable<E> iterable) {
      return iterable instanceof FluentIterable ? (FluentIterable)iterable : new FluentIterable<E>(iterable) {
         public Iterator<E> iterator() {
            return iterable.iterator();
         }
      };
   }

   /** @deprecated */
   @Deprecated
   @CheckReturnValue
   public static <E> FluentIterable<E> from(FluentIterable<E> iterable) {
      return (FluentIterable)Preconditions.checkNotNull(iterable);
   }

   @CheckReturnValue
   @Beta
   public static <E> FluentIterable<E> of(E[] elements) {
      return from((Iterable)Lists.newArrayList(elements));
   }

   @CheckReturnValue
   public String toString() {
      return Iterables.toString(this.iterable);
   }

   @CheckReturnValue
   public final int size() {
      return Iterables.size(this.iterable);
   }

   @CheckReturnValue
   public final boolean contains(@Nullable Object target) {
      return Iterables.contains(this.iterable, target);
   }

   @CheckReturnValue
   public final FluentIterable<E> cycle() {
      return from(Iterables.cycle(this.iterable));
   }

   @CheckReturnValue
   @Beta
   public final FluentIterable<E> append(Iterable<? extends E> other) {
      return from(Iterables.concat(this.iterable, other));
   }

   @CheckReturnValue
   @Beta
   public final FluentIterable<E> append(E... elements) {
      return from(Iterables.concat(this.iterable, Arrays.asList(elements)));
   }

   @CheckReturnValue
   public final FluentIterable<E> filter(Predicate<? super E> predicate) {
      return from(Iterables.filter(this.iterable, predicate));
   }

   @CheckReturnValue
   @GwtIncompatible("Class.isInstance")
   public final <T> FluentIterable<T> filter(Class<T> type) {
      return from(Iterables.filter(this.iterable, type));
   }

   @CheckReturnValue
   public final boolean anyMatch(Predicate<? super E> predicate) {
      return Iterables.any(this.iterable, predicate);
   }

   @CheckReturnValue
   public final boolean allMatch(Predicate<? super E> predicate) {
      return Iterables.all(this.iterable, predicate);
   }

   @CheckReturnValue
   public final Optional<E> firstMatch(Predicate<? super E> predicate) {
      return Iterables.tryFind(this.iterable, predicate);
   }

   @CheckReturnValue
   public final <T> FluentIterable<T> transform(Function<? super E, T> function) {
      return from(Iterables.transform(this.iterable, function));
   }

   @CheckReturnValue
   public <T> FluentIterable<T> transformAndConcat(Function<? super E, ? extends Iterable<? extends T>> function) {
      return from(Iterables.concat((Iterable)this.transform(function)));
   }

   @CheckReturnValue
   public final Optional<E> first() {
      Iterator<E> iterator = this.iterable.iterator();
      return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.absent();
   }

   @CheckReturnValue
   public final Optional<E> last() {
      if (this.iterable instanceof List) {
         List<E> list = (List)this.iterable;
         return list.isEmpty() ? Optional.absent() : Optional.of(list.get(list.size() - 1));
      } else {
         Iterator<E> iterator = this.iterable.iterator();
         if (!iterator.hasNext()) {
            return Optional.absent();
         } else if (this.iterable instanceof SortedSet) {
            SortedSet<E> sortedSet = (SortedSet)this.iterable;
            return Optional.of(sortedSet.last());
         } else {
            Object current;
            do {
               current = iterator.next();
            } while(iterator.hasNext());

            return Optional.of(current);
         }
      }
   }

   @CheckReturnValue
   public final FluentIterable<E> skip(int numberToSkip) {
      return from(Iterables.skip(this.iterable, numberToSkip));
   }

   @CheckReturnValue
   public final FluentIterable<E> limit(int maxSize) {
      return from(Iterables.limit(this.iterable, maxSize));
   }

   @CheckReturnValue
   public final boolean isEmpty() {
      return !this.iterable.iterator().hasNext();
   }

   @CheckReturnValue
   public final ImmutableList<E> toList() {
      return ImmutableList.copyOf(this.iterable);
   }

   @CheckReturnValue
   public final ImmutableList<E> toSortedList(Comparator<? super E> comparator) {
      return Ordering.from(comparator).immutableSortedCopy(this.iterable);
   }

   @CheckReturnValue
   public final ImmutableSet<E> toSet() {
      return ImmutableSet.copyOf(this.iterable);
   }

   @CheckReturnValue
   public final ImmutableSortedSet<E> toSortedSet(Comparator<? super E> comparator) {
      return ImmutableSortedSet.copyOf(comparator, this.iterable);
   }

   @CheckReturnValue
   public final ImmutableMultiset<E> toMultiset() {
      return ImmutableMultiset.copyOf(this.iterable);
   }

   @CheckReturnValue
   public final <V> ImmutableMap<E, V> toMap(Function<? super E, V> valueFunction) {
      return Maps.toMap(this.iterable, valueFunction);
   }

   @CheckReturnValue
   public final <K> ImmutableListMultimap<K, E> index(Function<? super E, K> keyFunction) {
      return Multimaps.index(this.iterable, keyFunction);
   }

   @CheckReturnValue
   public final <K> ImmutableMap<K, E> uniqueIndex(Function<? super E, K> keyFunction) {
      return Maps.uniqueIndex(this.iterable, keyFunction);
   }

   @CheckReturnValue
   @GwtIncompatible("Array.newArray(Class, int)")
   public final E[] toArray(Class<E> type) {
      return Iterables.toArray(this.iterable, type);
   }

   public final <C extends Collection<? super E>> C copyInto(C collection) {
      Preconditions.checkNotNull(collection);
      if (this.iterable instanceof Collection) {
         collection.addAll(Collections2.cast(this.iterable));
      } else {
         Iterator i$ = this.iterable.iterator();

         while(i$.hasNext()) {
            E item = i$.next();
            collection.add(item);
         }
      }

      return collection;
   }

   @CheckReturnValue
   @Beta
   public final String join(Joiner joiner) {
      return joiner.join((Iterable)this);
   }

   @CheckReturnValue
   public final E get(int position) {
      return Iterables.get(this.iterable, position);
   }

   private static class FromIterableFunction<E> implements Function<Iterable<E>, FluentIterable<E>> {
      public FluentIterable<E> apply(Iterable<E> fromObject) {
         return FluentIterable.from(fromObject);
      }
   }
}
