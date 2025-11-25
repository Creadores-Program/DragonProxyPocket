package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

public final class Throwables {
   private static final String JAVA_LANG_ACCESS_CLASSNAME = "sun.misc.JavaLangAccess";
   @VisibleForTesting
   static final String SHARED_SECRETS_CLASSNAME = "sun.misc.SharedSecrets";
   @Nullable
   private static final Object jla = getJLA();
   @Nullable
   private static final Method getStackTraceElementMethod;
   @Nullable
   private static final Method getStackTraceDepthMethod;

   private Throwables() {
   }

   public static <X extends Throwable> void propagateIfInstanceOf(@Nullable Throwable throwable, Class<X> declaredType) throws X {
      if (throwable != null && declaredType.isInstance(throwable)) {
         throw (Throwable)declaredType.cast(throwable);
      }
   }

   public static void propagateIfPossible(@Nullable Throwable throwable) {
      propagateIfInstanceOf(throwable, Error.class);
      propagateIfInstanceOf(throwable, RuntimeException.class);
   }

   public static <X extends Throwable> void propagateIfPossible(@Nullable Throwable throwable, Class<X> declaredType) throws X {
      propagateIfInstanceOf(throwable, declaredType);
      propagateIfPossible(throwable);
   }

   public static <X1 extends Throwable, X2 extends Throwable> void propagateIfPossible(@Nullable Throwable throwable, Class<X1> declaredType1, Class<X2> declaredType2) throws X1, X2 {
      Preconditions.checkNotNull(declaredType2);
      propagateIfInstanceOf(throwable, declaredType1);
      propagateIfPossible(throwable, declaredType2);
   }

   public static RuntimeException propagate(Throwable throwable) {
      propagateIfPossible((Throwable)Preconditions.checkNotNull(throwable));
      throw new RuntimeException(throwable);
   }

   @CheckReturnValue
   public static Throwable getRootCause(Throwable throwable) {
      Throwable cause;
      while((cause = throwable.getCause()) != null) {
         throwable = cause;
      }

      return throwable;
   }

   @CheckReturnValue
   @Beta
   public static List<Throwable> getCausalChain(Throwable throwable) {
      Preconditions.checkNotNull(throwable);

      ArrayList causes;
      for(causes = new ArrayList(4); throwable != null; throwable = throwable.getCause()) {
         causes.add(throwable);
      }

      return Collections.unmodifiableList(causes);
   }

   @CheckReturnValue
   public static String getStackTraceAsString(Throwable throwable) {
      StringWriter stringWriter = new StringWriter();
      throwable.printStackTrace(new PrintWriter(stringWriter));
      return stringWriter.toString();
   }

   @CheckReturnValue
   @Beta
   public static List<StackTraceElement> lazyStackTrace(Throwable throwable) {
      return lazyStackTraceIsLazy() ? jlaStackTrace(throwable) : Collections.unmodifiableList(Arrays.asList(throwable.getStackTrace()));
   }

   @CheckReturnValue
   @Beta
   public static boolean lazyStackTraceIsLazy() {
      return getStackTraceElementMethod != null & getStackTraceDepthMethod != null;
   }

   private static List<StackTraceElement> jlaStackTrace(final Throwable t) {
      Preconditions.checkNotNull(t);
      return new AbstractList<StackTraceElement>() {
         public StackTraceElement get(int n) {
            return (StackTraceElement)Throwables.invokeAccessibleNonThrowingMethod(Throwables.getStackTraceElementMethod, Throwables.jla, t, n);
         }

         public int size() {
            return (Integer)Throwables.invokeAccessibleNonThrowingMethod(Throwables.getStackTraceDepthMethod, Throwables.jla, t);
         }
      };
   }

   private static Object invokeAccessibleNonThrowingMethod(Method method, Object receiver, Object... params) {
      try {
         return method.invoke(receiver, params);
      } catch (IllegalAccessException var4) {
         throw new RuntimeException(var4);
      } catch (InvocationTargetException var5) {
         throw propagate(var5.getCause());
      }
   }

   @Nullable
   private static Object getJLA() {
      try {
         Class<?> sharedSecrets = Class.forName("sun.misc.SharedSecrets", false, (ClassLoader)null);
         Method langAccess = sharedSecrets.getMethod("getJavaLangAccess");
         return langAccess.invoke((Object)null);
      } catch (ThreadDeath var2) {
         throw var2;
      } catch (Throwable var3) {
         return null;
      }
   }

   @Nullable
   private static Method getGetMethod() {
      return getJlaMethod("getStackTraceElement", Throwable.class, Integer.TYPE);
   }

   @Nullable
   private static Method getSizeMethod() {
      return getJlaMethod("getStackTraceDepth", Throwable.class);
   }

   @Nullable
   private static Method getJlaMethod(String name, Class<?>... parameterTypes) throws ThreadDeath {
      try {
         return Class.forName("sun.misc.JavaLangAccess", false, (ClassLoader)null).getMethod(name, parameterTypes);
      } catch (ThreadDeath var3) {
         throw var3;
      } catch (Throwable var4) {
         return null;
      }
   }

   static {
      getStackTraceElementMethod = jla == null ? null : getGetMethod();
      getStackTraceDepthMethod = jla == null ? null : getSizeMethod();
   }
}
