package org.dragonet.proxy.nbt.tag;

import java.io.IOException;
import org.dragonet.proxy.nbt.stream.NBTInputStream;
import org.dragonet.proxy.nbt.stream.NBTOutputStream;

public class ShortTag extends NumberTag<Integer> {
   public int data;

   public Integer getData() {
      return this.data;
   }

   public void setData(Integer data) {
      this.data = data == null ? 0 : data;
   }

   public ShortTag(String name) {
      super(name);
   }

   public ShortTag(String name, int data) {
      super(name);
      this.data = data;
   }

   void write(NBTOutputStream dos) throws IOException {
      dos.writeShort(this.data);
   }

   void load(NBTInputStream dis) throws IOException {
      this.data = dis.readUnsignedShort();
   }

   public byte getId() {
      return 2;
   }

   public String toString() {
      return "" + this.data;
   }

   public Tag copy() {
      return new ShortTag(this.getName(), this.data);
   }

   public boolean equals(Object obj) {
      if (super.equals(obj)) {
         ShortTag o = (ShortTag)obj;
         return this.data == o.data;
      } else {
         return false;
      }
   }
}
