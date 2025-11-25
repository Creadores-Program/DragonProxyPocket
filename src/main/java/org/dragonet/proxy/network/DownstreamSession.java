package org.dragonet.proxy.network;

public interface DownstreamSession<PACKET> {
   void connect(String var1, int var2);

   boolean isConnected();

   void send(PACKET var1);

   void send(PACKET... var1);

   void sendChat(String var1);

   void disconnect();
}
