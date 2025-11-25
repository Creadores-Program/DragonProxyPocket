package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.RakNet;
import org.dragonet.raknet.protocol.Packet;

public class UNCONNECTED_PING extends Packet {
   public static byte ID = 1;
   public long pingID;

   public byte getID() {
      return ID;
   }

   public void encode() {
      super.encode();
      this.putLong(this.pingID);
      this.put(RakNet.MAGIC);
   }

   public void decode() {
      super.decode();
      this.pingID = this.getLong();
   }
}
