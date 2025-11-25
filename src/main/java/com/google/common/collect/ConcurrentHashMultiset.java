package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.math.IntMath;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;

public final class ConcurrentHashMultiset<E> extends AbstractMultiset<E> implements Serializable {
   private final transient ConcurrentMap<E, AtomicInteger> countMap;
   private static final long serialVersionUID = 1L;

   public static <E> ConcurrentHashMultiset<E> create() {
      return new ConcurrentHashMultiset(new ConcurrentHashMap());
   }

   public static <E> ConcurrentHashMultiset<E> create(Iterable<? extends E> elements) {
      ConcurrentHashMultiset<E> multiset = create();
      Iterables.addAll(multiset, elements);
      return multiset;
   }

   @Beta
   public static <E> ConcurrentHashMultiset<E> create(MapMaker mapMaker) {
      return new ConcurrentHashMultiset(mapMaker.makeMap());
   }

   @VisibleForTesting
   ConcurrentHashMultiset(ConcurrentMap<E, AtomicInteger> countMap) {
      Preconditions.checkArgument(countMap.isEmpty());
      this.countMap = countMap;
   }

   public int count(@Nullable Object element) {
      AtomicInteger existingCounter = (AtomicInteger)Maps.safeGet(this.countMap, element);
      return existingCounter == null ? 0 : existingCounter.get();
   }

   public int size() {
      long sum = 0L;

      AtomicInteger value;
      for(Iterator i$ = this.countMap.values().iterator(); i$.hasNext(); sum += (long)value.get()) {
         value = (AtomicInteger)i$.next();
      }

      return Ints.saturatedCast(sum);
   }

   public Object[] toArray() {
      return this.snapshot().toArray();
   }

   public <T> T[] toArray(T[] array) {
      return this.snapshot().toArray(array);
   }

   private List<E> snapshot() {
      List<E> list = Lists.newArrayListWithExpectedSize(this.size());
      Iterator i$ = this.entrySet().iterator();

      while(i$.hasNext()) {
         Multiset.Entry<E> entry = (Multiset.Entry)i$.next();
         E element = entry.getElement();

         for(int i = entry.getCount(); i > 0; --i) {
            list.add(element);
         }
      }

      return list;
   }

   public int add(E element, int occurrences) {
      Preconditions.checkNotNull(element);
      if (occurrences == 0) {
         return this.count(element);
      } else {
         CollectPreconditions.checkPositive(occurrences, "occurences");

         AtomicInteger existingCounter;
         AtomicInteger newCounter;
         do {
            existingCounter = (AtomicInteger)Maps.safeGet(this.countMap, element);
            if (existingCounter == null) {
               existingCounter = (AtomicInteger)this.countMap.putIfAbsent(element, new AtomicInteger(occurrences));
               if (existingCounter == null) {
                  return 0;
               }
            }

            while(true) {
               int oldValue = existingCounter.get();
               if (oldValue == 0) {
                  newCounter = new AtomicInteger(occurrences);
                  break;
               }

               try {
                  int newValue = IntMath.checkedAdd(oldValue, occurrences);
                  if (existingCounter.compareAndSet(oldValue, newValue)) {
                     return oldValue;
                  }
               } catch (ArithmeticException var6) {
                  throw new IllegalArgumentException("Overflow adding " + occurrences + " occurrences to a count of " + oldValue);
               }
            }
         } while(this.countMap.putIfAbsent(element, newCounter) != null && !this.countMap.replace(element, existingCounter, newCounter));

         return 0;
      }
   }

   public int remove(@Nullable Object element, int occurrences) {
      if (occurrences == 0) {
         return this.count(element);
      } else {
         CollectPreconditions.checkPositive(occurrences, "occurences");
         AtomicInteger existingCounter = (AtomicInteger)Maps.safeGet(this.countMap, element);
         if (existingCounter == null) {
            return 0;
         } else {
            int oldValue;
            int newValue;
            do {
               oldValue = existingCounter.get();
               if (oldValue == 0) {
                  return 0;
               }

               newValue = Math.max(0, oldValue - occurrences);
            } while(!existingCounter.compareAndSet(oldValue, newValue));

            if (newValue == 0) {
               this.countMap.remove(element, existingCounter);
            }

            return oldValue;
         }
      }
   }

   public boolean removeExactly(@Nullable Object element, int occurrences) {
      if (occurrences == 0) {
         return true;
      } else {
         CollectPreconditions.checkPositive(occurrences, "occurences");
         AtomicInteger existingCounter = (AtomicInteger)Maps.safeGet(this.countMap, element);
         if (existingCounter == null) {
            return false;
         } else {
            int oldValue;
            int newValue;
            do {
               oldValue = existingCounter.get();
               if (oldValue < occurrences) {
                  return false;
               }

               newValue = oldValue - occurrences;
            } while(!existingCounter.compareAndSet(oldValue, newValue));

            if (newValue == 0) {
               this.countMap.remove(element, existingCounter);
            }

            return true;
         }
      }
   }

