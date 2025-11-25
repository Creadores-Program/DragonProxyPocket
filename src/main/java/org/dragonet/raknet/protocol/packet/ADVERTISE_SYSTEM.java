package org.dragonet.raknet.protocol.packet;

public class ADVERTISE_SYSTEM extends UNCONNECTED_PONG {
   public static byte ID = 29;

   public byte getID() {
      return ID;
   }
}
