package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.DataPacket;

public class DATA_PACKET_2 extends DataPacket {
   public static byte ID = -126;

   public byte getID() {
      return ID;
   }
}
