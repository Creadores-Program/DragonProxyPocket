package org.dragonet.proxy.nbt.tag;

import java.io.IOException;
import org.dragonet.proxy.nbt.stream.NBTInputStream;
import org.dragonet.proxy.nbt.stream.NBTOutputStream;

public class EndTag extends Tag {
   public EndTag() {
      super((String)null);
   }

   void load(NBTInputStream dis) throws IOException {
   }

   void write(NBTOutputStream dos) throws IOException {
   }

   public byte getId() {
      return 0;
   }

   public String toString() {
      return "EndTag";
   }

   public Tag copy() {
      return new EndTag();
   }

   public boolean equals(Object obj) {
      return super.equals(obj);
   }
}
