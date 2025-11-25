package com.google.common.collect;

import java.util.Deque;
import java.util.Iterator;

public abstract class ForwardingDeque<E> extends ForwardingQueue<E> implements Deque<E> {
   protected ForwardingDeque() {
   }

   protected abstract Deque<E> delegate();

   public void addFirst(E e) {
      this.delegate().addFirst(e);
   }

   public void addLast(E e) {
      this.delegate().addLast(e);
   }

   public Iterator<E> descendingIterator() {
      return this.delegate().descendingIterator();
   }

   public E getFirst() {
      return this.delegate().getFirst();
   }

   public E getLast() {
      return this.delegate().getLast();
   }

   public boolean offerFirst(E e) {
      return this.delegate().offerFirst(e);
   }

   public boolean offerLast(E e) {
      return this.delegate().offerLast(e);
   }

   public E peekFirst() {
      return this.delegate().peekFirst();
   }

   public E peekLast() {
      return this.delegate().peekLast();
   }

   public E pollFirst() {
      return this.delegate().pollFirst();
   }

   public E pollLast() {
      return this.delegate().pollLast();
   }

   public E pop() {
      return this.delegate().pop();
   }

   public void push(E e) {
      this.delegate().push(e);
   }

   public E removeFirst() {
      return this.delegate().removeFirst();
   }

   public E removeLast() {
      return this.delegate().removeLast();
   }

   public boolean removeFirstOccurrence(Object o) {
      return this.delegate().removeFirstOccurrence(o);
   }

   public boolean removeLastOccurrence(Object o) {
      return this.delegate().removeLastOccurrence(o);
   }
}
