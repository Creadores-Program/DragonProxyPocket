package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.net.packet.minecraft.RespawnPacket;
import org.dragonet.net.packet.minecraft.SetHealthPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerUpdateHealthPacket;

public class PCUpdateHealthPacketTranslator implements PCPacketTranslator<ServerUpdateHealthPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerUpdateHealthPacket packet) {
      SetHealthPacket h = new SetHealthPacket((int)packet.getHealth());
      if (packet.getHealth() > 0.0F && h.health <= 0) {
         h.health = 1;
      }

      if (packet.getHealth() <= 0.0F) {
         RespawnPacket r = new RespawnPacket();
         r.x = 0.0F;
         r.y = 0.0F;
         r.z = 0.0F;
         return new PEPacket[]{h, r};
      } else {
         return new PEPacket[]{h};
      }
   }
}
