package org.dragonet.proxy.utilities;

public enum MCColor {
   BLACK("0"),
   DARK_BLUE("1"),
   DARK_GREEN("2"),
   DARK_AQUA("3"),
   DARK_RED("4"),
   DARK_PURPLE("5"),
   GOLD("6"),
   GRAY("7"),
   DARK_GRAY("8"),
   BLUE("9"),
   GREEN("a"),
   AQUA("b"),
   RED("c"),
   LIGHT_PURPLE("d"),
   YELLOW("e"),
   WHITE("f"),
   OBFUSCATED("k"),
   BOLD("l"),
   STRIKETHROUGH("m"),
   UNDERLINE("n"),
   ITALIC("o"),
   RESET("r");

   public static final String COLOR_PREFIX_PC = new String(new byte[]{-62, -89});
   public static final String COLOR_PREFIX_PE = "§";
   private String suffix;

   private MCColor(String suffix) {
      this.suffix = suffix;
   }

   public String getPECode() {
      return "§" + this.suffix;
   }

   public String getPCCode() {
      return COLOR_PREFIX_PC + this.suffix;
   }

   public static String cleanAll(String text) {
      String ret = new String(text);
      MCColor[] var2 = values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         MCColor c = var2[var4];
         ret = ret.replace(c.getPECode(), "");
         ret = ret.replace(c.getPCCode(), "");
      }

      return ret;
   }
}
