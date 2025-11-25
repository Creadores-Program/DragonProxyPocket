package org.dragonet.proxy.network.translator.pe;

import org.dragonet.net.packet.minecraft.WindowClosePacket;
import org.dragonet.proxy.network.InventoryTranslatorRegister;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.PEPacketTranslator;
import org.spacehq.packetlib.packet.Packet;

public class PEWindowClosePacketTranslator implements PEPacketTranslator<WindowClosePacket> {
   public Packet[] translate(UpstreamSession session, WindowClosePacket packet) {
      session.getProxy().getGeneralThreadPool().execute(() -> {
         InventoryTranslatorRegister.closeOpened(session, false);
      });
      return null;
   }
}
