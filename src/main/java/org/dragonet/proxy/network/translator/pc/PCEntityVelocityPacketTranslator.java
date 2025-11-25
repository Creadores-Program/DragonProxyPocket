package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.net.packet.minecraft.SetEntityMotionPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.cache.CachedEntity;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityVelocityPacket;

public class PCEntityVelocityPacketTranslator implements PCPacketTranslator<ServerEntityVelocityPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerEntityVelocityPacket packet) {
      CachedEntity e = session.getEntityCache().get(packet.getEntityId());
      if (e == null) {
         return null;
      } else {
         e.motionX = packet.getMotionX();
         e.motionY = packet.getMotionY();
         e.motionZ = packet.getMotionZ();
         SetEntityMotionPacket pk = new SetEntityMotionPacket();
         SetEntityMotionPacket.EntityMotionData data = new SetEntityMotionPacket.EntityMotionData();
         data.eid = (long)packet.getEntityId();
         data.motionX = (float)packet.getMotionX();
         data.motionY = (float)packet.getMotionY();
         data.motionZ = (float)packet.getMotionZ();
         pk.motions = new SetEntityMotionPacket.EntityMotionData[]{data};
         return new PEPacket[]{pk};
      }
   }
}
