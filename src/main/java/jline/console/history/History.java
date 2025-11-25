package jline.console.history;

import java.util.Iterator;
import java.util.ListIterator;

public interface History extends Iterable<History.Entry> {
   int size();

   boolean isEmpty();

   int index();

   void clear();

   CharSequence get(int var1);

   void add(CharSequence var1);

   void set(int var1, CharSequence var2);

   CharSequence remove(int var1);

   CharSequence removeFirst();

   CharSequence removeLast();

   void replace(CharSequence var1);

   ListIterator<History.Entry> entries(int var1);

   ListIterator<History.Entry> entries();

   Iterator<History.Entry> iterator();

   CharSequence current();

   boolean previous();

   boolean next();

   boolean moveToFirst();

   boolean moveToLast();

   boolean moveTo(int var1);

   void moveToEnd();

   public interface Entry {
      int index();

      CharSequence value();
   }
}
