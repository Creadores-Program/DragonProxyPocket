package com.google.common.util.concurrent;

import com.google.common.collect.ForwardingQueue;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class ForwardingBlockingQueue<E> extends ForwardingQueue<E> implements BlockingQueue<E> {
   protected ForwardingBlockingQueue() {
   }

   protected abstract BlockingQueue<E> delegate();

   public int drainTo(Collection<? super E> c, int maxElements) {
      return this.delegate().drainTo(c, maxElements);
   }

   public int drainTo(Collection<? super E> c) {
      return this.delegate().drainTo(c);
   }

   public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
      return this.delegate().offer(e, timeout, unit);
   }

   public E poll(long timeout, TimeUnit unit) throws InterruptedException {
      return this.delegate().poll(timeout, unit);
   }

   public void put(E e) throws InterruptedException {
      this.delegate().put(e);
   }

   public int remainingCapacity() {
      return this.delegate().remainingCapacity();
   }

   public E take() throws InterruptedException {
      return this.delegate().take();
   }
}
