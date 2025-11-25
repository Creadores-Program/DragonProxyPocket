package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.net.packet.minecraft.SetTimePacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerUpdateTimePacket;

public class PCUpdateTimePacketTranslator implements PCPacketTranslator<ServerUpdateTimePacket> {
   public PEPacket[] translate(UpstreamSession session, ServerUpdateTimePacket packet) {
      SetTimePacket pk = new SetTimePacket((int)packet.getTime(), true);
      return new PEPacket[]{pk};
   }
}
