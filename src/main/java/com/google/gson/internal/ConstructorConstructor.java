package com.google.gson.internal;

import com.google.gson.InstanceCreator;
import com.google.gson.JsonIOException;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public final class ConstructorConstructor {
   private final Map<Type, InstanceCreator<?>> instanceCreators;

   public ConstructorConstructor(Map<Type, InstanceCreator<?>> instanceCreators) {
      this.instanceCreators = instanceCreators;
   }

   public <T> ObjectConstructor<T> get(TypeToken<T> typeToken) {
      final Type type = typeToken.getType();
      Class<? super T> rawType = typeToken.getRawType();
      final InstanceCreator<T> typeCreator = (InstanceCreator)this.instanceCreators.get(type);
      if (typeCreator != null) {
         return new ObjectConstructor<T>() {
            public T construct() {
               return typeCreator.createInstance(type);
            }
         };
      } else {
         final InstanceCreator<T> rawTypeCreator = (InstanceCreator)this.instanceCreators.get(rawType);
         if (rawTypeCreator != null) {
            return new ObjectConstructor<T>() {
               public T construct() {
                  return rawTypeCreator.createInstance(type);
               }
            };
         } else {
            ObjectConstructor<T> defaultConstructor = this.newDefaultConstructor(rawType);
            if (defaultConstructor != null) {
               return defaultConstructor;
            } else {
               ObjectConstructor<T> defaultImplementation = this.newDefaultImplementationConstructor(type, rawType);
               return defaultImplementation != null ? defaultImplementation : this.newUnsafeAllocator(type, rawType);
            }
         }
      }
   }

   private <T> ObjectConstructor<T> newDefaultConstructor(Class<? super T> rawType) {
      try {
         final Constructor<? super T> constructor = rawType.getDeclaredConstructor();
         if (!constructor.isAccessible()) {
            constructor.setAccessible(true);
         }

         return new ObjectConstructor<T>() {
            public T construct() {
               try {
                  Object[] args = null;
                  return constructor.newInstance((Object[])args);
               } catch (InstantiationException var2) {
                  throw new RuntimeException("Failed to invoke " + constructor + " with no args", var2);
               } catch (InvocationTargetException var3) {
                  throw new RuntimeException("Failed to invoke " + constructor + " with no args", var3.getTargetException());
               } catch (IllegalAccessException var4) {
                  throw new AssertionError(var4);
               }
            }
         };
      } catch (NoSuchMethodException var3) {
         return null;
      }
   }

   private <T> ObjectConstructor<T> newDefaultImplementationConstructor(final Type type, Class<? super T> rawType) {
      if (Collection.class.isAssignableFrom(rawType)) {
         if (SortedSet.class.isAssignableFrom(rawType)) {
            return new ObjectConstructor<T>() {
               public T construct() {
                  return new TreeSet();
               }
            };
         } else if (EnumSet.class.isAssignableFrom(rawType)) {
            return new ObjectConstructor<T>() {
               public T construct() {
                  if (type instanceof ParameterizedType) {
                     Type elementType = ((ParameterizedType)type).getActualTypeArguments()[0];
                     if (elementType instanceof Class) {
                        return EnumSet.noneOf((Class)elementType);
                     } else {
                        throw new JsonIOException("Invalid EnumSet type: " + type.toString());
                     }
                  } else {
                     throw new JsonIOException("Invalid EnumSet type: " + type.toString());
                  }
               }
            };
         } else if (Set.class.isAssignableFrom(rawType)) {
            return new ObjectConstructor<T>() {
               public T construct() {
                  return new LinkedHashSet();
               }
            };
         } else {
            return Queue.class.isAssignableFrom(rawType) ? new ObjectConstructor<T>() {
               public T construct() {
                  return new LinkedList();
               }
            } : new ObjectConstructor<T>() {
               public T construct() {
                  return new ArrayList();
               }
            };
         }
      } else if (Map.class.isAssignableFrom(rawType)) {
         if (SortedMap.class.isAssignableFrom(rawType)) {
            return new ObjectConstructor<T>() {
               public T construct() {
                  return new TreeMap();
               }
            };
         } else {
            return type instanceof ParameterizedType && !String.class.isAssignableFrom(TypeToken.get(((ParameterizedType)type).getActualTypeArguments()[0]).getRawType()) ? new ObjectConstructor<T>() {
               public T construct() {
                  return new LinkedHashMap();
               }
            } : new ObjectConstructor<T>() {
               public T construct() {
                  return new LinkedTreeMap();
               }
            };
         }
      } else {
         return null;
      }
   }

   private <T> ObjectConstructor<T> newUnsafeAllocator(final Type type, final Class<? super T> rawType) {
      return new ObjectConstructor<T>() {
         private final UnsafeAllocator unsafeAllocator = UnsafeAllocator.create();

         public T construct() {
            try {
               Object newInstance = this.unsafeAllocator.newInstance(rawType);
               return newInstance;
            } catch (Exception var2) {
               throw new RuntimeException("Unable to invoke no-args constructor for " + type + ". " + "Register an InstanceCreator with Gson for this type may fix this problem.", var2);
            }
         }
      };
   }

   public String toString() {
      return this.instanceCreators.toString();
   }
}
