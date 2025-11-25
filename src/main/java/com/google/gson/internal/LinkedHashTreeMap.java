package com.google.gson.internal;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;

public final class LinkedHashTreeMap<K, V> extends AbstractMap<K, V> implements Serializable {
   private static final Comparator<Comparable> NATURAL_ORDER = new Comparator<Comparable>() {
      public int compare(Comparable a, Comparable b) {
         return a.compareTo(b);
      }
   };
   Comparator<? super K> comparator;
   LinkedHashTreeMap.Node<K, V>[] table;
   final LinkedHashTreeMap.Node<K, V> header;
   int size;
   int modCount;
   int threshold;
   private LinkedHashTreeMap<K, V>.EntrySet entrySet;
   private LinkedHashTreeMap<K, V>.KeySet keySet;

   public LinkedHashTreeMap() {
      this(NATURAL_ORDER);
   }

   public LinkedHashTreeMap(Comparator<? super K> comparator) {
      this.size = 0;
      this.modCount = 0;
      this.comparator = comparator != null ? comparator : NATURAL_ORDER;
      this.header = new LinkedHashTreeMap.Node();
      this.table = new LinkedHashTreeMap.Node[16];
      this.threshold = this.table.length / 2 + this.table.length / 4;
   }

   public int size() {
      return this.size;
   }

   public V get(Object key) {
      LinkedHashTreeMap.Node<K, V> node = this.findByObject(key);
      return node != null ? node.value : null;
   }

   public boolean containsKey(Object key) {
      return this.findByObject(key) != null;
   }

   public V put(K key, V value) {
      if (key == null) {
         throw new NullPointerException("key == null");
      } else {
         LinkedHashTreeMap.Node<K, V> created = this.find(key, true);
         V result = created.value;
         created.value = value;
         return result;
      }
   }

   public void clear() {
      Arrays.fill(this.table, (Object)null);
      this.size = 0;
      ++this.modCount;
      LinkedHashTreeMap.Node<K, V> header = this.header;

      LinkedHashTreeMap.Node next;
      for(LinkedHashTreeMap.Node e = header.next; e != header; e = next) {
         next = e.next;
         e.next = e.prev = null;
      }

      header.next = header.prev = header;
   }

   public V remove(Object key) {
      LinkedHashTreeMap.Node<K, V> node = this.removeInternalByKey(key);
      return node != null ? node.value : null;
   }

   LinkedHashTreeMap.Node<K, V> find(K key, boolean create) {
      Comparator<? super K> comparator = this.comparator;
      LinkedHashTreeMap.Node<K, V>[] table = this.table;
      int hash = secondaryHash(key.hashCode());
      int index = hash & table.length - 1;
      LinkedHashTreeMap.Node<K, V> nearest = table[index];
      int comparison = 0;
      LinkedHashTreeMap.Node created;
      if (nearest != null) {
         Comparable comparableKey = comparator == NATURAL_ORDER ? (Comparable)key : null;

         while(true) {
            comparison = comparableKey != null ? comparableKey.compareTo(nearest.key) : comparator.compare(key, nearest.key);
            if (comparison == 0) {
               return nearest;
            }

            created = comparison < 0 ? nearest.left : nearest.right;
            if (created == null) {
               break;
            }

            nearest = created;
         }
      }

      if (!create) {
         return null;
      } else {
         LinkedHashTreeMap.Node<K, V> header = this.header;
         if (nearest == null) {
            if (comparator == NATURAL_ORDER && !(key instanceof Comparable)) {
               throw new ClassCastException(key.getClass().getName() + " is not Comparable");
            }

            created = new LinkedHashTreeMap.Node(nearest, key, hash, header, header.prev);
            table[index] = created;
         } else {
            created = new LinkedHashTreeMap.Node(nearest, key, hash, header, header.prev);
            if (comparison < 0) {
               nearest.left = created;
            } else {
               nearest.right = created;
            }

            this.rebalance(nearest, true);
         }

         if (this.size++ > this.threshold) {
            this.doubleCapacity();
         }

         ++this.modCount;
         return created;
      }
   }

   LinkedHashTreeMap.Node<K, V> findByObject(Object key) {
      try {
         return key != null ? this.find(key, false) : null;
      } catch (ClassCastException var3) {
         return null;
      }
   }

