package org.dragonet.proxy.network.cache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.spacehq.mc.protocol.data.game.ItemStack;
import org.spacehq.mc.protocol.data.game.values.window.WindowType;

public class CachedWindow {
   public final int windowId;
   public final WindowType pcType;
   public final int size;
   public String title = "Window";
   public final Map<Integer, Integer> properties = Collections.synchronizedMap(new HashMap());
   public ItemStack[] slots;

   public CachedWindow(int windowId, WindowType pcType, int size) {
      this.windowId = windowId;
      this.pcType = pcType;
      this.size = size;
      this.slots = new ItemStack[this.size];
   }
}
