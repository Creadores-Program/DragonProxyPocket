package org.codehaus.jettison.mapped;

public class SimpleConverter implements TypeConverter {
   public Object convertToJSONPrimitive(String text) {
      return text;
   }
}
