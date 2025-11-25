package org.spacehq.packetlib.event.server;

public interface ServerListener {
   void serverBound(ServerBoundEvent var1);

   void serverClosing(ServerClosingEvent var1);

   void serverClosed(ServerClosedEvent var1);

   void sessionAdded(SessionAddedEvent var1);

   void sessionRemoved(SessionRemovedEvent var1);
}
