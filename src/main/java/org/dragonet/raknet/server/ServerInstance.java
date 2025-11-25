package org.dragonet.raknet.server;

import org.dragonet.raknet.protocol.EncapsulatedPacket;

public interface ServerInstance {
   void openSession(String var1, String var2, int var3, long var4);

   void closeSession(String var1, String var2);

   void handleEncapsulated(String var1, EncapsulatedPacket var2, int var3);

   void handleRaw(String var1, int var2, byte[] var3);

   void notifyACK(String var1, int var2);

   void handleOption(String var1, String var2);
}
