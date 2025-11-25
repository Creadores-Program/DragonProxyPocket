package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import java.util.Collection;
import java.util.Iterator;
import javax.annotation.Nullable;

@GwtCompatible(
   serializable = true
)
class RegularImmutableMultiset<E> extends ImmutableMultiset<E> {
   static final RegularImmutableMultiset<Object> EMPTY = new RegularImmutableMultiset(ImmutableList.of());
   private final transient Multisets.ImmutableEntry<E>[] entries;
   private final transient Multisets.ImmutableEntry<E>[] hashTable;
   private final transient int size;
   private final transient int hashCode;
   private transient ImmutableSet<E> elementSet;

   RegularImmutableMultiset(Collection<? extends Multiset.Entry<? extends E>> entries) {
      int distinct = entries.size();
      Multisets.ImmutableEntry<E>[] entryArray = new Multisets.ImmutableEntry[distinct];
      if (distinct == 0) {
         this.entries = entryArray;
         this.hashTable = null;
         this.size = 0;
         this.hashCode = 0;
         this.elementSet = ImmutableSet.of();
      } else {
         int tableSize = Hashing.closedTableSize(distinct, 1.0D);
         int mask = tableSize - 1;
         Multisets.ImmutableEntry<E>[] hashTable = new Multisets.ImmutableEntry[tableSize];
         int index = 0;
         int hashCode = 0;
         long size = 0L;

         int count;
         for(Iterator i$ = entries.iterator(); i$.hasNext(); size += (long)count) {
            Multiset.Entry<? extends E> entry = (Multiset.Entry)i$.next();
            E element = Preconditions.checkNotNull(entry.getElement());
            count = entry.getCount();
            int hash = element.hashCode();
            int bucket = Hashing.smear(hash) & mask;
            Multisets.ImmutableEntry<E> bucketHead = hashTable[bucket];
            Object newEntry;
            if (bucketHead != null) {
               newEntry = new RegularImmutableMultiset.NonTerminalEntry(element, count, bucketHead);
            } else {
               boolean canReuseEntry = entry instanceof Multisets.ImmutableEntry && !(entry instanceof RegularImmutableMultiset.NonTerminalEntry);
               newEntry = canReuseEntry ? (Multisets.ImmutableEntry)entry : new Multisets.ImmutableEntry(element, count);
            }

            hashCode += hash ^ count;
            entryArray[index++] = (Multisets.ImmutableEntry)newEntry;
            hashTable[bucket] = (Multisets.ImmutableEntry)newEntry;
         }

         this.entries = entryArray;
         this.hashTable = hashTable;
         this.size = Ints.saturatedCast(size);
         this.hashCode = hashCode;
      }

   }

   boolean isPartialView() {
      return false;
   }

   public int count(@Nullable Object element) {
      Multisets.ImmutableEntry<E>[] hashTable = this.hashTable;
      if (element != null && hashTable != null) {
         int hash = Hashing.smearedHash(element);
         int mask = hashTable.length - 1;

         for(Multisets.ImmutableEntry entry = hashTable[hash & mask]; entry != null; entry = entry.nextInBucket()) {
            if (Objects.equal(element, entry.getElement())) {
               return entry.getCount();
            }
         }

         return 0;
      } else {
         return 0;
      }
   }

   public int size() {
      return this.size;
   }

   public ImmutableSet<E> elementSet() {
      ImmutableSet<E> result = this.elementSet;
      return result == null ? (this.elementSet = new RegularImmutableMultiset.ElementSet()) : result;
   }

   Multiset.Entry<E> getEntry(int index) {
      return this.entries[index];
   }

   public int hashCode() {
      return this.hashCode;
   }

   private final class ElementSet extends ImmutableSet.Indexed<E> {
      private ElementSet() {
      }

      E get(int index) {
         return RegularImmutableMultiset.this.entries[index].getElement();
      }

      public boolean contains(@Nullable Object object) {
         return RegularImmutableMultiset.this.contains(object);
      }

      boolean isPartialView() {
         return true;
      }

      public int size() {
         return RegularImmutableMultiset.this.entries.length;
      }

      // $FF: synthetic method
      ElementSet(Object x1) {
         this();
      }
   }

   private static final class NonTerminalEntry<E> extends Multisets.ImmutableEntry<E> {
      private final Multisets.ImmutableEntry<E> nextInBucket;

      NonTerminalEntry(E element, int count, Multisets.ImmutableEntry<E> nextInBucket) {
         super(element, count);
         this.nextInBucket = nextInBucket;
      }

      public Multisets.ImmutableEntry<E> nextInBucket() {
         return this.nextInBucket;
      }
   }
}
