package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.AddPlayerPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.net.packet.minecraft.PlayerListPacket;
import org.dragonet.proxy.entity.EntityType;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.cache.CachedEntity;
import org.dragonet.proxy.network.translator.EntityMetaTranslator;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.dragonet.proxy.utilities.DefaultSkin;
import org.spacehq.mc.protocol.data.game.EntityMetadata;
import org.spacehq.mc.protocol.data.game.values.PlayerListEntry;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;

public class PCSpawnPlayerPacketTranslator implements PCPacketTranslator<ServerSpawnPlayerPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerSpawnPlayerPacket packet) {
      try {
         CachedEntity entity = session.getEntityCache().newPlayer(packet);
         AddPlayerPacket pkAddPlayer = new AddPlayerPacket();
         pkAddPlayer.eid = (long)entity.eid;
         EntityMetadata[] var5 = packet.getMetadata();
         int var6 = var5.length;

         for(int var7 = 0; var7 < var6; ++var7) {
            EntityMetadata meta = var5[var7];
            if (meta.getId() == 2) {
               pkAddPlayer.username = meta.getValue().toString();
               break;
            }
         }

         if (pkAddPlayer.username == null) {
            if (!session.getPlayerInfoCache().containsKey(packet.getUUID())) {
               return null;
            }

            pkAddPlayer.username = ((PlayerListEntry)session.getPlayerInfoCache().get(packet.getUUID())).getProfile().getName();
         }

         pkAddPlayer.uuid = packet.getUUID();
         pkAddPlayer.x = (float)packet.getX() / 32.0F;
         pkAddPlayer.y = (float)packet.getY() / 32.0F + 1.62F;
         pkAddPlayer.z = (float)packet.getZ() / 32.0F;
         pkAddPlayer.speedX = 0.0F;
         pkAddPlayer.speedY = 0.0F;
         pkAddPlayer.speedZ = 0.0F;
         pkAddPlayer.yaw = packet.getYaw() / 256.0F * 360.0F;
         pkAddPlayer.pitch = packet.getPitch() / 256.0F * 360.0F;
         pkAddPlayer.metadata = EntityMetaTranslator.translateToPE(packet.getMetadata(), (EntityType)null);
         PlayerListPacket lst = new PlayerListPacket(new PlayerListPacket.PlayerInfo[]{new PlayerListPacket.PlayerInfo(packet.getUUID(), (long)packet.getEntityId(), pkAddPlayer.username, DefaultSkin.getDefaultSkinName(), DefaultSkin.getDefaultSkin().getData())});
         return new PEPacket[]{lst, pkAddPlayer};
      } catch (Exception var9) {
         var9.printStackTrace();
         return null;
      }
   }
}
