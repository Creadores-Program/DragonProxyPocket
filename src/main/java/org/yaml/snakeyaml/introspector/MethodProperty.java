package org.yaml.snakeyaml.introspector;

import java.beans.PropertyDescriptor;
import org.yaml.snakeyaml.error.YAMLException;

public class MethodProperty extends GenericProperty {
   private final PropertyDescriptor property;
   private final boolean readable;
   private final boolean writable;

   public MethodProperty(PropertyDescriptor property) {
      super(property.getName(), property.getPropertyType(), property.getReadMethod() == null ? null : property.getReadMethod().getGenericReturnType());
      this.property = property;
      this.readable = property.getReadMethod() != null;
      this.writable = property.getWriteMethod() != null;
   }

   public void set(Object object, Object value) throws Exception {
      this.property.getWriteMethod().invoke(object, value);
   }

   public Object get(Object object) {
      try {
         this.property.getReadMethod().setAccessible(true);
         return this.property.getReadMethod().invoke(object);
      } catch (Exception var3) {
         throw new YAMLException("Unable to find getter for property '" + this.property.getName() + "' on object " + object + ":" + var3);
      }
   }

   public boolean isWritable() {
      return this.writable;
   }

   public boolean isReadable() {
      return this.readable;
   }
}
