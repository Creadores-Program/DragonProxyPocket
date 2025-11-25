package com.google.gson.internal.bind;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.ConstructorConstructor;
import com.google.gson.internal.Excluder;
import com.google.gson.internal.ObjectConstructor;
import com.google.gson.internal.Primitives;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ReflectiveTypeAdapterFactory implements TypeAdapterFactory {
   private final ConstructorConstructor constructorConstructor;
   private final FieldNamingStrategy fieldNamingPolicy;
   private final Excluder excluder;

   public ReflectiveTypeAdapterFactory(ConstructorConstructor constructorConstructor, FieldNamingStrategy fieldNamingPolicy, Excluder excluder) {
      this.constructorConstructor = constructorConstructor;
      this.fieldNamingPolicy = fieldNamingPolicy;
      this.excluder = excluder;
   }

   public boolean excludeField(Field f, boolean serialize) {
      return excludeField(f, serialize, this.excluder);
   }

   static boolean excludeField(Field f, boolean serialize, Excluder excluder) {
      return !excluder.excludeClass(f.getType(), serialize) && !excluder.excludeField(f, serialize);
   }

   private String getFieldName(Field f) {
      return getFieldName(this.fieldNamingPolicy, f);
   }

   static String getFieldName(FieldNamingStrategy fieldNamingPolicy, Field f) {
      SerializedName serializedName = (SerializedName)f.getAnnotation(SerializedName.class);
      return serializedName == null ? fieldNamingPolicy.translateName(f) : serializedName.value();
   }

   public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      Class<? super T> raw = type.getRawType();
      if (!Object.class.isAssignableFrom(raw)) {
         return null;
      } else {
         ObjectConstructor<T> constructor = this.constructorConstructor.get(type);
         return new ReflectiveTypeAdapterFactory.Adapter(constructor, this.getBoundFields(gson, type, raw));
      }
   }

   private ReflectiveTypeAdapterFactory.BoundField createBoundField(final Gson context, final Field field, String name, final TypeToken<?> fieldType, boolean serialize, boolean deserialize) {
      final boolean isPrimitive = Primitives.isPrimitive(fieldType.getRawType());
      return new ReflectiveTypeAdapterFactory.BoundField(name, serialize, deserialize) {
         final TypeAdapter<?> typeAdapter = ReflectiveTypeAdapterFactory.this.getFieldAdapter(context, field, fieldType);

         void write(JsonWriter writer, Object value) throws IOException, IllegalAccessException {
            Object fieldValue = field.get(value);
            TypeAdapter t = new TypeAdapterRuntimeTypeWrapper(context, this.typeAdapter, fieldType.getType());
            t.write(writer, fieldValue);
         }

         void read(JsonReader reader, Object value) throws IOException, IllegalAccessException {
            Object fieldValue = this.typeAdapter.read(reader);
            if (fieldValue != null || !isPrimitive) {
               field.set(value, fieldValue);
            }

         }

         public boolean writeField(Object value) throws IOException, IllegalAccessException {
            if (!this.serialized) {
               return false;
            } else {
               Object fieldValue = field.get(value);
               return fieldValue != value;
            }
         }
      };
   }

   private TypeAdapter<?> getFieldAdapter(Gson gson, Field field, TypeToken<?> fieldType) {
      JsonAdapter annotation = (JsonAdapter)field.getAnnotation(JsonAdapter.class);
      if (annotation != null) {
         TypeAdapter<?> adapter = JsonAdapterAnnotationTypeAdapterFactory.getTypeAdapter(this.constructorConstructor, gson, fieldType, annotation);
         if (adapter != null) {
            return adapter;
         }
      }

      return gson.getAdapter(fieldType);
   }

   private Map<String, ReflectiveTypeAdapterFactory.BoundField> getBoundFields(Gson context, TypeToken<?> type, Class<?> raw) {
      Map<String, ReflectiveTypeAdapterFactory.BoundField> result = new LinkedHashMap();
      if (raw.isInterface()) {
         return result;
      } else {
         for(Type declaredType = type.getType(); raw != Object.class; raw = type.getRawType()) {
            Field[] fields = raw.getDeclaredFields();
            Field[] arr$ = fields;
            int len$ = fields.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               Field field = arr$[i$];
               boolean serialize = this.excludeField(field, true);
               boolean deserialize = this.excludeField(field, false);
               if (serialize || deserialize) {
                  field.setAccessible(true);
                  Type fieldType = $Gson$Types.resolve(type.getType(), raw, field.getGenericType());
                  ReflectiveTypeAdapterFactory.BoundField boundField = this.createBoundField(context, field, this.getFieldName(field), TypeToken.get(fieldType), serialize, deserialize);
                  ReflectiveTypeAdapterFactory.BoundField previous = (ReflectiveTypeAdapterFactory.BoundField)result.put(boundField.name, boundField);
                  if (previous != null) {
                     throw new IllegalArgumentException(declaredType + " declares multiple JSON fields named " + previous.name);
                  }
               }
            }

            type = TypeToken.get($Gson$Types.resolve(type.getType(), raw, raw.getGenericSuperclass()));
         }

         return result;
      }
   }

   public static final class Adapter<T> extends TypeAdapter<T> {
      private final ObjectConstructor<T> constructor;
      private final Map<String, ReflectiveTypeAdapterFactory.BoundField> boundFields;

      private Adapter(ObjectConstructor<T> constructor, Map<String, ReflectiveTypeAdapterFactory.BoundField> boundFields) {
         this.constructor = constructor;
         this.boundFields = boundFields;
      }

      public T read(JsonReader in) throws IOException {
         if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
         } else {
            Object instance = this.constructor.construct();

            try {
               in.beginObject();

               while(in.hasNext()) {
                  String name = in.nextName();
                  ReflectiveTypeAdapterFactory.BoundField field = (ReflectiveTypeAdapterFactory.BoundField)this.boundFields.get(name);
                  if (field != null && field.deserialized) {
                     field.read(in, instance);
                  } else {
                     in.skipValue();
                  }
               }
            } catch (IllegalStateException var5) {
               throw new JsonSyntaxException(var5);
            } catch (IllegalAccessException var6) {
               throw new AssertionError(var6);
            }

            in.endObject();
            return instance;
         }
      }

      public void write(JsonWriter out, T value) throws IOException {
         if (value == null) {
            out.nullValue();
         } else {
            out.beginObject();

            try {
               Iterator i$ = this.boundFields.values().iterator();

               while(i$.hasNext()) {
                  ReflectiveTypeAdapterFactory.BoundField boundField = (ReflectiveTypeAdapterFactory.BoundField)i$.next();
                  if (boundField.writeField(value)) {
                     out.name(boundField.name);
                     boundField.write(out, value);
                  }
               }
            } catch (IllegalAccessException var5) {
               throw new AssertionError();
            }

            out.endObject();
         }
      }

      // $FF: synthetic method
      Adapter(ObjectConstructor x0, Map x1, Object x2) {
         this(x0, x1);
      }
   }

   abstract static class BoundField {
      final String name;
      final boolean serialized;
      final boolean deserialized;

      protected BoundField(String name, boolean serialized, boolean deserialized) {
         this.name = name;
         this.serialized = serialized;
         this.deserialized = deserialized;
      }

      abstract boolean writeField(Object var1) throws IOException, IllegalAccessException;

      abstract void write(JsonWriter var1, Object var2) throws IOException, IllegalAccessException;

      abstract void read(JsonReader var1, Object var2) throws IOException, IllegalAccessException;
   }
}
