package org.dragonet.proxy.nbt.tag;

import java.io.IOException;
import org.dragonet.proxy.nbt.stream.NBTInputStream;
import org.dragonet.proxy.nbt.stream.NBTOutputStream;

public class IntTag extends NumberTag<Integer> {
   public int data;

   public Integer getData() {
      return this.data;
   }

   public void setData(Integer data) {
      this.data = data == null ? 0 : data;
   }

   public IntTag(String name) {
      super(name);
   }

   public IntTag(String name, int data) {
      super(name);
      this.data = data;
   }

   void write(NBTOutputStream dos) throws IOException {
      dos.writeInt(this.data);
   }

   void load(NBTInputStream dis) throws IOException {
      this.data = dis.readInt();
   }

   public byte getId() {
      return 3;
   }

   public String toString() {
      return "IntTag" + this.getName() + "(data: " + this.data + ")";
   }

   public Tag copy() {
      return new IntTag(this.getName(), this.data);
   }

   public boolean equals(Object obj) {
      if (super.equals(obj)) {
         IntTag o = (IntTag)obj;
         return this.data == o.data;
      } else {
         return false;
      }
   }
}
