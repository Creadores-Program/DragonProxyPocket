package io.netty.handler.codec.socksx.v5;

public class Socks5AddressType implements Comparable<Socks5AddressType> {
   public static final Socks5AddressType IPv4 = new Socks5AddressType(1, "IPv4");
   public static final Socks5AddressType DOMAIN = new Socks5AddressType(3, "DOMAIN");
   public static final Socks5AddressType IPv6 = new Socks5AddressType(4, "IPv6");
   private final byte byteValue;
   private final String name;
   private String text;

   public static Socks5AddressType valueOf(byte b) {
      switch(b) {
      case 1:
         return IPv4;
      case 2:
      default:
         return new Socks5AddressType(b);
      case 3:
         return DOMAIN;
      case 4:
         return IPv6;
      }
   }

   public Socks5AddressType(int byteValue) {
      this(byteValue, "UNKNOWN");
   }

   public Socks5AddressType(int byteValue, String name) {
      if (name == null) {
         throw new NullPointerException("name");
      } else {
         this.byteValue = (byte)byteValue;
         this.name = name;
      }
   }

   public byte byteValue() {
      return this.byteValue;
   }

   public int hashCode() {
      return this.byteValue;
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof Socks5AddressType)) {
         return false;
      } else {
         return this.byteValue == ((Socks5AddressType)obj).byteValue;
      }
   }

   public int compareTo(Socks5AddressType o) {
      return this.byteValue - o.byteValue;
   }

   public String toString() {
      String text = this.text;
      if (text == null) {
         this.text = text = this.name + '(' + (this.byteValue & 255) + ')';
      }

      return text;
   }
}
