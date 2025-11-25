package org.spacehq.mc.protocol.data.message;

public enum ChatFormat {
   BOLD,
   UNDERLINED,
   STRIKETHROUGH,
   ITALIC,
   OBFUSCATED;

   public String toString() {
      return this.name().toLowerCase();
   }

   public static ChatFormat byName(String name) {
      name = name.toLowerCase();
      ChatFormat[] var1 = values();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         ChatFormat format = var1[var3];
         if (format.toString().equals(name)) {
            return format;
         }
      }

      return null;
   }
}
