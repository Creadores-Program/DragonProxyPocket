package org.spacehq.opennbt.conversion.builtin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.spacehq.opennbt.conversion.ConverterRegistry;
import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.ListTag;
import org.spacehq.opennbt.tag.builtin.Tag;

public class ListTagConverter implements TagConverter<ListTag, List> {
   public List convert(ListTag tag) {
      List<Object> ret = new ArrayList();
      List<? extends Tag> tags = tag.getValue();
      Iterator var4 = tags.iterator();

      while(var4.hasNext()) {
         Tag t = (Tag)var4.next();
         ret.add(ConverterRegistry.convertToValue(t));
      }

      return ret;
   }

   public ListTag convert(String name, List value) {
      if (value.isEmpty()) {
         throw new IllegalArgumentException("Cannot convert ListTag with size of 0.");
      } else {
         List<Tag> tags = new ArrayList();
         Iterator var4 = value.iterator();

         while(var4.hasNext()) {
            Object o = var4.next();
            tags.add(ConverterRegistry.convertToTag("", o));
         }

         return new ListTag(name, tags);
      }
   }
}
