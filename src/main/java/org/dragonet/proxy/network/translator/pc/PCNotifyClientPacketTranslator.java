package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.LevelEventPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.net.packet.minecraft.SetPlayerGameTypePacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.data.game.values.entity.player.GameMode;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerNotifyClientPacket;

public class PCNotifyClientPacketTranslator implements PCPacketTranslator<ServerNotifyClientPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerNotifyClientPacket packet) {
      switch(packet.getNotification()) {
      case CHANGE_GAMEMODE:
         GameMode gm = (GameMode)packet.getValue();
         SetPlayerGameTypePacket pk = new SetPlayerGameTypePacket();
         if (gm == GameMode.CREATIVE) {
            pk.gamemode = 1;
         } else {
            pk.gamemode = 0;
         }

         return new PEPacket[]{pk};
      case START_RAIN:
         LevelEventPacket evtStartRain = new LevelEventPacket();
         evtStartRain.eventID = 3001;
         return new PEPacket[]{evtStartRain};
      case STOP_RAIN:
         LevelEventPacket evtStopRain = new LevelEventPacket();
         evtStopRain.eventID = 3003;
         return new PEPacket[]{evtStopRain};
      default:
         return null;
      }
   }
}
