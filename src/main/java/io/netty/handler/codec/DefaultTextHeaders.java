package io.netty.handler.codec;

import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import java.text.ParseException;
import java.util.Comparator;
import java.util.Iterator;

public class DefaultTextHeaders extends DefaultConvertibleHeaders<CharSequence, String> implements TextHeaders {
   private static final DefaultHeaders.HashCodeGenerator<CharSequence> CHARSEQUECE_CASE_INSENSITIVE_HASH_CODE_GENERATOR = new DefaultHeaders.HashCodeGenerator<CharSequence>() {
      public int generateHashCode(CharSequence name) {
         return AsciiString.caseInsensitiveHashCode(name);
      }
   };
   private static final DefaultHeaders.HashCodeGenerator<CharSequence> CHARSEQUECE_CASE_SENSITIVE_HASH_CODE_GENERATOR = new DefaultHeaders.HashCodeGenerator<CharSequence>() {
      public int generateHashCode(CharSequence name) {
         return name.hashCode();
      }
   };
   private static final Headers.ValueConverter<CharSequence> CHARSEQUENCE_FROM_OBJECT_CONVERTER = new DefaultTextHeaders.DefaultTextValueTypeConverter();
   private static final ConvertibleHeaders.TypeConverter<CharSequence, String> CHARSEQUENCE_TO_STRING_CONVERTER = new ConvertibleHeaders.TypeConverter<CharSequence, String>() {
      public String toConvertedType(CharSequence value) {
         return value.toString();
      }

      public CharSequence toUnconvertedType(String value) {
         return value;
      }
   };
   private static final DefaultHeaders.NameConverter<CharSequence> CHARSEQUENCE_IDENTITY_CONVERTER = new DefaultHeaders.IdentityNameConverter();
   private static final int DEFAULT_VALUE_SIZE = 10;
   private final DefaultTextHeaders.ValuesComposer valuesComposer;

   public DefaultTextHeaders() {
      this(true);
   }

   public DefaultTextHeaders(boolean ignoreCase) {
      this(ignoreCase, CHARSEQUENCE_FROM_OBJECT_CONVERTER, CHARSEQUENCE_IDENTITY_CONVERTER, false);
   }

   public DefaultTextHeaders(boolean ignoreCase, boolean singleHeaderFields) {
      this(ignoreCase, CHARSEQUENCE_FROM_OBJECT_CONVERTER, CHARSEQUENCE_IDENTITY_CONVERTER, singleHeaderFields);
   }

   protected DefaultTextHeaders(boolean ignoreCase, Headers.ValueConverter<CharSequence> valueConverter, DefaultHeaders.NameConverter<CharSequence> nameConverter) {
      this(ignoreCase, valueConverter, nameConverter, false);
   }

   public DefaultTextHeaders(boolean ignoreCase, Headers.ValueConverter<CharSequence> valueConverter, DefaultHeaders.NameConverter<CharSequence> nameConverter, boolean singleHeaderFields) {
      super(comparator(ignoreCase), comparator(ignoreCase), ignoreCase ? CHARSEQUECE_CASE_INSENSITIVE_HASH_CODE_GENERATOR : CHARSEQUECE_CASE_SENSITIVE_HASH_CODE_GENERATOR, valueConverter, CHARSEQUENCE_TO_STRING_CONVERTER, nameConverter);
      this.valuesComposer = (DefaultTextHeaders.ValuesComposer)(singleHeaderFields ? new DefaultTextHeaders.SingleHeaderValuesComposer() : new DefaultTextHeaders.MultipleFieldsValueComposer());
   }

   public boolean contains(CharSequence name, CharSequence value, boolean ignoreCase) {
      return this.contains(name, value, comparator(ignoreCase));
   }

   public boolean containsObject(CharSequence name, Object value, boolean ignoreCase) {
      return this.containsObject(name, value, comparator(ignoreCase));
   }

   public TextHeaders add(CharSequence name, CharSequence value) {
      return this.valuesComposer.add(name, value);
   }

   public TextHeaders add(CharSequence name, Iterable<? extends CharSequence> values) {
      return this.valuesComposer.add(name, values);
   }

   public TextHeaders add(CharSequence name, CharSequence... values) {
      return this.valuesComposer.add(name, values);
   }

   public TextHeaders addObject(CharSequence name, Object value) {
      return this.valuesComposer.addObject(name, value);
   }

   public TextHeaders addObject(CharSequence name, Iterable<?> values) {
      return this.valuesComposer.addObject(name, values);
   }

