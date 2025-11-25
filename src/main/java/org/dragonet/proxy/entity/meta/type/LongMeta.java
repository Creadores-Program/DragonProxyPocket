package org.dragonet.proxy.entity.meta.type;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.dragonet.proxy.entity.meta.EntityMetaDataObject;

public class LongMeta implements EntityMetaDataObject {
   public long data;

   public LongMeta(long data) {
      this.data = data;
   }

   public int type() {
      return 8;
   }

   public byte[] encode() {
      ByteBuffer buff = ByteBuffer.allocate(8);
      buff.order(ByteOrder.LITTLE_ENDIAN);
      buff.putLong(this.data);
      return buff.array();
   }
}
