package org.dragonet.proxy.network.translator;

import java.util.Iterator;
import java.util.List;
import org.spacehq.mc.protocol.data.message.ChatColor;
import org.spacehq.mc.protocol.data.message.ChatFormat;
import org.spacehq.mc.protocol.data.message.Message;

public final class MessageTranslator {
   public static String translate(Message message) {
      StringBuilder build = new StringBuilder(message.getText());
      Iterator var2 = message.getExtra().iterator();

      while(var2.hasNext()) {
         Message msg = (Message)var2.next();
         build.append(toMinecraftColor(msg.getStyle().getColor()));
         build.append(toMinecraftFormat(msg.getStyle().getFormats()));
         build.append(msg.getFullText());
      }

      return build.toString();
   }

   private static String toMinecraftFormat(List<ChatFormat> formats) {
      String superBase = "";

      String base;
      for(Iterator var2 = formats.iterator(); var2.hasNext(); superBase = superBase + base) {
         ChatFormat cf = (ChatFormat)var2.next();
         base = "§";
         switch(cf) {
         case BOLD:
            base = base + "l";
            break;
         case ITALIC:
            base = base + "o";
            break;
         case OBFUSCATED:
            base = base + "k";
            break;
         case STRIKETHROUGH:
            base = base + "m";
            break;
         case UNDERLINED:
            base = base + "n";
         }
      }

      return superBase;
   }

   public static String toMinecraftColor(ChatColor color) {
      String base = "§";
      switch(color) {
      case AQUA:
         base = base + "b";
         break;
      case BLACK:
         base = base + "0";
         break;
      case BLUE:
         base = base + "9";
         break;
      case DARK_AQUA:
         base = base + "3";
         break;
      case DARK_BLUE:
         base = base + "1";
         break;
      case DARK_GRAY:
         base = base + "8";
         break;
      case DARK_GREEN:
         base = base + "2";
         break;
      case DARK_PURPLE:
         base = base + "5";
         break;
      case DARK_RED:
         base = base + "4";
         break;
      case GOLD:
         base = base + "6";
         break;
      case GRAY:
         base = base + "7";
         break;
      case GREEN:
         base = base + "a";
         break;
      case LIGHT_PURPLE:
         base = base + "d";
         break;
      case RED:
         base = base + "c";
         break;
      case RESET:
         base = base + "r";
         break;
      case WHITE:
         base = base + "f";
         break;
      case YELLOW:
         base = base + "e";
      }

      return base;
   }
}
