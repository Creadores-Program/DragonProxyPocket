package jline;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import jline.internal.Configuration;
import jline.internal.Log;
import org.fusesource.jansi.internal.WindowsSupport;

public class WindowsTerminal extends TerminalSupport {
   public static final String DIRECT_CONSOLE = WindowsTerminal.class.getName() + ".directConsole";
   public static final String ANSI = WindowsTerminal.class.getName() + ".ansi";
   private boolean directConsole;
   private int originalMode;

   public WindowsTerminal() throws Exception {
      super(true);
   }

   public void init() throws Exception {
      super.init();
      this.setAnsiSupported(Configuration.getBoolean(ANSI, true));
      this.setDirectConsole(Configuration.getBoolean(DIRECT_CONSOLE, true));
      this.originalMode = this.getConsoleMode();
      this.setConsoleMode(this.originalMode & ~WindowsTerminal.ConsoleMode.ENABLE_ECHO_INPUT.code);
      this.setEchoEnabled(false);
   }

   public void restore() throws Exception {
      this.setConsoleMode(this.originalMode);
      super.restore();
   }

   public int getWidth() {
      int w = this.getWindowsTerminalWidth();
      return w < 1 ? 80 : w;
   }

   public int getHeight() {
      int h = this.getWindowsTerminalHeight();
      return h < 1 ? 24 : h;
   }

   public void setEchoEnabled(boolean enabled) {
      if (enabled) {
         this.setConsoleMode(this.getConsoleMode() | WindowsTerminal.ConsoleMode.ENABLE_ECHO_INPUT.code | WindowsTerminal.ConsoleMode.ENABLE_LINE_INPUT.code | WindowsTerminal.ConsoleMode.ENABLE_PROCESSED_INPUT.code | WindowsTerminal.ConsoleMode.ENABLE_WINDOW_INPUT.code);
      } else {
         this.setConsoleMode(this.getConsoleMode() & ~(WindowsTerminal.ConsoleMode.ENABLE_LINE_INPUT.code | WindowsTerminal.ConsoleMode.ENABLE_ECHO_INPUT.code | WindowsTerminal.ConsoleMode.ENABLE_PROCESSED_INPUT.code | WindowsTerminal.ConsoleMode.ENABLE_WINDOW_INPUT.code));
      }

      super.setEchoEnabled(enabled);
   }

   public void setDirectConsole(boolean flag) {
      this.directConsole = flag;
      Log.debug("Direct console: ", flag);
   }

   public Boolean getDirectConsole() {
      return this.directConsole;
   }

   public InputStream wrapInIfNeeded(InputStream in) throws IOException {
      return this.directConsole && this.isSystemIn(in) ? new InputStream() {
         public int read() throws IOException {
            return WindowsTerminal.this.readByte();
         }
      } : super.wrapInIfNeeded(in);
   }

   protected boolean isSystemIn(InputStream in) throws IOException {
      if (in == null) {
         return false;
      } else if (in == System.in) {
         return true;
      } else {
         return in instanceof FileInputStream && ((FileInputStream)in).getFD() == FileDescriptor.in;
      }
   }

   private int getConsoleMode() {
      return WindowsSupport.getConsoleMode();
   }

   private void setConsoleMode(int mode) {
      WindowsSupport.setConsoleMode(mode);
   }

   private int readByte() {
      return WindowsSupport.readByte();
   }

   private int getWindowsTerminalWidth() {
      return WindowsSupport.getWindowsTerminalWidth();
   }

   private int getWindowsTerminalHeight() {
      return WindowsSupport.getWindowsTerminalHeight();
   }

   public static enum ConsoleMode {
      ENABLE_LINE_INPUT(2),
      ENABLE_ECHO_INPUT(4),
      ENABLE_PROCESSED_INPUT(1),
      ENABLE_WINDOW_INPUT(8),
      ENABLE_MOUSE_INPUT(16),
      ENABLE_PROCESSED_OUTPUT(1),
      ENABLE_WRAP_AT_EOL_OUTPUT(2);

      public final int code;

      private ConsoleMode(int code) {
         this.code = code;
      }
   }
}
