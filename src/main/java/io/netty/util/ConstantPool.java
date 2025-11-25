package io.netty.util;

import io.netty.util.internal.ObjectUtil;
import java.util.HashMap;
import java.util.Map;

public abstract class ConstantPool<T extends Constant<T>> {
   private final Map<String, T> constants = new HashMap();
   private int nextId = 1;

   public T valueOf(Class<?> firstNameComponent, String secondNameComponent) {
      if (firstNameComponent == null) {
         throw new NullPointerException("firstNameComponent");
      } else if (secondNameComponent == null) {
         throw new NullPointerException("secondNameComponent");
      } else {
         return this.valueOf(firstNameComponent.getName() + '#' + secondNameComponent);
      }
   }

   public T valueOf(String name) {
      if (name == null) {
         throw new NullPointerException("name");
      } else if (name.isEmpty()) {
         throw new IllegalArgumentException("empty name");
      } else {
         synchronized(this.constants) {
            T c = (Constant)this.constants.get(name);
            if (c == null) {
               c = this.newConstant(this.nextId, name);
               this.constants.put(name, c);
               ++this.nextId;
            }

            return c;
         }
      }
   }

   public boolean exists(String name) {
      ObjectUtil.checkNotNull(name, "name");
      synchronized(this.constants) {
         return this.constants.containsKey(name);
      }
   }

   public T newInstance(String name) {
      if (name == null) {
         throw new NullPointerException("name");
      } else if (name.isEmpty()) {
         throw new IllegalArgumentException("empty name");
      } else {
         synchronized(this.constants) {
            T c = (Constant)this.constants.get(name);
            if (c == null) {
               c = this.newConstant(this.nextId, name);
               this.constants.put(name, c);
               ++this.nextId;
               return c;
            } else {
               throw new IllegalArgumentException(String.format("'%s' is already in use", name));
            }
         }
      }
   }

   protected abstract T newConstant(int var1, String var2);
}