   LinkedHashTreeMap.Node<K, V> findByEntry(Entry<?, ?> entry) {
      LinkedHashTreeMap.Node<K, V> mine = this.findByObject(entry.getKey());
      boolean valuesEqual = mine != null && this.equal(mine.value, entry.getValue());
      return valuesEqual ? mine : null;
   }

   private boolean equal(Object a, Object b) {
      return a == b || a != null && a.equals(b);
   }

   private static int secondaryHash(int h) {
      h ^= h >>> 20 ^ h >>> 12;
      return h ^ h >>> 7 ^ h >>> 4;
   }

   void removeInternal(LinkedHashTreeMap.Node<K, V> node, boolean unlink) {
      if (unlink) {
         node.prev.next = node.next;
         node.next.prev = node.prev;
         node.next = node.prev = null;
      }

      LinkedHashTreeMap.Node<K, V> left = node.left;
      LinkedHashTreeMap.Node<K, V> right = node.right;
      LinkedHashTreeMap.Node<K, V> originalParent = node.parent;
      if (left != null && right != null) {
         LinkedHashTreeMap.Node<K, V> adjacent = left.height > right.height ? left.last() : right.first();
         this.removeInternal(adjacent, false);
         int leftHeight = 0;
         left = node.left;
         if (left != null) {
            leftHeight = left.height;
            adjacent.left = left;
            left.parent = adjacent;
            node.left = null;
         }

         int rightHeight = 0;
         right = node.right;
         if (right != null) {
            rightHeight = right.height;
            adjacent.right = right;
            right.parent = adjacent;
            node.right = null;
         }

         adjacent.height = Math.max(leftHeight, rightHeight) + 1;
         this.replaceInParent(node, adjacent);
      } else {
         if (left != null) {
            this.replaceInParent(node, left);
            node.left = null;
         } else if (right != null) {
            this.replaceInParent(node, right);
            node.right = null;
         } else {
            this.replaceInParent(node, (LinkedHashTreeMap.Node)null);
         }

         this.rebalance(originalParent, false);
         --this.size;
         ++this.modCount;
      }
   }

   LinkedHashTreeMap.Node<K, V> removeInternalByKey(Object key) {
      LinkedHashTreeMap.Node<K, V> node = this.findByObject(key);
      if (node != null) {
         this.removeInternal(node, true);
      }

      return node;
   }

   private void replaceInParent(LinkedHashTreeMap.Node<K, V> node, LinkedHashTreeMap.Node<K, V> replacement) {
      LinkedHashTreeMap.Node<K, V> parent = node.parent;
      node.parent = null;
      if (replacement != null) {
         replacement.parent = parent;
      }

      if (parent != null) {
         if (parent.left == node) {
            parent.left = replacement;
         } else {
            assert parent.right == node;

            parent.right = replacement;
         }
      } else {
         int index = node.hash & this.table.length - 1;
         this.table[index] = replacement;
      }

   }

   private void rebalance(LinkedHashTreeMap.Node<K, V> unbalanced, boolean insert) {
      for(LinkedHashTreeMap.Node node = unbalanced; node != null; node = node.parent) {
         LinkedHashTreeMap.Node<K, V> left = node.left;
         LinkedHashTreeMap.Node<K, V> right = node.right;
         int leftHeight = left != null ? left.height : 0;
         int rightHeight = right != null ? right.height : 0;
         int delta = leftHeight - rightHeight;
         LinkedHashTreeMap.Node leftLeft;
         LinkedHashTreeMap.Node leftRight;
         int leftRightHeight;
         int leftLeftHeight;
         int leftDelta;
         if (delta == -2) {
            leftLeft = right.left;
            leftRight = right.right;
            leftRightHeight = leftRight != null ? leftRight.height : 0;
            leftLeftHeight = leftLeft != null ? leftLeft.height : 0;
            leftDelta = leftLeftHeight - leftRightHeight;
            if (leftDelta != -1 && (leftDelta != 0 || insert)) {
               assert leftDelta == 1;

               this.rotateRight(right);
               this.rotateLeft(node);
            } else {
               this.rotateLeft(node);
            }

            if (insert) {
               break;
            }
         } else if (delta == 2) {
            leftLeft = left.left;
            leftRight = left.right;
            leftRightHeight = leftRight != null ? leftRight.height : 0;
            leftLeftHeight = leftLeft != null ? leftLeft.height : 0;
            leftDelta = leftLeftHeight - leftRightHeight;
            if (leftDelta == 1 || leftDelta == 0 && !insert) {
               this.rotateRight(node);
            } else {
               assert leftDelta == -1;

               this.rotateLeft(left);
               this.rotateRight(node);
            }

            if (insert) {
               break;
            }
         } else if (delta == 0) {
            node.height = leftHeight + 1;
            if (insert) {
               break;
            }
         } else {
            assert delta == -1 || delta == 1;

            node.height = Math.max(leftHeight, rightHeight) + 1;
            if (!insert) {
               break;
            }
         }
      }

   }

