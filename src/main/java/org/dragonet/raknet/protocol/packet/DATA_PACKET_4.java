package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.DataPacket;

public class DATA_PACKET_4 extends DataPacket {
   public static byte ID = -124;

   public byte getID() {
      return ID;
   }
}
