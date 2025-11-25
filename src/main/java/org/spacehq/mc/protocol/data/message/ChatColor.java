package org.spacehq.mc.protocol.data.message;

public enum ChatColor {
   BLACK,
   DARK_BLUE,
   DARK_GREEN,
   DARK_AQUA,
   DARK_RED,
   DARK_PURPLE,
   GOLD,
   GRAY,
   DARK_GRAY,
   BLUE,
   GREEN,
   AQUA,
   RED,
   LIGHT_PURPLE,
   YELLOW,
   WHITE,
   RESET;

   public String toString() {
      return this.name().toLowerCase();
   }

   public static ChatColor byName(String name) {
      name = name.toLowerCase();
      ChatColor[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         ChatColor color = var1[var3];
         if (color.toString().equals(name)) {
            return color;
         }
      }

      return null;
   }
}