   public TextHeaders addObject(CharSequence name, Object... values) {
      return this.valuesComposer.addObject(name, values);
   }

   public TextHeaders addBoolean(CharSequence name, boolean value) {
      super.addBoolean(name, value);
      return this;
   }

   public TextHeaders addChar(CharSequence name, char value) {
      super.addChar(name, value);
      return this;
   }

   public TextHeaders addByte(CharSequence name, byte value) {
      super.addByte(name, value);
      return this;
   }

   public TextHeaders addShort(CharSequence name, short value) {
      super.addShort(name, value);
      return this;
   }

   public TextHeaders addInt(CharSequence name, int value) {
      super.addInt(name, value);
      return this;
   }

   public TextHeaders addLong(CharSequence name, long value) {
      super.addLong(name, value);
      return this;
   }

   public TextHeaders addFloat(CharSequence name, float value) {
      super.addFloat(name, value);
      return this;
   }

   public TextHeaders addDouble(CharSequence name, double value) {
      super.addDouble(name, value);
      return this;
   }

   public TextHeaders addTimeMillis(CharSequence name, long value) {
      super.addTimeMillis(name, value);
      return this;
   }

   public TextHeaders add(TextHeaders headers) {
      super.add(headers);
      return this;
   }

   public TextHeaders set(CharSequence name, CharSequence value) {
      super.set(name, value);
      return this;
   }

   public TextHeaders set(CharSequence name, Iterable<? extends CharSequence> values) {
      return this.valuesComposer.set(name, values);
   }

   public TextHeaders set(CharSequence name, CharSequence... values) {
      return this.valuesComposer.set(name, values);
   }

   public TextHeaders setObject(CharSequence name, Object value) {
      super.setObject(name, value);
      return this;
   }

   public TextHeaders setObject(CharSequence name, Iterable<?> values) {
      return this.valuesComposer.setObject(name, values);
   }

   public TextHeaders setObject(CharSequence name, Object... values) {
      return this.valuesComposer.setObject(name, values);
   }

   public TextHeaders setBoolean(CharSequence name, boolean value) {
      super.setBoolean(name, value);
      return this;
   }

   public TextHeaders setChar(CharSequence name, char value) {
      super.setChar(name, value);
      return this;
   }

   public TextHeaders setByte(CharSequence name, byte value) {
      super.setByte(name, value);
      return this;
   }

   public TextHeaders setShort(CharSequence name, short value) {
      super.setShort(name, value);
      return this;
   }

   public TextHeaders setInt(CharSequence name, int value) {
      super.setInt(name, value);
      return this;
   }

   public TextHeaders setLong(CharSequence name, long value) {
      super.setLong(name, value);
      return this;
   }

   public TextHeaders setFloat(CharSequence name, float value) {
      super.setFloat(name, value);
      return this;
   }

   public TextHeaders setDouble(CharSequence name, double value) {
      super.setDouble(name, value);
      return this;
   }

   public TextHeaders setTimeMillis(CharSequence name, long value) {
      super.setTimeMillis(name, value);
      return this;
   }

   public TextHeaders set(TextHeaders headers) {
      super.set(headers);
      return this;
   }

   public TextHeaders setAll(TextHeaders headers) {
      super.setAll(headers);
      return this;
   }

   public TextHeaders clear() {
      super.clear();
      return this;
   }

   private static Comparator<CharSequence> comparator(boolean ignoreCase) {
      return ignoreCase ? AsciiString.CHARSEQUENCE_CASE_INSENSITIVE_ORDER : AsciiString.CHARSEQUENCE_CASE_SENSITIVE_ORDER;
   }

   private interface CsvValueEscaper<T> {
      CharSequence escape(T var1);
   }

   private final class SingleHeaderValuesComposer implements DefaultTextHeaders.ValuesComposer {
      private final Headers.ValueConverter<CharSequence> valueConverter;
      private DefaultTextHeaders.CsvValueEscaper<Object> objectEscaper;
      private DefaultTextHeaders.CsvValueEscaper<CharSequence> charSequenceEscaper;

      private SingleHeaderValuesComposer() {
         this.valueConverter = DefaultTextHeaders.this.valueConverter();
      }

      private DefaultTextHeaders.CsvValueEscaper<Object> objectEscaper() {
         if (this.objectEscaper == null) {
            this.objectEscaper = new DefaultTextHeaders.CsvValueEscaper<Object>() {
               public CharSequence escape(Object value) {
                  return StringUtil.escapeCsv((CharSequence)SingleHeaderValuesComposer.this.valueConverter.convertObject(value));
               }
            };
         }

         return this.objectEscaper;
      }

