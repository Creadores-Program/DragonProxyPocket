package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.net.packet.minecraft.RemoveEntityPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.cache.CachedEntity;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerDestroyEntitiesPacket;

public class PCDestroyEntitiesPacketTranslator implements PCPacketTranslator<ServerDestroyEntitiesPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerDestroyEntitiesPacket packet) {
      PEPacket[] ret = new PEPacket[packet.getEntityIds().length];

      for(int i = 0; i < ret.length; ++i) {
         CachedEntity e = session.getEntityCache().remove(packet.getEntityIds()[i]);
         if (e != null) {
            ret[i] = new RemoveEntityPacket();
            ((RemoveEntityPacket)ret[i]).eid = (long)e.eid;
         }
      }

      return ret;
   }
}
