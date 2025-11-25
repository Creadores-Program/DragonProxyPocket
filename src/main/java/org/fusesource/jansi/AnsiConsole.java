package org.fusesource.jansi;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import org.fusesource.jansi.internal.CLibrary;

public class AnsiConsole {
   public static final PrintStream system_out;
   public static final PrintStream out;
   public static final PrintStream system_err;
   public static final PrintStream err;
   private static int installed;

   public static OutputStream wrapOutputStream(OutputStream stream) {
      if (Boolean.getBoolean("jansi.passthrough")) {
         return stream;
      } else if (Boolean.getBoolean("jansi.strip")) {
         return new AnsiOutputStream(stream);
      } else {
         String os = System.getProperty("os.name");
         if (os.startsWith("Windows")) {
            try {
               return new WindowsAnsiOutputStream(stream);
            } catch (Throwable var3) {
               return new AnsiOutputStream(stream);
            }
         } else {
            try {
               int rc = CLibrary.isatty(CLibrary.STDOUT_FILENO);
               if (rc == 0) {
                  return new AnsiOutputStream(stream);
               }
            } catch (NoClassDefFoundError var4) {
            } catch (UnsatisfiedLinkError var5) {
            }

            return new FilterOutputStream(stream) {
               public void close() throws IOException {
                  this.write(AnsiOutputStream.REST_CODE);
                  this.flush();
                  super.close();
               }
            };
         }
      }
   }

   public static PrintStream out() {
      return out;
   }

   public static PrintStream err() {
      return err;
   }

   public static synchronized void systemInstall() {
      ++installed;
      if (installed == 1) {
         System.setOut(out);
         System.setErr(err);
      }

   }

   public static synchronized void systemUninstall() {
      --installed;
      if (installed == 0) {
         System.setOut(system_out);
         System.setErr(system_err);
      }

   }

   static {
      system_out = System.out;
      out = new PrintStream(wrapOutputStream(system_out));
      system_err = System.err;
      err = new PrintStream(wrapOutputStream(system_err));
   }
}
