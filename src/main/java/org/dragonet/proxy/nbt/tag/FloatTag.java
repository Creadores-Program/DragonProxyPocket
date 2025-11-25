package org.dragonet.proxy.nbt.tag;

import java.io.IOException;
import org.dragonet.proxy.nbt.stream.NBTInputStream;
import org.dragonet.proxy.nbt.stream.NBTOutputStream;

public class FloatTag extends NumberTag<Float> {
   public float data;

   public Float getData() {
      return this.data;
   }

   public void setData(Float data) {
      this.data = data == null ? 0.0F : data;
   }

   public FloatTag(String name) {
      super(name);
   }

   public FloatTag(String name, float data) {
      super(name);
      this.data = data;
   }

   void write(NBTOutputStream dos) throws IOException {
      dos.writeFloat(this.data);
   }

   void load(NBTInputStream dis) throws IOException {
      this.data = dis.readFloat();
   }

   public byte getId() {
      return 5;
   }

   public String toString() {
      return "FloatTag " + this.getName() + " (data: " + this.data + ")";
   }

   public Tag copy() {
      return new FloatTag(this.getName(), this.data);
   }

   public boolean equals(Object obj) {
      if (super.equals(obj)) {
         FloatTag o = (FloatTag)obj;
         return this.data == o.data;
      } else {
         return false;
      }
   }
}
