package org.dragonet.proxy.network.translator.pe;

import org.dragonet.net.packet.minecraft.PlayerEquipmentPacket;
import org.dragonet.proxy.network.InventoryTranslatorRegister;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.cache.CachedWindow;
import org.dragonet.proxy.network.translator.PEPacketTranslator;
import org.spacehq.mc.protocol.data.game.ItemStack;
import org.spacehq.mc.protocol.data.game.values.window.ClickItemParam;
import org.spacehq.mc.protocol.data.game.values.window.WindowAction;
import org.spacehq.mc.protocol.packet.ingame.client.player.ClientChangeHeldItemPacket;
import org.spacehq.mc.protocol.packet.ingame.client.window.ClientWindowActionPacket;
import org.spacehq.packetlib.packet.Packet;

public class PEPlayerEquipmentPacketTranslator implements PEPacketTranslator<PlayerEquipmentPacket> {
   public Packet[] translate(UpstreamSession session, PlayerEquipmentPacket packet) {
      if (packet.selectedSlot > 8) {
         return null;
      } else {
         if (packet.slot != 40 && packet.slot != 0 && packet.slot == 255) {
         }

         if (InventoryTranslatorRegister.HOTBAR_CONSTANTS[packet.selectedSlot] == packet.slot) {
            ClientChangeHeldItemPacket pk = new ClientChangeHeldItemPacket(packet.selectedSlot);
            return new Packet[]{pk};
         } else {
            CachedWindow playerInv = session.getWindowCache().getPlayerInventory();
            ItemStack tmp = playerInv.slots[36 + packet.selectedSlot];
            boolean hotbarSlotWasEmpty = tmp == null || tmp.getId() == 0;
            boolean invSlotWasEmpty = playerInv.slots[packet.slot] == null || playerInv.slots[packet.slot].getId() == 0;
            playerInv.slots[36 + packet.selectedSlot] = playerInv.slots[packet.slot];
            playerInv.slots[packet.slot] = tmp;
            session.sendAllPackets(InventoryTranslatorRegister.sendPlayerInventory(session), true);
            ClientWindowActionPacket act1;
            ClientWindowActionPacket act2;
            if (!invSlotWasEmpty) {
               act1 = new ClientWindowActionPacket(0, (short)((int)(System.currentTimeMillis() & 65535L)), packet.slot, playerInv.slots[36 + packet.selectedSlot], WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK);
               act2 = new ClientWindowActionPacket(0, (short)((int)(System.currentTimeMillis() & 65535L)), 36 + packet.selectedSlot, tmp, WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK);
               if (!hotbarSlotWasEmpty) {
                  ClientWindowActionPacket act3 = new ClientWindowActionPacket(0, (short)((int)(System.currentTimeMillis() & 65535L)), packet.slot, (ItemStack)null, WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK);
                  return new Packet[]{act1, act2, act3};
               } else {
                  return new Packet[]{act1, act2};
               }
            } else {
               act1 = new ClientWindowActionPacket(0, (short)((int)(System.currentTimeMillis() & 65535L)), 36 + packet.selectedSlot, playerInv.slots[36 + packet.selectedSlot], WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK);
               act2 = new ClientWindowActionPacket(0, (short)((int)(System.currentTimeMillis() & 65535L)), packet.slot, (ItemStack)null, WindowAction.CLICK_ITEM, ClickItemParam.LEFT_CLICK);
               return new Packet[]{act1, act2};
            }
         }
      }
   }
}
