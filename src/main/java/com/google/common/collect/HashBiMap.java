package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible(
   emulated = true
)
public final class HashBiMap<K, V> extends Maps.IteratorBasedAbstractMap<K, V> implements BiMap<K, V>, Serializable {
   private static final double LOAD_FACTOR = 1.0D;
   private transient HashBiMap.BiEntry<K, V>[] hashTableKToV;
   private transient HashBiMap.BiEntry<K, V>[] hashTableVToK;
   private transient HashBiMap.BiEntry<K, V> firstInKeyInsertionOrder;
   private transient HashBiMap.BiEntry<K, V> lastInKeyInsertionOrder;
   private transient int size;
   private transient int mask;
   private transient int modCount;
   private transient BiMap<V, K> inverse;
   @GwtIncompatible("Not needed in emulated source")
   private static final long serialVersionUID = 0L;

   public static <K, V> HashBiMap<K, V> create() {
      return create(16);
   }

   public static <K, V> HashBiMap<K, V> create(int expectedSize) {
      return new HashBiMap(expectedSize);
   }

   public static <K, V> HashBiMap<K, V> create(Map<? extends K, ? extends V> map) {
      HashBiMap<K, V> bimap = create(map.size());
      bimap.putAll(map);
      return bimap;
   }

   private HashBiMap(int expectedSize) {
      this.init(expectedSize);
   }

   private void init(int expectedSize) {
      CollectPreconditions.checkNonnegative(expectedSize, "expectedSize");
      int tableSize = Hashing.closedTableSize(expectedSize, 1.0D);
      this.hashTableKToV = this.createTable(tableSize);
      this.hashTableVToK = this.createTable(tableSize);
      this.firstInKeyInsertionOrder = null;
      this.lastInKeyInsertionOrder = null;
      this.size = 0;
      this.mask = tableSize - 1;
      this.modCount = 0;
   }

   private void delete(HashBiMap.BiEntry<K, V> entry) {
      int keyBucket = entry.keyHash & this.mask;
      HashBiMap.BiEntry<K, V> prevBucketEntry = null;

      for(HashBiMap.BiEntry bucketEntry = this.hashTableKToV[keyBucket]; bucketEntry != entry; bucketEntry = bucketEntry.nextInKToVBucket) {
         prevBucketEntry = bucketEntry;
      }

      if (prevBucketEntry == null) {
         this.hashTableKToV[keyBucket] = entry.nextInKToVBucket;
      } else {
         prevBucketEntry.nextInKToVBucket = entry.nextInKToVBucket;
      }

      int valueBucket = entry.valueHash & this.mask;
      prevBucketEntry = null;

      for(HashBiMap.BiEntry bucketEntry = this.hashTableVToK[valueBucket]; bucketEntry != entry; bucketEntry = bucketEntry.nextInVToKBucket) {
         prevBucketEntry = bucketEntry;
      }

      if (prevBucketEntry == null) {
         this.hashTableVToK[valueBucket] = entry.nextInVToKBucket;
      } else {
         prevBucketEntry.nextInVToKBucket = entry.nextInVToKBucket;
      }

      if (entry.prevInKeyInsertionOrder == null) {
         this.firstInKeyInsertionOrder = entry.nextInKeyInsertionOrder;
      } else {
         entry.prevInKeyInsertionOrder.nextInKeyInsertionOrder = entry.nextInKeyInsertionOrder;
      }

      if (entry.nextInKeyInsertionOrder == null) {
         this.lastInKeyInsertionOrder = entry.prevInKeyInsertionOrder;
      } else {
         entry.nextInKeyInsertionOrder.prevInKeyInsertionOrder = entry.prevInKeyInsertionOrder;
      }

      --this.size;
      ++this.modCount;
   }

