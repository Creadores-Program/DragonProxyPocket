package org.dragonet.proxy;

import java.util.Map;
import org.dragonet.proxy.configuration.RemoteServer;

public class DesktopServer extends RemoteServer {
   public static DesktopServer deserialize(Map<String, Object> map) {
      return (DesktopServer)delicatedDeserialize(new DesktopServer(), map);
   }
}
