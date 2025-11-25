package org.apache.commons.lang3.text;

import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;

public abstract class StrLookup<V> {
   private static final StrLookup<String> NONE_LOOKUP = new StrLookup.MapStrLookup((Map)null);

   public static StrLookup<?> noneLookup() {
      return NONE_LOOKUP;
   }

   private static Properties copyProperties(Properties input) {
      if (input == null) {
         return null;
      } else {
         Properties output = new Properties();
         Enumeration propertyNames = input.propertyNames();

         while(propertyNames.hasMoreElements()) {
            String propertyName = (String)propertyNames.nextElement();
            output.setProperty(propertyName, input.getProperty(propertyName));
         }

         return output;
      }
   }

   public static StrLookup<String> systemPropertiesLookup() {
      Properties systemProperties = null;

      try {
         systemProperties = System.getProperties();
      } catch (SecurityException var3) {
      }

      Properties properties = copyProperties(systemProperties);
      return new StrLookup.MapStrLookup(properties);
   }

   public static <V> StrLookup<V> mapLookup(Map<String, V> map) {
      return new StrLookup.MapStrLookup(map);
   }

   protected StrLookup() {
   }

   public abstract String lookup(String var1);

   static class MapStrLookup<V> extends StrLookup<V> {
      private final Map<String, V> map;

      MapStrLookup(Map<String, V> map) {
         this.map = map;
      }

      public String lookup(String key) {
         if (this.map == null) {
            return null;
         } else {
            Object obj = this.map.get(key);
            return obj == null ? null : obj.toString();
         }
      }
   }
}
