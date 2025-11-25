package org.dragonet.proxy.network.translator;

import org.dragonet.proxy.entity.EntityType;
import org.dragonet.proxy.entity.meta.EntityMetaData;
import org.dragonet.proxy.entity.meta.type.ByteArrayMeta;
import org.dragonet.proxy.entity.meta.type.ByteMeta;
import org.dragonet.proxy.entity.meta.type.ShortMeta;
import org.spacehq.mc.protocol.data.game.EntityMetadata;
import org.spacehq.mc.protocol.data.game.values.entity.MetadataType;

public final class EntityMetaTranslator {
   public static EntityMetaData translateToPE(EntityMetadata[] pcMeta, EntityType type) {
      EntityMetaData peMeta = EntityMetaData.createDefault();
      EntityMetadata[] var3 = pcMeta;
      int var4 = pcMeta.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         EntityMetadata m = var3[var5];
         if (m != null) {
            switch(m.getId()) {
            case 0:
               byte pcFlags = (Byte)m.getValue();
               byte peFlags = (byte)((pcFlags & 1) > 0 ? 0 : 0);
               peFlags = (byte)(peFlags | ((pcFlags & 2) > 0 ? 2 : 0));
               peFlags = (byte)(peFlags | ((pcFlags & 8) > 0 ? 8 : 0));
               peFlags = (byte)(peFlags | ((pcFlags & 16) > 0 ? 16 : 0));
               peFlags = (byte)(peFlags | ((pcFlags & 32) > 0 ? 32 : 0));
               peMeta.map.put(0, new ByteMeta(peFlags));
               break;
            case 1:
               peMeta.map.put(1, new ShortMeta((Short)m.getValue()));
               break;
            case 2:
               peMeta.map.put(2, new ByteArrayMeta((String)m.getValue()));
               break;
            case 3:
               byte data;
               if (m.getType() == MetadataType.BYTE) {
                  data = (Byte)m.getValue();
               } else if (m.getType() == MetadataType.INT) {
                  data = (byte)((Integer)m.getValue() & 255);
               } else {
                  data = 1;
               }

               peMeta.map.put(3, new ByteMeta(data));
            case 4:
            case 5:
            case 6:
            case 9:
            case 10:
            case 11:
            case 13:
            case 14:
            case 16:
            default:
               break;
            case 7:
               peMeta.map.put(7, new ByteMeta((byte)((Integer)m.getValue() & 255)));
               break;
            case 8:
               peMeta.map.put(8, new ByteMeta((Byte)m.getValue()));
               break;
            case 12:
               byte age;
               if (m.getType() == MetadataType.BYTE) {
                  age = (Byte)m.getValue();
               } else if (m.getType() == MetadataType.INT) {
                  age = (byte)((Integer)m.getValue() & 255);
               } else {
                  age = 0;
               }

               peMeta.map.put(14, new ByteMeta((byte)(age <= 0 ? 1 : 0)));
               break;
            case 15:
               peMeta.map.put(15, new ByteMeta((Byte)m.getValue()));
            }
         }
      }

      return peMeta;
   }
}
