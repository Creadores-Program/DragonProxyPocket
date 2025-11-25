package org.apache.commons.lang3.concurrent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;

public class MultiBackgroundInitializer extends BackgroundInitializer<MultiBackgroundInitializer.MultiBackgroundInitializerResults> {
   private final Map<String, BackgroundInitializer<?>> childInitializers = new HashMap();

   public MultiBackgroundInitializer() {
   }

   public MultiBackgroundInitializer(ExecutorService exec) {
      super(exec);
   }

   public void addInitializer(String name, BackgroundInitializer<?> init) {
      if (name == null) {
         throw new IllegalArgumentException("Name of child initializer must not be null!");
      } else if (init == null) {
         throw new IllegalArgumentException("Child initializer must not be null!");
      } else {
         synchronized(this) {
            if (this.isStarted()) {
               throw new IllegalStateException("addInitializer() must not be called after start()!");
            } else {
               this.childInitializers.put(name, init);
            }
         }
      }
   }

   protected int getTaskCount() {
      int result = 1;

      BackgroundInitializer bi;
      for(Iterator i$ = this.childInitializers.values().iterator(); i$.hasNext(); result += bi.getTaskCount()) {
         bi = (BackgroundInitializer)i$.next();
      }

      return result;
   }

   protected MultiBackgroundInitializer.MultiBackgroundInitializerResults initialize() throws Exception {
      HashMap inits;
      synchronized(this) {
         inits = new HashMap(this.childInitializers);
      }

      ExecutorService exec = this.getActiveExecutor();

      BackgroundInitializer bi;
      for(Iterator i$ = inits.values().iterator(); i$.hasNext(); bi.start()) {
         bi = (BackgroundInitializer)i$.next();
         if (bi.getExternalExecutor() == null) {
            bi.setExternalExecutor(exec);
         }
      }

      Map<String, Object> results = new HashMap();
      Map<String, ConcurrentException> excepts = new HashMap();
      Iterator i$ = inits.entrySet().iterator();

      while(i$.hasNext()) {
         Entry e = (Entry)i$.next();

         try {
            results.put(e.getKey(), ((BackgroundInitializer)e.getValue()).get());
         } catch (ConcurrentException var8) {
            excepts.put(e.getKey(), var8);
         }
      }

      return new MultiBackgroundInitializer.MultiBackgroundInitializerResults(inits, results, excepts);
   }

   public static class MultiBackgroundInitializerResults {
      private final Map<String, BackgroundInitializer<?>> initializers;
      private final Map<String, Object> resultObjects;
      private final Map<String, ConcurrentException> exceptions;

      private MultiBackgroundInitializerResults(Map<String, BackgroundInitializer<?>> inits, Map<String, Object> results, Map<String, ConcurrentException> excepts) {
         this.initializers = inits;
         this.resultObjects = results;
         this.exceptions = excepts;
      }

      public BackgroundInitializer<?> getInitializer(String name) {
         return this.checkName(name);
      }

      public Object getResultObject(String name) {
         this.checkName(name);
         return this.resultObjects.get(name);
      }

      public boolean isException(String name) {
         this.checkName(name);
         return this.exceptions.containsKey(name);
      }

      public ConcurrentException getException(String name) {
         this.checkName(name);
         return (ConcurrentException)this.exceptions.get(name);
      }

      public Set<String> initializerNames() {
         return Collections.unmodifiableSet(this.initializers.keySet());
      }

      public boolean isSuccessful() {
         return this.exceptions.isEmpty();
      }

      private BackgroundInitializer<?> checkName(String name) {
         BackgroundInitializer<?> init = (BackgroundInitializer)this.initializers.get(name);
         if (init == null) {
            throw new NoSuchElementException("No child initializer with name " + name);
         } else {
            return init;
         }
      }

      // $FF: synthetic method
      MultiBackgroundInitializerResults(Map x0, Map x1, Map x2, Object x3) {
         this(x0, x1, x2);
      }
   }
}
