package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.DataPacket;

public class DATA_PACKET_C extends DataPacket {
   public static byte ID = -116;

   public byte getID() {
      return ID;
   }
}
