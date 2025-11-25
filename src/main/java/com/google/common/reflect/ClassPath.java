package com.google.common.reflect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import java.util.logging.Logger;
import javax.annotation.Nullable;

@Beta
public final class ClassPath {
   private static final Logger logger = Logger.getLogger(ClassPath.class.getName());
   private static final Predicate<ClassPath.ClassInfo> IS_TOP_LEVEL = new Predicate<ClassPath.ClassInfo>() {
      public boolean apply(ClassPath.ClassInfo info) {
         return info.className.indexOf(36) == -1;
      }
   };
   private static final Splitter CLASS_PATH_ATTRIBUTE_SEPARATOR = Splitter.on(" ").omitEmptyStrings();
   private static final String CLASS_FILE_NAME_EXTENSION = ".class";
   private final ImmutableSet<ClassPath.ResourceInfo> resources;

   private ClassPath(ImmutableSet<ClassPath.ResourceInfo> resources) {
      this.resources = resources;
   }

   public static ClassPath from(ClassLoader classloader) throws IOException {
      ClassPath.DefaultScanner scanner = new ClassPath.DefaultScanner();
      scanner.scan(classloader);
      return new ClassPath(scanner.getResources());
   }

   public ImmutableSet<ClassPath.ResourceInfo> getResources() {
      return this.resources;
   }

   public ImmutableSet<ClassPath.ClassInfo> getAllClasses() {
      return FluentIterable.from((Iterable)this.resources).filter(ClassPath.ClassInfo.class).toSet();
   }

   public ImmutableSet<ClassPath.ClassInfo> getTopLevelClasses() {
      return FluentIterable.from((Iterable)this.resources).filter(ClassPath.ClassInfo.class).filter(IS_TOP_LEVEL).toSet();
   }

   public ImmutableSet<ClassPath.ClassInfo> getTopLevelClasses(String packageName) {
      Preconditions.checkNotNull(packageName);
      ImmutableSet.Builder<ClassPath.ClassInfo> builder = ImmutableSet.builder();
      Iterator i$ = this.getTopLevelClasses().iterator();

      while(i$.hasNext()) {
         ClassPath.ClassInfo classInfo = (ClassPath.ClassInfo)i$.next();
         if (classInfo.getPackageName().equals(packageName)) {
            builder.add((Object)classInfo);
         }
      }

      return builder.build();
   }

   public ImmutableSet<ClassPath.ClassInfo> getTopLevelClassesRecursive(String packageName) {
      Preconditions.checkNotNull(packageName);
      String packagePrefix = packageName + '.';
      ImmutableSet.Builder<ClassPath.ClassInfo> builder = ImmutableSet.builder();
      Iterator i$ = this.getTopLevelClasses().iterator();

      while(i$.hasNext()) {
         ClassPath.ClassInfo classInfo = (ClassPath.ClassInfo)i$.next();
         if (classInfo.getName().startsWith(packagePrefix)) {
            builder.add((Object)classInfo);
         }
      }

      return builder.build();
   }

   @VisibleForTesting
   static String getClassName(String filename) {
      int classNameEnd = filename.length() - ".class".length();
      return filename.substring(0, classNameEnd).replace('/', '.');
   }

   @VisibleForTesting
   static final class DefaultScanner extends ClassPath.Scanner {
      private final SetMultimap<ClassLoader, String> resources = MultimapBuilder.hashKeys().linkedHashSetValues().build();

      ImmutableSet<ClassPath.ResourceInfo> getResources() {
         ImmutableSet.Builder<ClassPath.ResourceInfo> builder = ImmutableSet.builder();
         Iterator i$ = this.resources.entries().iterator();

         while(i$.hasNext()) {
            Entry<ClassLoader, String> entry = (Entry)i$.next();
            builder.add((Object)ClassPath.ResourceInfo.of((String)entry.getValue(), (ClassLoader)entry.getKey()));
         }

         return builder.build();
      }

      protected void scanJarFile(ClassLoader classloader, JarFile file) {
         Enumeration entries = file.entries();

         while(entries.hasMoreElements()) {
            JarEntry entry = (JarEntry)entries.nextElement();
            if (!entry.isDirectory() && !entry.getName().equals("META-INF/MANIFEST.MF")) {
               this.resources.get(classloader).add(entry.getName());
            }
         }

      }

      protected void scanDirectory(ClassLoader classloader, File directory) throws IOException {
         this.scanDirectory(directory, classloader, "");
      }

