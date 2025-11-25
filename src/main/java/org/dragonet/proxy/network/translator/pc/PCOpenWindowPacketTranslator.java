package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.network.InventoryTranslatorRegister;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;

public class PCOpenWindowPacketTranslator implements PCPacketTranslator<ServerOpenWindowPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerOpenWindowPacket packet) {
      session.getProxy().getGeneralThreadPool().execute(() -> {
         InventoryTranslatorRegister.open(session, packet);
      });
      return null;
   }
}
