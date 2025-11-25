package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.MobEffectPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.cache.CachedEntity;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityRemoveEffectPacket;

public class PCEntityRemoveEffectPacketTranslator implements PCPacketTranslator<ServerEntityRemoveEffectPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerEntityRemoveEffectPacket packet) {
      CachedEntity entity = session.getEntityCache().get(packet.getEntityId());
      if (entity == null) {
         return null;
      } else {
         int effectId = (Integer)MagicValues.value(Integer.class, packet.getEffect());
         if (!entity.effects.contains(effectId)) {
            return null;
         } else {
            MobEffectPacket eff = new MobEffectPacket();
            eff.eid = packet.getEntityId() == (Integer)session.getDataCache().get("player_entity_id") ? 0L : (long)packet.getEntityId();
            eff.action = MobEffectPacket.EffectAction.REMOVE;
            return new PEPacket[]{eff};
         }
      }
   }
}
