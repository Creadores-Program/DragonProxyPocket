package org.dragonet.proxy.nbt.tag;

import java.io.IOException;
import java.util.Arrays;
import org.dragonet.proxy.nbt.stream.NBTInputStream;
import org.dragonet.proxy.nbt.stream.NBTOutputStream;

public class IntArrayTag extends Tag {
   public int[] data;

   public IntArrayTag(String name) {
      super(name);
   }

   public IntArrayTag(String name, int[] data) {
      super(name);
      this.data = data;
   }

   void write(NBTOutputStream dos) throws IOException {
      dos.writeInt(this.data.length);
      int[] var2 = this.data;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         int aData = var2[var4];
         dos.writeInt(aData);
      }

   }

   void load(NBTInputStream dis) throws IOException {
      int length = dis.readInt();
      this.data = new int[length];

      for(int i = 0; i < length; ++i) {
         this.data[i] = dis.readInt();
      }

   }

   public byte getId() {
      return 11;
   }

   public String toString() {
      return "IntArrayTag " + this.getName() + " [" + this.data.length + " bytes]";
   }

   public boolean equals(Object obj) {
      if (!super.equals(obj)) {
         return false;
      } else {
         IntArrayTag intArrayTag = (IntArrayTag)obj;
         return this.data == null && intArrayTag.data == null || this.data != null && Arrays.equals(this.data, intArrayTag.data);
      }
   }

   public Tag copy() {
      int[] cp = new int[this.data.length];
      System.arraycopy(this.data, 0, cp, 0, this.data.length);
      return new IntArrayTag(this.getName(), cp);
   }
}
