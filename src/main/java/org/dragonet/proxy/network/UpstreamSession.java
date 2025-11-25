package org.dragonet.proxy.network;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.dragonet.net.packet.minecraft.BatchPacket;
import org.dragonet.net.packet.minecraft.ChatPacket;
import org.dragonet.net.packet.minecraft.LoginPacket;
import org.dragonet.net.packet.minecraft.LoginStatusPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.net.packet.minecraft.RemoveEntityPacket;
import org.dragonet.net.packet.minecraft.SetSpawnPositionPacket;
import org.dragonet.net.packet.minecraft.StartGamePacket;
import org.dragonet.net.packet.minecraft.UpdateBlockPacket;
import org.dragonet.proxy.DesktopServer;
import org.dragonet.proxy.DragonProxy;
import org.dragonet.proxy.PocketServer;
import org.dragonet.proxy.configuration.RemoteServer;
import org.dragonet.proxy.network.cache.EntityCache;
import org.dragonet.proxy.network.cache.WindowCache;
import org.dragonet.proxy.utilities.HTTP;
import org.dragonet.raknet.protocol.EncapsulatedPacket;
import org.spacehq.mc.auth.exception.request.RequestException;
import org.spacehq.mc.auth.service.AuthenticationService;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.data.game.values.PlayerListEntry;

public class UpstreamSession {
   private final DragonProxy proxy;
   private final String raknetID;
   private final InetSocketAddress remoteAddress;
   private final PEPacketProcessor packetProcessor;
   private final ScheduledFuture<?> packetProcessorScheule;
   private String username;
   private DownstreamSession downstream;
   private final Map<String, Object> dataCache = Collections.synchronizedMap(new HashMap());
   private final Map<UUID, PlayerListEntry> playerInfoCache = Collections.synchronizedMap(new HashMap());
   private final EntityCache entityCache = new EntityCache(this);
   private final WindowCache windowCache = new WindowCache(this);
   protected boolean connecting;
   private MinecraftProtocol protocol;

   public UpstreamSession(DragonProxy proxy, String raknetID, InetSocketAddress remoteAddress) {
      this.proxy = proxy;
      this.raknetID = raknetID;
      this.remoteAddress = remoteAddress;
      this.packetProcessor = new PEPacketProcessor(this);
      this.packetProcessorScheule = proxy.getGeneralThreadPool().scheduleAtFixedRate(this.packetProcessor, 10L, 50L, TimeUnit.MILLISECONDS);
   }

   public void sendPacket(PEPacket packet) {
      this.sendPacket(packet, false);
   }

   public void sendPacket(PEPacket packet, boolean immediate) {
      this.proxy.getNetwork().sendPacket(this.raknetID, packet, immediate);
   }

