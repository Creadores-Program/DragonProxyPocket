package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.MoveEntitiesPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.cache.CachedEntity;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityPositionPacket;

public class PCEntityPositionPacketTranslator implements PCPacketTranslator<ServerEntityPositionPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerEntityPositionPacket packet) {
      CachedEntity e = session.getEntityCache().get(packet.getEntityId());
      if (e == null) {
         return null;
      } else {
         e.relativeMove(packet.getMovementX(), packet.getMovementY(), packet.getMovementZ());
         MoveEntitiesPacket.MoveEntityData data = new MoveEntitiesPacket.MoveEntityData();
         data.eid = (long)e.eid;
         data.yaw = e.yaw;
         data.headYaw = e.yaw;
         data.pitch = e.pitch;
         data.x = (float)e.x;
         data.y = (float)e.y;
         if (e.player) {
            ++data.y;
         }

         data.z = (float)e.z;
         MoveEntitiesPacket pk = new MoveEntitiesPacket(new MoveEntitiesPacket.MoveEntityData[]{data});
         return new PEPacket[]{pk};
      }
   }
}
