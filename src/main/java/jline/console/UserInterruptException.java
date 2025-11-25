package jline.console;

public class UserInterruptException extends RuntimeException {
   private final String partialLine;

   public UserInterruptException(String partialLine) {
      this.partialLine = partialLine;
   }

   public String getPartialLine() {
      return this.partialLine;
   }
}
