package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.DataPacket;

public class DATA_PACKET_5 extends DataPacket {
   public static byte ID = -123;

   public byte getID() {
      return ID;
   }
}
