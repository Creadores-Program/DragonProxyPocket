package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.DataPacket;

public class DATA_PACKET_A extends DataPacket {
   public static byte ID = -118;

   public byte getID() {
      return ID;
   }
}
