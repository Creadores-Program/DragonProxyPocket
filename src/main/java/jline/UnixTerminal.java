package jline;

import jline.internal.Log;
import jline.internal.TerminalLineSettings;

public class UnixTerminal extends TerminalSupport {
   private final TerminalLineSettings settings = new TerminalLineSettings();

   public UnixTerminal() throws Exception {
      super(true);
   }

   protected TerminalLineSettings getSettings() {
      return this.settings;
   }

   public void init() throws Exception {
      super.init();
      this.setAnsiSupported(true);
      this.settings.set("-icanon min 1 -icrnl -inlcr -ixon");
      this.setEchoEnabled(false);
   }

   public void restore() throws Exception {
      this.settings.restore();
      super.restore();
   }

   public int getWidth() {
      int w = this.settings.getProperty("columns");
      return w < 1 ? 80 : w;
   }

   public int getHeight() {
      int h = this.settings.getProperty("rows");
      return h < 1 ? 24 : h;
   }

   public synchronized void setEchoEnabled(boolean enabled) {
      try {
         if (enabled) {
            this.settings.set("echo");
         } else {
            this.settings.set("-echo");
         }

         super.setEchoEnabled(enabled);
      } catch (Exception var3) {
         Log.error("Failed to ", enabled ? "enable" : "disable", " echo", var3);
      }

   }

   public void disableInterruptCharacter() {
      try {
         this.settings.set("intr undef");
      } catch (Exception var2) {
         if (var2 instanceof InterruptedException) {
            Thread.currentThread().interrupt();
         }

         Log.error("Failed to disable interrupt character", var2);
      }

   }

   public void enableInterruptCharacter() {
      try {
         this.settings.set("intr ^C");
      } catch (Exception var2) {
         if (var2 instanceof InterruptedException) {
            Thread.currentThread().interrupt();
         }

         Log.error("Failed to enable interrupt character", var2);
      }

   }
}