   private void insert(HashBiMap.BiEntry<K, V> entry, @Nullable HashBiMap.BiEntry<K, V> oldEntryForKey) {
      int keyBucket = entry.keyHash & this.mask;
      entry.nextInKToVBucket = this.hashTableKToV[keyBucket];
      this.hashTableKToV[keyBucket] = entry;
      int valueBucket = entry.valueHash & this.mask;
      entry.nextInVToKBucket = this.hashTableVToK[valueBucket];
      this.hashTableVToK[valueBucket] = entry;
      if (oldEntryForKey == null) {
         entry.prevInKeyInsertionOrder = this.lastInKeyInsertionOrder;
         entry.nextInKeyInsertionOrder = null;
         if (this.lastInKeyInsertionOrder == null) {
            this.firstInKeyInsertionOrder = entry;
         } else {
            this.lastInKeyInsertionOrder.nextInKeyInsertionOrder = entry;
         }

         this.lastInKeyInsertionOrder = entry;
      } else {
         entry.prevInKeyInsertionOrder = oldEntryForKey.prevInKeyInsertionOrder;
         if (entry.prevInKeyInsertionOrder == null) {
            this.firstInKeyInsertionOrder = entry;
         } else {
            entry.prevInKeyInsertionOrder.nextInKeyInsertionOrder = entry;
         }

         entry.nextInKeyInsertionOrder = oldEntryForKey.nextInKeyInsertionOrder;
         if (entry.nextInKeyInsertionOrder == null) {
            this.lastInKeyInsertionOrder = entry;
         } else {
            entry.nextInKeyInsertionOrder.prevInKeyInsertionOrder = entry;
         }
      }

      ++this.size;
      ++this.modCount;
   }

   private HashBiMap.BiEntry<K, V> seekByKey(@Nullable Object key, int keyHash) {
      for(HashBiMap.BiEntry entry = this.hashTableKToV[keyHash & this.mask]; entry != null; entry = entry.nextInKToVBucket) {
         if (keyHash == entry.keyHash && Objects.equal(key, entry.key)) {
            return entry;
         }
      }

      return null;
   }

   private HashBiMap.BiEntry<K, V> seekByValue(@Nullable Object value, int valueHash) {
      for(HashBiMap.BiEntry entry = this.hashTableVToK[valueHash & this.mask]; entry != null; entry = entry.nextInVToKBucket) {
         if (valueHash == entry.valueHash && Objects.equal(value, entry.value)) {
            return entry;
         }
      }

      return null;
   }

   public boolean containsKey(@Nullable Object key) {
      return this.seekByKey(key, Hashing.smearedHash(key)) != null;
   }

   public boolean containsValue(@Nullable Object value) {
      return this.seekByValue(value, Hashing.smearedHash(value)) != null;
   }

   @Nullable
   public V get(@Nullable Object key) {
      return Maps.valueOrNull(this.seekByKey(key, Hashing.smearedHash(key)));
   }

   public V put(@Nullable K key, @Nullable V value) {
      return this.put(key, value, false);
   }

   public V forcePut(@Nullable K key, @Nullable V value) {
      return this.put(key, value, true);
   }

   private V put(@Nullable K key, @Nullable V value, boolean force) {
      int keyHash = Hashing.smearedHash(key);
      int valueHash = Hashing.smearedHash(value);
      HashBiMap.BiEntry<K, V> oldEntryForKey = this.seekByKey(key, keyHash);
      if (oldEntryForKey != null && valueHash == oldEntryForKey.valueHash && Objects.equal(value, oldEntryForKey.value)) {
         return value;
      } else {
         HashBiMap.BiEntry<K, V> oldEntryForValue = this.seekByValue(value, valueHash);
         if (oldEntryForValue != null) {
            if (!force) {
               throw new IllegalArgumentException("value already present: " + value);
            }

            this.delete(oldEntryForValue);
         }

         HashBiMap.BiEntry<K, V> newEntry = new HashBiMap.BiEntry(key, keyHash, value, valueHash);
         if (oldEntryForKey != null) {
            this.delete(oldEntryForKey);
            this.insert(newEntry, oldEntryForKey);
            oldEntryForKey.prevInKeyInsertionOrder = null;
            oldEntryForKey.nextInKeyInsertionOrder = null;
            this.rehashIfNecessary();
            return oldEntryForKey.value;
         } else {
            this.insert(newEntry, (HashBiMap.BiEntry)null);
            this.rehashIfNecessary();
            return null;
         }
      }
   }

