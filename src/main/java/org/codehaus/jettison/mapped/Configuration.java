package org.codehaus.jettison.mapped;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Configuration {
   private static final String JETTISON_TYPE_CONVERTER_CLASS_KEY = "jettison.mapped.typeconverter.class";
   private static final Configuration.ConverterFactory converterFactory;
   private Map xmlToJsonNamespaces;
   private List attributesAsElements;
   private List ignoredElements;
   private boolean supressAtAttributes;
   private String attributeKey = "@";
   private boolean implicitCollections = false;
   private boolean ignoreNamespaces;
   private Set primitiveArrayKeys;
   private TypeConverter typeConverter;

   public Configuration() {
      this.primitiveArrayKeys = Collections.EMPTY_SET;
      this.typeConverter = converterFactory.newDefaultConverterInstance();
      this.xmlToJsonNamespaces = new HashMap();
   }

   public Configuration(Map xmlToJsonNamespaces) {
      this.primitiveArrayKeys = Collections.EMPTY_SET;
      this.typeConverter = converterFactory.newDefaultConverterInstance();
      this.xmlToJsonNamespaces = xmlToJsonNamespaces;
   }

   public Configuration(Map xmlToJsonNamespaces, List attributesAsElements, List ignoredElements) {
      this.primitiveArrayKeys = Collections.EMPTY_SET;
      this.typeConverter = converterFactory.newDefaultConverterInstance();
      this.xmlToJsonNamespaces = xmlToJsonNamespaces;
      this.attributesAsElements = attributesAsElements;
      this.ignoredElements = ignoredElements;
   }

   public boolean isIgnoreNamespaces() {
      return this.ignoreNamespaces;
   }

   public void setIgnoreNamespaces(boolean ignoreNamespaces) {
      this.ignoreNamespaces = ignoreNamespaces;
   }

   public List getAttributesAsElements() {
      return this.attributesAsElements;
   }

   public void setAttributesAsElements(List attributesAsElements) {
      this.attributesAsElements = attributesAsElements;
   }

   public List getIgnoredElements() {
      return this.ignoredElements;
   }

   public void setIgnoredElements(List ignoredElements) {
      this.ignoredElements = ignoredElements;
   }

   public Map getXmlToJsonNamespaces() {
      return this.xmlToJsonNamespaces;
   }

   public void setXmlToJsonNamespaces(Map xmlToJsonNamespaces) {
      this.xmlToJsonNamespaces = xmlToJsonNamespaces;
   }

   public TypeConverter getTypeConverter() {
      return this.typeConverter;
   }

   public void setTypeConverter(TypeConverter typeConverter) {
      this.typeConverter = typeConverter;
   }

   public boolean isSupressAtAttributes() {
      return this.supressAtAttributes;
   }

   public void setSupressAtAttributes(boolean supressAtAttributes) {
      this.supressAtAttributes = supressAtAttributes;
   }

   public String getAttributeKey() {
      return this.attributeKey;
   }

   public void setAttributeKey(String attributeKey) {
      this.attributeKey = attributeKey;
   }

   public boolean isImplicitCollections() {
      return this.implicitCollections;
   }

   public void setImplicitCollections(boolean implicitCollections) {
      this.implicitCollections = implicitCollections;
   }

   static TypeConverter newDefaultConverterInstance() {
      return converterFactory.newDefaultConverterInstance();
   }

   public Set getPrimitiveArrayKeys() {
      return this.primitiveArrayKeys;
   }

   public void setPrimitiveArrayKeys(Set primitiveArrayKeys) {
      this.primitiveArrayKeys = primitiveArrayKeys;
   }

   static {
      Configuration.ConverterFactory cf = null;
      String userSpecifiedClass = System.getProperty("jettison.mapped.typeconverter.class");
      if (userSpecifiedClass != null && userSpecifiedClass.length() > 0) {
         try {
            final Class<? extends TypeConverter> tc = Class.forName(userSpecifiedClass).asSubclass(TypeConverter.class);
            tc.newInstance();
            cf = new Configuration.ConverterFactory() {
               public TypeConverter newDefaultConverterInstance() {
                  try {
                     return (TypeConverter)tc.newInstance();
                  } catch (Exception var2) {
                     throw new ExceptionInInitializerError(var2);
                  }
               }
            };
         } catch (Exception var3) {
            throw new ExceptionInInitializerError(var3);
         }
      }

      if (cf == null) {
         cf = new Configuration.ConverterFactory();
      }

      converterFactory = cf;
   }

   private static class ConverterFactory {
      private ConverterFactory() {
      }

      TypeConverter newDefaultConverterInstance() {
         return new DefaultConverter();
      }

      // $FF: synthetic method
      ConverterFactory(Object x0) {
         this();
      }
   }
}
