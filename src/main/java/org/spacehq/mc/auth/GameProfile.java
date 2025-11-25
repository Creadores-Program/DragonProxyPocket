package org.spacehq.mc.auth;

import java.util.UUID;
import org.spacehq.mc.auth.properties.PropertyMap;

public class GameProfile {
   private UUID id;
   private String name;
   private PropertyMap properties;
   private boolean legacy;

   public GameProfile(String id, String name) {
      this(id != null && !id.equals("") ? UUID.fromString(id) : null, name);
   }

   public GameProfile(UUID id, String name) {
      this.properties = new PropertyMap();
      if (id != null || name != null && !name.equals("")) {
         this.id = id;
         this.name = name;
      } else {
         throw new IllegalArgumentException("Name and ID cannot both be blank");
      }
   }

   public UUID getId() {
      return this.id;
   }

   public String getIdAsString() {
      return this.id != null ? this.id.toString() : "";
   }

   public String getName() {
      return this.name;
   }

   public PropertyMap getProperties() {
      return this.properties;
   }

   public boolean isLegacy() {
      return this.legacy;
   }

   public boolean isComplete() {
      return this.id != null && this.name != null && !this.name.equals("");
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         boolean var10000;
         label44: {
            label30: {
               GameProfile that = (GameProfile)o;
               if (this.id != null) {
                  if (!this.id.equals(that.id)) {
                     break label30;
                  }
               } else if (that.id != null) {
                  break label30;
               }

               if (this.name != null) {
                  if (this.name.equals(that.name)) {
                     break label44;
                  }
               } else if (that.name == null) {
                  break label44;
               }
            }

            var10000 = false;
            return var10000;
         }

         var10000 = true;
         return var10000;
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.id != null ? this.id.hashCode() : 0;
      result = 31 * result + (this.name != null ? this.name.hashCode() : 0);
      return result;
   }

   public String toString() {
      return "GameProfile{id=" + this.id + ", name=" + this.name + ", properties=" + this.properties + ", legacy=" + this.legacy + "}";
   }
}
