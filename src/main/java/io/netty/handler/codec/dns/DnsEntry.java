package io.netty.handler.codec.dns;

import io.netty.util.internal.StringUtil;

public class DnsEntry {
   private final String name;
   private final DnsType type;
   private final DnsClass dnsClass;

   DnsEntry(String name, DnsType type, DnsClass dnsClass) {
      if (name == null) {
         throw new NullPointerException("name");
      } else if (type == null) {
         throw new NullPointerException("type");
      } else if (dnsClass == null) {
         throw new NullPointerException("dnsClass");
      } else {
         this.name = name;
         this.type = type;
         this.dnsClass = dnsClass;
      }
   }

   public String name() {
      return this.name;
   }

   public DnsType type() {
      return this.type;
   }

   public DnsClass dnsClass() {
      return this.dnsClass;
   }

   public int hashCode() {
      return (this.name.hashCode() * 31 + this.type.hashCode()) * 31 + this.dnsClass.hashCode();
   }

   public String toString() {
      return (new StringBuilder(128)).append(StringUtil.simpleClassName((Object)this)).append("(name: ").append(this.name).append(", type: ").append(this.type).append(", class: ").append(this.dnsClass).append(')').toString();
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof DnsEntry)) {
         return false;
      } else {
         DnsEntry that = (DnsEntry)o;
         return this.type().intValue() == that.type().intValue() && this.dnsClass().intValue() == that.dnsClass().intValue() && this.name().equals(that.name());
      }
   }
}
