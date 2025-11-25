package org.spacehq.mc.protocol.data.message;

public enum HoverAction {
   SHOW_TEXT,
   SHOW_ITEM,
   SHOW_ACHIEVEMENT,
   SHOW_ENTITY;

   public String toString() {
      return this.name().toLowerCase();
   }

   public static HoverAction byName(String name) {
      name = name.toLowerCase();
      HoverAction[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         HoverAction action = var1[var3];
         if (action.toString().equals(name)) {
            return action;
         }
      }

      return null;
   }
}
