package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.DataPacket;

public class DATA_PACKET_F extends DataPacket {
   public static byte ID = -113;

   public byte getID() {
      return ID;
   }
}
