package org.dragonet.net.packet.minecraft;

public class UnknownPacket extends PEPacket {
   private final int packetId;

   public UnknownPacket(int packetId, byte[] buffer) {
      this.packetId = packetId;
      this.setData(buffer);
   }

   public int pid() {
      return this.packetId;
   }

   public void encode() {
   }

   public void decode() {
   }
}
