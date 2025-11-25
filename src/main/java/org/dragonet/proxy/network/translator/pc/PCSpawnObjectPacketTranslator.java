package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.cache.CachedEntity;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.data.game.values.entity.ObjectType;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnObjectPacket;

public class PCSpawnObjectPacketTranslator implements PCPacketTranslator<ServerSpawnObjectPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerSpawnObjectPacket packet) {
      if (packet.getType() == ObjectType.ITEM) {
         CachedEntity futureEntity = session.getEntityCache().newObject(packet);
         return null;
      } else {
         return null;
      }
   }
}
