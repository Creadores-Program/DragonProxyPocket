package jline;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import jline.internal.Configuration;
import jline.internal.Log;
import jline.internal.Preconditions;

public class TerminalFactory {
   public static final String JLINE_TERMINAL = "jline.terminal";
   public static final String AUTO = "auto";
   public static final String UNIX = "unix";
   public static final String WIN = "win";
   public static final String WINDOWS = "windows";
   public static final String NONE = "none";
   public static final String OFF = "off";
   public static final String FALSE = "false";
   private static final InheritableThreadLocal<Terminal> holder = new InheritableThreadLocal();
   private static final Map<TerminalFactory.Flavor, Class<? extends Terminal>> FLAVORS = new HashMap();

   public static synchronized Terminal create() {
      if (Log.TRACE) {
         Log.trace(new Throwable("CREATE MARKER"));
      }

      String type = Configuration.getString("jline.terminal", "auto");
      if ("dumb".equals(System.getenv("TERM"))) {
         type = "none";
         Log.debug("$TERM=dumb; setting type=", type);
      }

      Log.debug("Creating terminal; type=", type);

      Object t;
      try {
         String tmp = type.toLowerCase();
         if (tmp.equals("unix")) {
            t = getFlavor(TerminalFactory.Flavor.UNIX);
         } else if (tmp.equals("win") | tmp.equals("windows")) {
            t = getFlavor(TerminalFactory.Flavor.WINDOWS);
         } else if (!tmp.equals("none") && !tmp.equals("off") && !tmp.equals("false")) {
            if (tmp.equals("auto")) {
               String os = Configuration.getOsName();
               TerminalFactory.Flavor flavor = TerminalFactory.Flavor.UNIX;
               if (os.contains("windows")) {
                  flavor = TerminalFactory.Flavor.WINDOWS;
               }

               t = getFlavor(flavor);
            } else {
               try {
                  t = (Terminal)Thread.currentThread().getContextClassLoader().loadClass(type).newInstance();
               } catch (Exception var6) {
                  throw new IllegalArgumentException(MessageFormat.format("Invalid terminal type: {0}", type), var6);
               }
            }
         } else {
            t = new UnsupportedTerminal();
         }
      } catch (Exception var7) {
         Log.error("Failed to construct terminal; falling back to unsupported", var7);
         t = new UnsupportedTerminal();
      }

      Log.debug("Created Terminal: ", t);

      try {
         ((Terminal)t).init();
         return (Terminal)t;
      } catch (Throwable var5) {
         Log.error("Terminal initialization failed; falling back to unsupported", var5);
         return new UnsupportedTerminal();
      }
   }

   public static synchronized void reset() {
      holder.remove();
   }

   public static synchronized void resetIf(Terminal t) {
      if (holder.get() == t) {
         reset();
      }

   }

   public static synchronized void configure(String type) {
      Preconditions.checkNotNull(type);
      System.setProperty("jline.terminal", type);
   }

   public static synchronized void configure(TerminalFactory.Type type) {
      Preconditions.checkNotNull(type);
      configure(type.name().toLowerCase());
   }

   public static synchronized Terminal get() {
      Terminal t = (Terminal)holder.get();
      if (t == null) {
         t = create();
         holder.set(t);
      }

      return t;
   }

   public static Terminal getFlavor(TerminalFactory.Flavor flavor) throws Exception {
      Class<? extends Terminal> type = (Class)FLAVORS.get(flavor);
      if (type != null) {
         return (Terminal)type.newInstance();
      } else {
         throw new InternalError();
      }
   }

   public static void registerFlavor(TerminalFactory.Flavor flavor, Class<? extends Terminal> type) {
      FLAVORS.put(flavor, type);
   }

   static {
      registerFlavor(TerminalFactory.Flavor.WINDOWS, AnsiWindowsTerminal.class);
      registerFlavor(TerminalFactory.Flavor.UNIX, UnixTerminal.class);
   }

   public static enum Flavor {
      WINDOWS,
      UNIX;
   }

   public static enum Type {
      AUTO,
      WINDOWS,
      UNIX,
      NONE;
   }
}
