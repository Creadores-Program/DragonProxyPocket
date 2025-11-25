package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.Packet;

public class CLIENT_CONNECT_DataPacket extends Packet {
   public static byte ID = 9;
   public long clientID;
   public long sendPing;
   public boolean useSecurity = false;

   public byte getID() {
      return ID;
   }

   public void encode() {
      super.encode();
      this.putLong(this.clientID);
      this.putLong(this.sendPing);
      this.putByte((byte)(this.useSecurity ? 1 : 0));
   }

   public void decode() {
      super.decode();
      this.clientID = this.getLong();
      this.sendPing = this.getLong();
      this.useSecurity = this.getByte() > 0;
   }
}
