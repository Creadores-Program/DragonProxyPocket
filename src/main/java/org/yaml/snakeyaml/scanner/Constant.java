package org.yaml.snakeyaml.scanner;

import java.util.Arrays;

public final class Constant {
   private static final String ALPHA_S = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-_";
   private static final String LINEBR_S = "\n\u0085\u2028\u2029";
   private static final String FULL_LINEBR_S = "\r\n\u0085\u2028\u2029";
   private static final String NULL_OR_LINEBR_S = "\u0000\r\n\u0085\u2028\u2029";
   private static final String NULL_BL_LINEBR_S = " \u0000\r\n\u0085\u2028\u2029";
   private static final String NULL_BL_T_LINEBR_S = "\t \u0000\r\n\u0085\u2028\u2029";
   private static final String NULL_BL_T_S = "\u0000 \t";
   private static final String URI_CHARS_S = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-_-;/?:@&=+$,_.!~*'()[]%";
   public static final Constant LINEBR = new Constant("\n\u0085\u2028\u2029");
   public static final Constant FULL_LINEBR = new Constant("\r\n\u0085\u2028\u2029");
   public static final Constant NULL_OR_LINEBR = new Constant("\u0000\r\n\u0085\u2028\u2029");
   public static final Constant NULL_BL_LINEBR = new Constant(" \u0000\r\n\u0085\u2028\u2029");
   public static final Constant NULL_BL_T_LINEBR = new Constant("\t \u0000\r\n\u0085\u2028\u2029");
   public static final Constant NULL_BL_T = new Constant("\u0000 \t");
   public static final Constant URI_CHARS = new Constant("abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-_-;/?:@&=+$,_.!~*'()[]%");
   public static final Constant ALPHA = new Constant("abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-_");
   private String content;
   boolean[] contains = new boolean[128];
   boolean noASCII = false;

   private Constant(String content) {
      Arrays.fill(this.contains, false);
      StringBuilder sb = new StringBuilder();

      for(int i = 0; i < content.length(); ++i) {
         char ch = content.charAt(i);
         if (ch < 128) {
            this.contains[ch] = true;
         } else {
            sb.append(ch);
         }
      }

      if (sb.length() > 0) {
         this.noASCII = true;
         this.content = sb.toString();
      }

   }

   public boolean has(char ch) {
      return ch < 128 ? this.contains[ch] : this.noASCII && this.content.indexOf(ch, 0) != -1;
   }

   public boolean hasNo(char ch) {
      return !this.has(ch);
   }

   public boolean has(char ch, String additional) {
      return this.has(ch) || additional.indexOf(ch, 0) != -1;
   }

   public boolean hasNo(char ch, String additional) {
      return !this.has(ch, additional);
   }
}
