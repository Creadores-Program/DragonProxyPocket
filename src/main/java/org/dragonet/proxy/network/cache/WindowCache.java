package org.dragonet.proxy.network.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dragonet.proxy.network.UpstreamSession;
import org.spacehq.mc.protocol.data.game.values.window.WindowType;
import org.spacehq.packetlib.packet.Packet;

public final class WindowCache {
   private final UpstreamSession upstream;
   private final Map<Integer, CachedWindow> windows = Collections.synchronizedMap(new HashMap());
   private final Map<Integer, List<Packet>> cachedItems = Collections.synchronizedMap(new HashMap());

   public WindowCache(UpstreamSession upstream) {
      this.upstream = upstream;
      CachedWindow inv = new CachedWindow(0, (WindowType)null, 45);
      this.windows.put(0, inv);
   }

   public CachedWindow getPlayerInventory() {
      return (CachedWindow)this.windows.get(0);
   }

   public void cacheWindow(CachedWindow win) {
      this.windows.put(win.windowId, win);
   }

   public CachedWindow removeWindow(int id) {
      return (CachedWindow)this.windows.remove(id);
   }

   public CachedWindow get(int id) {
      return (CachedWindow)this.windows.get(id);
   }

   public boolean hasWindow(int id) {
      return this.windows.containsKey(id);
   }

   public void newCachedPacket(int windowId, Packet packet) {
      List<Packet> packets = null;
      synchronized(this.cachedItems) {
         packets = (List)this.cachedItems.get(windowId);
         if (packets == null) {
            packets = new ArrayList();
            this.cachedItems.put(windowId, packets);
         }
      }

      ((List)packets).add(packet);
   }

   public Packet[] getCachedPackets(int windowId) {
      List<Packet> packets = null;
      packets = (List)this.cachedItems.remove(windowId);
      return packets == null ? null : (Packet[])packets.toArray(new Packet[0]);
   }

   public UpstreamSession getUpstream() {
      return this.upstream;
   }
}
