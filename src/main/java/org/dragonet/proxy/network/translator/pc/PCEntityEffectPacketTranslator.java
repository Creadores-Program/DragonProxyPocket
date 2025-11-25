package org.dragonet.proxy.network.translator.pc;

import org.dragonet.PocketPotionEffect;
import org.dragonet.net.packet.minecraft.MobEffectPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.cache.CachedEntity;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.packet.ingame.server.entity.ServerEntityEffectPacket;

public class PCEntityEffectPacketTranslator implements PCPacketTranslator<ServerEntityEffectPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerEntityEffectPacket packet) {
      CachedEntity entity = session.getEntityCache().get(packet.getEntityId());
      if (entity == null) {
         return null;
      } else {
         int effectId = (Integer)MagicValues.value(Integer.class, packet.getEffect());
         MobEffectPacket eff = new MobEffectPacket();
         eff.eid = packet.getEntityId() == (Integer)session.getDataCache().get("player_entity_id") ? 0L : (long)packet.getEntityId();
         eff.effect = PocketPotionEffect.getByID(effectId);
         if (eff.effect == null) {
            return null;
         } else {
            if (entity.effects.contains(effectId)) {
               eff.action = MobEffectPacket.EffectAction.MODIFY;
            } else {
               eff.action = MobEffectPacket.EffectAction.ADD;
               entity.effects.add(effectId);
            }

            eff.effect.setAmpilifier(packet.getAmplifier());
            eff.effect.setDuration(packet.getDuration());
            eff.effect.setParticles(!packet.getHideParticles());
            return new PEPacket[]{eff};
         }
      }
   }
}
