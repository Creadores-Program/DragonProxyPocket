package org.dragonet.proxy.configuration;

import java.util.Map;

public class ServerConfig {
   private String lang = "default";
   private String udp_bind_ip = "0.0.0.0";
   private int udp_bind_port = 19132;
   private String motd = "&aServer by DragonProxy";
   private String default_server = "NONE";
   private Map<String, RemoteServer> remote_servers;
   private String mode = "cls";
   private String command_prefix = "/";
   private int max_players = -1;
   private boolean log_console = true;
   private int thread_pool_size;

   public String getLang() {
      return this.lang;
   }

   public void setLang(String lang) {
      this.lang = lang;
   }

   public String getUdp_bind_ip() {
      return this.udp_bind_ip;
   }

   public void setUdp_bind_ip(String udp_bind_ip) {
      this.udp_bind_ip = udp_bind_ip;
   }

   public int getUdp_bind_port() {
      return this.udp_bind_port;
   }

   public void setUdp_bind_port(int udp_bind_port) {
      this.udp_bind_port = udp_bind_port;
   }

   public String getMotd() {
      return this.motd;
   }

   public void setMotd(String motd) {
      this.motd = motd;
   }

   public String getDefault_server() {
      return this.default_server;
   }

   public void setDefault_server(String default_server) {
      this.default_server = default_server;
   }

   public Map<String, RemoteServer> getRemote_servers() {
      return this.remote_servers;
   }

   public void setRemote_servers(Map<String, RemoteServer> remote_servers) {
      this.remote_servers = remote_servers;
   }

   public String getMode() {
      return this.mode;
   }

   public void setMode(String mode) {
      this.mode = mode;
   }

   public String getCommand_prefix() {
      return this.command_prefix;
   }

   public void setCommand_prefix(String command_prefix) {
      this.command_prefix = command_prefix;
   }

   public int getMax_players() {
      return this.max_players;
   }

   public void setMax_players(int max_players) {
      this.max_players = max_players;
   }

   public boolean isLog_console() {
      return this.log_console;
   }

   public void setLog_console(boolean log_console) {
      this.log_console = log_console;
   }

   public int getThread_pool_size() {
      return this.thread_pool_size;
   }

   public void setThread_pool_size(int thread_pool_size) {
      this.thread_pool_size = thread_pool_size;
   }
}
