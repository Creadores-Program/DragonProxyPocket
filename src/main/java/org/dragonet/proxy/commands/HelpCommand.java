package org.dragonet.proxy.commands;

import org.dragonet.proxy.DragonProxy;

public class HelpCommand implements ConsoleCommand {
   public void execute(DragonProxy proxy, String[] args) {
      proxy.getLogger().info("---- All commands for DragonProxy ----");
      proxy.getLogger().info("help - Show this help page");
      proxy.getLogger().info("stop - Stop DragonProxy server!");
      proxy.getLogger().info("test - For testing only\n");
   }
}