   public int setCount(E element, int count) {
      Preconditions.checkNotNull(element);
      CollectPreconditions.checkNonnegative(count, "count");

      AtomicInteger existingCounter;
      AtomicInteger newCounter;
      label40:
      do {
         existingCounter = (AtomicInteger)Maps.safeGet(this.countMap, element);
         if (existingCounter == null) {
            if (count == 0) {
               return 0;
            }

            existingCounter = (AtomicInteger)this.countMap.putIfAbsent(element, new AtomicInteger(count));
            if (existingCounter == null) {
               return 0;
            }
         }

         int oldValue;
         do {
            oldValue = existingCounter.get();
            if (oldValue == 0) {
               if (count == 0) {
                  return 0;
               }

               newCounter = new AtomicInteger(count);
               continue label40;
            }
         } while(!existingCounter.compareAndSet(oldValue, count));

         if (count == 0) {
            this.countMap.remove(element, existingCounter);
         }

         return oldValue;
      } while(this.countMap.putIfAbsent(element, newCounter) != null && !this.countMap.replace(element, existingCounter, newCounter));

      return 0;
   }

   public boolean setCount(E element, int expectedOldCount, int newCount) {
      Preconditions.checkNotNull(element);
      CollectPreconditions.checkNonnegative(expectedOldCount, "oldCount");
      CollectPreconditions.checkNonnegative(newCount, "newCount");
      AtomicInteger existingCounter = (AtomicInteger)Maps.safeGet(this.countMap, element);
      if (existingCounter == null) {
         if (expectedOldCount != 0) {
            return false;
         } else if (newCount == 0) {
            return true;
         } else {
            return this.countMap.putIfAbsent(element, new AtomicInteger(newCount)) == null;
         }
      } else {
         int oldValue = existingCounter.get();
         if (oldValue == expectedOldCount) {
            if (oldValue == 0) {
               if (newCount == 0) {
                  this.countMap.remove(element, existingCounter);
                  return true;
               }

               AtomicInteger newCounter = new AtomicInteger(newCount);
               return this.countMap.putIfAbsent(element, newCounter) == null || this.countMap.replace(element, existingCounter, newCounter);
            }

            if (existingCounter.compareAndSet(oldValue, newCount)) {
               if (newCount == 0) {
                  this.countMap.remove(element, existingCounter);
               }

               return true;
            }
         }

         return false;
      }
   }

   Set<E> createElementSet() {
      final Set<E> delegate = this.countMap.keySet();
      return new ForwardingSet<E>() {
         protected Set<E> delegate() {
            return delegate;
         }

         public boolean contains(@Nullable Object object) {
            return object != null && Collections2.safeContains(delegate, object);
         }

         public boolean containsAll(Collection<?> collection) {
            return this.standardContainsAll(collection);
         }

         public boolean remove(Object object) {
            return object != null && Collections2.safeRemove(delegate, object);
         }

         public boolean removeAll(Collection<?> c) {
            return this.standardRemoveAll(c);
         }
      };
   }

   public Set<Multiset.Entry<E>> createEntrySet() {
      return new ConcurrentHashMultiset.EntrySet();
   }

   int distinctElements() {
      return this.countMap.size();
   }

   public boolean isEmpty() {
      return this.countMap.isEmpty();
   }

   Iterator<Multiset.Entry<E>> entryIterator() {
      final Iterator<Multiset.Entry<E>> readOnlyIterator = new AbstractIterator<Multiset.Entry<E>>() {
         private Iterator<java.util.Map.Entry<E, AtomicInteger>> mapEntries;

         {
            this.mapEntries = ConcurrentHashMultiset.this.countMap.entrySet().iterator();
         }

         protected Multiset.Entry<E> computeNext() {
            java.util.Map.Entry mapEntry;
            int count;
            do {
               if (!this.mapEntries.hasNext()) {
                  return (Multiset.Entry)this.endOfData();
               }

               mapEntry = (java.util.Map.Entry)this.mapEntries.next();
               count = ((AtomicInteger)mapEntry.getValue()).get();
            } while(count == 0);

            return Multisets.immutableEntry(mapEntry.getKey(), count);
         }
      };
      return new ForwardingIterator<Multiset.Entry<E>>() {
         private Multiset.Entry<E> last;

         protected Iterator<Multiset.Entry<E>> delegate() {
            return readOnlyIterator;
         }

         public Multiset.Entry<E> next() {
            this.last = (Multiset.Entry)super.next();
            return this.last;
         }

         public void remove() {
            CollectPreconditions.checkRemove(this.last != null);
            ConcurrentHashMultiset.this.setCount(this.last.getElement(), 0);
            this.last = null;
         }
      };
   }

   public void clear() {
      this.countMap.clear();
   }

   private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
      stream.writeObject(this.countMap);
   }

   private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      ConcurrentMap<E, Integer> deserializedCountMap = (ConcurrentMap)stream.readObject();
      ConcurrentHashMultiset.FieldSettersHolder.COUNT_MAP_FIELD_SETTER.set(this, deserializedCountMap);
   }

   private class EntrySet extends AbstractMultiset<E>.EntrySet {
      private EntrySet() {
         super();
      }

      ConcurrentHashMultiset<E> multiset() {
         return ConcurrentHashMultiset.this;
      }

      public Object[] toArray() {
         return this.snapshot().toArray();
      }

      public <T> T[] toArray(T[] array) {
         return this.snapshot().toArray(array);
      }

      private List<Multiset.Entry<E>> snapshot() {
         List<Multiset.Entry<E>> list = Lists.newArrayListWithExpectedSize(this.size());
         Iterators.addAll(list, this.iterator());
         return list;
      }

      // $FF: synthetic method
      EntrySet(Object x1) {
         this();
      }
   }

   private static class FieldSettersHolder {
      static final Serialization.FieldSetter<ConcurrentHashMultiset> COUNT_MAP_FIELD_SETTER = Serialization.getFieldSetter(ConcurrentHashMultiset.class, "countMap");
   }
}
