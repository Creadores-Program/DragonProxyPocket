package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.network.InventoryTranslatorRegister;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.cache.CachedWindow;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;

public class PCSetSlotPacketTranslator implements PCPacketTranslator<ServerSetSlotPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerSetSlotPacket packet) {
      if (!session.getWindowCache().hasWindow(packet.getWindowId())) {
         session.getWindowCache().newCachedPacket(packet.getWindowId(), packet);
         return null;
      } else {
         CachedWindow win = session.getWindowCache().get(packet.getWindowId());
         if (win.pcType == null && packet.getWindowId() != 0) {
            return null;
         } else if (packet.getWindowId() == 0) {
            if (packet.getSlot() >= win.slots.length) {
               return null;
            } else {
               win.slots[packet.getSlot()] = packet.getItem();
               return InventoryTranslatorRegister.sendPlayerInventory(session);
            }
         } else {
            InventoryTranslatorRegister.updateSlot(session, packet);
            return null;
         }
      }
   }
}
