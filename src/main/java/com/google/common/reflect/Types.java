package com.google.common.reflect;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

final class Types {
   private static final Function<Type, String> TYPE_NAME = new Function<Type, String>() {
      public String apply(Type from) {
         return Types.JavaVersion.CURRENT.typeName(from);
      }
   };
   private static final Joiner COMMA_JOINER = Joiner.on(", ").useForNull("null");

   static Type newArrayType(Type componentType) {
      if (componentType instanceof WildcardType) {
         WildcardType wildcard = (WildcardType)componentType;
         Type[] lowerBounds = wildcard.getLowerBounds();
         Preconditions.checkArgument(lowerBounds.length <= 1, "Wildcard cannot have more than one lower bounds.");
         if (lowerBounds.length == 1) {
            return supertypeOf(newArrayType(lowerBounds[0]));
         } else {
            Type[] upperBounds = wildcard.getUpperBounds();
            Preconditions.checkArgument(upperBounds.length == 1, "Wildcard should have only one upper bound.");
            return subtypeOf(newArrayType(upperBounds[0]));
         }
      } else {
         return Types.JavaVersion.CURRENT.newArrayType(componentType);
      }
   }

   static ParameterizedType newParameterizedTypeWithOwner(@Nullable Type ownerType, Class<?> rawType, Type... arguments) {
      if (ownerType == null) {
         return newParameterizedType(rawType, arguments);
      } else {
         Preconditions.checkNotNull(arguments);
         Preconditions.checkArgument(rawType.getEnclosingClass() != null, "Owner type for unenclosed %s", rawType);
         return new Types.ParameterizedTypeImpl(ownerType, rawType, arguments);
      }
   }

   static ParameterizedType newParameterizedType(Class<?> rawType, Type... arguments) {
      return new Types.ParameterizedTypeImpl(Types.ClassOwnership.JVM_BEHAVIOR.getOwnerType(rawType), rawType, arguments);
   }

   static <D extends GenericDeclaration> TypeVariable<D> newArtificialTypeVariable(D declaration, String name, Type... bounds) {
      return newTypeVariableImpl(declaration, name, bounds.length == 0 ? new Type[]{Object.class} : bounds);
   }

   @VisibleForTesting
   static WildcardType subtypeOf(Type upperBound) {
      return new Types.WildcardTypeImpl(new Type[0], new Type[]{upperBound});
   }

   @VisibleForTesting
   static WildcardType supertypeOf(Type lowerBound) {
      return new Types.WildcardTypeImpl(new Type[]{lowerBound}, new Type[]{Object.class});
   }

   static String toString(Type type) {
      return type instanceof Class ? ((Class)type).getName() : type.toString();
   }

   @Nullable
   static Type getComponentType(Type type) {
      Preconditions.checkNotNull(type);
      final AtomicReference<Type> result = new AtomicReference();
      (new TypeVisitor() {
         void visitTypeVariable(TypeVariable<?> t) {
            result.set(Types.subtypeOfComponentType(t.getBounds()));
         }

         void visitWildcardType(WildcardType t) {
            result.set(Types.subtypeOfComponentType(t.getUpperBounds()));
         }

         void visitGenericArrayType(GenericArrayType t) {
            result.set(t.getGenericComponentType());
         }

         void visitClass(Class<?> t) {
            result.set(t.getComponentType());
         }
      }).visit(new Type[]{type});
      return (Type)result.get();
   }

   @Nullable
   private static Type subtypeOfComponentType(Type[] bounds) {
      Type[] arr$ = bounds;
      int len$ = bounds.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         Type bound = arr$[i$];
         Type componentType = getComponentType(bound);
         if (componentType != null) {
            if (componentType instanceof Class) {
               Class<?> componentClass = (Class)componentType;
               if (componentClass.isPrimitive()) {
                  return componentClass;
               }
            }

            return subtypeOf(componentType);
         }
      }