   public void sendAllPackets(PEPacket[] packets, boolean immediate) {
      if (packets.length < 5) {
         PEPacket[] var3 = packets;
         int var4 = packets.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            PEPacket packet = var3[var5];
            this.sendPacket(packet);
         }
      } else {
         BatchPacket batch = new BatchPacket();
         boolean mustImmediate = immediate;
         if (!immediate) {
            PEPacket[] var11 = packets;
            int var12 = packets.length;

            for(int var7 = 0; var7 < var12; ++var7) {
               PEPacket packet = var11[var7];
               if (packet.isShouldSendImmidate()) {
                  batch.packets.add(packet);
                  mustImmediate = true;
                  break;
               }
            }
         }

         this.sendPacket(batch, mustImmediate);
      }

   }

   public void onTick() {
      this.entityCache.onTick();
   }

   public void disconnect(String reason) {
      if (!this.connecting) {
         this.proxy.getNetwork().closeSession(this.raknetID, reason);
      }

   }

   public void onDisconnect(String reason) {
      this.proxy.getLogger().info(this.proxy.getLang().get("client_disconnected", this.proxy.getAuthMode().equals("cls") ? "unknown(CLS)" : this.username, this.remoteAddress, reason));
      if (this.downstream != null) {
         this.downstream.disconnect();
      }

      this.proxy.getSessionRegister().removeSession(this);
      this.packetProcessorScheule.cancel(true);
   }

   public void handlePacketBinary(EncapsulatedPacket packet) {
      this.packetProcessor.putPacket(packet.buffer);
   }

   public void onLogin(LoginPacket packet) {
      if (this.username != null) {
         this.disconnect("Already logged in, \nthis must be an error! ");
      } else {
         LoginStatusPacket status = new LoginStatusPacket();
         if (packet.protocol != 80 && packet.protocol != 81 && packet.protocol != 82 && packet.protocol != 83 && packet.protocol != 84) {
            status.status = 1;
            this.sendPacket(status, true);
            this.disconnect(this.proxy.getLang().get("message_unsupported_client"));
         } else {
            status.status = 0;
            this.sendPacket(status, true);
            this.username = packet.username;
            this.proxy.getLogger().info(this.proxy.getLang().get("message_client_connected", this.username, this.remoteAddress));
            if (this.proxy.getAuthMode().equals("online")) {
               StartGamePacket pkStartGame = new StartGamePacket();
               pkStartGame.eid = 0L;
               pkStartGame.dimension = 0;
               pkStartGame.seed = 0;
               pkStartGame.generator = 1;
               pkStartGame.spawnX = 0;
               pkStartGame.spawnY = 0;
               pkStartGame.spawnZ = 0;
               pkStartGame.x = 0.0F;
               pkStartGame.y = 72.0F;
               pkStartGame.z = 0.0F;
               this.sendPacket(pkStartGame, true);
               SetSpawnPositionPacket pkSpawn = new SetSpawnPositionPacket();
               pkSpawn.x = 0;
               pkSpawn.y = 72;
               pkSpawn.z = 0;
               this.sendPacket(pkSpawn, true);
               LoginStatusPacket pkStat = new LoginStatusPacket();
               pkStat.status = 3;
               this.sendPacket(pkStat, true);
               this.dataCache.put("auth_state", "email");
               this.sendChat(this.proxy.getLang().get("message_online_notice", this.username));
               this.sendChat(this.proxy.getLang().get("message_online_email"));
            } else {
               this.protocol = new MinecraftProtocol(this.username);
               if (this.proxy.isDebug()) {
                  System.out.println("[DEBUG] Initially joining [" + this.proxy.getConfig().getDefault_server() + "]... ");
               }

               this.connectToServer((RemoteServer)this.proxy.getConfig().getRemote_servers().get(this.proxy.getConfig().getDefault_server()));
            }

         }
      }
   }

   public void connectToServer(RemoteServer server) {
      if (server != null) {
         this.connecting = true;
         if (this.downstream != null && this.downstream.isConnected()) {
            this.downstream.disconnect();
            BatchPacket batch = new BatchPacket();
            this.entityCache.getEntities().entrySet().forEach((ent) -> {
               if ((Integer)ent.getKey() != 0) {
                  batch.packets.add(new RemoveEntityPacket((long)(Integer)ent.getKey()));
               }

            });
            this.entityCache.reset(true);
            this.sendPacket(batch, true);
         } else {
            if (server.getClass().isAssignableFrom(DesktopServer.class)) {
               this.downstream = new PCDownstreamSession(this.proxy, this);
               ((PCDownstreamSession)this.downstream).setProtocol(this.protocol);
               ((PCDownstreamSession)this.downstream).connect(server.getRemoteAddr(), server.getRemotePort());
            } else {
               this.downstream = new PEDownstreamSession(this.proxy, this);
               ((PEDownstreamSession)this.downstream).connect((PocketServer)server);
            }

         }
      }
   }

   public void sendStartGameAndDisconnect(String reason) {
      StartGamePacket pkStartGame = new StartGamePacket();
      pkStartGame.dimension = 1;
      pkStartGame.generator = 1;
      pkStartGame.y = 72.0F;
      this.sendPacket(pkStartGame, true);
      LoginStatusPacket pkStat = new LoginStatusPacket();
      pkStat.status = 3;
      this.sendPacket(pkStat, true);
      this.sendChat(reason);
      this.disconnect(reason);
   }

   public void sendChat(String chat) {
      if (!chat.contains("\n")) {
         ChatPacket pk = new ChatPacket();
         pk.type = ChatPacket.TextType.CHAT;
         pk.source = "";
         pk.message = chat;
         this.sendPacket(pk, true);
      } else {
         String[] lines = chat.split("\n");
         String[] var3 = lines;
         int var4 = lines.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            String line = var3[var5];
            this.sendChat(line);
         }

      }
   }

   public void sendPopup(String text) {
      ChatPacket pk = new ChatPacket();
      pk.type = ChatPacket.TextType.POPUP;
      pk.source = "";
      pk.message = text;
      this.sendPacket(pk, true);
   }

   public void sendFakeBlock(int x, int y, int z, int id, int meta) {
      UpdateBlockPacket pkBlock = new UpdateBlockPacket();
      UpdateBlockPacket.UpdateBlockRecord rec = new UpdateBlockPacket.UpdateBlockRecord();
      rec.flags = 3;
      rec.x = x;
      rec.y = (byte)(y & 255);
      rec.z = z;
      rec.block = (byte)(id & 255);
      rec.meta = (byte)(meta & 255);
      pkBlock.records = new UpdateBlockPacket.UpdateBlockRecord[]{rec};
      this.sendPacket(pkBlock, true);
   }

   public void authenticate(String password) {
      this.proxy.getGeneralThreadPool().execute(() -> {
         try {
            this.protocol = new MinecraftProtocol((String)this.dataCache.get("auth_mail"), password, false);
         } catch (RequestException var3) {
            if (var3.getMessage().toLowerCase().contains("invalid")) {
               this.sendChat(this.proxy.getLang().get("message_online_login_faild"));
               this.disconnect(this.proxy.getLang().get("message_online_login_faild"));
               return;
            }

            this.sendChat(this.proxy.getLang().get("message_online_error"));
            this.disconnect(this.proxy.getLang().get("message_online_error"));
            return;
         }

         if (!this.username.equals(this.protocol.getProfile().getName())) {
            this.username = this.protocol.getProfile().getName();
            this.sendChat(this.proxy.getLang().get("message_online_username", this.username));
         }

         this.sendChat(this.proxy.getLang().get("message_online_login_success", this.username));
         this.proxy.getLogger().info(this.proxy.getLang().get("message_online_login_success_console", this.username, this.remoteAddress, this.username));
         this.connectToServer((RemoteServer)this.proxy.getConfig().getRemote_servers().get(this.proxy.getConfig().getDefault_server()));
      });
   }

   public void onConnected() {
      this.connecting = false;
   }

   public DragonProxy getProxy() {
      return this.proxy;
   }

   public String getRaknetID() {
      return this.raknetID;
   }

   public InetSocketAddress getRemoteAddress() {
      return this.remoteAddress;
   }

   public PEPacketProcessor getPacketProcessor() {
      return this.packetProcessor;
   }

   public String getUsername() {
      return this.username;
   }

   public DownstreamSession getDownstream() {
      return this.downstream;
   }

   public Map<String, Object> getDataCache() {
      return this.dataCache;
   }

   public Map<UUID, PlayerListEntry> getPlayerInfoCache() {
      return this.playerInfoCache;
   }

   public EntityCache getEntityCache() {
      return this.entityCache;
   }

   public WindowCache getWindowCache() {
      return this.windowCache;
   }
}
