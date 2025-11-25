package org.dragonet.proxy.network;

import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.DesktopServer;
import org.dragonet.proxy.DragonProxy;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.packet.ingame.client.ClientChatPacket;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.event.session.ConnectedEvent;
import org.spacehq.packetlib.event.session.DisconnectedEvent;
import org.spacehq.packetlib.event.session.PacketReceivedEvent;
import org.spacehq.packetlib.event.session.SessionAdapter;
import org.spacehq.packetlib.packet.Packet;
import org.spacehq.packetlib.tcp.TcpSessionFactory;

public class PCDownstreamSession implements DownstreamSession<Packet> {
   private final DragonProxy proxy;
   private final UpstreamSession upstream;
   private DesktopServer serverInfo;
   private Client remoteClient;
   private MinecraftProtocol protocol;

   public PCDownstreamSession(DragonProxy proxy, UpstreamSession upstream) {
      this.proxy = proxy;
      this.upstream = upstream;
   }

   public boolean isConnected() {
      return this.remoteClient != null && this.remoteClient.getSession().isConnected();
   }

   public void send(Packet... packets) {
      Packet[] var2 = packets;
      int var3 = packets.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Packet p = var2[var4];
         this.send(p);
      }

   }

   public void send(Packet packet) {
      this.remoteClient.getSession().send(packet);
   }

   public void connect(DesktopServer serverInfo) {
      this.serverInfo = serverInfo;
      this.connect(serverInfo.getRemoteAddr(), serverInfo.getRemotePort());
   }

   public void connect(String addr, int port) {
      if (this.protocol == null) {
         this.upstream.onConnected();
         this.upstream.disconnect("ERROR! ");
      } else {
         this.remoteClient = new Client(addr, port, this.protocol, new TcpSessionFactory());
         this.remoteClient.getSession().addListener(new SessionAdapter() {
            public void connected(ConnectedEvent event) {
               PCDownstreamSession.this.proxy.getLogger().info(PCDownstreamSession.this.proxy.getLang().get("message_remote_connected", PCDownstreamSession.this.upstream.getUsername(), PCDownstreamSession.this.upstream.getRemoteAddress()));
               PCDownstreamSession.this.upstream.onConnected();
            }

            public void disconnected(DisconnectedEvent event) {
               PCDownstreamSession.this.upstream.disconnect(PCDownstreamSession.this.proxy.getLang().get(event.getReason()));
            }

            public void packetReceived(PacketReceivedEvent event) {
               try {
                  PEPacket[] packets = PacketTranslatorRegister.translateToPE(PCDownstreamSession.this.upstream, event.getPacket());
                  if (packets != null) {
                     if (packets.length > 0) {
                        if (packets.length == 1) {
                           PCDownstreamSession.this.upstream.sendPacket(packets[0]);
                        } else {
                           PCDownstreamSession.this.upstream.sendAllPackets(packets, true);
                        }

                     }
                  }
               } catch (Exception var3) {
                  var3.printStackTrace();
                  throw var3;
               }
            }
         });
         this.remoteClient.getSession().connect();
      }
   }

   public void disconnect() {
      if (this.remoteClient != null && this.remoteClient.getSession().isConnected()) {
         this.remoteClient.getSession().disconnect("Disconnect");
      }

   }

   public void sendChat(String chat) {
      this.remoteClient.getSession().send(new ClientChatPacket(chat));
   }

   public DragonProxy getProxy() {
      return this.proxy;
   }

   public UpstreamSession getUpstream() {
      return this.upstream;
   }

   public DesktopServer getServerInfo() {
      return this.serverInfo;
   }

   public MinecraftProtocol getProtocol() {
      return this.protocol;
   }

   public void setProtocol(MinecraftProtocol protocol) {
      this.protocol = protocol;
   }
}
