package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.net.packet.minecraft.UpdateBlockPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.ItemBlockTranslator;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerBlockChangePacket;

public class PCBlockChangePacketTranslator implements PCPacketTranslator<ServerBlockChangePacket> {
   public PEPacket[] translate(UpstreamSession session, ServerBlockChangePacket packet) {
      UpdateBlockPacket pk = new UpdateBlockPacket();
      pk.records = new UpdateBlockPacket.UpdateBlockRecord[]{new UpdateBlockPacket.UpdateBlockRecord()};
      pk.records[0].flags = 3;
      pk.records[0].block = (byte)(ItemBlockTranslator.translateToPE(packet.getRecord().getId()) & 255);
      pk.records[0].meta = (byte)(packet.getRecord().getData() & 255);
      pk.records[0].x = packet.getRecord().getPosition().getX();
      pk.records[0].y = (byte)(packet.getRecord().getPosition().getY() & 255);
      pk.records[0].z = packet.getRecord().getPosition().getZ();
      return new PEPacket[]{pk};
   }
}
