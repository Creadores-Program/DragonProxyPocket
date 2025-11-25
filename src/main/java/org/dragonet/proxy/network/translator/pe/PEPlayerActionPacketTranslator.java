package org.dragonet.proxy.network.translator.pe;

import org.dragonet.net.packet.minecraft.PlayerActionPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.PEPacketTranslator;
import org.spacehq.mc.protocol.data.game.Position;
import org.spacehq.mc.protocol.data.game.values.ClientRequest;
import org.spacehq.mc.protocol.data.game.values.Face;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.player.PlayerAction;
import org.spacehq.mc.protocol.data.game.values.entity.player.PlayerState;
import org.spacehq.mc.protocol.packet.ingame.client.ClientRequestPacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientPlayerActionPacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientPlayerStatePacket;
import org.spacehq.packetlib.packet.Packet;

public class PEPlayerActionPacketTranslator implements PEPacketTranslator<PlayerActionPacket> {
   public Packet[] translate(UpstreamSession session, PlayerActionPacket packet) {
      if (packet.action == 7) {
         return new Packet[]{new ClientRequestPacket(ClientRequest.RESPAWN)};
      } else {
         ClientPlayerStatePacket stat;
         if (packet.action == 9) {
            stat = new ClientPlayerStatePacket((Integer)session.getDataCache().get("player_entity_id"), PlayerState.START_SPRINTING);
            return new Packet[]{stat};
         } else if (packet.action == 10) {
            stat = new ClientPlayerStatePacket((Integer)session.getDataCache().get("player_entity_id"), PlayerState.STOP_SPRINTING);
            return new Packet[]{stat};
         } else if (packet.action == 11) {
            stat = new ClientPlayerStatePacket((Integer)session.getDataCache().get("player_entity_id"), PlayerState.START_SNEAKING);
            return new Packet[]{stat};
         } else if (packet.action == 12) {
            stat = new ClientPlayerStatePacket((Integer)session.getDataCache().get("player_entity_id"), PlayerState.START_SNEAKING);
            return new Packet[]{stat};
         } else if (packet.action == 6) {
            stat = new ClientPlayerStatePacket((Integer)session.getDataCache().get("player_entity_id"), PlayerState.LEAVE_BED);
            return new Packet[]{stat};
         } else {
            ClientPlayerActionPacket act;
            if (packet.action == 5) {
               act = new ClientPlayerActionPacket(PlayerAction.DROP_ITEM, new Position(0, 0, 0), Face.TOP);
               return new Packet[]{act};
            } else if (packet.action == 0) {
               act = new ClientPlayerActionPacket(PlayerAction.START_DIGGING, new Position(packet.x, packet.y, packet.z), (Face)MagicValues.key(Face.class, packet.face));
               session.getDataCache().put("block_breaking_position", act.getPosition());
               return new Packet[]{act};
            } else {
               if (session.getDataCache().containsKey("block_breaking_position")) {
                  if (packet.action == 2) {
                     act = new ClientPlayerActionPacket(PlayerAction.FINISH_DIGGING, (Position)session.getDataCache().remove("block_breaking_position"), (Face)MagicValues.key(Face.class, packet.face));
                     return new Packet[]{act};
                  }

                  if (packet.action == 1) {
                     act = new ClientPlayerActionPacket(PlayerAction.CANCEL_DIGGING, (Position)session.getDataCache().remove("block_breaking_position"), (Face)MagicValues.key(Face.class, packet.face));
                     return new Packet[]{act};
                  }
               }

               return null;
            }
         }
      }
   }
}
