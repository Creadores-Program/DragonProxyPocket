package org.dragonet.proxy.configuration;

import java.util.LinkedHashMap;
import java.util.Map;
import org.dragonet.configuration.serialization.ConfigurationSerializable;

public abstract class RemoteServer implements ConfigurationSerializable {
   private String remoteAddr;
   private int remotePort;

   public void setRemote_addr(String remoteAddr) {
      this.setRemoteAddr(remoteAddr);
   }

   public void setRemote_port(int reportPort) {
      this.setRemotePort(reportPort);
   }

   public Map<String, Object> serialize() {
      Map<String, Object> map = new LinkedHashMap();
      map.put("remote_addr", this.remoteAddr);
      map.put("remote_port", this.remotePort);
      return map;
   }

   public static RemoteServer delicatedDeserialize(RemoteServer server, Map<String, Object> map) {
      server.remoteAddr = (String)map.get("remote_addr");
      server.remotePort = ((Number)map.get("remote_port")).intValue();
      return server;
   }

   public String getRemoteAddr() {
      return this.remoteAddr;
   }

   public void setRemoteAddr(String remoteAddr) {
      this.remoteAddr = remoteAddr;
   }

   public int getRemotePort() {
      return this.remotePort;
   }

   public void setRemotePort(int remotePort) {
      this.remotePort = remotePort;
   }
}
