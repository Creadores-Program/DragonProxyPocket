package com.google.common.escape;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

@Beta
@GwtCompatible
public final class CharEscaperBuilder {
   private final Map<Character, String> map = new HashMap();
   private int max = -1;

   public CharEscaperBuilder addEscape(char c, String r) {
      this.map.put(c, Preconditions.checkNotNull(r));
      if (c > this.max) {
         this.max = c;
      }

      return this;
   }

   public CharEscaperBuilder addEscapes(char[] cs, String r) {
      Preconditions.checkNotNull(r);
      char[] arr$ = cs;
      int len$ = cs.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         char c = arr$[i$];
         this.addEscape(c, r);
      }

      return this;
   }

   public char[][] toArray() {
      char[][] result = new char[this.max + 1][];

      Entry entry;
      for(Iterator i$ = this.map.entrySet().iterator(); i$.hasNext(); result[(Character)entry.getKey()] = ((String)entry.getValue()).toCharArray()) {
         entry = (Entry)i$.next();
      }

      return result;
   }

   public Escaper toEscaper() {
      return new CharEscaperBuilder.CharArrayDecorator(this.toArray());
   }

   private static class CharArrayDecorator extends CharEscaper {
      private final char[][] replacements;
      private final int replaceLength;

      CharArrayDecorator(char[][] replacements) {
         this.replacements = replacements;
         this.replaceLength = replacements.length;
      }

      public String escape(String s) {
         int slen = s.length();

         for(int index = 0; index < slen; ++index) {
            char c = s.charAt(index);
            if (c < this.replacements.length && this.replacements[c] != null) {
               return this.escapeSlow(s, index);
            }
         }

         return s;
      }

      protected char[] escape(char c) {
         return c < this.replaceLength ? this.replacements[c] : null;
      }
   }
}
