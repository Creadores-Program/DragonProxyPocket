package org.codehaus.jettison.json;

public class JSONException extends Exception {
   private Throwable cause;
   private int line = -1;
   private int column = -1;

   public JSONException(String message) {
      super(message);
   }

   public JSONException(String message, int line, int column) {
      super(message);
      this.line = line;
      this.column = column;
   }

   public JSONException(Throwable t) {
      super(t.getMessage(), t);
      this.cause = t;
   }

   public Throwable getCause() {
      return this.cause;
   }

   public int getColumn() {
      return this.column;
   }

   public int getLine() {
      return this.line;
   }
}
