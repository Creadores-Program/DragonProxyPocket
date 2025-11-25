package org.dragonet.proxy.entity.meta.type;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.dragonet.proxy.entity.meta.EntityMetaDataObject;

public class SlotMeta implements EntityMetaDataObject {
   public short data1;
   public byte data2;
   public short data3;

   public SlotMeta(short data1, byte data2, short data3) {
      this.data1 = data1;
      this.data2 = data2;
      this.data3 = data3;
   }

   public int type() {
      return 5;
   }

   public byte[] encode() {
      ByteBuffer buff = ByteBuffer.allocate(5);
      buff.order(ByteOrder.LITTLE_ENDIAN);
      buff.putShort(this.data1);
      buff.put(this.data2);
      buff.putShort(this.data3);
      return buff.array();
   }
}
