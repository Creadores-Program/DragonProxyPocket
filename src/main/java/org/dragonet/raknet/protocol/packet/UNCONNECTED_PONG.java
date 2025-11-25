package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.RakNet;
import org.dragonet.raknet.protocol.Packet;

public class UNCONNECTED_PONG extends Packet {
   public static byte ID = 28;
   public long pingID;
   public long serverID;
   public String serverName;

   public byte getID() {
      return ID;
   }

   public void encode() {
      super.encode();
      this.putLong(this.pingID);
      this.putLong(this.serverID);
      this.put(RakNet.MAGIC);
      this.putString(this.serverName);
   }

   public void decode() {
      super.decode();
      this.pingID = this.getLong();
      this.serverID = this.getLong();
      this.offset += 16;
      this.serverName = this.getString();
   }
}
