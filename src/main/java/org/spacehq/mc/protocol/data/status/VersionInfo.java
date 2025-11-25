package org.spacehq.mc.protocol.data.status;

public class VersionInfo {
   private String name;
   private int protocol;

   public VersionInfo(String name, int protocol) {
      this.name = name;
      this.protocol = protocol;
   }

   public String getVersionName() {
      return this.name;
   }

   public int getProtocolVersion() {
      return this.protocol;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         VersionInfo that = (VersionInfo)o;
         if (this.protocol != that.protocol) {
            return false;
         } else {
            return this.name.equals(that.name);
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.name.hashCode();
      result = 31 * result + this.protocol;
      return result;
   }
}