   @Nullable
   private K putInverse(@Nullable V value, @Nullable K key, boolean force) {
      int valueHash = Hashing.smearedHash(value);
      int keyHash = Hashing.smearedHash(key);
      HashBiMap.BiEntry<K, V> oldEntryForValue = this.seekByValue(value, valueHash);
      if (oldEntryForValue != null && keyHash == oldEntryForValue.keyHash && Objects.equal(key, oldEntryForValue.key)) {
         return key;
      } else {
         HashBiMap.BiEntry<K, V> oldEntryForKey = this.seekByKey(key, keyHash);
         if (oldEntryForKey != null) {
            if (!force) {
               throw new IllegalArgumentException("value already present: " + key);
            }

            this.delete(oldEntryForKey);
         }

         if (oldEntryForValue != null) {
            this.delete(oldEntryForValue);
         }

         HashBiMap.BiEntry<K, V> newEntry = new HashBiMap.BiEntry(key, keyHash, value, valueHash);
         this.insert(newEntry, oldEntryForKey);
         if (oldEntryForKey != null) {
            oldEntryForKey.prevInKeyInsertionOrder = null;
            oldEntryForKey.nextInKeyInsertionOrder = null;
         }

         this.rehashIfNecessary();
         return Maps.keyOrNull(oldEntryForValue);
      }
   }

   private void rehashIfNecessary() {
      HashBiMap.BiEntry<K, V>[] oldKToV = this.hashTableKToV;
      if (Hashing.needsResizing(this.size, oldKToV.length, 1.0D)) {
         int newTableSize = oldKToV.length * 2;
         this.hashTableKToV = this.createTable(newTableSize);
         this.hashTableVToK = this.createTable(newTableSize);
         this.mask = newTableSize - 1;
         this.size = 0;

         for(HashBiMap.BiEntry entry = this.firstInKeyInsertionOrder; entry != null; entry = entry.nextInKeyInsertionOrder) {
            this.insert(entry, entry);
         }

         ++this.modCount;
      }

   }

   private HashBiMap.BiEntry<K, V>[] createTable(int length) {
      return new HashBiMap.BiEntry[length];
   }

   public V remove(@Nullable Object key) {
      HashBiMap.BiEntry<K, V> entry = this.seekByKey(key, Hashing.smearedHash(key));
      if (entry == null) {
         return null;
      } else {
         this.delete(entry);
         entry.prevInKeyInsertionOrder = null;
         entry.nextInKeyInsertionOrder = null;
         return entry.value;
      }
   }

   public void clear() {
      this.size = 0;
      Arrays.fill(this.hashTableKToV, (Object)null);
      Arrays.fill(this.hashTableVToK, (Object)null);
      this.firstInKeyInsertionOrder = null;
      this.lastInKeyInsertionOrder = null;
      ++this.modCount;
   }

   public int size() {
      return this.size;
   }

   public Set<K> keySet() {
      return new HashBiMap.KeySet();
   }

   public Set<V> values() {
      return this.inverse().keySet();
   }

   Iterator<Entry<K, V>> entryIterator() {
      return new HashBiMap<K, V>.Itr<Entry<K, V>>() {
         Entry<K, V> output(HashBiMap.BiEntry<K, V> entry) {
            return new null.MapEntry(entry);
         }

         class MapEntry extends AbstractMapEntry<K, V> {
            HashBiMap.BiEntry<K, V> delegate;

            MapEntry(HashBiMap.BiEntry<K, V> entry) {
               this.delegate = entry;
            }

            public K getKey() {
               return this.delegate.key;
            }

            public V getValue() {
               return this.delegate.value;
            }

            public V setValue(V value) {
               V oldValue = this.delegate.value;
               int valueHash = Hashing.smearedHash(value);
               if (valueHash == this.delegate.valueHash && Objects.equal(value, oldValue)) {
                  return value;
               } else {
                  Preconditions.checkArgument(HashBiMap.this.seekByValue(value, valueHash) == null, "value already present: %s", value);
                  HashBiMap.this.delete(this.delegate);
                  HashBiMap.BiEntry<K, V> newEntry = new HashBiMap.BiEntry(this.delegate.key, this.delegate.keyHash, value, valueHash);
                  HashBiMap.this.insert(newEntry, this.delegate);
                  this.delegate.prevInKeyInsertionOrder = null;
                  this.delegate.nextInKeyInsertionOrder = null;
                  expectedModCount = HashBiMap.this.modCount;
                  if (toRemove == this.delegate) {
                     toRemove = newEntry;
                  }

                  this.delegate = newEntry;
                  return oldValue;
               }
            }
         }
      };
   }

