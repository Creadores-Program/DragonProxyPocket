package org.dragonet.proxy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import jline.console.ConsoleReader;

public class ConsoleManager {
   private static final String CONSOLE_DATE = "HH:mm:ss";
   private static final String FILE_DATE = "yyyy/MM/dd HH:mm:ss";
   private static final Logger logger = Logger.getLogger("");
   private final DragonProxy proxy;
   private ConsoleReader reader;

   public ConsoleManager(DragonProxy proxy) {
      this.proxy = proxy;
      Handler[] var2 = logger.getHandlers();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Handler h = var2[var4];
         logger.removeHandler(h);
      }

      logger.addHandler(new ConsoleManager.FancyConsoleHandler());

      try {
         this.reader = new ConsoleReader();
      } catch (IOException var6) {
         logger.log(Level.SEVERE, "Exception initializing console reader", var6);
      }

      System.setOut(new PrintStream(new ConsoleManager.LoggerOutputStream(Level.INFO), true));
      System.setErr(new PrintStream(new ConsoleManager.LoggerOutputStream(Level.WARNING), true));
   }

   public void startConsole() {
      Thread thread = new ConsoleManager.ConsoleCommandThread();
      thread.setName("ConsoleCommandThread");
      thread.setDaemon(true);
      thread.start();
   }

   public void startFile(String logfile) {
      File parent = (new File(logfile)).getParentFile();
      if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
         logger.warning("Could not create log folder: " + parent);
      }

      Handler fileHandler = new ConsoleManager.RotatingFileHandler(logfile);
      fileHandler.setFormatter(new ConsoleManager.DateOutputFormatter("yyyy/MM/dd HH:mm:ss", false));
      logger.addHandler(fileHandler);
   }

   private class DateOutputFormatter extends Formatter {
      private final SimpleDateFormat date;
      private final boolean color;

      public DateOutputFormatter(String pattern, boolean color) {
         this.date = new SimpleDateFormat(pattern);
         this.color = color;
      }

      public String format(LogRecord record) {
         StringBuilder builder = new StringBuilder();
         builder.append("\u001b[36m[");
         builder.append(this.date.format(record.getMillis()));
         builder.append("]\u001b[37m");
         builder.append(" [");
         builder.append(record.getLevel().getLocalizedName().toUpperCase());
         builder.append("] ");
         builder.append(this.formatMessage(record) + "\u001b[37m");
         builder.append('\n');
         if (record.getThrown() != null) {
            StringWriter writer = new StringWriter();
            record.getThrown().printStackTrace(new PrintWriter(writer));
            builder.append(writer.toString());
         }

         return builder.toString();
      }
   }

   private static class RotatingFileHandler extends StreamHandler {
      private final SimpleDateFormat dateFormat;
      private final String template;
      private final boolean rotate;
      private String filename;

      public RotatingFileHandler(String template) {
         this.template = template;
         this.rotate = template.contains("%D");
         this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");
         this.filename = this.calculateFilename();
         this.updateOutput();
      }

      private void updateOutput() {
         try {
            this.setOutputStream(new FileOutputStream(this.filename, true));
         } catch (IOException var2) {
            ConsoleManager.logger.log(Level.SEVERE, "Unable to open " + this.filename + " for writing", var2);
         }

      }

      private void checkRotate() {
         if (this.rotate) {
            String newFilename = this.calculateFilename();
            if (!this.filename.equals(newFilename)) {
               this.filename = newFilename;
               super.publish(new LogRecord(Level.INFO, "Log rotating to: " + this.filename));
               this.updateOutput();
            }
         }

      }

      private String calculateFilename() {
         return this.template.replace("%D", this.dateFormat.format(new Date()));
      }

      public synchronized void publish(LogRecord record) {
         if (this.isLoggable(record)) {
            this.checkRotate();
            super.publish(record);
            super.flush();
         }
      }

      public synchronized void flush() {
         this.checkRotate();
         super.flush();
      }
   }

   private class FancyConsoleHandler extends ConsoleHandler {
      public FancyConsoleHandler() {
         this.setFormatter(ConsoleManager.this.new DateOutputFormatter("HH:mm:ss", true));
         this.setOutputStream(System.out);
      }

      public synchronized void flush() {
         try {
            ConsoleManager.this.reader.print((CharSequence)"\r");
            ConsoleManager.this.reader.flush();
            super.flush();

            try {
               ConsoleManager.this.reader.drawLine();
            } catch (Throwable var2) {
               ConsoleManager.this.reader.getCursorBuffer().clear();
            }

            ConsoleManager.this.reader.flush();
         } catch (IOException var3) {
            ConsoleManager.logger.log(Level.SEVERE, "I/O exception flushing console output", var3);
         }

      }
   }

   private static class LoggerOutputStream extends ByteArrayOutputStream {
      private final String separator = System.getProperty("line.separator");
      private final Level level;

      public LoggerOutputStream(Level level) {
         this.level = level;
      }

      public synchronized void flush() throws IOException {
         super.flush();
         String record = this.toString();
         super.reset();
         if (record.length() > 0 && !record.equals(this.separator)) {
            ConsoleManager.logger.logp(this.level, "LoggerOutputStream", "log" + this.level, record);
         }

      }
   }

   private class ConsoleCommandThread extends Thread {
      private ConsoleCommandThread() {
      }

      public void run() {
         String command = "";

         while(!ConsoleManager.this.proxy.isShuttingDown()) {
            try {
               command = ConsoleManager.this.reader.readLine(">", (Character)null);
               if (command != null && command.trim().length() != 0) {
                  ConsoleManager.this.proxy.getCommandRegister().callCommand(command);
               }
            } catch (Exception var3) {
               ConsoleManager.logger.log(Level.SEVERE, "Error while reading commands", var3);
            }
         }

      }

      // $FF: synthetic method
      ConsoleCommandThread(Object x1) {
         this();
      }
   }
}