      private void scanDirectory(File directory, ClassLoader classloader, String packagePrefix) throws IOException {
         File[] files = directory.listFiles();
         if (files == null) {
            ClassPath.logger.warning("Cannot read directory " + directory);
         } else {
            File[] arr$ = files;
            int len$ = files.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               File f = arr$[i$];
               String name = f.getName();
               if (f.isDirectory()) {
                  this.scanDirectory(f, classloader, packagePrefix + name + "/");
               } else {
                  String resourceName = packagePrefix + name;
                  if (!resourceName.equals("META-INF/MANIFEST.MF")) {
                     this.resources.get(classloader).add(resourceName);
                  }
               }
            }

         }
      }
   }

   abstract static class Scanner {
      private final Set<File> scannedUris = Sets.newHashSet();

      public final void scan(ClassLoader classloader) throws IOException {
         Iterator i$ = getClassPathEntries(classloader).entrySet().iterator();

         while(i$.hasNext()) {
            Entry<File, ClassLoader> entry = (Entry)i$.next();
            this.scan((File)entry.getKey(), (ClassLoader)entry.getValue());
         }

      }

      protected abstract void scanDirectory(ClassLoader var1, File var2) throws IOException;

      protected abstract void scanJarFile(ClassLoader var1, JarFile var2) throws IOException;

      @VisibleForTesting
      final void scan(File file, ClassLoader classloader) throws IOException {
         if (this.scannedUris.add(file.getCanonicalFile())) {
            this.scanFrom(file, classloader);
         }

      }

      private void scanFrom(File file, ClassLoader classloader) throws IOException {
         if (file.exists()) {
            if (file.isDirectory()) {
               this.scanDirectory(classloader, file);
            } else {
               this.scanJar(file, classloader);
            }

         }
      }

      private void scanJar(File file, ClassLoader classloader) throws IOException {
         JarFile jarFile;
         try {
            jarFile = new JarFile(file);
         } catch (IOException var13) {
            return;
         }

         try {
            Iterator i$ = getClassPathFromManifest(file, jarFile.getManifest()).iterator();

            while(i$.hasNext()) {
               File path = (File)i$.next();
               this.scan(path, classloader);
            }

            this.scanJarFile(classloader, jarFile);
         } finally {
            try {
               jarFile.close();
            } catch (IOException var12) {
            }

         }
      }

      @VisibleForTesting
      static ImmutableSet<File> getClassPathFromManifest(File jarFile, @Nullable Manifest manifest) {
         if (manifest == null) {
            return ImmutableSet.of();
         } else {
            ImmutableSet.Builder<File> builder = ImmutableSet.builder();
            String classpathAttribute = manifest.getMainAttributes().getValue(Name.CLASS_PATH.toString());
            if (classpathAttribute != null) {
               Iterator i$ = ClassPath.CLASS_PATH_ATTRIBUTE_SEPARATOR.split(classpathAttribute).iterator();

               while(i$.hasNext()) {
                  String path = (String)i$.next();

                  URL url;
                  try {
                     url = getClassPathEntry(jarFile, path);
                  } catch (MalformedURLException var8) {
                     ClassPath.logger.warning("Invalid Class-Path entry: " + path);
                     continue;
                  }

                  if (url.getProtocol().equals("file")) {
                     builder.add((Object)(new File(url.getFile())));
                  }
               }
            }

            return builder.build();
         }
      }

      @VisibleForTesting
      static ImmutableMap<File, ClassLoader> getClassPathEntries(ClassLoader classloader) {
         LinkedHashMap<File, ClassLoader> entries = Maps.newLinkedHashMap();
         ClassLoader parent = classloader.getParent();
         if (parent != null) {
            entries.putAll(getClassPathEntries(parent));
         }

         if (classloader instanceof URLClassLoader) {
            URLClassLoader urlClassLoader = (URLClassLoader)classloader;
            URL[] arr$ = urlClassLoader.getURLs();
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               URL entry = arr$[i$];
               if (entry.getProtocol().equals("file")) {
                  File file = new File(entry.getFile());
                  if (!entries.containsKey(file)) {
                     entries.put(file, classloader);
                  }
               }
            }
         }

         return ImmutableMap.copyOf((Map)entries);
      }

      @VisibleForTesting
      static URL getClassPathEntry(File jarFile, String path) throws MalformedURLException {
         return new URL(jarFile.toURI().toURL(), path);
      }
   }

   @Beta
   public static final class ClassInfo extends ClassPath.ResourceInfo {
      private final String className;

      ClassInfo(String resourceName, ClassLoader loader) {
         super(resourceName, loader);
         this.className = ClassPath.getClassName(resourceName);
      }

      public String getPackageName() {
         return Reflection.getPackageName(this.className);
      }

      public String getSimpleName() {
         int lastDollarSign = this.className.lastIndexOf(36);
         String packageName;
         if (lastDollarSign != -1) {
            packageName = this.className.substring(lastDollarSign + 1);
            return CharMatcher.DIGIT.trimLeadingFrom(packageName);
         } else {
            packageName = this.getPackageName();
            return packageName.isEmpty() ? this.className : this.className.substring(packageName.length() + 1);
         }
      }

      public String getName() {
         return this.className;
      }

      public Class<?> load() {
         try {
            return this.loader.loadClass(this.className);
         } catch (ClassNotFoundException var2) {
            throw new IllegalStateException(var2);
         }
      }

      public String toString() {
         return this.className;
      }
   }

   @Beta
   public static class ResourceInfo {
      private final String resourceName;
      final ClassLoader loader;

      static ClassPath.ResourceInfo of(String resourceName, ClassLoader loader) {
         return (ClassPath.ResourceInfo)(resourceName.endsWith(".class") ? new ClassPath.ClassInfo(resourceName, loader) : new ClassPath.ResourceInfo(resourceName, loader));
      }

      ResourceInfo(String resourceName, ClassLoader loader) {
         this.resourceName = (String)Preconditions.checkNotNull(resourceName);
         this.loader = (ClassLoader)Preconditions.checkNotNull(loader);
      }

      public final URL url() throws NoSuchElementException {
         URL url = this.loader.getResource(this.resourceName);
         if (url == null) {
            throw new NoSuchElementException(this.resourceName);
         } else {
            return url;
         }
      }

      public final String getResourceName() {
         return this.resourceName;
      }

      public int hashCode() {
         return this.resourceName.hashCode();
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof ClassPath.ResourceInfo)) {
            return false;
         } else {
            ClassPath.ResourceInfo that = (ClassPath.ResourceInfo)obj;
            return this.resourceName.equals(that.resourceName) && this.loader == that.loader;
         }
      }

      public String toString() {
         return this.resourceName;
      }
   }
}
