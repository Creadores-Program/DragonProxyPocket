package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.network.InventoryTranslatorRegister;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.cache.CachedWindow;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.packet.ingame.server.window.ServerWindowItemsPacket;

public class PCWindowItemsTranslator implements PCPacketTranslator<ServerWindowItemsPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerWindowItemsPacket packet) {
      if (!session.getWindowCache().hasWindow(packet.getWindowId())) {
         session.getWindowCache().newCachedPacket(packet.getWindowId(), packet);
         return null;
      } else {
         CachedWindow win = session.getWindowCache().get(packet.getWindowId());
         if (win.pcType == null && packet.getWindowId() == 0) {
            if (packet.getItems().length < 45) {
               return null;
            } else {
               win.slots = packet.getItems();
               return InventoryTranslatorRegister.sendPlayerInventory(session);
            }
         } else {
            InventoryTranslatorRegister.updateContent(session, packet);
            return null;
         }
      }
   }
}
