package org.dragonet.proxy.network;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.dragonet.proxy.DragonProxy;

public class SessionRegister {
   private final DragonProxy proxy;
   private final Map<String, UpstreamSession> clients = Collections.synchronizedMap(new HashMap());

   public SessionRegister(DragonProxy proxy) {
      this.proxy = proxy;
   }

   public void onTick() {
      Iterator iterator = this.clients.entrySet().iterator();

      while(iterator.hasNext()) {
         Entry<String, UpstreamSession> ent = (Entry)iterator.next();
         ((UpstreamSession)ent.getValue()).onTick();
      }

   }

   public void newSession(UpstreamSession session) {
      this.clients.put(session.getRaknetID(), session);
   }

   public void removeSession(UpstreamSession session) {
      this.clients.remove(session.getRaknetID());
   }

   public UpstreamSession getSession(String identifier) {
      return (UpstreamSession)this.clients.get(identifier);
   }

   public Map<String, UpstreamSession> getAll() {
      return Collections.unmodifiableMap(this.clients);
   }

   public int getOnlineCount() {
      return this.clients.size();
   }
}
