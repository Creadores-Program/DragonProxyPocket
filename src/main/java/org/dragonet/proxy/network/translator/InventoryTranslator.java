package org.dragonet.proxy.network.translator;

import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.cache.CachedWindow;

public interface InventoryTranslator {
   boolean open(UpstreamSession var1, CachedWindow var2);

   void updateContent(UpstreamSession var1, CachedWindow var2);

   void updateSlot(UpstreamSession var1, CachedWindow var2, int var3);
}
