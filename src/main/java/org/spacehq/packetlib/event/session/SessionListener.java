package org.spacehq.packetlib.event.session;

public interface SessionListener {
   void packetReceived(PacketReceivedEvent var1);

   void packetSent(PacketSentEvent var1);

   void connected(ConnectedEvent var1);

   void disconnecting(DisconnectingEvent var1);

   void disconnected(DisconnectedEvent var1);
}
