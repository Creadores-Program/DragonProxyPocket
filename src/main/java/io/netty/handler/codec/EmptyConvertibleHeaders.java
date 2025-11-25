package io.netty.handler.codec;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

public class EmptyConvertibleHeaders<UnconvertedType, ConvertedType> extends EmptyHeaders<UnconvertedType> implements ConvertibleHeaders<UnconvertedType, ConvertedType> {
   public ConvertedType getAndConvert(UnconvertedType name) {
      return null;
   }

   public ConvertedType getAndConvert(UnconvertedType name, ConvertedType defaultValue) {
      return defaultValue;
   }

   public ConvertedType getAndRemoveAndConvert(UnconvertedType name) {
      return null;
   }

   public ConvertedType getAndRemoveAndConvert(UnconvertedType name, ConvertedType defaultValue) {
      return defaultValue;
   }

   public List<ConvertedType> getAllAndConvert(UnconvertedType name) {
      return Collections.emptyList();
   }

   public List<ConvertedType> getAllAndRemoveAndConvert(UnconvertedType name) {
      return Collections.emptyList();
   }

   public List<Entry<ConvertedType, ConvertedType>> entriesConverted() {
      return Collections.emptyList();
   }

   public Iterator<Entry<ConvertedType, ConvertedType>> iteratorConverted() {
      return this.entriesConverted().iterator();
   }

   public Set<ConvertedType> namesAndConvert(Comparator<ConvertedType> comparator) {
      return Collections.emptySet();
   }
}
