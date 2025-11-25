package org.dragonet.proxy.entity.meta;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import org.dragonet.proxy.entity.meta.type.ByteArrayMeta;
import org.dragonet.proxy.entity.meta.type.ByteMeta;
import org.dragonet.proxy.entity.meta.type.ShortMeta;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class EntityMetaData {
   public HashMap<Integer, EntityMetaDataObject> map = new HashMap();

   public void set(int key, EntityMetaDataObject object) {
      this.map.put(key, object);
   }

   public byte[] encode() {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      PEBinaryWriter writer = new PEBinaryWriter(bos);

      try {
         Iterator var3 = this.map.entrySet().iterator();

         while(var3.hasNext()) {
            Entry<Integer, EntityMetaDataObject> entry = (Entry)var3.next();
            writer.writeByte((byte)((((EntityMetaDataObject)entry.getValue()).type() & 255) << 5 | (Integer)entry.getKey() & 31));
            writer.write(((EntityMetaDataObject)entry.getValue()).encode());
         }

         writer.writeByte((byte)127);
         return bos.toByteArray();
      } catch (IOException var5) {
         return null;
      }
   }

   public static EntityMetaData createDefault() {
      EntityMetaData data = new EntityMetaData();
      data.set(0, new ByteMeta((byte)0));
      data.set(1, new ShortMeta((short)300));
      data.set(2, new ByteArrayMeta(""));
      data.set(3, new ByteMeta((byte)1));
      data.set(4, new ByteMeta((byte)0));
      data.set(15, new ByteMeta((byte)0));
      return data;
   }

   public static class Constants {
      public static final int DATA_TYPE_BYTE = 0;
      public static final int DATA_TYPE_SHORT = 1;
      public static final int DATA_TYPE_INT = 2;
      public static final int DATA_TYPE_FLOAT = 3;
      public static final int DATA_TYPE_STRING = 4;
      public static final int DATA_TYPE_SLOT = 5;
      public static final int DATA_TYPE_POS = 6;
      public static final int DATA_TYPE_ROTATION = 7;
      public static final int DATA_TYPE_LONG = 8;
      public static final int DATA_FLAGS = 0;
      public static final int DATA_AIR = 1;
      public static final int DATA_NAMETAG = 2;
      public static final int DATA_SHOW_NAMETAG = 3;
      public static final int DATA_SILENT = 4;
      public static final int DATA_POTION_COLOR = 7;
      public static final int DATA_POTION_VISIBLE = 8;
      public static final int DATA_AGE = 14;
      public static final int DATA_NO_AI = 15;
      public static final int DATA_FLAG_ONFIRE = 0;
      public static final int DATA_FLAG_SNEAKING = 2;
      public static final int DATA_FLAG_RIDING = 4;
      public static final int DATA_FLAG_SPRINTING = 8;
      public static final int DATA_FLAG_ACTION = 16;
      public static final int DATA_FLAG_INVISIBLE = 32;
   }
}
