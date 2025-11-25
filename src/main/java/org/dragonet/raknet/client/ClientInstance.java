package org.dragonet.raknet.client;

import org.dragonet.raknet.protocol.EncapsulatedPacket;

public interface ClientInstance {
   void connectionOpened(long var1);

   void connectionClosed(String var1);

   void handleEncapsulated(EncapsulatedPacket var1, int var2);

   void handleRaw(byte[] var1);

   void handleOption(String var1, String var2);
}
