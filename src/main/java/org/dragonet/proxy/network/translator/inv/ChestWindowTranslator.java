package org.dragonet.proxy.network.translator.inv;

import org.dragonet.inventory.PEInventorySlot;
import org.dragonet.net.packet.minecraft.BlockEntityDataPacket;
import org.dragonet.net.packet.minecraft.WindowItemsPacket;
import org.dragonet.net.packet.minecraft.WindowOpenPacket;
import org.dragonet.proxy.nbt.tag.CompoundTag;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.cache.CachedWindow;
import org.dragonet.proxy.network.translator.InventoryTranslator;
import org.spacehq.mc.protocol.data.game.Position;

public class ChestWindowTranslator implements InventoryTranslator {
   public boolean open(UpstreamSession session, CachedWindow window) {
      Position pos = new Position((int)session.getEntityCache().getClientEntity().x, (int)session.getEntityCache().getClientEntity().y - 4, (int)session.getEntityCache().getClientEntity().z);
      session.getDataCache().put("window_opened_id", window.windowId);
      session.getDataCache().put("window_block_position", pos);
      session.sendFakeBlock(pos.getX(), pos.getY(), pos.getZ(), 54, 0);
      CompoundTag tag = (new CompoundTag()).putString("id", "Chest").putInt("x", pos.getX()).putInt("y", pos.getY()).putInt("z", pos.getZ());
      session.sendPacket(new BlockEntityDataPacket(pos.getX(), pos.getY(), pos.getZ(), tag));
      WindowOpenPacket pk = new WindowOpenPacket();
      pk.windowID = (byte)(window.windowId & 255);
      pk.slots = (short)(window.size <= 27 ? 27 : 54);
      pk.type = (byte)(window.size <= 27 ? 0 : 0);
      pk.x = pos.getX();
      pk.y = pos.getY();
      pk.z = pos.getZ();
      session.sendPacket(pk);
      return true;
   }

   public void updateContent(UpstreamSession session, CachedWindow window) {
      this.sendContent(session, window);
   }

   public void updateSlot(UpstreamSession session, CachedWindow window, int slotIndex) {
      this.sendContent(session, window);
   }

   private void sendContent(UpstreamSession session, CachedWindow win) {
      WindowItemsPacket pk = new WindowItemsPacket();
      pk.windowID = (byte)(win.windowId & 255);
      pk.slots = new PEInventorySlot[win.slots.length];

      for(int i = 0; i < pk.slots.length; ++i) {
         pk.slots[i] = PEInventorySlot.fromItemStack(win.slots[i]);
      }

      session.sendPacket(pk, true);
   }
}
