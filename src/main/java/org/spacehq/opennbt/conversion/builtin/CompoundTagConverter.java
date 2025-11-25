package org.spacehq.opennbt.conversion.builtin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.spacehq.opennbt.conversion.ConverterRegistry;
import org.spacehq.opennbt.conversion.TagConverter;
import org.spacehq.opennbt.tag.builtin.CompoundTag;
import org.spacehq.opennbt.tag.builtin.Tag;

public class CompoundTagConverter implements TagConverter<CompoundTag, Map> {
   public Map convert(CompoundTag tag) {
      Map<String, Object> ret = new HashMap();
      Map<String, Tag> tags = tag.getValue();
      Iterator var4 = tags.keySet().iterator();

      while(var4.hasNext()) {
         String name = (String)var4.next();
         Tag t = (Tag)tags.get(name);
         ret.put(t.getName(), ConverterRegistry.convertToValue(t));
      }

      return ret;
   }

   public CompoundTag convert(String name, Map value) {
      Map<String, Tag> tags = new HashMap();
      Iterator var4 = value.keySet().iterator();

      while(var4.hasNext()) {
         Object na = var4.next();
         String n = (String)na;
         tags.put(n, ConverterRegistry.convertToTag(n, value.get(n)));
      }

      return new CompoundTag(name, tags);
   }
}
