package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.DataPacket;

public class DATA_PACKET_1 extends DataPacket {
   public static byte ID = -127;

   public byte getID() {
      return ID;
   }
}
