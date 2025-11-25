package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.DataPacket;

public class DATA_PACKET_D extends DataPacket {
   public static byte ID = -115;

   public byte getID() {
      return ID;
   }
}
