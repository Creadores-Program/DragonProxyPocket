package org.dragonet.proxy.entity.meta.type;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import org.dragonet.proxy.entity.meta.EntityMetaDataObject;

public class ByteArrayMeta implements EntityMetaDataObject {
   public byte[] data;

   public ByteArrayMeta(byte[] data) {
      this.data = data;
   }

   public ByteArrayMeta(String data) {
      this(data.getBytes(StandardCharsets.UTF_8));
   }

   public int type() {
      return 4;
   }

   public byte[] encode() {
      ByteBuffer buff = ByteBuffer.allocate(2 + this.data.length);
      buff.order(ByteOrder.LITTLE_ENDIAN);
      buff.putShort((short)(this.data.length & '\uffff'));
      buff.put(this.data);
      return buff.array();
   }
}
