package jline.internal;

import java.util.ArrayList;
import java.util.List;

public class ShutdownHooks {
   public static final String JLINE_SHUTDOWNHOOK = "jline.shutdownhook";
   private static final boolean enabled = Configuration.getBoolean("jline.shutdownhook", true);
   private static final List<ShutdownHooks.Task> tasks = new ArrayList();
   private static Thread hook;

   public static synchronized <T extends ShutdownHooks.Task> T add(T task) {
      Preconditions.checkNotNull(task);
      if (!enabled) {
         Log.debug("Shutdown-hook is disabled; not installing: ", task);
         return task;
      } else {
         if (hook == null) {
            hook = addHook(new Thread("JLine Shutdown Hook") {
               public void run() {
                  ShutdownHooks.runTasks();
               }
            });
         }

         Log.debug("Adding shutdown-hook task: ", task);
         tasks.add(task);
         return task;
      }
   }

   private static synchronized void runTasks() {
      Log.debug("Running all shutdown-hook tasks");
      ShutdownHooks.Task[] arr$ = (ShutdownHooks.Task[])tasks.toArray(new ShutdownHooks.Task[tasks.size()]);
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         ShutdownHooks.Task task = arr$[i$];
         Log.debug("Running task: ", task);

         try {
            task.run();
         } catch (Throwable var5) {
            Log.warn("Task failed", var5);
         }
      }

      tasks.clear();
   }

   private static Thread addHook(Thread thread) {
      Log.debug("Registering shutdown-hook: ", thread);

      try {
         Runtime.getRuntime().addShutdownHook(thread);
      } catch (AbstractMethodError var2) {
         Log.debug("Failed to register shutdown-hook", var2);
      }

      return thread;
   }

   public static synchronized void remove(ShutdownHooks.Task task) {
      Preconditions.checkNotNull(task);
      if (enabled && hook != null) {
         tasks.remove(task);
         if (tasks.isEmpty()) {
            removeHook(hook);
            hook = null;
         }

      }
   }

   private static void removeHook(Thread thread) {
      Log.debug("Removing shutdown-hook: ", thread);

      try {
         Runtime.getRuntime().removeShutdownHook(thread);
      } catch (AbstractMethodError var2) {
         Log.debug("Failed to remove shutdown-hook", var2);
      } catch (IllegalStateException var3) {
      }

   }

   public interface Task {
      void run() throws Exception;
   }
}