   private void rotateLeft(LinkedHashTreeMap.Node<K, V> root) {
      LinkedHashTreeMap.Node<K, V> left = root.left;
      LinkedHashTreeMap.Node<K, V> pivot = root.right;
      LinkedHashTreeMap.Node<K, V> pivotLeft = pivot.left;
      LinkedHashTreeMap.Node<K, V> pivotRight = pivot.right;
      root.right = pivotLeft;
      if (pivotLeft != null) {
         pivotLeft.parent = root;
      }

      this.replaceInParent(root, pivot);
      pivot.left = root;
      root.parent = pivot;
      root.height = Math.max(left != null ? left.height : 0, pivotLeft != null ? pivotLeft.height : 0) + 1;
      pivot.height = Math.max(root.height, pivotRight != null ? pivotRight.height : 0) + 1;
   }

   private void rotateRight(LinkedHashTreeMap.Node<K, V> root) {
      LinkedHashTreeMap.Node<K, V> pivot = root.left;
      LinkedHashTreeMap.Node<K, V> right = root.right;
      LinkedHashTreeMap.Node<K, V> pivotLeft = pivot.left;
      LinkedHashTreeMap.Node<K, V> pivotRight = pivot.right;
      root.left = pivotRight;
      if (pivotRight != null) {
         pivotRight.parent = root;
      }

      this.replaceInParent(root, pivot);
      pivot.right = root;
      root.parent = pivot;
      root.height = Math.max(right != null ? right.height : 0, pivotRight != null ? pivotRight.height : 0) + 1;
      pivot.height = Math.max(root.height, pivotLeft != null ? pivotLeft.height : 0) + 1;
   }

   public Set<Entry<K, V>> entrySet() {
      LinkedHashTreeMap<K, V>.EntrySet result = this.entrySet;
      return result != null ? result : (this.entrySet = new LinkedHashTreeMap.EntrySet());
   }

   public Set<K> keySet() {
      LinkedHashTreeMap<K, V>.KeySet result = this.keySet;
      return result != null ? result : (this.keySet = new LinkedHashTreeMap.KeySet());
   }

   private void doubleCapacity() {
      this.table = doubleCapacity(this.table);
      this.threshold = this.table.length / 2 + this.table.length / 4;
   }

   static <K, V> LinkedHashTreeMap.Node<K, V>[] doubleCapacity(LinkedHashTreeMap.Node<K, V>[] oldTable) {
      int oldCapacity = oldTable.length;
      LinkedHashTreeMap.Node<K, V>[] newTable = new LinkedHashTreeMap.Node[oldCapacity * 2];
      LinkedHashTreeMap.AvlIterator<K, V> iterator = new LinkedHashTreeMap.AvlIterator();
      LinkedHashTreeMap.AvlBuilder<K, V> leftBuilder = new LinkedHashTreeMap.AvlBuilder();
      LinkedHashTreeMap.AvlBuilder<K, V> rightBuilder = new LinkedHashTreeMap.AvlBuilder();

      for(int i = 0; i < oldCapacity; ++i) {
         LinkedHashTreeMap.Node<K, V> root = oldTable[i];
         if (root != null) {
            iterator.reset(root);
            int leftSize = 0;
            int rightSize = 0;

            LinkedHashTreeMap.Node node;
            while((node = iterator.next()) != null) {
               if ((node.hash & oldCapacity) == 0) {
                  ++leftSize;
               } else {
                  ++rightSize;
               }
            }

            leftBuilder.reset(leftSize);
            rightBuilder.reset(rightSize);
            iterator.reset(root);

            while((node = iterator.next()) != null) {
               if ((node.hash & oldCapacity) == 0) {
                  leftBuilder.add(node);
               } else {
                  rightBuilder.add(node);
               }
            }

            newTable[i] = leftSize > 0 ? leftBuilder.root() : null;
            newTable[i + oldCapacity] = rightSize > 0 ? rightBuilder.root() : null;
         }
      }

      return newTable;
   }

