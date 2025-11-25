package org.dragonet.proxy.network.translator.pe;

import org.dragonet.net.packet.minecraft.UseItemPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.ItemBlockTranslator;
import org.dragonet.proxy.network.translator.PEPacketTranslator;
import org.spacehq.mc.protocol.data.game.ItemStack;
import org.spacehq.mc.protocol.data.game.Position;
import org.spacehq.mc.protocol.data.game.values.Face;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientPlayerPlaceBlockPacket;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientSwingArmPacket;
import org.spacehq.packetlib.packet.Packet;

public class PEUseItemPacketTranslator implements PEPacketTranslator<UseItemPacket> {
   public Packet[] translate(UpstreamSession session, UseItemPacket packet) {
      if (packet.face == 255) {
         ClientSwingArmPacket pk = new ClientSwingArmPacket();
         return new Packet[]{pk};
      } else {
         ItemStack pcItem = ItemBlockTranslator.translateToPC(packet.item);
         ClientPlayerPlaceBlockPacket pk = new ClientPlayerPlaceBlockPacket(new Position(packet.x, packet.y, packet.z), (Face)MagicValues.key(Face.class, packet.face), pcItem, 0.5F, 0.5F, 0.5F);
         return new Packet[]{pk};
      }
   }
}
