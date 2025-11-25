package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.Packet;

public class PONG_DataPacket extends Packet {
   public static byte ID = 3;
   public long pingID;

   public byte getID() {
      return ID;
   }

   public void encode() {
      super.encode();
      this.putLong(this.pingID);
   }

   public void decode() {
      super.decode();
      this.pingID = this.getLong();
   }
}
