package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.DataPacket;

public class DATA_PACKET_3 extends DataPacket {
   public static byte ID = -125;

   public byte getID() {
      return ID;
   }
}
