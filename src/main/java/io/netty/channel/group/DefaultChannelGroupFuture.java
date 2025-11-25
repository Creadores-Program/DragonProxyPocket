package io.netty.channel.group;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.BlockingOperationException;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.ImmediateEventExecutor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

final class DefaultChannelGroupFuture extends DefaultPromise<Void> implements ChannelGroupFuture {
   private final ChannelGroup group;
   private final Map<Channel, ChannelFuture> futures;
   private int successCount;
   private int failureCount;
   private final ChannelFutureListener childListener = new ChannelFutureListener() {
      public void operationComplete(ChannelFuture future) throws Exception {
         boolean success = future.isSuccess();
         boolean callSetDone;
         synchronized(DefaultChannelGroupFuture.this) {
            if (success) {
               DefaultChannelGroupFuture.this.successCount++;
            } else {
               DefaultChannelGroupFuture.this.failureCount++;
            }

            callSetDone = DefaultChannelGroupFuture.this.successCount + DefaultChannelGroupFuture.this.failureCount == DefaultChannelGroupFuture.this.futures.size();

            assert DefaultChannelGroupFuture.this.successCount + DefaultChannelGroupFuture.this.failureCount <= DefaultChannelGroupFuture.this.futures.size();
         }

         if (callSetDone) {
            if (DefaultChannelGroupFuture.this.failureCount > 0) {
               List<Entry<Channel, Throwable>> failed = new ArrayList(DefaultChannelGroupFuture.this.failureCount);
               Iterator i$ = DefaultChannelGroupFuture.this.futures.values().iterator();

               while(i$.hasNext()) {
                  ChannelFuture f = (ChannelFuture)i$.next();
                  if (!f.isSuccess()) {
                     failed.add(new DefaultChannelGroupFuture.DefaultEntry(f.channel(), f.cause()));
                  }
               }

               DefaultChannelGroupFuture.this.setFailure0(new ChannelGroupException(failed));
            } else {
               DefaultChannelGroupFuture.this.setSuccess0();
            }
         }

      }
   };

   DefaultChannelGroupFuture(ChannelGroup group, Collection<ChannelFuture> futures, EventExecutor executor) {
      super(executor);
      if (group == null) {
         throw new NullPointerException("group");
      } else if (futures == null) {
         throw new NullPointerException("futures");
      } else {
         this.group = group;
         Map<Channel, ChannelFuture> futureMap = new LinkedHashMap();
         Iterator i$ = futures.iterator();

         ChannelFuture f;
         while(i$.hasNext()) {
            f = (ChannelFuture)i$.next();
            futureMap.put(f.channel(), f);
         }

         this.futures = Collections.unmodifiableMap(futureMap);
         i$ = this.futures.values().iterator();

         while(i$.hasNext()) {
            f = (ChannelFuture)i$.next();
            f.addListener(this.childListener);
         }

         if (this.futures.isEmpty()) {
            this.setSuccess0();
         }

      }
   }

   DefaultChannelGroupFuture(ChannelGroup group, Map<Channel, ChannelFuture> futures, EventExecutor executor) {
      super(executor);
      this.group = group;
      this.futures = Collections.unmodifiableMap(futures);
      Iterator i$ = this.futures.values().iterator();

      while(i$.hasNext()) {
         ChannelFuture f = (ChannelFuture)i$.next();
         f.addListener(this.childListener);
      }

      if (this.futures.isEmpty()) {
         this.setSuccess0();
      }

   }

   public ChannelGroup group() {
      return this.group;
   }

   public ChannelFuture find(Channel channel) {
      return (ChannelFuture)this.futures.get(channel);
   }

   public Iterator<ChannelFuture> iterator() {
      return this.futures.values().iterator();
   }

   public synchronized boolean isPartialSuccess() {
      return this.successCount != 0 && this.successCount != this.futures.size();
   }

   public synchronized boolean isPartialFailure() {
      return this.failureCount != 0 && this.failureCount != this.futures.size();
   }

   public DefaultChannelGroupFuture addListener(GenericFutureListener<? extends Future<? super Void>> listener) {
      super.addListener(listener);
      return this;
   }

   public DefaultChannelGroupFuture addListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
      super.addListeners(listeners);
      return this;
   }

   public DefaultChannelGroupFuture removeListener(GenericFutureListener<? extends Future<? super Void>> listener) {
      super.removeListener(listener);
      return this;
   }

   public DefaultChannelGroupFuture removeListeners(GenericFutureListener<? extends Future<? super Void>>... listeners) {
      super.removeListeners(listeners);
      return this;
   }

   public DefaultChannelGroupFuture await() throws InterruptedException {
      super.await();
      return this;
   }

   public DefaultChannelGroupFuture awaitUninterruptibly() {
      super.awaitUninterruptibly();
      return this;
   }

   public DefaultChannelGroupFuture syncUninterruptibly() {
      super.syncUninterruptibly();
      return this;
   }

   public DefaultChannelGroupFuture sync() throws InterruptedException {
      super.sync();
      return this;
   }

   public ChannelGroupException cause() {
      return (ChannelGroupException)super.cause();
   }

   private void setSuccess0() {
      super.setSuccess((Object)null);
   }

   private void setFailure0(ChannelGroupException cause) {
      super.setFailure(cause);
   }

   public DefaultChannelGroupFuture setSuccess(Void result) {
      throw new IllegalStateException();
   }

   public boolean trySuccess(Void result) {
      throw new IllegalStateException();
   }

   public DefaultChannelGroupFuture setFailure(Throwable cause) {
      throw new IllegalStateException();
   }

   public boolean tryFailure(Throwable cause) {
      throw new IllegalStateException();
   }

   protected void checkDeadLock() {
      EventExecutor e = this.executor();
      if (e != null && e != ImmediateEventExecutor.INSTANCE && e.inEventLoop()) {
         throw new BlockingOperationException();
      }
   }

   private static final class DefaultEntry<K, V> implements Entry<K, V> {
      private final K key;
      private final V value;

      DefaultEntry(K key, V value) {
         this.key = key;
         this.value = value;
      }

      public K getKey() {
         return this.key;
      }

      public V getValue() {
         return this.value;
      }

      public V setValue(V value) {
         throw new UnsupportedOperationException("read-only");
      }
   }
}
