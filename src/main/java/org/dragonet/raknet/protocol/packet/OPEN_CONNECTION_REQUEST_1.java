package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.RakNet;
import org.dragonet.raknet.protocol.Packet;

public class OPEN_CONNECTION_REQUEST_1 extends Packet {
   public static byte ID = 5;
   public byte protocol = 6;
   public short mtuSize;

   public byte getID() {
      return ID;
   }

   public void encode() {
      super.encode();
      this.put(RakNet.MAGIC);
      this.putByte(this.protocol);
      this.put(new byte[this.mtuSize - 18]);
   }

   public void decode() {
      super.decode();
      this.offset += 16;
      this.protocol = this.getByte();
      this.mtuSize = (short)(this.get().length + 18);
   }
}
