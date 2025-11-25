package org.dragonet.raknet.protocol.packet;

public class UNCONNECTED_PING_OPEN_CONNECTIONS extends UNCONNECTED_PING {
   public static byte ID = 2;

   public byte getID() {
      return ID;
   }
}
