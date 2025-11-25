package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.DataPacket;

public class DATA_PACKET_B extends DataPacket {
   public static byte ID = -117;

   public byte getID() {
      return ID;
   }
}
