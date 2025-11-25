package org.fusesource.hawtjni.runtime;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

public class Library {
   static final String SLASH = System.getProperty("file.separator");
   private final String name;
   private final String version;
   private final ClassLoader classLoader;
   private boolean loaded;

   public Library(String name) {
      this(name, (String)null, (ClassLoader)null);
   }

   public Library(String name, Class<?> clazz) {
      this(name, version(clazz), clazz.getClassLoader());
   }

   public Library(String name, String version) {
      this(name, version, (ClassLoader)null);
   }

   public Library(String name, String version, ClassLoader classLoader) {
      if (name == null) {
         throw new IllegalArgumentException("name cannot be null");
      } else {
         this.name = name;
         this.version = version;
         this.classLoader = classLoader;
      }
   }

   private static String version(Class<?> clazz) {
      try {
         return clazz.getPackage().getImplementationVersion();
      } catch (Throwable var2) {
         return null;
      }
   }

   public static String getOperatingSystem() {
      String name = System.getProperty("os.name").toLowerCase().trim();
      if (name.startsWith("linux")) {
         return "linux";
      } else if (name.startsWith("mac os x")) {
         return "osx";
      } else {
         return name.startsWith("win") ? "windows" : name.replaceAll("\\W+", "_");
      }
   }

   public static String getPlatform() {
      return getOperatingSystem() + getBitModel();
   }

   public static int getBitModel() {
      String prop = System.getProperty("sun.arch.data.model");
      if (prop == null) {
         prop = System.getProperty("com.ibm.vm.bitmode");
      }

      return prop != null ? Integer.parseInt(prop) : -1;
   }

   public synchronized void load() {
      if (!this.loaded) {
         this.doLoad();
         this.loaded = true;
      }
   }

   private void doLoad() {
      String version = System.getProperty("library." + this.name + ".version");
      if (version == null) {
         version = this.version;
      }

      ArrayList<String> errors = new ArrayList();
      String customPath = System.getProperty("library." + this.name + ".path");
      if (customPath != null) {
         if (version != null && this.load(errors, this.file(customPath, this.map(this.name + "-" + version)))) {
            return;
         }

         if (this.load(errors, this.file(customPath, this.map(this.name)))) {
            return;
         }
      }

      if (version == null || !this.load(errors, this.name + getBitModel() + "-" + version)) {
         if (version == null || !this.load(errors, this.name + "-" + version)) {
            if (!this.load(errors, this.name)) {
               if (this.classLoader != null) {
                  if (this.exractAndLoad(errors, version, customPath, this.getPlatformSpecifcResourcePath())) {
                     return;
                  }

                  if (this.exractAndLoad(errors, version, customPath, this.getOperatingSystemSpecifcResourcePath())) {
                     return;
                  }

                  if (this.exractAndLoad(errors, version, customPath, this.getResorucePath())) {
                     return;
                  }
               }

               throw new UnsatisfiedLinkError("Could not load library. Reasons: " + errors.toString());
            }
         }
      }
   }

   public final String getOperatingSystemSpecifcResourcePath() {
      return this.getPlatformSpecifcResourcePath(getOperatingSystem());
   }

   public final String getPlatformSpecifcResourcePath() {
      return this.getPlatformSpecifcResourcePath(getPlatform());
   }

   public final String getPlatformSpecifcResourcePath(String platform) {
      return "META-INF/native/" + platform + "/" + this.map(this.name);
   }

   public final String getResorucePath() {
      return "META-INF/native/" + this.map(this.name);
   }

   public final String getLibraryFileName() {
      return this.map(this.name);
   }

   private boolean exractAndLoad(ArrayList<String> errors, String version, String customPath, String resourcePath) {
      URL resource = this.classLoader.getResource(resourcePath);
      if (resource != null) {
         String libName = this.name + "-" + getBitModel();
         if (version != null) {
            libName = libName + "-" + version;
         }

         String[] libNameParts = this.map(libName).split("\\.");
         String prefix = libNameParts[0] + "-";
         String suffix = "." + libNameParts[1];
         File target;
         if (customPath != null) {
            target = this.extract(errors, resource, prefix, suffix, this.file(customPath));
            if (target != null && this.load(errors, target)) {
               return true;
            }
         }

         customPath = System.getProperty("java.io.tmpdir");
         target = this.extract(errors, resource, prefix, suffix, this.file(customPath));
         if (target != null && this.load(errors, target)) {
            return true;
         }
      }

      return false;
   }

   private File file(String... paths) {
      File rc = null;
      String[] arr$ = paths;
      int len$ = paths.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String path = arr$[i$];
         if (rc == null) {
            rc = new File(path);
         } else {
            rc = new File(rc, path);
         }
      }

      return rc;
   }

   private String map(String libName) {
      libName = System.mapLibraryName(libName);
      String ext = ".dylib";
      if (libName.endsWith(ext)) {
         libName = libName.substring(0, libName.length() - ext.length()) + ".jnilib";
      }

      return libName;
   }

   private File extract(ArrayList<String> errors, URL source, String prefix, String suffix, File directory) {
      File target = null;

      try {
         FileOutputStream os = null;
         InputStream is = null;

         File var16;
         try {
            target = File.createTempFile(prefix, suffix, directory);
            is = source.openStream();
            if (is != null) {
               byte[] buffer = new byte[4096];
               os = new FileOutputStream(target);

               int read;
               while((read = is.read(buffer)) != -1) {
                  os.write(buffer, 0, read);
               }

               this.chmod("755", target);
            }

            target.deleteOnExit();
            var16 = target;
         } finally {
            close(os);
            close(is);
         }

         return var16;
      } catch (Throwable var15) {
         if (target != null) {
            target.delete();
         }

         errors.add(var15.getMessage());
         return null;
      }
   }

   private static void close(Closeable file) {
      if (file != null) {
         try {
            file.close();
         } catch (Exception var2) {
         }
      }

   }

   private void chmod(String permision, File path) {
      if (!getPlatform().startsWith("windows")) {
         try {
            Runtime.getRuntime().exec(new String[]{"chmod", permision, path.getCanonicalPath()}).waitFor();
         } catch (Throwable var4) {
         }

      }
   }

   private boolean load(ArrayList<String> errors, File lib) {
      try {
         System.load(lib.getPath());
         return true;
      } catch (UnsatisfiedLinkError var4) {
         errors.add(var4.getMessage());
         return false;
      }
   }

   private boolean load(ArrayList<String> errors, String lib) {
      try {
         System.loadLibrary(lib);
         return true;
      } catch (UnsatisfiedLinkError var4) {
         errors.add(var4.getMessage());
         return false;
      }
   }
}
