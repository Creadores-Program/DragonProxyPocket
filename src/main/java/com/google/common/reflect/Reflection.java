package com.google.common.reflect;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

@Beta
public final class Reflection {
   public static String getPackageName(Class<?> clazz) {
      return getPackageName(clazz.getName());
   }

   public static String getPackageName(String classFullName) {
      int lastDot = classFullName.lastIndexOf(46);
      return lastDot < 0 ? "" : classFullName.substring(0, lastDot);
   }

   public static void initialize(Class<?>... classes) {
      Class[] arr$ = classes;
      int len$ = classes.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         Class clazz = arr$[i$];

         try {
            Class.forName(clazz.getName(), true, clazz.getClassLoader());
         } catch (ClassNotFoundException var6) {
            throw new AssertionError(var6);
         }
      }

   }

   public static <T> T newProxy(Class<T> interfaceType, InvocationHandler handler) {
      Preconditions.checkNotNull(handler);
      Preconditions.checkArgument(interfaceType.isInterface(), "%s is not an interface", interfaceType);
      Object object = Proxy.newProxyInstance(interfaceType.getClassLoader(), new Class[]{interfaceType}, handler);
      return interfaceType.cast(object);
   }

   private Reflection() {
   }
}
