package org.dragonet.proxy.entity.meta.type;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.dragonet.proxy.entity.meta.EntityMetaDataObject;

public class ShortMeta implements EntityMetaDataObject {
   public short data;

   public ShortMeta(short data) {
      this.data = data;
   }

   public int type() {
      return 1;
   }

   public byte[] encode() {
      ByteBuffer buff = ByteBuffer.allocate(2);
      buff.order(ByteOrder.LITTLE_ENDIAN);
      buff.putShort(this.data);
      return buff.array();
   }
}
