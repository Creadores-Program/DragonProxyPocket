package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.net.packet.minecraft.UpdateBlockPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.ItemBlockTranslator;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerMultiBlockChangePacket;

public class PCMultiBlockChangePacketTranslator implements PCPacketTranslator<ServerMultiBlockChangePacket> {
   public PEPacket[] translate(UpstreamSession session, ServerMultiBlockChangePacket packet) {
      UpdateBlockPacket pk = new UpdateBlockPacket();
      pk.records = new UpdateBlockPacket.UpdateBlockRecord[packet.getRecords().length];
      byte generalFlag = packet.getRecords().length > 64 ? 8 : 3;

      for(int i = 0; i < pk.records.length; ++i) {
         pk.records[i] = new UpdateBlockPacket.UpdateBlockRecord();
         pk.records[i].flags = (byte)generalFlag;
         pk.records[i].x = packet.getRecords()[i].getPosition().getX();
         pk.records[i].y = (byte)(packet.getRecords()[i].getPosition().getY() & 255);
         pk.records[i].z = packet.getRecords()[i].getPosition().getZ();
         pk.records[i].block = (byte)(ItemBlockTranslator.translateToPE(packet.getRecords()[i].getId()) & 255);
         pk.records[i].meta = (byte)(packet.getRecords()[i].getData() & 255);
      }

      return new PEPacket[]{pk};
   }
}
