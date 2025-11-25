package org.apache.commons.lang3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnumUtils {
   private static final String NULL_ELEMENTS_NOT_PERMITTED = "null elements not permitted";
   private static final String CANNOT_STORE_S_S_VALUES_IN_S_BITS = "Cannot store %s %s values in %s bits";
   private static final String S_DOES_NOT_SEEM_TO_BE_AN_ENUM_TYPE = "%s does not seem to be an Enum type";
   private static final String ENUM_CLASS_MUST_BE_DEFINED = "EnumClass must be defined.";

   public static <E extends Enum<E>> Map<String, E> getEnumMap(Class<E> enumClass) {
      Map<String, E> map = new LinkedHashMap();
      Enum[] arr$ = (Enum[])enumClass.getEnumConstants();
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         E e = arr$[i$];
         map.put(e.name(), e);
      }

      return map;
   }

   public static <E extends Enum<E>> List<E> getEnumList(Class<E> enumClass) {
      return new ArrayList(Arrays.asList(enumClass.getEnumConstants()));
   }

   public static <E extends Enum<E>> boolean isValidEnum(Class<E> enumClass, String enumName) {
      if (enumName == null) {
         return false;
      } else {
         try {
            Enum.valueOf(enumClass, enumName);
            return true;
         } catch (IllegalArgumentException var3) {
            return false;
         }
      }
   }

   public static <E extends Enum<E>> E getEnum(Class<E> enumClass, String enumName) {
      if (enumName == null) {
         return null;
      } else {
         try {
            return Enum.valueOf(enumClass, enumName);
         } catch (IllegalArgumentException var3) {
            return null;
         }
      }
   }

   public static <E extends Enum<E>> long generateBitVector(Class<E> enumClass, Iterable<? extends E> values) {
      checkBitVectorable(enumClass);
      Validate.notNull(values);
      long total = 0L;

      Enum constant;
      for(Iterator i$ = values.iterator(); i$.hasNext(); total |= (long)(1 << constant.ordinal())) {
         constant = (Enum)i$.next();
         Validate.isTrue(constant != null, "null elements not permitted");
      }

      return total;
   }

   public static <E extends Enum<E>> long[] generateBitVectors(Class<E> enumClass, Iterable<? extends E> values) {
      asEnum(enumClass);
      Validate.notNull(values);
      EnumSet<E> condensed = EnumSet.noneOf(enumClass);
      Iterator i$ = values.iterator();

      while(i$.hasNext()) {
         E constant = (Enum)i$.next();
         Validate.isTrue(constant != null, "null elements not permitted");
         condensed.add(constant);
      }

      long[] result = new long[(((Enum[])enumClass.getEnumConstants()).length - 1) / 64 + 1];

      int var10001;
      Enum value;
      for(Iterator i$ = condensed.iterator(); i$.hasNext(); result[var10001] |= (long)(1 << value.ordinal() % 64)) {
         value = (Enum)i$.next();
         var10001 = value.ordinal() / 64;
      }

      ArrayUtils.reverse(result);
      return result;
   }

   public static <E extends Enum<E>> long generateBitVector(Class<E> enumClass, E... values) {
      Validate.noNullElements((Object[])values);
      return generateBitVector(enumClass, (Iterable)Arrays.asList(values));
   }

   public static <E extends Enum<E>> long[] generateBitVectors(Class<E> enumClass, E... values) {
      asEnum(enumClass);
      Validate.noNullElements((Object[])values);
      EnumSet<E> condensed = EnumSet.noneOf(enumClass);
      Collections.addAll(condensed, values);
      long[] result = new long[(((Enum[])enumClass.getEnumConstants()).length - 1) / 64 + 1];

      int var10001;
      Enum value;
      for(Iterator i$ = condensed.iterator(); i$.hasNext(); result[var10001] |= (long)(1 << value.ordinal() % 64)) {
         value = (Enum)i$.next();
         var10001 = value.ordinal() / 64;
      }

      ArrayUtils.reverse(result);
      return result;
   }

   public static <E extends Enum<E>> EnumSet<E> processBitVector(Class<E> enumClass, long value) {
      checkBitVectorable(enumClass).getEnumConstants();
      return processBitVectors(enumClass, value);
   }

   public static <E extends Enum<E>> EnumSet<E> processBitVectors(Class<E> enumClass, long... values) {
      EnumSet<E> results = EnumSet.noneOf(asEnum(enumClass));
      long[] lvalues = ArrayUtils.clone((long[])Validate.notNull(values));
      ArrayUtils.reverse(lvalues);
      Enum[] arr$ = (Enum[])enumClass.getEnumConstants();
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         E constant = arr$[i$];
         int block = constant.ordinal() / 64;
         if (block < lvalues.length && (lvalues[block] & (long)(1 << constant.ordinal() % 64)) != 0L) {
            results.add(constant);
         }
      }

      return results;
   }

   private static <E extends Enum<E>> Class<E> checkBitVectorable(Class<E> enumClass) {
      E[] constants = (Enum[])asEnum(enumClass).getEnumConstants();
      Validate.isTrue(constants.length <= 64, "Cannot store %s %s values in %s bits", constants.length, enumClass.getSimpleName(), 64);
      return enumClass;
   }

   private static <E extends Enum<E>> Class<E> asEnum(Class<E> enumClass) {
      Validate.notNull(enumClass, "EnumClass must be defined.");
      Validate.isTrue(enumClass.isEnum(), "%s does not seem to be an Enum type", enumClass);
      return enumClass;
   }
}
