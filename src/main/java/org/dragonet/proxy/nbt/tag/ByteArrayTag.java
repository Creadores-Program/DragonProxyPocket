package org.dragonet.proxy.nbt.tag;

import java.io.IOException;
import java.util.Arrays;
import org.dragonet.proxy.nbt.stream.NBTInputStream;
import org.dragonet.proxy.nbt.stream.NBTOutputStream;
import org.dragonet.proxy.utilities.Binary;

public class ByteArrayTag extends Tag {
   public byte[] data;

   public ByteArrayTag(String name) {
      super(name);
   }

   public ByteArrayTag(String name, byte[] data) {
      super(name);
      this.data = data;
   }

   void write(NBTOutputStream dos) throws IOException {
      if (this.data == null) {
         dos.writeInt(0);
      } else {
         dos.writeInt(this.data.length);
         dos.write(this.data);
      }
   }

   void load(NBTInputStream dis) throws IOException {
      int length = dis.readInt();
      this.data = new byte[length];
      dis.readFully(this.data);
   }

   public byte getId() {
      return 7;
   }

   public String toString() {
      return "ByteArrayTag " + this.getName() + " (data: 0x" + Binary.bytesToHexString(this.data, true) + " [" + this.data.length + " bytes])";
   }

   public boolean equals(Object obj) {
      if (!super.equals(obj)) {
         return false;
      } else {
         ByteArrayTag byteArrayTag = (ByteArrayTag)obj;
         return this.data == null && byteArrayTag.data == null || this.data != null && Arrays.equals(this.data, byteArrayTag.data);
      }
   }

   public Tag copy() {
      byte[] cp = new byte[this.data.length];
      System.arraycopy(this.data, 0, cp, 0, this.data.length);
      return new ByteArrayTag(this.getName(), cp);
   }
}