   public BiMap<V, K> inverse() {
      return this.inverse == null ? (this.inverse = new HashBiMap.Inverse()) : this.inverse;
   }

   @GwtIncompatible("java.io.ObjectOutputStream")
   private void writeObject(ObjectOutputStream stream) throws IOException {
      stream.defaultWriteObject();
      Serialization.writeMap(this, stream);
   }

   @GwtIncompatible("java.io.ObjectInputStream")
   private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      this.init(16);
      int size = Serialization.readCount(stream);
      Serialization.populateMap(this, stream, size);
   }

   private static final class InverseSerializedForm<K, V> implements Serializable {
      private final HashBiMap<K, V> bimap;

      InverseSerializedForm(HashBiMap<K, V> bimap) {
         this.bimap = bimap;
      }

      Object readResolve() {
         return this.bimap.inverse();
      }
   }

   private final class Inverse extends AbstractMap<V, K> implements BiMap<V, K>, Serializable {
      private Inverse() {
      }

      BiMap<K, V> forward() {
         return HashBiMap.this;
      }

      public int size() {
         return HashBiMap.this.size;
      }

      public void clear() {
         this.forward().clear();
      }

      public boolean containsKey(@Nullable Object value) {
         return this.forward().containsValue(value);
      }

      public K get(@Nullable Object value) {
         return Maps.keyOrNull(HashBiMap.this.seekByValue(value, Hashing.smearedHash(value)));
      }

      public K put(@Nullable V value, @Nullable K key) {
         return HashBiMap.this.putInverse(value, key, false);
      }

      public K forcePut(@Nullable V value, @Nullable K key) {
         return HashBiMap.this.putInverse(value, key, true);
      }

      public K remove(@Nullable Object value) {
         HashBiMap.BiEntry<K, V> entry = HashBiMap.this.seekByValue(value, Hashing.smearedHash(value));
         if (entry == null) {
            return null;
         } else {
            HashBiMap.this.delete(entry);
            entry.prevInKeyInsertionOrder = null;
            entry.nextInKeyInsertionOrder = null;
            return entry.key;
         }
      }

      public BiMap<K, V> inverse() {
         return this.forward();
      }

      public Set<V> keySet() {
         return new HashBiMap.Inverse.InverseKeySet();
      }

      public Set<K> values() {
         return this.forward().keySet();
      }

      public Set<Entry<V, K>> entrySet() {
         return new Maps.EntrySet<V, K>() {
            Map<V, K> map() {
               return Inverse.this;
            }

            public Iterator<Entry<V, K>> iterator() {
               return new HashBiMap<K, V>.Itr<Entry<V, K>>() {
                  Entry<V, K> output(HashBiMap.BiEntry<K, V> entry) {
                     return new null.InverseEntry(entry);
                  }

                  class InverseEntry extends AbstractMapEntry<V, K> {
                     HashBiMap.BiEntry<K, V> delegate;

                     InverseEntry(HashBiMap.BiEntry<K, V> entry) {
                        this.delegate = entry;
                     }

                     public V getKey() {
                        return this.delegate.value;
                     }

                     public K getValue() {
                        return this.delegate.key;
                     }

                     public K setValue(K key) {
                        K oldKey = this.delegate.key;
                        int keyHash = Hashing.smearedHash(key);
                        if (keyHash == this.delegate.keyHash && Objects.equal(key, oldKey)) {
                           return key;
                        } else {
                           Preconditions.checkArgument(HashBiMap.this.seekByKey(key, keyHash) == null, "value already present: %s", key);
                           HashBiMap.this.delete(this.delegate);
                           HashBiMap.BiEntry<K, V> newEntry = new HashBiMap.BiEntry(key, keyHash, this.delegate.value, this.delegate.valueHash);
                           this.delegate = newEntry;
                           HashBiMap.this.insert(newEntry, (HashBiMap.BiEntry)null);
                           expectedModCount = HashBiMap.this.modCount;
                           return oldKey;
                        }
                     }
                  }
               };
            }
         };
      }

      Object writeReplace() {
         return new HashBiMap.InverseSerializedForm(HashBiMap.this);
      }

      // $FF: synthetic method
      Inverse(Object x1) {
         this();
      }

      private final class InverseKeySet extends Maps.KeySet<V, K> {
         InverseKeySet() {
            super(Inverse.this);
         }

         public boolean remove(@Nullable Object o) {
            HashBiMap.BiEntry<K, V> entry = HashBiMap.this.seekByValue(o, Hashing.smearedHash(o));
            if (entry == null) {
               return false;
            } else {
               HashBiMap.this.delete(entry);
               return true;
            }
         }

         public Iterator<V> iterator() {
            return new HashBiMap<K, V>.Itr<V>() {
               V output(HashBiMap.BiEntry<K, V> entry) {
                  return entry.value;
               }
            };
         }
      }
   }

   private final class KeySet extends Maps.KeySet<K, V> {
      KeySet() {
         super(HashBiMap.this);
      }

      public Iterator<K> iterator() {
         return new HashBiMap<K, V>.Itr<K>() {
            K output(HashBiMap.BiEntry<K, V> entry) {
               return entry.key;
            }
         };
      }

      public boolean remove(@Nullable Object o) {
         HashBiMap.BiEntry<K, V> entry = HashBiMap.this.seekByKey(o, Hashing.smearedHash(o));
         if (entry == null) {
            return false;
         } else {
            HashBiMap.this.delete(entry);
            entry.prevInKeyInsertionOrder = null;
            entry.nextInKeyInsertionOrder = null;
            return true;
         }
      }
   }

   abstract class Itr<T> implements Iterator<T> {
      HashBiMap.BiEntry<K, V> next;
      HashBiMap.BiEntry<K, V> toRemove;
      int expectedModCount;

      Itr() {
         this.next = HashBiMap.this.firstInKeyInsertionOrder;
         this.toRemove = null;
         this.expectedModCount = HashBiMap.this.modCount;
      }

      public boolean hasNext() {
         if (HashBiMap.this.modCount != this.expectedModCount) {
            throw new ConcurrentModificationException();
         } else {
            return this.next != null;
         }
      }

      public T next() {
         if (!this.hasNext()) {
            throw new NoSuchElementException();
         } else {
            HashBiMap.BiEntry<K, V> entry = this.next;
            this.next = entry.nextInKeyInsertionOrder;
            this.toRemove = entry;
            return this.output(entry);
         }
      }

      public void remove() {
         if (HashBiMap.this.modCount != this.expectedModCount) {
            throw new ConcurrentModificationException();
         } else {
            CollectPreconditions.checkRemove(this.toRemove != null);
            HashBiMap.this.delete(this.toRemove);
            this.expectedModCount = HashBiMap.this.modCount;
            this.toRemove = null;
         }
      }

      abstract T output(HashBiMap.BiEntry<K, V> var1);
   }

   private static final class BiEntry<K, V> extends ImmutableEntry<K, V> {
      final int keyHash;
      final int valueHash;
      @Nullable
      HashBiMap.BiEntry<K, V> nextInKToVBucket;
      @Nullable
      HashBiMap.BiEntry<K, V> nextInVToKBucket;
      @Nullable
      HashBiMap.BiEntry<K, V> nextInKeyInsertionOrder;
      @Nullable
      HashBiMap.BiEntry<K, V> prevInKeyInsertionOrder;

      BiEntry(K key, int keyHash, V value, int valueHash) {
         super(key, value);
         this.keyHash = keyHash;
         this.valueHash = valueHash;
      }
   }
}