   private Object writeReplace() throws ObjectStreamException {
      return new LinkedHashMap(this);
   }

   final class KeySet extends AbstractSet<K> {
      public int size() {
         return LinkedHashTreeMap.this.size;
      }

      public Iterator<K> iterator() {
         return new LinkedHashTreeMap<K, V>.LinkedTreeMapIterator<K>() {
            public K next() {
               return this.nextNode().key;
            }
         };
      }

      public boolean contains(Object o) {
         return LinkedHashTreeMap.this.containsKey(o);
      }

      public boolean remove(Object key) {
         return LinkedHashTreeMap.this.removeInternalByKey(key) != null;
      }

      public void clear() {
         LinkedHashTreeMap.this.clear();
      }
   }

   final class EntrySet extends AbstractSet<Entry<K, V>> {
      public int size() {
         return LinkedHashTreeMap.this.size;
      }

      public Iterator<Entry<K, V>> iterator() {
         return new LinkedHashTreeMap<K, V>.LinkedTreeMapIterator<Entry<K, V>>() {
            public Entry<K, V> next() {
               return this.nextNode();
            }
         };
      }

      public boolean contains(Object o) {
         return o instanceof Entry && LinkedHashTreeMap.this.findByEntry((Entry)o) != null;
      }

      public boolean remove(Object o) {
         if (!(o instanceof Entry)) {
            return false;
         } else {
            LinkedHashTreeMap.Node<K, V> node = LinkedHashTreeMap.this.findByEntry((Entry)o);
            if (node == null) {
               return false;
            } else {
               LinkedHashTreeMap.this.removeInternal(node, true);
               return true;
            }
         }
      }

      public void clear() {
         LinkedHashTreeMap.this.clear();
      }
   }

   private abstract class LinkedTreeMapIterator<T> implements Iterator<T> {
      LinkedHashTreeMap.Node<K, V> next;
      LinkedHashTreeMap.Node<K, V> lastReturned;
      int expectedModCount;

      private LinkedTreeMapIterator() {
         this.next = LinkedHashTreeMap.this.header.next;
         this.lastReturned = null;
         this.expectedModCount = LinkedHashTreeMap.this.modCount;
      }

      public final boolean hasNext() {
         return this.next != LinkedHashTreeMap.this.header;
      }

      final LinkedHashTreeMap.Node<K, V> nextNode() {
         LinkedHashTreeMap.Node<K, V> e = this.next;
         if (e == LinkedHashTreeMap.this.header) {
            throw new NoSuchElementException();
         } else if (LinkedHashTreeMap.this.modCount != this.expectedModCount) {
            throw new ConcurrentModificationException();
         } else {
            this.next = e.next;
            return this.lastReturned = e;
         }
      }

      public final void remove() {
         if (this.lastReturned == null) {
            throw new IllegalStateException();
         } else {
            LinkedHashTreeMap.this.removeInternal(this.lastReturned, true);
            this.lastReturned = null;
            this.expectedModCount = LinkedHashTreeMap.this.modCount;
         }
      }

      // $FF: synthetic method
      LinkedTreeMapIterator(Object x1) {
         this();
      }
   }

   static final class AvlBuilder<K, V> {
      private LinkedHashTreeMap.Node<K, V> stack;
      private int leavesToSkip;
      private int leavesSkipped;
      private int size;

      void reset(int targetSize) {
         int treeCapacity = Integer.highestOneBit(targetSize) * 2 - 1;
         this.leavesToSkip = treeCapacity - targetSize;
         this.size = 0;
         this.leavesSkipped = 0;
         this.stack = null;
      }

      void add(LinkedHashTreeMap.Node<K, V> node) {
         node.left = node.parent = node.right = null;
         node.height = 1;
         if (this.leavesToSkip > 0 && (this.size & 1) == 0) {
            ++this.size;
            --this.leavesToSkip;
            ++this.leavesSkipped;
         }

         node.parent = this.stack;
         this.stack = node;
         ++this.size;
         if (this.leavesToSkip > 0 && (this.size & 1) == 0) {
            ++this.size;
            --this.leavesToSkip;
            ++this.leavesSkipped;
         }

         for(int scale = 4; (this.size & scale - 1) == scale - 1; scale *= 2) {
            LinkedHashTreeMap.Node right;
            LinkedHashTreeMap.Node center;
            if (this.leavesSkipped == 0) {
               right = this.stack;
               center = right.parent;
               LinkedHashTreeMap.Node<K, V> left = center.parent;
               center.parent = left.parent;
               this.stack = center;
               center.left = left;
               center.right = right;
               center.height = right.height + 1;
               left.parent = center;
               right.parent = center;
            } else if (this.leavesSkipped == 1) {
               right = this.stack;
               center = right.parent;
               this.stack = center;
               center.right = right;
               center.height = right.height + 1;
               right.parent = center;
               this.leavesSkipped = 0;
            } else if (this.leavesSkipped == 2) {
               this.leavesSkipped = 0;
            }
         }

      }

