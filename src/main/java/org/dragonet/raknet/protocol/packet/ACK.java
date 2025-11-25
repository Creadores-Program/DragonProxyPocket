package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.AcknowledgePacket;

public class ACK extends AcknowledgePacket {
   public static byte ID = -64;

   public byte getID() {
      return ID;
   }
}
