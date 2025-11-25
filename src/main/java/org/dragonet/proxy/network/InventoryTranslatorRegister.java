package org.dragonet.proxy.network;

import java.util.HashMap;
import java.util.Map;
import org.dragonet.inventory.PEInventorySlot;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.net.packet.minecraft.WindowClosePacket;
import org.dragonet.net.packet.minecraft.WindowItemsPacket;
import org.dragonet.proxy.network.cache.CachedWindow;
import org.dragonet.proxy.network.translator.InventoryTranslator;
import org.dragonet.proxy.network.translator.inv.ChestWindowTranslator;
import org.spacehq.mc.protocol.data.game.Position;
import org.spacehq.mc.protocol.data.game.values.window.WindowType;
import org.spacehq.mc.protocol.packet.ingame.client.window.ClientCloseWindowPacket;
import org.spacehq.mc.protocol.packet.ingame.server.window.ServerOpenWindowPacket;
import org.spacehq.mc.protocol.packet.ingame.server.window.ServerSetSlotPacket;
import org.spacehq.mc.protocol.packet.ingame.server.window.ServerWindowItemsPacket;
import org.spacehq.packetlib.packet.Packet;

public final class InventoryTranslatorRegister {
   public static final int[] HOTBAR_CONSTANTS = new int[]{36, 37, 38, 39, 40, 41, 42, 43, 44};
   private static final Map<WindowType, InventoryTranslator> TRANSLATORS = new HashMap();

   public static PEPacket[] sendPlayerInventory(UpstreamSession session) {
      CachedWindow win = session.getWindowCache().getPlayerInventory();
      WindowItemsPacket ret = new WindowItemsPacket();
      ret.windowID = 0;
      ret.slots = new PEInventorySlot[45];

      int i;
      for(i = 9; i < win.slots.length; ++i) {
         if (win.slots[i] != null) {
            ret.slots[i - 9] = PEInventorySlot.fromItemStack(win.slots[i]);
         }
      }

      for(i = 36; i < 45; ++i) {
         ret.slots[i] = ret.slots[i - 9];
      }

      ret.hotbar = HOTBAR_CONSTANTS;
      return new PEPacket[]{ret};
   }

   public static void closeOpened(UpstreamSession session, boolean byServer) {
      if (session.getDataCache().containsKey("window_opened_id")) {
         int id = (Integer)session.getDataCache().remove("window_opened_id");
         if (!byServer) {
            session.getDownstream().send((Object)(new ClientCloseWindowPacket(id)));
         }

         if (session.getDataCache().containsKey("window_block_position")) {
            session.sendFakeBlock(((Position)session.getDataCache().get("window_block_position")).getX(), ((Position)session.getDataCache().get("window_block_position")).getY(), ((Position)session.getDataCache().remove("window_block_position")).getZ(), 1, 0);
         }

         if (byServer) {
            WindowClosePacket pkClose = new WindowClosePacket();
            pkClose.windowID = (byte)(id & 255);
            session.sendPacket(pkClose, true);
         }
      }

   }

   public static void open(UpstreamSession session, ServerOpenWindowPacket win) {
      closeOpened(session, true);
      if (TRANSLATORS.containsKey(win.getType())) {
         CachedWindow cached = new CachedWindow(win.getWindowId(), win.getType(), 36 + win.getSlots());
         session.getWindowCache().cacheWindow(cached);
         ((InventoryTranslator)TRANSLATORS.get(win.getType())).open(session, cached);
         Packet[] items = session.getWindowCache().getCachedPackets(win.getWindowId());
         Packet[] var4 = items;
         int var5 = items.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            Packet item = var4[var6];
            if (item != null) {
               if (ServerWindowItemsPacket.class.isAssignableFrom(item.getClass())) {
                  updateContent(session, (ServerWindowItemsPacket)item);
               } else {
                  updateSlot(session, (ServerSetSlotPacket)item);
               }
            }
         }
      } else {
         session.getDownstream().send((Object)(new ClientCloseWindowPacket(win.getWindowId())));
      }

   }

   public static void updateSlot(UpstreamSession session, ServerSetSlotPacket packet) {
      if (packet.getWindowId() != 0) {
         if (session.getDataCache().containsKey("window_opened_id") && session.getWindowCache().hasWindow(packet.getWindowId())) {
            int openedId = (Integer)session.getDataCache().get("window_opened_id");
            if (packet.getWindowId() != openedId) {
               closeOpened(session, true);
               session.getDownstream().send((Object)(new ClientCloseWindowPacket(packet.getWindowId())));
            } else {
               CachedWindow win = session.getWindowCache().get(openedId);
               System.out.println("WIN=" + win.slots.length + ", REQ_SLOT=" + packet.getSlot());
               if (win.size <= packet.getSlot()) {
                  session.getDownstream().send((Object)(new ClientCloseWindowPacket(packet.getWindowId())));
               } else {
                  InventoryTranslator t = (InventoryTranslator)TRANSLATORS.get(win.pcType);
                  if (t == null) {
                     session.getDownstream().send((Object)(new ClientCloseWindowPacket(packet.getWindowId())));
                  } else {
                     win.slots[packet.getSlot()] = packet.getItem();
                     t.updateSlot(session, win, packet.getSlot());
                  }
               }
            }
         } else {
            session.getDownstream().send((Object)(new ClientCloseWindowPacket(packet.getWindowId())));
         }
      }
   }

   public static void updateContent(UpstreamSession session, ServerWindowItemsPacket packet) {
      if (packet.getWindowId() != 0) {
         if (session.getDataCache().containsKey("window_opened_id") && session.getWindowCache().hasWindow(packet.getWindowId())) {
            int openedId = (Integer)session.getDataCache().get("window_opened_id");
            if (packet.getWindowId() != openedId) {
               closeOpened(session, true);
            } else {
               CachedWindow win = session.getWindowCache().get(openedId);
               InventoryTranslator t = (InventoryTranslator)TRANSLATORS.get(win.pcType);
               if (t == null) {
                  session.getDownstream().send((Object)(new ClientCloseWindowPacket(packet.getWindowId())));
               } else {
                  win.slots = packet.getItems();
                  t.updateContent(session, win);
               }
            }
         } else {
            session.getDownstream().send((Object)(new ClientCloseWindowPacket(packet.getWindowId())));
         }
      }
   }

   static {
      TRANSLATORS.put(WindowType.CHEST, new ChestWindowTranslator());
   }
}
