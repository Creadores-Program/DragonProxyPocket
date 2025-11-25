package org.dragonet.proxy.commands;

import org.dragonet.proxy.DragonProxy;

public class StopCommand implements ConsoleCommand {
   public void execute(DragonProxy proxy, String[] args) {
      proxy.shutdown();
   }
}
