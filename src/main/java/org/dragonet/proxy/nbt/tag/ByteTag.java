package org.dragonet.proxy.nbt.tag;

import java.io.IOException;
import org.dragonet.proxy.nbt.stream.NBTInputStream;
import org.dragonet.proxy.nbt.stream.NBTOutputStream;

public class ByteTag extends NumberTag<Byte> {
   public byte data;

   public Byte getData() {
      return this.data;
   }

   public void setData(Byte data) {
      this.data = data == null ? 0 : data;
   }

   public ByteTag(String name) {
      super(name);
   }

   public ByteTag(String name, byte data) {
      super(name);
      this.data = data;
   }

   void write(NBTOutputStream dos) throws IOException {
      dos.writeByte(this.data);
   }

   void load(NBTInputStream dis) throws IOException {
      this.data = dis.readByte();
   }

   public byte getId() {
      return 1;
   }

   public String toString() {
      String hex = Integer.toHexString(this.data & 255);
      if (hex.length() < 2) {
         hex = "0" + hex;
      }

      return "ByteTag " + this.getName() + " (data: 0x" + hex + ")";
   }

   public boolean equals(Object obj) {
      if (super.equals(obj)) {
         ByteTag byteTag = (ByteTag)obj;
         return this.data == byteTag.data;
      } else {
         return false;
      }
   }

   public Tag copy() {
      return new ByteTag(this.getName(), this.data);
   }
}
