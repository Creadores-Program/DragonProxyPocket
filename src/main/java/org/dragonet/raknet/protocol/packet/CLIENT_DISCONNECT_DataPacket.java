package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.Packet;

public class CLIENT_DISCONNECT_DataPacket extends Packet {
   public static byte ID = 21;

   public byte getID() {
      return ID;
   }
}
