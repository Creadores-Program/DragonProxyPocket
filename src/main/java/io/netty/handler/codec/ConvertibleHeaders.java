package io.netty.handler.codec;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

public interface ConvertibleHeaders<UnconvertedType, ConvertedType> extends Headers<UnconvertedType> {
   ConvertedType getAndConvert(UnconvertedType var1);

   ConvertedType getAndConvert(UnconvertedType var1, ConvertedType var2);

   ConvertedType getAndRemoveAndConvert(UnconvertedType var1);

   ConvertedType getAndRemoveAndConvert(UnconvertedType var1, ConvertedType var2);

   List<ConvertedType> getAllAndConvert(UnconvertedType var1);

   List<ConvertedType> getAllAndRemoveAndConvert(UnconvertedType var1);

   List<Entry<ConvertedType, ConvertedType>> entriesConverted();

   Iterator<Entry<ConvertedType, ConvertedType>> iteratorConverted();

   Set<ConvertedType> namesAndConvert(Comparator<ConvertedType> var1);

   public interface TypeConverter<UnconvertedType, ConvertedType> {
      ConvertedType toConvertedType(UnconvertedType var1);

      UnconvertedType toUnconvertedType(ConvertedType var1);
   }
}
