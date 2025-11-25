package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.DataPacket;

public class DATA_PACKET_E extends DataPacket {
   public static byte ID = -114;

   public byte getID() {
      return ID;
   }
}
