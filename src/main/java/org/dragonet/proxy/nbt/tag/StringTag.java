package org.dragonet.proxy.nbt.tag;

import java.io.IOException;
import org.dragonet.proxy.nbt.stream.NBTInputStream;
import org.dragonet.proxy.nbt.stream.NBTOutputStream;

public class StringTag extends Tag {
   public String data;

   public StringTag(String name) {
      super(name);
   }

   public StringTag(String name, String data) {
      super(name);
      this.data = data;
      if (data == null) {
         throw new IllegalArgumentException("Empty string not allowed");
      }
   }

   void write(NBTOutputStream dos) throws IOException {
      dos.writeUTF(this.data);
   }

   void load(NBTInputStream dis) throws IOException {
      this.data = dis.readUTF();
   }

   public byte getId() {
      return 8;
   }

   public String toString() {
      return "StringTag " + this.getName() + " (data: " + this.data + ")";
   }

   public Tag copy() {
      return new StringTag(this.getName(), this.data);
   }

   public boolean equals(Object obj) {
      if (!super.equals(obj)) {
         return false;
      } else {
         StringTag o = (StringTag)obj;
         return this.data == null && o.data == null || this.data != null && this.data.equals(o.data);
      }
   }
}
