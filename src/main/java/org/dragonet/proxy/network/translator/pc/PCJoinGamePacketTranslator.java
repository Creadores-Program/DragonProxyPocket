package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.net.packet.minecraft.SetPlayerGameTypePacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.data.game.values.entity.player.GameMode;
import org.spacehq.mc.protocol.packet.ingame.server.ServerJoinGamePacket;

public class PCJoinGamePacketTranslator implements PCPacketTranslator<ServerJoinGamePacket> {
   public PEPacket[] translate(UpstreamSession session, ServerJoinGamePacket packet) {
      session.getDataCache().put("player_entity_id", packet.getEntityId());
      if (session.getProxy().getAuthMode().equals("online")) {
         SetPlayerGameTypePacket pkSetGameMode = new SetPlayerGameTypePacket(packet.getGameMode() == GameMode.CREATIVE ? 1 : 0);
         return new PEPacket[]{pkSetGameMode};
      } else {
         session.getDataCache().put("achedJoinGamePacket", packet);
         return null;
      }
   }
}
