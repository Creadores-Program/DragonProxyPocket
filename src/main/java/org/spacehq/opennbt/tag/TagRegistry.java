package org.spacehq.opennbt.tag;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import org.spacehq.opennbt.tag.builtin.ByteArrayTag;
import org.spacehq.opennbt.tag.builtin.ByteTag;
import org.spacehq.opennbt.tag.builtin.CompoundTag;
import org.spacehq.opennbt.tag.builtin.DoubleTag;
import org.spacehq.opennbt.tag.builtin.FloatTag;
import org.spacehq.opennbt.tag.builtin.IntArrayTag;
import org.spacehq.opennbt.tag.builtin.IntTag;
import org.spacehq.opennbt.tag.builtin.ListTag;
import org.spacehq.opennbt.tag.builtin.LongTag;
import org.spacehq.opennbt.tag.builtin.ShortTag;
import org.spacehq.opennbt.tag.builtin.StringTag;
import org.spacehq.opennbt.tag.builtin.Tag;
import org.spacehq.opennbt.tag.builtin.custom.DoubleArrayTag;
import org.spacehq.opennbt.tag.builtin.custom.FloatArrayTag;
import org.spacehq.opennbt.tag.builtin.custom.LongArrayTag;
import org.spacehq.opennbt.tag.builtin.custom.SerializableArrayTag;
import org.spacehq.opennbt.tag.builtin.custom.SerializableTag;
import org.spacehq.opennbt.tag.builtin.custom.ShortArrayTag;
import org.spacehq.opennbt.tag.builtin.custom.StringArrayTag;

public class TagRegistry {
   private static final Map<Integer, Class<? extends Tag>> idToTag = new HashMap();
   private static final Map<Class<? extends Tag>, Integer> tagToId = new HashMap();

   public static void register(int id, Class<? extends Tag> tag) throws TagRegisterException {
      if (idToTag.containsKey(id)) {
         throw new TagRegisterException("Tag ID \"" + id + "\" is already in use.");
      } else if (tagToId.containsKey(tag)) {
         throw new TagRegisterException("Tag \"" + tag.getSimpleName() + "\" is already registered.");
      } else {
         idToTag.put(id, tag);
         tagToId.put(tag, id);
      }
   }

   public static Class<? extends Tag> getClassFor(int id) {
      return !idToTag.containsKey(id) ? null : (Class)idToTag.get(id);
   }

   public static int getIdFor(Class<? extends Tag> clazz) {
      return !tagToId.containsKey(clazz) ? -1 : (Integer)tagToId.get(clazz);
   }

   public static Tag createInstance(int id, String tagName) throws TagCreateException {
      Class<? extends Tag> clazz = (Class)idToTag.get(id);
      if (clazz == null) {
         throw new TagCreateException("Could not find tag with ID \"" + id + "\".");
      } else {
         try {
            Constructor<? extends Tag> constructor = clazz.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            return (Tag)constructor.newInstance(tagName);
         } catch (Exception var4) {
            throw new TagCreateException("Failed to create instance of tag \"" + clazz.getSimpleName() + "\".", var4);
         }
      }
   }

   static {
      register(1, ByteTag.class);
      register(2, ShortTag.class);
      register(3, IntTag.class);
      register(4, LongTag.class);
      register(5, FloatTag.class);
      register(6, DoubleTag.class);
      register(7, ByteArrayTag.class);
      register(8, StringTag.class);
      register(9, ListTag.class);
      register(10, CompoundTag.class);
      register(11, IntArrayTag.class);
      register(60, DoubleArrayTag.class);
      register(61, FloatArrayTag.class);
      register(62, LongArrayTag.class);
      register(63, SerializableArrayTag.class);
      register(64, SerializableTag.class);
      register(65, ShortArrayTag.class);
      register(66, StringArrayTag.class);
   }
}