      private DefaultTextHeaders.CsvValueEscaper<CharSequence> charSequenceEscaper() {
         if (this.charSequenceEscaper == null) {
            this.charSequenceEscaper = new DefaultTextHeaders.CsvValueEscaper<CharSequence>() {
               public CharSequence escape(CharSequence value) {
                  return StringUtil.escapeCsv(value);
               }
            };
         }

         return this.charSequenceEscaper;
      }

      public TextHeaders add(CharSequence name, CharSequence value) {
         return this.addEscapedValue(name, StringUtil.escapeCsv(value));
      }

      public TextHeaders add(CharSequence name, CharSequence... values) {
         return this.addEscapedValue(name, this.commaSeparate(this.charSequenceEscaper(), (Object[])values));
      }

      public TextHeaders add(CharSequence name, Iterable<? extends CharSequence> values) {
         return this.addEscapedValue(name, this.commaSeparate(this.charSequenceEscaper(), values));
      }

      public TextHeaders addObject(CharSequence name, Iterable<?> values) {
         return this.addEscapedValue(name, this.commaSeparate(this.objectEscaper(), values));
      }

      public TextHeaders addObject(CharSequence name, Object... values) {
         return this.addEscapedValue(name, this.commaSeparate(this.objectEscaper(), values));
      }

      public TextHeaders set(CharSequence name, CharSequence... values) {
         DefaultTextHeaders.super.set(name, (Object)this.commaSeparate(this.charSequenceEscaper(), (Object[])values));
         return DefaultTextHeaders.this;
      }

      public TextHeaders set(CharSequence name, Iterable<? extends CharSequence> values) {
         DefaultTextHeaders.super.set(name, (Object)this.commaSeparate(this.charSequenceEscaper(), values));
         return DefaultTextHeaders.this;
      }

      public TextHeaders setObject(CharSequence name, Object... values) {
         DefaultTextHeaders.super.set(name, (Object)this.commaSeparate(this.objectEscaper(), values));
         return DefaultTextHeaders.this;
      }

      public TextHeaders setObject(CharSequence name, Iterable<?> values) {
         DefaultTextHeaders.super.set(name, (Object)this.commaSeparate(this.objectEscaper(), values));
         return DefaultTextHeaders.this;
      }

      private TextHeaders addEscapedValue(CharSequence name, CharSequence escapedValue) {
         CharSequence currentValue = (CharSequence)DefaultTextHeaders.super.get(name);
         if (currentValue == null) {
            DefaultTextHeaders.super.add(name, (Object)escapedValue);
         } else {
            DefaultTextHeaders.super.set(name, (Object)this.commaSeparateEscapedValues(currentValue, escapedValue));
         }

         return DefaultTextHeaders.this;
      }

      private <T> CharSequence commaSeparate(DefaultTextHeaders.CsvValueEscaper<T> escaper, T... values) {
         StringBuilder sb = new StringBuilder(values.length * 10);
         if (values.length > 0) {
            int end = values.length - 1;

            for(int i = 0; i < end; ++i) {
               sb.append(escaper.escape(values[i])).append(',');
            }

            sb.append(escaper.escape(values[end]));
         }

         return sb;
      }

      private <T> CharSequence commaSeparate(DefaultTextHeaders.CsvValueEscaper<T> escaper, Iterable<? extends T> values) {
         StringBuilder sb = new StringBuilder();
         Iterator<? extends T> iterator = values.iterator();
         if (iterator.hasNext()) {
            Object next;
            for(next = iterator.next(); iterator.hasNext(); next = iterator.next()) {
               sb.append(escaper.escape(next)).append(',');
            }

            sb.append(escaper.escape(next));
         }

         return sb;
      }

      private CharSequence commaSeparateEscapedValues(CharSequence currentValue, CharSequence value) {
         return (new StringBuilder(currentValue.length() + 1 + value.length())).append(currentValue).append(',').append(value);
      }

      // $FF: synthetic method
      SingleHeaderValuesComposer(Object x1) {
         this();
      }
   }

   private final class MultipleFieldsValueComposer implements DefaultTextHeaders.ValuesComposer {
      private MultipleFieldsValueComposer() {
      }

      public TextHeaders add(CharSequence name, CharSequence value) {
         DefaultTextHeaders.super.add(name, (Object)value);
         return DefaultTextHeaders.this;
      }

