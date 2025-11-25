package org.spacehq.mc.protocol.data.message;

public enum ClickAction {
   RUN_COMMAND,
   SUGGEST_COMMAND,
   OPEN_URL,
   OPEN_FILE;

   public String toString() {
      return this.name().toLowerCase();
   }

   public static ClickAction byName(String name) {
      name = name.toLowerCase();
      ClickAction[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         ClickAction action = var1[var3];
         if (action.toString().equals(name)) {
            return action;
         }
      }

      return null;
   }
}
