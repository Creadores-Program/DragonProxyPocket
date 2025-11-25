package org.apache.commons.lang3.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.Validate;

public class ConstructorUtils {
   public static <T> T invokeConstructor(Class<T> cls, Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
      args = ArrayUtils.nullToEmpty(args);
      Class<?>[] parameterTypes = ClassUtils.toClass(args);
      return invokeConstructor(cls, args, parameterTypes);
   }

   public static <T> T invokeConstructor(Class<T> cls, Object[] args, Class<?>[] parameterTypes) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
      args = ArrayUtils.nullToEmpty(args);
      parameterTypes = ArrayUtils.nullToEmpty(parameterTypes);
      Constructor<T> ctor = getMatchingAccessibleConstructor(cls, parameterTypes);
      if (ctor == null) {
         throw new NoSuchMethodException("No such accessible constructor on object: " + cls.getName());
      } else {
         return ctor.newInstance(args);
      }
   }

   public static <T> T invokeExactConstructor(Class<T> cls, Object... args) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
      args = ArrayUtils.nullToEmpty(args);
      Class<?>[] parameterTypes = ClassUtils.toClass(args);
      return invokeExactConstructor(cls, args, parameterTypes);
   }

   public static <T> T invokeExactConstructor(Class<T> cls, Object[] args, Class<?>[] parameterTypes) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
      args = ArrayUtils.nullToEmpty(args);
      parameterTypes = ArrayUtils.nullToEmpty(parameterTypes);
      Constructor<T> ctor = getAccessibleConstructor(cls, parameterTypes);
      if (ctor == null) {
         throw new NoSuchMethodException("No such accessible constructor on object: " + cls.getName());
      } else {
         return ctor.newInstance(args);
      }
   }

   public static <T> Constructor<T> getAccessibleConstructor(Class<T> cls, Class<?>... parameterTypes) {
      Validate.notNull(cls, "class cannot be null");

      try {
         return getAccessibleConstructor(cls.getConstructor(parameterTypes));
      } catch (NoSuchMethodException var3) {
         return null;
      }
   }

   public static <T> Constructor<T> getAccessibleConstructor(Constructor<T> ctor) {
      Validate.notNull(ctor, "constructor cannot be null");
      return MemberUtils.isAccessible(ctor) && isAccessible(ctor.getDeclaringClass()) ? ctor : null;
   }

   public static <T> Constructor<T> getMatchingAccessibleConstructor(Class<T> cls, Class<?>... parameterTypes) {
      Validate.notNull(cls, "class cannot be null");

      Constructor result;
      try {
         result = cls.getConstructor(parameterTypes);
         MemberUtils.setAccessibleWorkaround(result);
         return result;
      } catch (NoSuchMethodException var9) {
         result = null;
         Constructor<?>[] ctors = cls.getConstructors();
         Constructor[] arr$ = ctors;
         int len$ = ctors.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            Constructor<?> ctor = arr$[i$];
            if (ClassUtils.isAssignable(parameterTypes, ctor.getParameterTypes(), true)) {
               ctor = getAccessibleConstructor(ctor);
               if (ctor != null) {
                  MemberUtils.setAccessibleWorkaround(ctor);
                  if (result == null || MemberUtils.compareParameterTypes(ctor.getParameterTypes(), result.getParameterTypes(), parameterTypes) < 0) {
                     result = ctor;
                  }
               }
            }
         }

         return result;
      }
   }

   private static boolean isAccessible(Class<?> type) {
      for(Class cls = type; cls != null; cls = cls.getEnclosingClass()) {
         if (!Modifier.isPublic(cls.getModifiers())) {
            return false;
         }
      }

      return true;
   }
}
