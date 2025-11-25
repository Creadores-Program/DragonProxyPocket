package io.netty.handler.codec.dns;

public final class DnsClass implements Comparable<DnsClass> {
   public static final DnsClass IN = new DnsClass(1, "IN");
   public static final DnsClass CSNET = new DnsClass(2, "CSNET");
   public static final DnsClass CHAOS = new DnsClass(3, "CHAOS");
   public static final DnsClass HESIOD = new DnsClass(4, "HESIOD");
   public static final DnsClass NONE = new DnsClass(254, "NONE");
   public static final DnsClass ANY = new DnsClass(255, "ANY");
   private static final String EXPECTED;
   private final int intValue;
   private final String name;

   public static DnsClass valueOf(String name) {
      if (IN.name().equals(name)) {
         return IN;
      } else if (NONE.name().equals(name)) {
         return NONE;
      } else if (ANY.name().equals(name)) {
         return ANY;
      } else if (CSNET.name().equals(name)) {
         return CSNET;
      } else if (CHAOS.name().equals(name)) {
         return CHAOS;
      } else if (HESIOD.name().equals(name)) {
         return HESIOD;
      } else {
         throw new IllegalArgumentException("name: " + name + EXPECTED);
      }
   }

   public static DnsClass valueOf(int intValue) {
      switch(intValue) {
      case 1:
         return IN;
      case 2:
         return CSNET;
      case 3:
         return CHAOS;
      case 4:
         return HESIOD;
      case 254:
         return NONE;
      case 255:
         return ANY;
      default:
         return new DnsClass(intValue, "UNKNOWN");
      }
   }

   public static DnsClass valueOf(int clazz, String name) {
      return new DnsClass(clazz, name);
   }

   private DnsClass(int intValue, String name) {
      if ((intValue & '\uffff') != intValue) {
         throw new IllegalArgumentException("intValue: " + intValue + " (expected: 0 ~ 65535)");
      } else {
         this.intValue = intValue;
         this.name = name;
      }
   }

   public String name() {
      return this.name;
   }

   public int intValue() {
      return this.intValue;
   }

   public int hashCode() {
      return this.intValue;
   }

   public boolean equals(Object o) {
      return o instanceof DnsClass && ((DnsClass)o).intValue == this.intValue;
   }

   public int compareTo(DnsClass o) {
      return this.intValue() - o.intValue();
   }

   public String toString() {
      return this.name;
   }

   static {
      EXPECTED = " (expected: " + IN + '(' + IN.intValue() + "), " + CSNET + '(' + CSNET.intValue() + "), " + CHAOS + '(' + CHAOS.intValue() + "), " + HESIOD + '(' + HESIOD.intValue() + "), " + NONE + '(' + NONE.intValue() + "), " + ANY + '(' + ANY.intValue() + "))";
   }
}
