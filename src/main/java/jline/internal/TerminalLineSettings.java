package jline.internal;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TerminalLineSettings {
   public static final String JLINE_STTY = "jline.stty";
   public static final String DEFAULT_STTY = "stty";
   public static final String JLINE_SH = "jline.sh";
   public static final String DEFAULT_SH = "sh";
   private String sttyCommand = Configuration.getString("jline.stty", "stty");
   private String shCommand = Configuration.getString("jline.sh", "sh");
   private String config = this.get("-a");
   private long configLastFetched = System.currentTimeMillis();

   public TerminalLineSettings() throws IOException, InterruptedException {
      Log.debug("Config: ", this.config);
      if (this.config.length() == 0) {
         throw new IOException(MessageFormat.format("Unrecognized stty code: {0}", this.config));
      }
   }

   public String getConfig() {
      return this.config;
   }

   public void restore() throws IOException, InterruptedException {
      this.set("sane");
   }

   public String get(String args) throws IOException, InterruptedException {
      return this.stty(args);
   }

   public void set(String args) throws IOException, InterruptedException {
      this.stty(args);
   }

   public int getProperty(String name) {
      Preconditions.checkNotNull(name);

      try {
         if (this.config == null || System.currentTimeMillis() - this.configLastFetched > 1000L) {
            this.config = this.get("-a");
            this.configLastFetched = System.currentTimeMillis();
         }

         return getProperty(name, this.config);
      } catch (Exception var3) {
         Log.warn("Failed to query stty ", name, var3);
         return -1;
      }
   }

   protected static int getProperty(String name, String stty) {
      Pattern pattern = Pattern.compile(name + "\\s+=\\s+([^;]*)[;\\n\\r]");
      Matcher matcher = pattern.matcher(stty);
      if (!matcher.find()) {
         pattern = Pattern.compile(name + "\\s+([^;]*)[;\\n\\r]");
         matcher = pattern.matcher(stty);
         if (!matcher.find()) {
            pattern = Pattern.compile("(\\S*)\\s+" + name);
            matcher = pattern.matcher(stty);
            if (!matcher.find()) {
               return -1;
            }
         }
      }

      return parseControlChar(matcher.group(1));
   }

   private static int parseControlChar(String str) {
      if ("<undef>".equals(str)) {
         return -1;
      } else if (str.charAt(0) == '0') {
         return Integer.parseInt(str, 8);
      } else if (str.charAt(0) >= '1' && str.charAt(0) <= '9') {
         return Integer.parseInt(str, 10);
      } else if (str.charAt(0) == '^') {
         return str.charAt(1) == '?' ? 127 : str.charAt(1) - 64;
      } else if (str.charAt(0) == 'M' && str.charAt(1) == '-') {
         if (str.charAt(2) == '^') {
            return str.charAt(3) == '?' ? 255 : str.charAt(3) - 64 + 128;
         } else {
            return str.charAt(2) + 128;
         }
      } else {
         return str.charAt(0);
      }
   }

   private String stty(String args) throws IOException, InterruptedException {
      Preconditions.checkNotNull(args);
      return this.exec(String.format("%s %s < /dev/tty", this.sttyCommand, args));
   }

   private String exec(String cmd) throws IOException, InterruptedException {
      Preconditions.checkNotNull(cmd);
      return this.exec(this.shCommand, "-c", cmd);
   }

   private String exec(String... cmd) throws IOException, InterruptedException {
      Preconditions.checkNotNull(cmd);
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      Log.trace("Running: ", cmd);
      Process p = Runtime.getRuntime().exec(cmd);
      InputStream in = null;
      InputStream err = null;
      OutputStream out = null;

      try {
         in = p.getInputStream();

         int c;
         while((c = in.read()) != -1) {
            bout.write(c);
         }

         err = p.getErrorStream();

         while(true) {
            if ((c = err.read()) == -1) {
               out = p.getOutputStream();
               p.waitFor();
               break;
            }

            bout.write(c);
         }
      } finally {
         close(in, out, err);
      }

      String result = bout.toString();
      Log.trace("Result: ", result);
      return result;
   }

   private static void close(Closeable... closeables) {
      Closeable[] arr$ = closeables;
      int len$ = closeables.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         Closeable c = arr$[i$];

         try {
            c.close();
         } catch (Exception var6) {
         }
      }

   }
}
