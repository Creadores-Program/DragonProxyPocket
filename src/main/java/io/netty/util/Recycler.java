package io.netty.util;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Recycler<T> {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(Recycler.class);
   private static final AtomicInteger ID_GENERATOR = new AtomicInteger(Integer.MIN_VALUE);
   private static final int OWN_THREAD_ID;
   private static final int DEFAULT_MAX_CAPACITY;
   private static final int INITIAL_CAPACITY;
   private final int maxCapacity;
   private final FastThreadLocal<Recycler.Stack<T>> threadLocal;
   private static final FastThreadLocal<Map<Recycler.Stack<?>, Recycler.WeakOrderQueue>> DELAYED_RECYCLED;

   protected Recycler() {
      this(DEFAULT_MAX_CAPACITY);
   }

   protected Recycler(int maxCapacity) {
      this.threadLocal = new FastThreadLocal<Recycler.Stack<T>>() {
         protected Recycler.Stack<T> initialValue() {
            return new Recycler.Stack(Recycler.this, Thread.currentThread(), Recycler.this.maxCapacity);
         }
      };
      this.maxCapacity = Math.max(0, maxCapacity);
   }

   public final T get() {
      Recycler.Stack<T> stack = (Recycler.Stack)this.threadLocal.get();
      Recycler.DefaultHandle<T> handle = stack.pop();
      if (handle == null) {
         handle = stack.newHandle();
         handle.value = this.newObject(handle);
      }

      return handle.value;
   }

   public final boolean recycle(T o, Recycler.Handle<T> handle) {
      Recycler.DefaultHandle<T> h = (Recycler.DefaultHandle)handle;
      if (h.stack.parent != this) {
         return false;
      } else {
         h.recycle(o);
         return true;
      }
   }

   final int threadLocalCapacity() {
      return ((Recycler.Stack)this.threadLocal.get()).elements.length;
   }

   final int threadLocalSize() {
      return ((Recycler.Stack)this.threadLocal.get()).size;
   }

   protected abstract T newObject(Recycler.Handle<T> var1);

   static {
      OWN_THREAD_ID = ID_GENERATOR.getAndIncrement();
      int maxCapacity = SystemPropertyUtil.getInt("io.netty.recycler.maxCapacity", 0);
      if (maxCapacity <= 0) {
         maxCapacity = 262144;
      }

      DEFAULT_MAX_CAPACITY = maxCapacity;
      if (logger.isDebugEnabled()) {
         logger.debug("-Dio.netty.recycler.maxCapacity: {}", (Object)DEFAULT_MAX_CAPACITY);
      }

      INITIAL_CAPACITY = Math.min(DEFAULT_MAX_CAPACITY, 256);
      DELAYED_RECYCLED = new FastThreadLocal<Map<Recycler.Stack<?>, Recycler.WeakOrderQueue>>() {
         protected Map<Recycler.Stack<?>, Recycler.WeakOrderQueue> initialValue() {
            return new WeakHashMap();
         }
      };
   }

   static final class Stack<T> {
      final Recycler<T> parent;
      final Thread thread;
      private Recycler.DefaultHandle<?>[] elements;
      private final int maxCapacity;
      private int size;
      private volatile Recycler.WeakOrderQueue head;
      private Recycler.WeakOrderQueue cursor;
      private Recycler.WeakOrderQueue prev;

      Stack(Recycler<T> parent, Thread thread, int maxCapacity) {
         this.parent = parent;
         this.thread = thread;
         this.maxCapacity = maxCapacity;
         this.elements = new Recycler.DefaultHandle[Math.min(Recycler.INITIAL_CAPACITY, maxCapacity)];
      }

      int increaseCapacity(int expectedCapacity) {
         int newCapacity = this.elements.length;
         int maxCapacity = this.maxCapacity;

         do {
            newCapacity <<= 1;
         } while(newCapacity < expectedCapacity && newCapacity < maxCapacity);

         newCapacity = Math.min(newCapacity, maxCapacity);
         if (newCapacity != this.elements.length) {
            this.elements = (Recycler.DefaultHandle[])Arrays.copyOf(this.elements, newCapacity);
         }

         return newCapacity;
      }

      Recycler.DefaultHandle<T> pop() {
         int size = this.size;
         if (size == 0) {
            if (!this.scavenge()) {
               return null;
            }

            size = this.size;
         }

         --size;
         Recycler.DefaultHandle ret = this.elements[size];
         if (ret.lastRecycledId != ret.recycleId) {
            throw new IllegalStateException("recycled multiple times");
         } else {
            ret.recycleId = 0;
            ret.lastRecycledId = 0;
            this.size = size;
            return ret;
         }
      }

      boolean scavenge() {
         if (this.scavengeSome()) {
            return true;
         } else {
            this.prev = null;
            this.cursor = this.head;
            return false;
         }
      }

      boolean scavengeSome() {
         Recycler.WeakOrderQueue cursor = this.cursor;
         if (cursor == null) {
            cursor = this.head;
            if (cursor == null) {
               return false;
            }
         }

         boolean success = false;
         Recycler.WeakOrderQueue prev = this.prev;

         Recycler.WeakOrderQueue next;
         do {
            if (cursor.transfer(this)) {
               success = true;
               break;
            }

            next = cursor.next;
            if (cursor.owner.get() == null) {
               if (cursor.hasFinalData()) {
                  while(cursor.transfer(this)) {
                     success = true;
                  }
               }

               if (prev != null) {
                  prev.next = next;
               }
            } else {
               prev = cursor;
            }

            cursor = next;
         } while(next != null && !success);

         this.prev = prev;
         this.cursor = cursor;
         return success;
      }

      void push(Recycler.DefaultHandle<?> item) {
         if ((item.recycleId | item.lastRecycledId) != 0) {
            throw new IllegalStateException("recycled already");
         } else {
            item.recycleId = item.lastRecycledId = Recycler.OWN_THREAD_ID;
            int size = this.size;
            if (size < this.maxCapacity) {
               if (size == this.elements.length) {
                  this.elements = (Recycler.DefaultHandle[])Arrays.copyOf(this.elements, Math.min(size << 1, this.maxCapacity));
               }

               this.elements[size] = item;
               this.size = size + 1;
            }
         }
      }

      Recycler.DefaultHandle<T> newHandle() {
         return new Recycler.DefaultHandle(this);
      }
   }

   private static final class WeakOrderQueue {
      private static final int LINK_CAPACITY = 16;
      private Recycler.WeakOrderQueue.Link head;
      private Recycler.WeakOrderQueue.Link tail;
      private Recycler.WeakOrderQueue next;
      private final WeakReference<Thread> owner;
      private final int id;

      WeakOrderQueue(Recycler.Stack<?> stack, Thread thread) {
         this.id = Recycler.ID_GENERATOR.getAndIncrement();
         this.head = this.tail = new Recycler.WeakOrderQueue.Link();
         this.owner = new WeakReference(thread);
         synchronized(stack) {
            this.next = stack.head;
            stack.head = this;
         }
      }

      void add(Recycler.DefaultHandle<?> handle) {
         handle.lastRecycledId = this.id;
         Recycler.WeakOrderQueue.Link tail = this.tail;
         int writeIndex;
         if ((writeIndex = tail.get()) == 16) {
            this.tail = tail = tail.next = new Recycler.WeakOrderQueue.Link();
            writeIndex = tail.get();
         }

         tail.elements[writeIndex] = handle;
         handle.stack = null;
         tail.lazySet(writeIndex + 1);
      }

      boolean hasFinalData() {
         return this.tail.readIndex != this.tail.get();
      }

      boolean transfer(Recycler.Stack<?> dst) {
         Recycler.WeakOrderQueue.Link head = this.head;
         if (head == null) {
            return false;
         } else {
            if (head.readIndex == 16) {
               if (head.next == null) {
                  return false;
               }

               this.head = head = head.next;
            }

            int srcStart = head.readIndex;
            int srcEnd = head.get();
            int srcSize = srcEnd - srcStart;
            if (srcSize == 0) {
               return false;
            } else {
               int dstSize = dst.size;
               int expectedCapacity = dstSize + srcSize;
               if (expectedCapacity > dst.elements.length) {
                  int actualCapacity = dst.increaseCapacity(expectedCapacity);
                  srcEnd = Math.min(srcStart + actualCapacity - dstSize, srcEnd);
               }

               if (srcStart != srcEnd) {
                  Recycler.DefaultHandle[] srcElems = head.elements;
                  Recycler.DefaultHandle[] dstElems = dst.elements;
                  int newDstSize = dstSize;

                  for(int i = srcStart; i < srcEnd; ++i) {
                     Recycler.DefaultHandle element = srcElems[i];
                     if (element.recycleId == 0) {
                        element.recycleId = element.lastRecycledId;
                     } else if (element.recycleId != element.lastRecycledId) {
                        throw new IllegalStateException("recycled already");
                     }

                     element.stack = dst;
                     dstElems[newDstSize++] = element;
                     srcElems[i] = null;
                  }

                  dst.size = newDstSize;
                  if (srcEnd == 16 && head.next != null) {
                     this.head = head.next;
                  }

                  head.readIndex = srcEnd;
                  return true;
               } else {
                  return false;
               }
            }
         }
      }

      private static final class Link extends AtomicInteger {
         private final Recycler.DefaultHandle<?>[] elements;
         private int readIndex;
         private Recycler.WeakOrderQueue.Link next;

         private Link() {
            this.elements = new Recycler.DefaultHandle[16];
         }

         // $FF: synthetic method
         Link(Object x0) {
            this();
         }
      }
   }

   static final class DefaultHandle<T> implements Recycler.Handle<T> {
      private int lastRecycledId;
      private int recycleId;
      private Recycler.Stack<?> stack;
      private Object value;

      DefaultHandle(Recycler.Stack<?> stack) {
         this.stack = stack;
      }

      public void recycle(Object object) {
         if (object != this.value) {
            throw new IllegalArgumentException("object does not belong to handle");
         } else {
            Thread thread = Thread.currentThread();
            if (thread == this.stack.thread) {
               this.stack.push(this);
            } else {
               Map<Recycler.Stack<?>, Recycler.WeakOrderQueue> delayedRecycled = (Map)Recycler.DELAYED_RECYCLED.get();
               Recycler.WeakOrderQueue queue = (Recycler.WeakOrderQueue)delayedRecycled.get(this.stack);
               if (queue == null) {
                  delayedRecycled.put(this.stack, queue = new Recycler.WeakOrderQueue(this.stack, thread));
               }

               queue.add(this);
            }
         }
      }
   }

   public interface Handle<T> {
      void recycle(T var1);
   }
}