      LinkedHashTreeMap.Node<K, V> root() {
         LinkedHashTreeMap.Node<K, V> stackTop = this.stack;
         if (stackTop.parent != null) {
            throw new IllegalStateException();
         } else {
            return stackTop;
         }
      }
   }

   static class AvlIterator<K, V> {
      private LinkedHashTreeMap.Node<K, V> stackTop;

      void reset(LinkedHashTreeMap.Node<K, V> root) {
         LinkedHashTreeMap.Node<K, V> stackTop = null;

         for(LinkedHashTreeMap.Node n = root; n != null; n = n.left) {
            n.parent = stackTop;
            stackTop = n;
         }

         this.stackTop = stackTop;
      }

      public LinkedHashTreeMap.Node<K, V> next() {
         LinkedHashTreeMap.Node<K, V> stackTop = this.stackTop;
         if (stackTop == null) {
            return null;
         } else {
            LinkedHashTreeMap.Node<K, V> result = stackTop;
            stackTop = stackTop.parent;
            result.parent = null;

            for(LinkedHashTreeMap.Node n = result.right; n != null; n = n.left) {
               n.parent = stackTop;
               stackTop = n;
            }

            this.stackTop = stackTop;
            return result;
         }
      }
   }

   static final class Node<K, V> implements Entry<K, V> {
      LinkedHashTreeMap.Node<K, V> parent;
      LinkedHashTreeMap.Node<K, V> left;
      LinkedHashTreeMap.Node<K, V> right;
      LinkedHashTreeMap.Node<K, V> next;
      LinkedHashTreeMap.Node<K, V> prev;
      final K key;
      final int hash;
      V value;
      int height;

      Node() {
         this.key = null;
         this.hash = -1;
         this.next = this.prev = this;
      }

      Node(LinkedHashTreeMap.Node<K, V> parent, K key, int hash, LinkedHashTreeMap.Node<K, V> next, LinkedHashTreeMap.Node<K, V> prev) {
         this.parent = parent;
         this.key = key;
         this.hash = hash;
         this.height = 1;
         this.next = next;
         this.prev = prev;
         prev.next = this;
         next.prev = this;
      }

      public K getKey() {
         return this.key;
      }

      public V getValue() {
         return this.value;
      }

      public V setValue(V value) {
         V oldValue = this.value;
         this.value = value;
         return oldValue;
      }

      public boolean equals(Object o) {
         if (!(o instanceof Entry)) {
            return false;
         } else {
            boolean var10000;
            label38: {
               label27: {
                  Entry other = (Entry)o;
                  if (this.key == null) {
                     if (other.getKey() != null) {
                        break label27;
                     }
                  } else if (!this.key.equals(other.getKey())) {
                     break label27;
                  }

                  if (this.value == null) {
                     if (other.getValue() == null) {
                        break label38;
                     }
                  } else if (this.value.equals(other.getValue())) {
                     break label38;
                  }
               }

               var10000 = false;
               return var10000;
            }

            var10000 = true;
            return var10000;
         }
      }

      public int hashCode() {
         return (this.key == null ? 0 : this.key.hashCode()) ^ (this.value == null ? 0 : this.value.hashCode());
      }

      public String toString() {
         return this.key + "=" + this.value;
      }

      public LinkedHashTreeMap.Node<K, V> first() {
         LinkedHashTreeMap.Node<K, V> node = this;

         for(LinkedHashTreeMap.Node child = this.left; child != null; child = child.left) {
            node = child;
         }

         return node;
      }

      public LinkedHashTreeMap.Node<K, V> last() {
         LinkedHashTreeMap.Node<K, V> node = this;

         for(LinkedHashTreeMap.Node child = this.right; child != null; child = child.right) {
            node = child;
         }

         return node;
      }
   }
}
