package io.netty.handler.codec;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

public class DefaultConvertibleHeaders<UnconvertedType, ConvertedType> extends DefaultHeaders<UnconvertedType> implements ConvertibleHeaders<UnconvertedType, ConvertedType> {
   private final ConvertibleHeaders.TypeConverter<UnconvertedType, ConvertedType> typeConverter;

   public DefaultConvertibleHeaders(Comparator<? super UnconvertedType> keyComparator, Comparator<? super UnconvertedType> valueComparator, DefaultHeaders.HashCodeGenerator<UnconvertedType> hashCodeGenerator, Headers.ValueConverter<UnconvertedType> valueConverter, ConvertibleHeaders.TypeConverter<UnconvertedType, ConvertedType> typeConverter) {
      super(keyComparator, valueComparator, hashCodeGenerator, valueConverter);
      this.typeConverter = typeConverter;
   }

   public DefaultConvertibleHeaders(Comparator<? super UnconvertedType> keyComparator, Comparator<? super UnconvertedType> valueComparator, DefaultHeaders.HashCodeGenerator<UnconvertedType> hashCodeGenerator, Headers.ValueConverter<UnconvertedType> valueConverter, ConvertibleHeaders.TypeConverter<UnconvertedType, ConvertedType> typeConverter, DefaultHeaders.NameConverter<UnconvertedType> nameConverter) {
      super(keyComparator, valueComparator, hashCodeGenerator, valueConverter, nameConverter);
      this.typeConverter = typeConverter;
   }

   public ConvertedType getAndConvert(UnconvertedType name) {
      return this.getAndConvert(name, (Object)null);
   }

   public ConvertedType getAndConvert(UnconvertedType name, ConvertedType defaultValue) {
      UnconvertedType v = this.get(name);
      return v == null ? defaultValue : this.typeConverter.toConvertedType(v);
   }

   public ConvertedType getAndRemoveAndConvert(UnconvertedType name) {
      return this.getAndRemoveAndConvert(name, (Object)null);
   }

   public ConvertedType getAndRemoveAndConvert(UnconvertedType name, ConvertedType defaultValue) {
      UnconvertedType v = this.getAndRemove(name);
      return v == null ? defaultValue : this.typeConverter.toConvertedType(v);
   }

   public List<ConvertedType> getAllAndConvert(UnconvertedType name) {
      List<UnconvertedType> all = this.getAll(name);
      List<ConvertedType> allConverted = new ArrayList(all.size());

      for(int i = 0; i < all.size(); ++i) {
         allConverted.add(this.typeConverter.toConvertedType(all.get(i)));
      }

      return allConverted;
   }

   public List<ConvertedType> getAllAndRemoveAndConvert(UnconvertedType name) {
      List<UnconvertedType> all = this.getAllAndRemove(name);
      List<ConvertedType> allConverted = new ArrayList(all.size());

      for(int i = 0; i < all.size(); ++i) {
         allConverted.add(this.typeConverter.toConvertedType(all.get(i)));
      }

      return allConverted;
   }

   public List<Entry<ConvertedType, ConvertedType>> entriesConverted() {
      List<Entry<UnconvertedType, UnconvertedType>> entries = this.entries();
      List<Entry<ConvertedType, ConvertedType>> entriesConverted = new ArrayList(entries.size());

      for(int i = 0; i < entries.size(); ++i) {
         entriesConverted.add(new DefaultConvertibleHeaders.ConvertedEntry((Entry)entries.get(i)));
      }

      return entriesConverted;
   }

   public Iterator<Entry<ConvertedType, ConvertedType>> iteratorConverted() {
      return new DefaultConvertibleHeaders.ConvertedIterator();
   }

   public Set<ConvertedType> namesAndConvert(Comparator<ConvertedType> comparator) {
      Set<UnconvertedType> names = this.names();
      Set<ConvertedType> namesConverted = new TreeSet(comparator);
      Iterator i$ = names.iterator();

      while(i$.hasNext()) {
         UnconvertedType unconverted = i$.next();
         namesConverted.add(this.typeConverter.toConvertedType(unconverted));
      }

      return namesConverted;
   }

   private final class ConvertedEntry implements Entry<ConvertedType, ConvertedType> {
      private final Entry<UnconvertedType, UnconvertedType> entry;
      private ConvertedType name;
      private ConvertedType value;

      ConvertedEntry(Entry<UnconvertedType, UnconvertedType> entry) {
         this.entry = entry;
      }

      public ConvertedType getKey() {
         if (this.name == null) {
            this.name = DefaultConvertibleHeaders.this.typeConverter.toConvertedType(this.entry.getKey());
         }

         return this.name;
      }

      public ConvertedType getValue() {
         if (this.value == null) {
            this.value = DefaultConvertibleHeaders.this.typeConverter.toConvertedType(this.entry.getValue());
         }

         return this.value;
      }

      public ConvertedType setValue(ConvertedType value) {
         ConvertedType old = this.getValue();
         this.entry.setValue(DefaultConvertibleHeaders.this.typeConverter.toUnconvertedType(value));
         return old;
      }

      public String toString() {
         return this.entry.toString();
      }
   }

   private final class ConvertedIterator implements Iterator<Entry<ConvertedType, ConvertedType>> {
      private final Iterator<Entry<UnconvertedType, UnconvertedType>> iter;

      private ConvertedIterator() {
         this.iter = DefaultConvertibleHeaders.this.iterator();
      }

      public boolean hasNext() {
         return this.iter.hasNext();
      }

      public Entry<ConvertedType, ConvertedType> next() {
         Entry<UnconvertedType, UnconvertedType> next = (Entry)this.iter.next();
         return DefaultConvertibleHeaders.this.new ConvertedEntry(next);
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }

      // $FF: synthetic method
      ConvertedIterator(Object x1) {
         this();
      }
   }
}