      return null;
   }

   private static <D extends GenericDeclaration> TypeVariable<D> newTypeVariableImpl(D genericDeclaration, String name, Type[] bounds) {
      Types.TypeVariableImpl<D> typeVariableImpl = new Types.TypeVariableImpl(genericDeclaration, name, bounds);
      TypeVariable<D> typeVariable = (TypeVariable)Reflection.newProxy(TypeVariable.class, new Types.TypeVariableInvocationHandler(typeVariableImpl));
      return typeVariable;
   }

   private static Type[] toArray(Collection<Type> types) {
      return (Type[])types.toArray(new Type[types.size()]);
   }

   private static Iterable<Type> filterUpperBounds(Iterable<Type> bounds) {
      return Iterables.filter(bounds, Predicates.not(Predicates.equalTo(Object.class)));
   }

   private static void disallowPrimitiveType(Type[] types, String usedAs) {
      Type[] arr$ = types;
      int len$ = types.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         Type type = arr$[i$];
         if (type instanceof Class) {
            Class<?> cls = (Class)type;
            Preconditions.checkArgument(!cls.isPrimitive(), "Primitive type '%s' used as %s", cls, usedAs);
         }
      }

   }

   static Class<?> getArrayClass(Class<?> componentType) {
      return Array.newInstance(componentType, 0).getClass();
   }

   private Types() {
   }

   static final class NativeTypeVariableEquals<X> {
      static final boolean NATIVE_TYPE_VARIABLE_ONLY = !Types.NativeTypeVariableEquals.class.getTypeParameters()[0].equals(Types.newArtificialTypeVariable(Types.NativeTypeVariableEquals.class, "X"));
   }

   static enum JavaVersion {
      JAVA6 {
         GenericArrayType newArrayType(Type componentType) {
            return new Types.GenericArrayTypeImpl(componentType);
         }

         Type usedInGenericType(Type type) {
            Preconditions.checkNotNull(type);
            if (type instanceof Class) {
               Class<?> cls = (Class)type;
               if (cls.isArray()) {
                  return new Types.GenericArrayTypeImpl(cls.getComponentType());
               }
            }

            return type;
         }
      },
      JAVA7 {
         Type newArrayType(Type componentType) {
            return (Type)(componentType instanceof Class ? Types.getArrayClass((Class)componentType) : new Types.GenericArrayTypeImpl(componentType));
         }

         Type usedInGenericType(Type type) {
            return (Type)Preconditions.checkNotNull(type);
         }
      },
      JAVA8 {
         Type newArrayType(Type componentType) {
            return JAVA7.newArrayType(componentType);
         }

         Type usedInGenericType(Type type) {
            return JAVA7.usedInGenericType(type);
         }

         String typeName(Type type) {
            try {
               Method getTypeName = Type.class.getMethod("getTypeName");
               return (String)getTypeName.invoke(type);
            } catch (NoSuchMethodException var3) {
               throw new AssertionError("Type.getTypeName should be available in Java 8");
            } catch (InvocationTargetException var4) {
               throw new RuntimeException(var4);
            } catch (IllegalAccessException var5) {
               throw new RuntimeException(var5);
            }
         }
      };

      static final Types.JavaVersion CURRENT;

      private JavaVersion() {
      }

      abstract Type newArrayType(Type var1);

      abstract Type usedInGenericType(Type var1);

      String typeName(Type type) {
         return Types.toString(type);
      }

      final ImmutableList<Type> usedInGenericType(Type[] types) {
         ImmutableList.Builder<Type> builder = ImmutableList.builder();
         Type[] arr$ = types;
         int len$ = types.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            Type type = arr$[i$];
            builder.add((Object)this.usedInGenericType(type));
         }

         return builder.build();
      }

      // $FF: synthetic method
      JavaVersion(Object x2) {
         this();
      }

      static {
         if (AnnotatedElement.class.isAssignableFrom(TypeVariable.class)) {
            CURRENT = JAVA8;
         } else if ((new TypeCapture<int[]>() {
         }).capture() instanceof Class) {
            CURRENT = JAVA7;
         } else {
            CURRENT = JAVA6;
         }

      }
   }

   static final class WildcardTypeImpl implements WildcardType, Serializable {
      private final ImmutableList<Type> lowerBounds;
      private final ImmutableList<Type> upperBounds;
      private static final long serialVersionUID = 0L;

      WildcardTypeImpl(Type[] lowerBounds, Type[] upperBounds) {
         Types.disallowPrimitiveType(lowerBounds, "lower bound for wildcard");
         Types.disallowPrimitiveType(upperBounds, "upper bound for wildcard");
         this.lowerBounds = Types.JavaVersion.CURRENT.usedInGenericType(lowerBounds);
         this.upperBounds = Types.JavaVersion.CURRENT.usedInGenericType(upperBounds);
      }

      public Type[] getLowerBounds() {
         return Types.toArray(this.lowerBounds);
      }

      public Type[] getUpperBounds() {
         return Types.toArray(this.upperBounds);
      }

      public boolean equals(Object obj) {
         if (!(obj instanceof WildcardType)) {
            return false;
         } else {
            WildcardType that = (WildcardType)obj;
            return this.lowerBounds.equals(Arrays.asList(that.getLowerBounds())) && this.upperBounds.equals(Arrays.asList(that.getUpperBounds()));
         }
      }

      public int hashCode() {
         return this.lowerBounds.hashCode() ^ this.upperBounds.hashCode();
      }

      public String toString() {
         StringBuilder builder = new StringBuilder("?");
         Iterator i$ = this.lowerBounds.iterator();

         Type upperBound;
         while(i$.hasNext()) {
            upperBound = (Type)i$.next();
            builder.append(" super ").append(Types.JavaVersion.CURRENT.typeName(upperBound));
         }

         i$ = Types.filterUpperBounds(this.upperBounds).iterator();

         while(i$.hasNext()) {
            upperBound = (Type)i$.next();
            builder.append(" extends ").append(Types.JavaVersion.CURRENT.typeName(upperBound));
         }

         return builder.toString();
      }
   }

   private static final class TypeVariableImpl<D extends GenericDeclaration> {
      private final D genericDeclaration;
      private final String name;
      private final ImmutableList<Type> bounds;

      TypeVariableImpl(D genericDeclaration, String name, Type[] bounds) {
         Types.disallowPrimitiveType(bounds, "bound for type variable");
         this.genericDeclaration = (GenericDeclaration)Preconditions.checkNotNull(genericDeclaration);
         this.name = (String)Preconditions.checkNotNull(name);
         this.bounds = ImmutableList.copyOf((Object[])bounds);
      }

      public Type[] getBounds() {
         return Types.toArray(this.bounds);
      }

      public D getGenericDeclaration() {
         return this.genericDeclaration;
      }

      public String getName() {
         return this.name;
      }

      public String getTypeName() {
         return this.name;
      }

      public String toString() {
         return this.name;
      }

      public int hashCode() {
         return this.genericDeclaration.hashCode() ^ this.name.hashCode();
      }

      public boolean equals(Object obj) {
         if (Types.NativeTypeVariableEquals.NATIVE_TYPE_VARIABLE_ONLY) {
            if (obj != null && Proxy.isProxyClass(obj.getClass()) && Proxy.getInvocationHandler(obj) instanceof Types.TypeVariableInvocationHandler) {
               Types.TypeVariableInvocationHandler typeVariableInvocationHandler = (Types.TypeVariableInvocationHandler)Proxy.getInvocationHandler(obj);
               Types.TypeVariableImpl<?> that = typeVariableInvocationHandler.typeVariableImpl;
               return this.name.equals(that.getName()) && this.genericDeclaration.equals(that.getGenericDeclaration()) && this.bounds.equals(that.bounds);
            } else {
               return false;
            }
         } else if (!(obj instanceof TypeVariable)) {
            return false;
         } else {
            TypeVariable<?> that = (TypeVariable)obj;
            return this.name.equals(that.getName()) && this.genericDeclaration.equals(that.getGenericDeclaration());
         }
      }
   }

   private static final class TypeVariableInvocationHandler implements InvocationHandler {
      private static final ImmutableMap<String, Method> typeVariableMethods;
      private final Types.TypeVariableImpl<?> typeVariableImpl;

      TypeVariableInvocationHandler(Types.TypeVariableImpl<?> typeVariableImpl) {
         this.typeVariableImpl = typeVariableImpl;
      }

      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
         String methodName = method.getName();
         Method typeVariableMethod = (Method)typeVariableMethods.get(methodName);
         if (typeVariableMethod == null) {
            throw new UnsupportedOperationException(methodName);
         } else {
            try {
               return typeVariableMethod.invoke(this.typeVariableImpl, args);
            } catch (InvocationTargetException var7) {
               throw var7.getCause();
            }
         }
      }

      static {
         ImmutableMap.Builder<String, Method> builder = ImmutableMap.builder();
         Method[] arr$ = Types.TypeVariableImpl.class.getMethods();
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            Method method = arr$[i$];
            if (method.getDeclaringClass().equals(Types.TypeVariableImpl.class)) {
               try {
                  method.setAccessible(true);
               } catch (AccessControlException var6) {
               }

               builder.put(method.getName(), method);
            }
         }

         typeVariableMethods = builder.build();
      }
   }

   private static final class ParameterizedTypeImpl implements ParameterizedType, Serializable {
      private final Type ownerType;
      private final ImmutableList<Type> argumentsList;
      private final Class<?> rawType;
      private static final long serialVersionUID = 0L;

      ParameterizedTypeImpl(@Nullable Type ownerType, Class<?> rawType, Type[] typeArguments) {
         Preconditions.checkNotNull(rawType);
         Preconditions.checkArgument(typeArguments.length == rawType.getTypeParameters().length);
         Types.disallowPrimitiveType(typeArguments, "type parameter");
         this.ownerType = ownerType;
         this.rawType = rawType;
         this.argumentsList = Types.JavaVersion.CURRENT.usedInGenericType(typeArguments);
      }

      public Type[] getActualTypeArguments() {
         return Types.toArray(this.argumentsList);
      }

      public Type getRawType() {
         return this.rawType;
      }

      public Type getOwnerType() {
         return this.ownerType;
      }

      public String toString() {
         StringBuilder builder = new StringBuilder();
         if (this.ownerType != null) {
            builder.append(Types.JavaVersion.CURRENT.typeName(this.ownerType)).append('.');
         }

         builder.append(this.rawType.getName()).append('<').append(Types.COMMA_JOINER.join(Iterables.transform(this.argumentsList, Types.TYPE_NAME))).append('>');
         return builder.toString();
      }

      public int hashCode() {
         return (this.ownerType == null ? 0 : this.ownerType.hashCode()) ^ this.argumentsList.hashCode() ^ this.rawType.hashCode();
      }

      public boolean equals(Object other) {
         if (!(other instanceof ParameterizedType)) {
            return false;
         } else {
            ParameterizedType that = (ParameterizedType)other;
            return this.getRawType().equals(that.getRawType()) && Objects.equal(this.getOwnerType(), that.getOwnerType()) && Arrays.equals(this.getActualTypeArguments(), that.getActualTypeArguments());
         }
      }
   }

   private static final class GenericArrayTypeImpl implements GenericArrayType, Serializable {
      private final Type componentType;
      private static final long serialVersionUID = 0L;

      GenericArrayTypeImpl(Type componentType) {
         this.componentType = Types.JavaVersion.CURRENT.usedInGenericType(componentType);
      }

      public Type getGenericComponentType() {
         return this.componentType;
      }

      public String toString() {
         return Types.toString(this.componentType) + "[]";
      }

      public int hashCode() {
         return this.componentType.hashCode();
      }

      public boolean equals(Object obj) {
         if (obj instanceof GenericArrayType) {
            GenericArrayType that = (GenericArrayType)obj;
            return Objects.equal(this.getGenericComponentType(), that.getGenericComponentType());
         } else {
            return false;
         }
      }
   }

   private static enum ClassOwnership {
      OWNED_BY_ENCLOSING_CLASS {
         @Nullable
         Class<?> getOwnerType(Class<?> rawType) {
            return rawType.getEnclosingClass();
         }
      },
      LOCAL_CLASS_HAS_NO_OWNER {
         @Nullable
         Class<?> getOwnerType(Class<?> rawType) {
            return rawType.isLocalClass() ? null : rawType.getEnclosingClass();
         }
      };

      static final Types.ClassOwnership JVM_BEHAVIOR = detectJvmBehavior();

      private ClassOwnership() {
      }

      @Nullable
      abstract Class<?> getOwnerType(Class<?> var1);

      private static Types.ClassOwnership detectJvmBehavior() {
         class LocalClass<T> {
         }

         Class<?> subclass = (new LocalClass<String>() {
         }).getClass();
         ParameterizedType parameterizedType = (ParameterizedType)subclass.getGenericSuperclass();
         Types.ClassOwnership[] arr$ = values();
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            Types.ClassOwnership behavior = arr$[i$];
            if (behavior.getOwnerType(LocalClass.class) == parameterizedType.getOwnerType()) {
               return behavior;
            }
         }

         throw new AssertionError();
      }

      // $FF: synthetic method
      ClassOwnership(Object x2) {
         this();
      }
   }
}
