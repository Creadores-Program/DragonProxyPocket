package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;

@GwtCompatible(
   emulated = true
)
abstract class DescendingMultiset<E> extends ForwardingMultiset<E> implements SortedMultiset<E> {
   private transient Comparator<? super E> comparator;
   private transient NavigableSet<E> elementSet;
   private transient Set<Multiset.Entry<E>> entrySet;

   abstract SortedMultiset<E> forwardMultiset();

   public Comparator<? super E> comparator() {
      Comparator<? super E> result = this.comparator;
      return result == null ? (this.comparator = Ordering.from(this.forwardMultiset().comparator()).reverse()) : result;
   }

   public NavigableSet<E> elementSet() {
      NavigableSet<E> result = this.elementSet;
      return result == null ? (this.elementSet = new SortedMultisets.NavigableElementSet(this)) : result;
   }

   public Multiset.Entry<E> pollFirstEntry() {
      return this.forwardMultiset().pollLastEntry();
   }

   public Multiset.Entry<E> pollLastEntry() {
      return this.forwardMultiset().pollFirstEntry();
   }

   public SortedMultiset<E> headMultiset(E toElement, BoundType boundType) {
      return this.forwardMultiset().tailMultiset(toElement, boundType).descendingMultiset();
   }

   public SortedMultiset<E> subMultiset(E fromElement, BoundType fromBoundType, E toElement, BoundType toBoundType) {
      return this.forwardMultiset().subMultiset(toElement, toBoundType, fromElement, fromBoundType).descendingMultiset();
   }

   public SortedMultiset<E> tailMultiset(E fromElement, BoundType boundType) {
      return this.forwardMultiset().headMultiset(fromElement, boundType).descendingMultiset();
   }

   protected Multiset<E> delegate() {
      return this.forwardMultiset();
   }

   public SortedMultiset<E> descendingMultiset() {
      return this.forwardMultiset();
   }

   public Multiset.Entry<E> firstEntry() {
      return this.forwardMultiset().lastEntry();
   }

   public Multiset.Entry<E> lastEntry() {
      return this.forwardMultiset().firstEntry();
   }

   abstract Iterator<Multiset.Entry<E>> entryIterator();

   public Set<Multiset.Entry<E>> entrySet() {
      Set<Multiset.Entry<E>> result = this.entrySet;
      return result == null ? (this.entrySet = this.createEntrySet()) : result;
   }

   Set<Multiset.Entry<E>> createEntrySet() {
      class EntrySetImpl extends Multisets.EntrySet<E> {
         Multiset<E> multiset() {
            return DescendingMultiset.this;
         }

         public Iterator<Multiset.Entry<E>> iterator() {
            return DescendingMultiset.this.entryIterator();
         }

         public int size() {
            return DescendingMultiset.this.forwardMultiset().entrySet().size();
         }
      }

      return new EntrySetImpl();
   }

   public Iterator<E> iterator() {
      return Multisets.iteratorImpl(this);
   }

   public Object[] toArray() {
      return this.standardToArray();
   }

   public <T> T[] toArray(T[] array) {
      return this.standardToArray(array);
   }

   public String toString() {
      return this.entrySet().toString();
   }
}
