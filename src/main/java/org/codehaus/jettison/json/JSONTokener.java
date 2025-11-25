package org.codehaus.jettison.json;

public class JSONTokener {
   private int myIndex = 0;
   private String mySource;
   private int threshold = -1;

   public JSONTokener(String s) {
      this.mySource = s;
   }

   public JSONTokener(String s, int threshold) {
      this.mySource = s;
      this.threshold = threshold;
   }

   public int getThreshold() {
      return this.threshold;
   }

   public void back() {
      if (this.myIndex > 0) {
         --this.myIndex;
      }

   }

   public static int dehexchar(char c) {
      if (c >= '0' && c <= '9') {
         return c - 48;
      } else if (c >= 'A' && c <= 'F') {
         return c - 55;
      } else {
         return c >= 'a' && c <= 'f' ? c - 87 : -1;
      }
   }

   public boolean more() {
      return this.myIndex < this.mySource.length();
   }

   public char next() {
      if (this.more()) {
         char c = this.mySource.charAt(this.myIndex);
         ++this.myIndex;
         return c;
      } else {
         return '\u0000';
      }
   }

   public char next(char c) throws JSONException {
      char n = this.next();
      if (n != c) {
         throw this.syntaxError("Expected '" + c + "' and instead saw '" + n + "'.");
      } else {
         return n;
      }
   }

   public String next(int n) throws JSONException {
      int i = this.myIndex;
      int j = i + n;
      if (j >= this.mySource.length()) {
         throw this.syntaxError("Substring bounds error");
      } else {
         this.myIndex += n;
         return this.mySource.substring(i, j);
      }
   }

   public char nextClean() throws JSONException {
      label51:
      while(true) {
         char c = this.next();
         if (c == '/') {
            switch(this.next()) {
            case '*':
               while(true) {
                  c = this.next();
                  if (c == 0) {
                     throw this.syntaxError("Unclosed comment.");
                  }

                  if (c == '*') {
                     if (this.next() == '/') {
                        continue label51;
                     }

                     this.back();
                  }
               }
            case '/':
               while(true) {
                  c = this.next();
                  if (c == '\n' || c == '\r' || c == 0) {
                     continue label51;
                  }
               }
            default:
               this.back();
               return '/';
            }
         } else if (c == '#') {
            while(true) {
               c = this.next();
               if (c == '\n' || c == '\r' || c == 0) {
                  break;
               }
            }
         } else if (c == 0 || c > ' ') {
            return c;
         }
      }
   }

   public String nextString(char quote) throws JSONException {
      StringBuilder sb = new StringBuilder();

      while(true) {
         char c = this.next();
         switch(c) {
         case '\u0000':
         case '\n':
         case '\r':
            throw this.syntaxError("Unterminated string");
         case '\\':
            c = this.next();
            switch(c) {
            case 'b':
               sb.append('\b');
               continue;
            case 'c':
            case 'd':
            case 'e':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'o':
            case 'p':
            case 'q':
            case 's':
            case 'v':
            case 'w':
            default:
               sb.append(c);
               continue;
            case 'f':
               sb.append('\f');
               continue;
            case 'n':
               sb.append('\n');
               continue;
            case 'r':
               sb.append('\r');
               continue;
            case 't':
               sb.append('\t');
               continue;
            case 'u':
               sb.append((char)Integer.parseInt(this.next((int)4), 16));
               continue;
            case 'x':
               sb.append((char)Integer.parseInt(this.next((int)2), 16));
               continue;
            }
         default:
            if (c == quote) {
               return sb.toString();
            }

            sb.append(c);
         }
      }
   }

   public String nextTo(char d) {
      StringBuilder sb = new StringBuilder();

      while(true) {
         char c = this.next();
         if (c == d || c == 0 || c == '\n' || c == '\r') {
            if (c != 0) {
               this.back();
            }

            return sb.toString().trim();
         }

         sb.append(c);
      }
   }

   public String nextTo(String delimiters) {
      StringBuilder sb = new StringBuilder();

      while(true) {
         char c = this.next();
         if (delimiters.indexOf(c) >= 0 || c == 0 || c == '\n' || c == '\r') {
            if (c != 0) {
               this.back();
            }

            return sb.toString().trim();
         }

         sb.append(c);
      }
   }

   public Object nextValue() throws JSONException {
      char c = this.nextClean();
      switch(c) {
      case '"':
      case '\'':
         return this.nextString(c);
      case '[':
         this.back();
         return this.newJSONArray();
      case '{':
         this.back();
         return this.newJSONObject();
      default:
         StringBuilder sb = new StringBuilder();

         char b;
         for(b = c; c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0; c = this.next()) {
            sb.append(c);
         }

         this.back();
         String s = sb.toString().trim();
         if (s.equals("")) {
            throw this.syntaxError("Missing value.");
         } else if (s.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
         } else if (s.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
         } else if (s.equalsIgnoreCase("null")) {
            return JSONObject.NULL;
         } else if ((b < '0' || b > '9') && b != '.' && b != '-' && b != '+') {
            return s;
         } else {
            if (b == '0') {
               if (s.length() > 2 && (s.charAt(1) == 'x' || s.charAt(1) == 'X')) {
                  try {
                     return Integer.parseInt(s.substring(2), 16);
                  } catch (Exception var12) {
                  }
               } else {
                  try {
                     return Integer.parseInt(s, 8);
                  } catch (Exception var11) {
                  }
               }
            }

            try {
               return new Integer(s);
            } catch (Exception var10) {
               try {
                  return new Long(s);
               } catch (Exception var9) {
                  try {
                     return new Double(s);
                  } catch (Exception var8) {
                     return s;
                  }
               }
            }
         }
      }
   }

   protected JSONObject newJSONObject() throws JSONException {
      return new JSONObject(this);
   }

   protected JSONArray newJSONArray() throws JSONException {
      return new JSONArray(this);
   }

   public char skipTo(char to) {
      int index = this.myIndex;

      char c;
      do {
         c = this.next();
         if (c == 0) {
            this.myIndex = index;
            return c;
         }
      } while(c != to);

      this.back();
      return c;
   }

   public void skipPast(String to) {
      this.myIndex = this.mySource.indexOf(to, this.myIndex);
      if (this.myIndex < 0) {
         this.myIndex = this.mySource.length();
      } else {
         this.myIndex += to.length();
      }

   }

   public JSONException syntaxError(String message) {
      return new JSONException(message + this.toString(), 0, this.myIndex);
   }

   public String toString() {
      return " at character " + this.myIndex + " of " + this.mySource;
   }
}
