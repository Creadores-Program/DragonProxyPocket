package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible
public abstract class ForwardingMultiset<E> extends ForwardingCollection<E> implements Multiset<E> {
   protected ForwardingMultiset() {
   }

   protected abstract Multiset<E> delegate();

   public int count(Object element) {
      return this.delegate().count(element);
   }

   public int add(E element, int occurrences) {
      return this.delegate().add(element, occurrences);
   }

   public int remove(Object element, int occurrences) {
      return this.delegate().remove(element, occurrences);
   }

   public Set<E> elementSet() {
      return this.delegate().elementSet();
   }

   public Set<Multiset.Entry<E>> entrySet() {
      return this.delegate().entrySet();
   }

   public boolean equals(@Nullable Object object) {
      return object == this || this.delegate().equals(object);
   }

   public int hashCode() {
      return this.delegate().hashCode();
   }

   public int setCount(E element, int count) {
      return this.delegate().setCount(element, count);
   }

   public boolean setCount(E element, int oldCount, int newCount) {
      return this.delegate().setCount(element, oldCount, newCount);
   }

   protected boolean standardContains(@Nullable Object object) {
      return this.count(object) > 0;
   }

   protected void standardClear() {
      Iterators.clear(this.entrySet().iterator());
   }

   @Beta
   protected int standardCount(@Nullable Object object) {
      Iterator i$ = this.entrySet().iterator();

      Multiset.Entry entry;
      do {
         if (!i$.hasNext()) {
            return 0;
         }

         entry = (Multiset.Entry)i$.next();
      } while(!Objects.equal(entry.getElement(), object));

      return entry.getCount();
   }

   protected boolean standardAdd(E element) {
      this.add(element, 1);
      return true;
   }

   @Beta
   protected boolean standardAddAll(Collection<? extends E> elementsToAdd) {
      return Multisets.addAllImpl(this, elementsToAdd);
   }

   protected boolean standardRemove(Object element) {
      return this.remove(element, 1) > 0;
   }

   protected boolean standardRemoveAll(Collection<?> elementsToRemove) {
      return Multisets.removeAllImpl(this, elementsToRemove);
   }

   protected boolean standardRetainAll(Collection<?> elementsToRetain) {
      return Multisets.retainAllImpl(this, elementsToRetain);
   }

   protected int standardSetCount(E element, int count) {
      return Multisets.setCountImpl(this, element, count);
   }

   protected boolean standardSetCount(E element, int oldCount, int newCount) {
      return Multisets.setCountImpl(this, element, oldCount, newCount);
   }

   protected Iterator<E> standardIterator() {
      return Multisets.iteratorImpl(this);
   }

   protected int standardSize() {
      return Multisets.sizeImpl(this);
   }

   protected boolean standardEquals(@Nullable Object object) {
      return Multisets.equalsImpl(this, object);
   }

   protected int standardHashCode() {
      return this.entrySet().hashCode();
   }

   protected String standardToString() {
      return this.entrySet().toString();
   }

   @Beta
   protected class StandardElementSet extends Multisets.ElementSet<E> {
      public StandardElementSet() {
      }

      Multiset<E> multiset() {
         return ForwardingMultiset.this;
      }
   }
}
