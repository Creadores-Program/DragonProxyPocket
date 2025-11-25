package org.fusesource.jansi.internal;

import org.fusesource.hawtjni.runtime.FieldFlag;
import org.fusesource.hawtjni.runtime.JniClass;
import org.fusesource.hawtjni.runtime.JniField;
import org.fusesource.hawtjni.runtime.JniMethod;
import org.fusesource.hawtjni.runtime.Library;
import org.fusesource.hawtjni.runtime.MethodFlag;

@JniClass
public class CLibrary {
   private static final Library LIBRARY = new Library("jansi", CLibrary.class);
   @JniField(
      flags = {FieldFlag.CONSTANT},
      conditional = "defined(STDIN_FILENO)"
   )
   public static int STDIN_FILENO;
   @JniField(
      flags = {FieldFlag.CONSTANT},
      conditional = "defined(STDOUT_FILENO)"
   )
   public static int STDOUT_FILENO;
   @JniField(
      flags = {FieldFlag.CONSTANT},
      conditional = "defined(STDERR_FILENO)"
   )
   public static int STDERR_FILENO;
   @JniField(
      flags = {FieldFlag.CONSTANT},
      accessor = "1",
      conditional = "defined(HAVE_ISATTY)"
   )
   public static boolean HAVE_ISATTY;

   @JniMethod(
      flags = {MethodFlag.CONSTANT_INITIALIZER}
   )
   private static final native void init();

   @JniMethod(
      conditional = "defined(HAVE_ISATTY)"
   )
   public static final native int isatty(int var0);

   static {
      LIBRARY.load();
      init();
   }
}
