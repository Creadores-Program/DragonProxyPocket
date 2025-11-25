package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.DataPacket;

public class DATA_PACKET_8 extends DataPacket {
   public static byte ID = -120;

   public byte getID() {
      return ID;
   }
}
