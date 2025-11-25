package org.dragonet.proxy.nbt.tag;

import java.io.IOException;
import org.dragonet.proxy.nbt.stream.NBTInputStream;
import org.dragonet.proxy.nbt.stream.NBTOutputStream;

public class LongTag extends NumberTag<Long> {
   public long data;

   public Long getData() {
      return this.data;
   }

   public void setData(Long data) {
      this.data = data == null ? 0L : data;
   }

   public LongTag(String name) {
      super(name);
   }

   public LongTag(String name, long data) {
      super(name);
      this.data = data;
   }

   void write(NBTOutputStream dos) throws IOException {
      dos.writeLong(this.data);
   }

   void load(NBTInputStream dis) throws IOException {
      this.data = dis.readLong();
   }

   public byte getId() {
      return 4;
   }

   public String toString() {
      return "LongTag" + this.getName() + " (data:" + this.data + ")";
   }

   public Tag copy() {
      return new LongTag(this.getName(), this.data);
   }

   public boolean equals(Object obj) {
      if (super.equals(obj)) {
         LongTag o = (LongTag)obj;
         return this.data == o.data;
      } else {
         return false;
      }
   }
}
