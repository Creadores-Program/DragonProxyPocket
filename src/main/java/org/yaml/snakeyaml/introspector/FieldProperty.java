package org.yaml.snakeyaml.introspector;

import java.lang.reflect.Field;
import org.yaml.snakeyaml.error.YAMLException;

public class FieldProperty extends GenericProperty {
   private final Field field;

   public FieldProperty(Field field) {
      super(field.getName(), field.getType(), field.getGenericType());
      this.field = field;
      field.setAccessible(true);
   }

   public void set(Object object, Object value) throws Exception {
      this.field.set(object, value);
   }

   public Object get(Object object) {
      try {
         return this.field.get(object);
      } catch (Exception var3) {
         throw new YAMLException("Unable to access field " + this.field.getName() + " on object " + object + " : " + var3);
      }
   }
}
