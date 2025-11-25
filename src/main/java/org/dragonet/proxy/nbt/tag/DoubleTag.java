package org.dragonet.proxy.nbt.tag;

import java.io.IOException;
import org.dragonet.proxy.nbt.stream.NBTInputStream;
import org.dragonet.proxy.nbt.stream.NBTOutputStream;

public class DoubleTag extends NumberTag<Double> {
   public double data;

   public Double getData() {
      return this.data;
   }

   public void setData(Double data) {
      this.data = data == null ? 0.0D : data;
   }

   public DoubleTag(String name) {
      super(name);
   }

   public DoubleTag(String name, double data) {
      super(name);
      this.data = data;
   }

   void write(NBTOutputStream dos) throws IOException {
      dos.writeDouble(this.data);
   }

   void load(NBTInputStream dis) throws IOException {
      this.data = dis.readDouble();
   }

   public byte getId() {
      return 6;
   }

   public String toString() {
      return "DoubleTag " + this.getName() + " (data: " + this.data + ")";
   }

   public Tag copy() {
      return new DoubleTag(this.getName(), this.data);
   }

   public boolean equals(Object obj) {
      if (super.equals(obj)) {
         DoubleTag o = (DoubleTag)obj;
         return this.data == o.data;
      } else {
         return false;
      }
   }
}
