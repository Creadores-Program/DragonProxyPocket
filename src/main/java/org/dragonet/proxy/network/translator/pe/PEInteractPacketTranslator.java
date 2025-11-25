package org.dragonet.proxy.network.translator.pe;

import org.dragonet.net.packet.minecraft.InteractPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.PEPacketTranslator;
import org.spacehq.mc.protocol.data.game.values.entity.player.InteractAction;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientPlayerInteractEntityPacket;
import org.spacehq.packetlib.packet.Packet;

public class PEInteractPacketTranslator implements PEPacketTranslator<InteractPacket> {
   public Packet[] translate(UpstreamSession session, InteractPacket packet) {
      ClientPlayerInteractEntityPacket pk = new ClientPlayerInteractEntityPacket((int)(packet.target & -1L), InteractAction.ATTACK);
      return new Packet[]{pk};
   }
}
