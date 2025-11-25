package org.dragonet.proxy.commands;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.dragonet.proxy.DragonProxy;

public final class CommandRegister {
   private final Map<String, ConsoleCommand> commandMap = Collections.synchronizedMap(new HashMap());
   private final DragonProxy proxy;

   public CommandRegister(DragonProxy proxy) {
      this.proxy = proxy;
      this.registerDefaults();
   }

   public void registerDefaults() {
      this.commandMap.put("stop", new StopCommand());
      this.commandMap.put("help", new HelpCommand());
      this.commandMap.put("test", new TestCommand());
   }

   public void callCommand(String cmd) {
      String trimedCmd = cmd.trim();
      String label = null;
      String[] args = null;
      if (!cmd.trim().contains(" ")) {
         label = trimedCmd.toLowerCase();
         args = new String[0];
      } else {
         label = trimedCmd.substring(0, trimedCmd.indexOf(" ")).toLowerCase();
         String argLine = trimedCmd.substring(trimedCmd.indexOf(" ") + 1);
         args = argLine.contains(" ") ? argLine.split(" ") : new String[]{argLine};
      }

      if (label == null) {
         this.proxy.getLogger().warning(this.proxy.getLang().get("command_not_found"));
      } else {
         ConsoleCommand command = (ConsoleCommand)this.commandMap.get(label);
         if (command == null) {
            this.proxy.getLogger().warning(this.proxy.getLang().get("command_not_found"));
         } else {
            command.execute(this.proxy, args);
         }
      }
   }
}
