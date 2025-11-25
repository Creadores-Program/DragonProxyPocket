package org.dragonet.proxy.entity.meta.type;

import org.dragonet.proxy.entity.meta.EntityMetaDataObject;

public class ByteMeta implements EntityMetaDataObject {
   public byte data;

   public ByteMeta(byte data) {
      this.data = data;
   }

   public int type() {
      return 0;
   }

   public byte[] encode() {
      return new byte[]{this.data};
   }
}
