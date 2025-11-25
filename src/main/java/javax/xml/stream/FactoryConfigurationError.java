package javax.xml.stream;

public class FactoryConfigurationError extends Error {
   Exception nested;

   public FactoryConfigurationError() {
   }

   public FactoryConfigurationError(Exception e) {
      this.nested = e;
   }

   public FactoryConfigurationError(Exception e, String msg) {
      super(msg);
      this.nested = e;
   }

   public FactoryConfigurationError(String msg, Exception e) {
      super(msg);
      this.nested = e;
   }

   public FactoryConfigurationError(String msg) {
      super(msg);
   }

   public Exception getException() {
      return this.nested;
   }

   public String getMessage() {
      String msg = super.getMessage();
      if (msg != null) {
         return msg;
      } else {
         if (this.nested != null) {
            msg = this.nested.getMessage();
            if (msg == null) {
               msg = this.nested.getClass().toString();
            }
         }

         return msg;
      }
   }
}
