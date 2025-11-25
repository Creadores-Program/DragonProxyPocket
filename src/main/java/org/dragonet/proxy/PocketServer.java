package org.dragonet.proxy;

import java.util.Map;
import org.dragonet.proxy.configuration.RemoteServer;

public class PocketServer extends RemoteServer {
   public static PocketServer deserialize(Map<String, Object> map) {
      return (PocketServer)delicatedDeserialize(new PocketServer(), map);
   }
}
