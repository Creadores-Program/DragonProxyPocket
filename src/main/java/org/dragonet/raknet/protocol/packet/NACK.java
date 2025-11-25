package org.dragonet.raknet.protocol.packet;

import org.dragonet.raknet.protocol.AcknowledgePacket;

public class NACK extends AcknowledgePacket {
   public static byte ID = -96;

   public byte getID() {
      return ID;
   }
}
