package org.dragonet.proxy.network;

import java.net.InetSocketAddress;
import java.util.Arrays;
import org.dragonet.net.packet.minecraft.BatchPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.DragonProxy;
import org.dragonet.proxy.utilities.Binary;
import org.dragonet.raknet.protocol.EncapsulatedPacket;
import org.dragonet.raknet.server.RakNetServer;
import org.dragonet.raknet.server.ServerHandler;
import org.dragonet.raknet.server.ServerInstance;

public class RaknetInterface implements ServerInstance {
   private final DragonProxy proxy;
   private final SessionRegister sessions;
   private final RakNetServer rakServer;
   private final ServerHandler handler;

   public RaknetInterface(DragonProxy proxy, String ip, int port) {
      this.proxy = proxy;
      this.rakServer = new RakNetServer(port, ip);
      this.handler = new ServerHandler(this.rakServer, this);
      this.sessions = this.proxy.getSessionRegister();
   }

   public void setBroadcastName(String serverName, int players, int maxPlayers) {
      String name = "MCPE;";
      name = name + serverName + ";";
      name = name + "82;";
      name = name + "0.15.4;";
      name = name + players + ";" + maxPlayers;
      if (this.handler != null) {
         this.handler.sendOption("name", name);
      }

   }

   public void onTick() {
      while(this.handler.handlePacket()) {
      }

   }

   public void openSession(String identifier, String address, int port, long clientID) {
      UpstreamSession session = new UpstreamSession(this.proxy, identifier, new InetSocketAddress(address, port));
      this.sessions.newSession(session);
   }

   public void closeSession(String identifier, String reason) {
      UpstreamSession session = this.sessions.getSession(identifier);
      if (session != null) {
         session.onDisconnect(this.proxy.getLang().get("message_client_disconnect"));
      }
   }

   public void handleEncapsulated(String identifier, EncapsulatedPacket packet, int flags) {
      UpstreamSession session = this.sessions.getSession(identifier);
      if (session != null) {
         packet.buffer = Arrays.copyOfRange(packet.buffer, 1, packet.buffer.length);
         session.handlePacketBinary(packet);
      }
   }

   public void handleRaw(String address, int port, byte[] payload) {
   }

   public void notifyACK(String identifier, int identifierACK) {
   }

   public void handleOption(String option, String value) {
   }

   public void shutdown() {
      this.handler.shutdown();
   }

   public void disconnect(String identifier, String reason) {
      this.handler.closeSession(identifier, reason);
   }

   public void sendPacket(String identifier, PEPacket packet, boolean immediate) {
      if (identifier != null && packet != null) {
         boolean overridedImmediate = immediate || packet.isShouldSendImmidate();
         packet.encode();
         if (packet.getData().length > 512 && !BatchPacket.class.isAssignableFrom(packet.getClass())) {
            BatchPacket pkBatch = new BatchPacket();
            pkBatch.packets.add(packet);
            this.sendPacket(identifier, pkBatch, overridedImmediate);
         } else {
            EncapsulatedPacket encapsulated = new EncapsulatedPacket();
            encapsulated.buffer = Binary.appendBytes((byte)-2, packet.getData());
            encapsulated.needACK = true;
            encapsulated.reliability = 2;
            encapsulated.messageIndex = 0;
            this.handler.sendEncapsulated(identifier, encapsulated, 8 | (overridedImmediate ? 1 : 0));
         }
      }
   }

   public DragonProxy getProxy() {
      return this.proxy;
   }

   public RakNetServer getRakServer() {
      return this.rakServer;
   }

   public ServerHandler getHandler() {
      return this.handler;
   }
}
