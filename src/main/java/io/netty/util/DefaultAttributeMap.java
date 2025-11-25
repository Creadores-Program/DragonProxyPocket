package io.netty.util;

import io.netty.util.internal.PlatformDependent;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class DefaultAttributeMap implements AttributeMap {
   private static final AtomicReferenceFieldUpdater<DefaultAttributeMap, AtomicReferenceArray> updater;
   private static final int BUCKET_SIZE = 4;
   private static final int MASK = 3;
   private volatile AtomicReferenceArray<DefaultAttributeMap.DefaultAttribute<?>> attributes;

   public <T> Attribute<T> attr(AttributeKey<T> key) {
      if (key == null) {
         throw new NullPointerException("key");
      } else {
         AtomicReferenceArray<DefaultAttributeMap.DefaultAttribute<?>> attributes = this.attributes;
         if (attributes == null) {
            attributes = new AtomicReferenceArray(4);
            if (!updater.compareAndSet(this, (Object)null, attributes)) {
               attributes = this.attributes;
            }
         }

         int i = index(key);
         DefaultAttributeMap.DefaultAttribute<?> head = (DefaultAttributeMap.DefaultAttribute)attributes.get(i);
         if (head == null) {
            head = new DefaultAttributeMap.DefaultAttribute(key);
            if (attributes.compareAndSet(i, (Object)null, head)) {
               return head;
            }

            head = (DefaultAttributeMap.DefaultAttribute)attributes.get(i);
         }

         synchronized(head) {
            DefaultAttributeMap.DefaultAttribute curr;
            DefaultAttributeMap.DefaultAttribute next;
            for(curr = head; curr.removed || curr.key != key; curr = next) {
               next = curr.next;
               if (next == null) {
                  DefaultAttributeMap.DefaultAttribute<T> attr = new DefaultAttributeMap.DefaultAttribute(head, key);
                  curr.next = attr;
                  attr.prev = curr;
                  return attr;
               }
            }

            return curr;
         }
      }
   }

   public <T> boolean hasAttr(AttributeKey<T> key) {
      if (key == null) {
         throw new NullPointerException("key");
      } else {
         AtomicReferenceArray<DefaultAttributeMap.DefaultAttribute<?>> attributes = this.attributes;
         if (attributes == null) {
            return false;
         } else {
            int i = index(key);
            DefaultAttributeMap.DefaultAttribute<?> head = (DefaultAttributeMap.DefaultAttribute)attributes.get(i);
            if (head == null) {
               return false;
            } else if (head.key == key && !head.removed) {
               return true;
            } else {
               synchronized(head) {
                  for(DefaultAttributeMap.DefaultAttribute curr = head.next; curr != null; curr = curr.next) {
                     if (!curr.removed && curr.key == key) {
                        return true;
                     }
                  }

                  return false;
               }
            }
         }
      }
   }

   private static int index(AttributeKey<?> key) {
      return key.id() & 3;
   }

   static {
      AtomicReferenceFieldUpdater<DefaultAttributeMap, AtomicReferenceArray> referenceFieldUpdater = PlatformDependent.newAtomicReferenceFieldUpdater(DefaultAttributeMap.class, "attributes");
      if (referenceFieldUpdater == null) {
         referenceFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(DefaultAttributeMap.class, AtomicReferenceArray.class, "attributes");
      }

      updater = referenceFieldUpdater;
   }

   private static final class DefaultAttribute<T> extends AtomicReference<T> implements Attribute<T> {
      private static final long serialVersionUID = -2661411462200283011L;
      private final DefaultAttributeMap.DefaultAttribute<?> head;
      private final AttributeKey<T> key;
      private DefaultAttributeMap.DefaultAttribute<?> prev;
      private DefaultAttributeMap.DefaultAttribute<?> next;
      private volatile boolean removed;

      DefaultAttribute(DefaultAttributeMap.DefaultAttribute<?> head, AttributeKey<T> key) {
         this.head = head;
         this.key = key;
      }

      DefaultAttribute(AttributeKey<T> key) {
         this.head = this;
         this.key = key;
      }

      public AttributeKey<T> key() {
         return this.key;
      }

      public T setIfAbsent(T value) {
         while(true) {
            if (!this.compareAndSet((Object)null, value)) {
               T old = this.get();
               if (old == null) {
                  continue;
               }

               return old;
            }

            return null;
         }
      }

      public T getAndRemove() {
         this.removed = true;
         T oldValue = this.getAndSet((Object)null);
         this.remove0();
         return oldValue;
      }

      public void remove() {
         this.removed = true;
         this.set((Object)null);
         this.remove0();
      }

      private void remove0() {
         synchronized(this.head) {
            if (this.prev != null) {
               this.prev.next = this.next;
               if (this.next != null) {
                  this.next.prev = this.prev;
               }
            }

         }
      }
   }
}
