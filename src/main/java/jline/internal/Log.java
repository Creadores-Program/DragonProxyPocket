package jline.internal;

import java.io.PrintStream;

public final class Log {
   public static final boolean TRACE = Boolean.getBoolean(Log.class.getName() + ".trace");
   public static final boolean DEBUG;
   private static PrintStream output;

   public static PrintStream getOutput() {
      return output;
   }

   public static void setOutput(PrintStream out) {
      output = (PrintStream)Preconditions.checkNotNull(out);
   }

   @TestAccessible
   static void render(PrintStream out, Object message) {
      if (message.getClass().isArray()) {
         Object[] array = (Object[])((Object[])message);
         out.print("[");

         for(int i = 0; i < array.length; ++i) {
            out.print(array[i]);
            if (i + 1 < array.length) {
               out.print(",");
            }
         }

         out.print("]");
      } else {
         out.print(message);
      }

   }

   @TestAccessible
   static void log(Log.Level level, Object... messages) {
      synchronized(output) {
         output.format("[%s] ", level);

         for(int i = 0; i < messages.length; ++i) {
            if (i + 1 == messages.length && messages[i] instanceof Throwable) {
               output.println();
               ((Throwable)messages[i]).printStackTrace(output);
            } else {
               render(output, messages[i]);
            }
         }

         output.println();
         output.flush();
      }
   }

   public static void trace(Object... messages) {
      if (TRACE) {
         log(Log.Level.TRACE, messages);
      }

   }

   public static void debug(Object... messages) {
      if (TRACE || DEBUG) {
         log(Log.Level.DEBUG, messages);
      }

   }

   public static void info(Object... messages) {
      log(Log.Level.INFO, messages);
   }

   public static void warn(Object... messages) {
      log(Log.Level.WARN, messages);
   }

   public static void error(Object... messages) {
      log(Log.Level.ERROR, messages);
   }

   static {
      DEBUG = TRACE || Boolean.getBoolean(Log.class.getName() + ".debug");
      output = System.err;
   }

   public static enum Level {
      TRACE,
      DEBUG,
      INFO,
      WARN,
      ERROR;
   }
}
