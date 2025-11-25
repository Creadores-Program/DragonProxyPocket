package org.dragonet.proxy.entity.meta.type;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.dragonet.proxy.entity.meta.EntityMetaDataObject;

public class FloatMeta implements EntityMetaDataObject {
   public float data;

   public FloatMeta(float data) {
      this.data = data;
   }

   public int type() {
      return 3;
   }

   public byte[] encode() {
      ByteBuffer buff = ByteBuffer.allocate(4);
      buff.order(ByteOrder.LITTLE_ENDIAN);
      buff.putFloat(this.data);
      return buff.array();
   }
}
