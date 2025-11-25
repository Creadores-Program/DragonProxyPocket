package javax.xml.stream;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

class FactoryFinder {
   private static boolean debug = false;
   // $FF: synthetic field
   static Class class$javax$xml$stream$FactoryFinder;

   private static void debugPrintln(String msg) {
      if (debug) {
         System.err.println("STREAM: " + msg);
      }

   }

   private static ClassLoader findClassLoader() throws FactoryConfigurationError {
      ClassLoader var0;
      try {
         Class clazz = Class.forName((class$javax$xml$stream$FactoryFinder == null ? (class$javax$xml$stream$FactoryFinder = class$("javax.xml.stream.FactoryFinder")) : class$javax$xml$stream$FactoryFinder).getName() + "$ClassLoaderFinderConcrete");
         FactoryFinder.ClassLoaderFinder clf = (FactoryFinder.ClassLoaderFinder)clazz.newInstance();
         var0 = clf.getContextClassLoader();
      } catch (LinkageError var3) {
         var0 = (class$javax$xml$stream$FactoryFinder == null ? (class$javax$xml$stream$FactoryFinder = class$("javax.xml.stream.FactoryFinder")) : class$javax$xml$stream$FactoryFinder).getClassLoader();
      } catch (ClassNotFoundException var4) {
         var0 = (class$javax$xml$stream$FactoryFinder == null ? (class$javax$xml$stream$FactoryFinder = class$("javax.xml.stream.FactoryFinder")) : class$javax$xml$stream$FactoryFinder).getClassLoader();
      } catch (Exception var5) {
         throw new FactoryConfigurationError(var5.toString(), var5);
      }

      return var0;
   }

   private static Object newInstance(String className, ClassLoader classLoader) throws FactoryConfigurationError {
      try {
         Class spiClass;
         if (classLoader == null) {
            spiClass = Class.forName(className);
         } else {
            spiClass = classLoader.loadClass(className);
         }

         return spiClass.newInstance();
      } catch (ClassNotFoundException var3) {
         throw new FactoryConfigurationError("Provider " + className + " not found", var3);
      } catch (Exception var4) {
         throw new FactoryConfigurationError("Provider " + className + " could not be instantiated: " + var4, var4);
      }
   }

   static Object find(String factoryId) throws FactoryConfigurationError {
      return find(factoryId, (String)null);
   }

   static Object find(String factoryId, String fallbackClassName) throws FactoryConfigurationError {
      ClassLoader classLoader = findClassLoader();
      return find(factoryId, fallbackClassName, classLoader);
   }

   static Object find(String factoryId, String fallbackClassName, ClassLoader classLoader) throws FactoryConfigurationError {
      String serviceId;
      try {
         serviceId = System.getProperty(factoryId);
         if (serviceId != null) {
            debugPrintln("found system property" + serviceId);
            return newInstance(serviceId, classLoader);
         }
      } catch (SecurityException var8) {
      }

      String is;
      try {
         serviceId = System.getProperty("java.home");
         is = serviceId + File.separator + "lib" + File.separator + "jaxp.properties";
         File f = new File(is);
         if (f.exists()) {
            Properties props = new Properties();
            props.load(new FileInputStream(f));
            String factoryClassName = props.getProperty(factoryId);
            if (factoryClassName != null && factoryClassName.length() > 0) {
               debugPrintln("found java.home property " + factoryClassName);
               return newInstance(factoryClassName, classLoader);
            }
         }
      } catch (Exception var10) {
         if (debug) {
            var10.printStackTrace();
         }
      }

      serviceId = "META-INF/services/" + factoryId;

      try {
         is = null;
         InputStream is;
         if (classLoader == null) {
            is = ClassLoader.getSystemResourceAsStream(serviceId);
         } else {
            is = classLoader.getResourceAsStream(serviceId);
         }

         if (is != null) {
            debugPrintln("found " + serviceId);
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String factoryClassName = rd.readLine();
            rd.close();
            if (factoryClassName != null && !"".equals(factoryClassName)) {
               debugPrintln("loaded from services: " + factoryClassName);
               return newInstance(factoryClassName, classLoader);
            }
         }
      } catch (Exception var9) {
         if (debug) {
            var9.printStackTrace();
         }
      }

      if (fallbackClassName == null) {
         throw new FactoryConfigurationError("Provider for " + factoryId + " cannot be found", (Exception)null);
      } else {
         debugPrintln("loaded from fallback value: " + fallbackClassName);
         return newInstance(fallbackClassName, classLoader);
      }
   }

   // $FF: synthetic method
   static Class class$(String x0) {
      try {
         return Class.forName(x0);
      } catch (ClassNotFoundException var2) {
         throw new NoClassDefFoundError(var2.getMessage());
      }
   }

   static {
      try {
         debug = System.getProperty("xml.stream.debug") != null;
      } catch (Exception var1) {
      }

   }

   static class ClassLoaderFinderConcrete extends FactoryFinder.ClassLoaderFinder {
      ClassLoaderFinderConcrete() {
         super(null);
      }

      ClassLoader getContextClassLoader() {
         return Thread.currentThread().getContextClassLoader();
      }
   }

   private abstract static class ClassLoaderFinder {
      private ClassLoaderFinder() {
      }

      abstract ClassLoader getContextClassLoader();

      // $FF: synthetic method
      ClassLoaderFinder(Object x0) {
         this();
      }
   }
}
