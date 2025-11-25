package org.yaml.snakeyaml.introspector;

public class MissingProperty extends Property {
   public MissingProperty(String name) {
      super(name, Object.class);
   }

   public Class<?>[] getActualTypeArguments() {
      return new Class[0];
   }

   public void set(Object object, Object value) throws Exception {
   }

   public Object get(Object object) {
      return object;
   }
}
