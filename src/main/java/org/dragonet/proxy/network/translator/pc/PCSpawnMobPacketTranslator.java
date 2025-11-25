package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.AddEntityPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.cache.CachedEntity;
import org.dragonet.proxy.network.translator.EntityMetaTranslator;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket;

public class PCSpawnMobPacketTranslator implements PCPacketTranslator<ServerSpawnMobPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerSpawnMobPacket packet) {
      try {
         CachedEntity e = session.getEntityCache().newEntity(packet);
         if (e == null) {
            return null;
         } else {
            AddEntityPacket pk = new AddEntityPacket();
            pk.eid = (long)e.eid;
            pk.type = e.peType.getPeType();
            pk.x = (float)e.x;
            pk.y = (float)e.y;
            pk.z = (float)e.z;
            pk.speedX = (float)e.motionX;
            pk.speedY = (float)e.motionY;
            pk.speedZ = (float)e.motionZ;
            pk.meta = EntityMetaTranslator.translateToPE(e.pcMeta, e.peType);
            return new PEPacket[]{pk};
         }
      } catch (Exception var5) {
         var5.printStackTrace();
         return null;
      }
   }
}
