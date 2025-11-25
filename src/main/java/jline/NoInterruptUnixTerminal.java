package jline;

public class NoInterruptUnixTerminal extends UnixTerminal {
   public NoInterruptUnixTerminal() throws Exception {
   }

   public void init() throws Exception {
      super.init();
      this.getSettings().set("intr undef");
   }

   public void restore() throws Exception {
      this.getSettings().set("intr ^C");
      super.restore();
   }
}