      public TextHeaders add(CharSequence name, CharSequence... values) {
         DefaultTextHeaders.super.add(name, (Object[])values);
         return DefaultTextHeaders.this;
      }

      public TextHeaders add(CharSequence name, Iterable<? extends CharSequence> values) {
         DefaultTextHeaders.super.add(name, (Iterable)values);
         return DefaultTextHeaders.this;
      }

      public TextHeaders addObject(CharSequence name, Iterable<?> values) {
         DefaultTextHeaders.super.addObject(name, (Iterable)values);
         return DefaultTextHeaders.this;
      }

      public TextHeaders addObject(CharSequence name, Object... values) {
         DefaultTextHeaders.super.addObject(name, (Object[])values);
         return DefaultTextHeaders.this;
      }

      public TextHeaders set(CharSequence name, CharSequence... values) {
         DefaultTextHeaders.super.set(name, (Object[])values);
         return DefaultTextHeaders.this;
      }

      public TextHeaders set(CharSequence name, Iterable<? extends CharSequence> values) {
         DefaultTextHeaders.super.set(name, (Iterable)values);
         return DefaultTextHeaders.this;
      }

      public TextHeaders setObject(CharSequence name, Object... values) {
         DefaultTextHeaders.super.setObject(name, (Object[])values);
         return DefaultTextHeaders.this;
      }

      public TextHeaders setObject(CharSequence name, Iterable<?> values) {
         DefaultTextHeaders.super.setObject(name, (Iterable)values);
         return DefaultTextHeaders.this;
      }

      // $FF: synthetic method
      MultipleFieldsValueComposer(Object x1) {
         this();
      }
   }

   private interface ValuesComposer {
      TextHeaders add(CharSequence var1, CharSequence var2);

      TextHeaders add(CharSequence var1, CharSequence... var2);

      TextHeaders add(CharSequence var1, Iterable<? extends CharSequence> var2);

      TextHeaders addObject(CharSequence var1, Iterable<?> var2);

      TextHeaders addObject(CharSequence var1, Object... var2);

      TextHeaders set(CharSequence var1, CharSequence... var2);

      TextHeaders set(CharSequence var1, Iterable<? extends CharSequence> var2);

      TextHeaders setObject(CharSequence var1, Object... var2);

      TextHeaders setObject(CharSequence var1, Iterable<?> var2);
   }

   public static class DefaultTextValueTypeConverter implements Headers.ValueConverter<CharSequence> {
      public CharSequence convertObject(Object value) {
         return (CharSequence)(value instanceof CharSequence ? (CharSequence)value : value.toString());
      }

      public CharSequence convertInt(int value) {
         return String.valueOf(value);
      }

      public CharSequence convertLong(long value) {
         return String.valueOf(value);
      }

      public CharSequence convertDouble(double value) {
         return String.valueOf(value);
      }

      public CharSequence convertChar(char value) {
         return String.valueOf(value);
      }

      public CharSequence convertBoolean(boolean value) {
         return String.valueOf(value);
      }

      public CharSequence convertFloat(float value) {
         return String.valueOf(value);
      }

      public boolean convertToBoolean(CharSequence value) {
         return Boolean.parseBoolean(value.toString());
      }

      public CharSequence convertByte(byte value) {
         return String.valueOf(value);
      }

      public byte convertToByte(CharSequence value) {
         return Byte.valueOf(value.toString());
      }

      public char convertToChar(CharSequence value) {
         return value.charAt(0);
      }

      public CharSequence convertShort(short value) {
         return String.valueOf(value);
      }

      public short convertToShort(CharSequence value) {
         return Short.valueOf(value.toString());
      }

      public int convertToInt(CharSequence value) {
         return Integer.parseInt(value.toString());
      }

      public long convertToLong(CharSequence value) {
         return Long.parseLong(value.toString());
      }

      public AsciiString convertTimeMillis(long value) {
         return new AsciiString(String.valueOf(value));
      }

      public long convertToTimeMillis(CharSequence value) {
         try {
            return DefaultHeaders.HeaderDateFormat.get().parse(value.toString());
         } catch (ParseException var3) {
            PlatformDependent.throwException(var3);
            return 0L;
         }
      }

      public float convertToFloat(CharSequence value) {
         return Float.valueOf(value.toString());
      }

      public double convertToDouble(CharSequence value) {
         return Double.valueOf(value.toString());
      }
   }
}
