package org.dragonet.net.packet;

public abstract class BinaryPacket {
   private byte[] data;

   public BinaryPacket() {
   }

   public BinaryPacket(byte[] data) {
      this.data = data;
   }

   public abstract void encode();

   public abstract void decode();

   public byte[] getData() {
      return this.data;
   }

   public void setData(byte[] data) {
      this.data = data;
   }
}
