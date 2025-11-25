package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import java.util.Collection;
import java.util.Iterator;
import javax.annotation.Nullable;

@GwtCompatible
public abstract class ForwardingCollection<E> extends ForwardingObject implements Collection<E> {
   protected ForwardingCollection() {
   }

   protected abstract Collection<E> delegate();

   public Iterator<E> iterator() {
      return this.delegate().iterator();
   }

   public int size() {
      return this.delegate().size();
   }

   public boolean removeAll(Collection<?> collection) {
      return this.delegate().removeAll(collection);
   }

   public boolean isEmpty() {
      return this.delegate().isEmpty();
   }

   public boolean contains(Object object) {
      return this.delegate().contains(object);
   }

   public boolean add(E element) {
      return this.delegate().add(element);
   }

   public boolean remove(Object object) {
      return this.delegate().remove(object);
   }

   public boolean containsAll(Collection<?> collection) {
      return this.delegate().containsAll(collection);
   }

   public boolean addAll(Collection<? extends E> collection) {
      return this.delegate().addAll(collection);
   }

   public boolean retainAll(Collection<?> collection) {
      return this.delegate().retainAll(collection);
   }

   public void clear() {
      this.delegate().clear();
   }

   public Object[] toArray() {
      return this.delegate().toArray();
   }

   public <T> T[] toArray(T[] array) {
      return this.delegate().toArray(array);
   }

   protected boolean standardContains(@Nullable Object object) {
      return Iterators.contains(this.iterator(), object);
   }

   protected boolean standardContainsAll(Collection<?> collection) {
      return Collections2.containsAllImpl(this, collection);
   }

   protected boolean standardAddAll(Collection<? extends E> collection) {
      return Iterators.addAll(this, collection.iterator());
   }

   protected boolean standardRemove(@Nullable Object object) {
      Iterator iterator = this.iterator();

      do {
         if (!iterator.hasNext()) {
            return false;
         }
      } while(!Objects.equal(iterator.next(), object));

      iterator.remove();
      return true;
   }

   protected boolean standardRemoveAll(Collection<?> collection) {
      return Iterators.removeAll(this.iterator(), collection);
   }

   protected boolean standardRetainAll(Collection<?> collection) {
      return Iterators.retainAll(this.iterator(), collection);
   }

   protected void standardClear() {
      Iterators.clear(this.iterator());
   }

   protected boolean standardIsEmpty() {
      return !this.iterator().hasNext();
   }

   protected String standardToString() {
      return Collections2.toStringImpl(this);
   }

   protected Object[] standardToArray() {
      Object[] newArray = new Object[this.size()];
      return this.toArray(newArray);
   }

   protected <T> T[] standardToArray(T[] array) {
      return ObjectArrays.toArrayImpl(this, array);
   }
}
