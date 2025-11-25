package org.dragonet.proxy.entity.meta.type;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.dragonet.proxy.entity.meta.EntityMetaDataObject;

public class CoordinateMeta implements EntityMetaDataObject {
   public int data1;
   public int data2;
   public int data3;

   public CoordinateMeta(int data1, int data2, int data3) {
      this.data1 = data1;
      this.data2 = data2;
      this.data3 = data3;
   }

   public int type() {
      return 6;
   }

   public byte[] encode() {
      ByteBuffer buff = ByteBuffer.allocate(12);
      buff.order(ByteOrder.LITTLE_ENDIAN);
      buff.putInt(this.data1);
      buff.putInt(this.data2);
      buff.putInt(this.data3);
      return buff.array();
   }
}
