package com.google.gson.internal;

import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public abstract class UnsafeAllocator {
   public abstract <T> T newInstance(Class<T> var1) throws Exception;

   public static UnsafeAllocator create() {
      try {
         Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
         Field f = unsafeClass.getDeclaredField("theUnsafe");
         f.setAccessible(true);
         final Object unsafe = f.get((Object)null);
         final Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
         return new UnsafeAllocator() {
            public <T> T newInstance(Class<T> c) throws Exception {
               return allocateInstance.invoke(unsafe, c);
            }
         };
      } catch (Exception var6) {
         final Method newInstance;
         try {
            newInstance = ObjectStreamClass.class.getDeclaredMethod("getConstructorId", Class.class);
            newInstance.setAccessible(true);
            final int constructorId = (Integer)newInstance.invoke((Object)null, Object.class);
            final Method newInstance = ObjectStreamClass.class.getDeclaredMethod("newInstance", Class.class, Integer.TYPE);
            newInstance.setAccessible(true);
            return new UnsafeAllocator() {
               public <T> T newInstance(Class<T> c) throws Exception {
                  return newInstance.invoke((Object)null, c, constructorId);
               }
            };
         } catch (Exception var5) {
            try {
               newInstance = ObjectInputStream.class.getDeclaredMethod("newInstance", Class.class, Class.class);
               newInstance.setAccessible(true);
               return new UnsafeAllocator() {
                  public <T> T newInstance(Class<T> c) throws Exception {
                     return newInstance.invoke((Object)null, c, Object.class);
                  }
               };
            } catch (Exception var4) {
               return new UnsafeAllocator() {
                  public <T> T newInstance(Class<T> c) {
                     throw new UnsupportedOperationException("Cannot allocate " + c);
                  }
               };
            }
         }
      }
   }
}
