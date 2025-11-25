package org.spacehq.opennbt.conversion;

import org.spacehq.opennbt.tag.builtin.Tag;

public interface TagConverter<T extends Tag, V> {
   V convert(T var1);

   T convert(String var1, V var2);
}
