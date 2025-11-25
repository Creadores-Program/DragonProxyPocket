package org.spacehq.packetlib;

import java.util.List;
import java.util.Map;
import org.spacehq.packetlib.event.session.SessionEvent;
import org.spacehq.packetlib.event.session.SessionListener;
import org.spacehq.packetlib.packet.Packet;
import org.spacehq.packetlib.packet.PacketProtocol;

public interface Session {
   void connect();

   void connect(boolean var1);

   String getHost();

   int getPort();

   PacketProtocol getPacketProtocol();

   Map<String, Object> getFlags();

   boolean hasFlag(String var1);

   <T> T getFlag(String var1);

   void setFlag(String var1, Object var2);

   List<SessionListener> getListeners();

   void addListener(SessionListener var1);

   void removeListener(SessionListener var1);

   void callEvent(SessionEvent var1);

   int getCompressionThreshold();

   void setCompressionThreshold(int var1);

   int getConnectTimeout();

   void setConnectTimeout(int var1);

   int getReadTimeout();

   void setReadTimeout(int var1);

   int getWriteTimeout();

   void setWriteTimeout(int var1);

   boolean isConnected();

   void send(Packet var1);

   void disconnect(String var1);

   void disconnect(String var1, boolean var2);

   void disconnect(String var1, Throwable var2);

   void disconnect(String var1, Throwable var2, boolean var3);
}
