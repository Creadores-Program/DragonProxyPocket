package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.RakNet;
import org.dragonet.raknet.protocol.Packet;

public class OPEN_CONNECTION_REPLY_1 extends Packet {
   public static byte ID = 6;
   public long serverID;
   public short mtuSize;

   public byte getID() {
      return ID;
   }

   public void encode() {
      super.encode();
      this.put(RakNet.MAGIC);
      this.putLong(this.serverID);
      this.putByte((byte)0);
      this.putShort(this.mtuSize);
   }

   public void decode() {
      super.decode();
      this.offset += 16;
      this.serverID = this.getLong();
      this.getByte();
      this.mtuSize = this.getSignedShort();
   }
}
