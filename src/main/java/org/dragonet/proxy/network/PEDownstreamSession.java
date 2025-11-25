package org.dragonet.proxy.network;

import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Logger;
import org.dragonet.net.packet.Protocol;
import org.dragonet.net.packet.minecraft.BatchPacket;
import org.dragonet.net.packet.minecraft.ChatPacket;
import org.dragonet.net.packet.minecraft.LoginPacket;
import org.dragonet.net.packet.minecraft.MovePlayerPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.net.packet.minecraft.SetSpawnPositionPacket;
import org.dragonet.net.packet.minecraft.StartGamePacket;
import org.dragonet.proxy.DragonProxy;
import org.dragonet.proxy.PocketServer;
import org.dragonet.proxy.utilities.Binary;
import org.dragonet.raknet.client.ClientHandler;
import org.dragonet.raknet.client.ClientInstance;
import org.dragonet.raknet.client.JRakLibClient;
import org.dragonet.raknet.protocol.EncapsulatedPacket;

public class PEDownstreamSession implements DownstreamSession<PEPacket>, ClientInstance {
   public static final String ENTITY_ID_KEY = "ENTITYID";
   private JRakLibClient client;
   private ClientHandler handler;
   private final DragonProxy proxy;
   private final UpstreamSession upstream;
   private PocketServer serverInfo;
   private boolean connected;

   public PEDownstreamSession(DragonProxy proxy, UpstreamSession upstream) {
      this.proxy = proxy;
      this.upstream = upstream;
   }

   public void connect(PocketServer serverInfo) {
      this.serverInfo = serverInfo;
      this.connect(serverInfo.getRemoteAddr(), serverInfo.getRemotePort());
   }

   public void connect(String addr, int port) {
      System.out.println("[" + this.upstream.getUsername() + "] Connecting to remote pocket server at [" + String.format("%s:%s", addr, port) + "] ");
      if (this.client != null) {
         this.upstream.onConnected();
         this.upstream.disconnect("ERROR! ");
      }

      this.client = new JRakLibClient(Logger.getLogger(this.upstream.getRemoteAddress().toString() + "<->" + this.serverInfo.toString()), addr, port);
      this.handler = new ClientHandler(this.client, this);
   }

   public boolean isConnected() {
      return this.connected;
   }

   public void send(PEPacket packet) {
      this.sendPacket(packet, true);
   }

   public void send(PEPacket... packets) {
      PEPacket[] var2 = packets;
      int var3 = packets.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         PEPacket packet = var2[var4];
         this.sendPacket(packet, true);
      }

   }

   public void sendChat(String chat) {
      ChatPacket pk = new ChatPacket();
      pk.type = ChatPacket.TextType.CHAT;
      pk.source = "";
      pk.message = chat;
      this.send((PEPacket)pk);
   }

   public void disconnect() {
      this.connected = false;
      this.handler.disconnectFromServer();
   }

   public void connectionOpened(long serverId) {
      this.connected = true;
      LoginPacket pkLogin = new LoginPacket();
      pkLogin.clientID = this.client.getId();
      pkLogin.clientUuid = UUID.nameUUIDFromBytes(("DragonProxyPlayer:" + this.upstream.getUsername()).getBytes());
      pkLogin.protocol = 82;
      pkLogin.serverAddress = "0.0.0.0:0";
      pkLogin.username = this.upstream.getUsername();
      System.out.println("[DEBUG] Remote pocket server downstream established! ");
      this.sendPacket(pkLogin, true);
   }

   public void connectionClosed(String reason) {
      this.connected = false;
      this.upstream.disconnect(reason);
      System.out.println("[DEBUG] Remote pocket server downstream CLOSED! ");
   }

   public void handleEncapsulated(EncapsulatedPacket packet, int flags) {
      byte[] buffer = Arrays.copyOfRange(packet.buffer, 1, packet.buffer.length);
      PEPacket pk = Protocol.decode(buffer);
      if (StartGamePacket.class.isAssignableFrom(pk.getClass())) {
         StartGamePacket start = (StartGamePacket)pk;
         this.upstream.getDataCache().put("ENTITYID", start.eid);
         SetSpawnPositionPacket spawn = new SetSpawnPositionPacket();
         spawn.x = start.spawnX;
         spawn.y = start.spawnY;
         spawn.z = start.spawnZ;
         this.upstream.sendPacket(spawn, true);
         MovePlayerPacket teleport = new MovePlayerPacket(0L, start.x, start.y, start.z, 0.0F, 0.0F, 0.0F, true);
         this.upstream.sendPacket(teleport, true);
      } else {
         this.upstream.sendPacket(pk);
      }
   }

   public void handleRaw(byte[] payload) {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   public void handleOption(String option, String value) {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   public void sendPacket(PEPacket packet, boolean immediate) {
      if (packet != null) {
         boolean overridedImmediate = immediate || packet.isShouldSendImmidate();
         packet.encode();
         if (packet.getData().length > 512 && !BatchPacket.class.isAssignableFrom(packet.getClass())) {
            BatchPacket pkBatch = new BatchPacket();
            pkBatch.packets.add(packet);
            this.sendPacket(pkBatch, overridedImmediate);
         } else {
            EncapsulatedPacket encapsulated = new EncapsulatedPacket();
            encapsulated.buffer = Binary.appendBytes((byte)-2, packet.getData());
            encapsulated.needACK = true;
            encapsulated.reliability = 2;
            encapsulated.messageIndex = 0;
            this.handler.sendEncapsulated("", encapsulated, 8 | (overridedImmediate ? 1 : 0));
         }
      }
   }

   public DragonProxy getProxy() {
      return this.proxy;
   }

   public UpstreamSession getUpstream() {
      return this.upstream;
   }
}
