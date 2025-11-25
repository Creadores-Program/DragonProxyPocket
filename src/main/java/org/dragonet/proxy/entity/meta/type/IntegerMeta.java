package org.dragonet.proxy.entity.meta.type;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.dragonet.proxy.entity.meta.EntityMetaDataObject;

public class IntegerMeta implements EntityMetaDataObject {
   public int data;

   public IntegerMeta(int data) {
      this.data = data;
   }

   public int type() {
      return 2;
   }

   public byte[] encode() {
      ByteBuffer buff = ByteBuffer.allocate(4);
      buff.order(ByteOrder.LITTLE_ENDIAN);
      buff.putInt(this.data);
      return buff.array();
   }
}
