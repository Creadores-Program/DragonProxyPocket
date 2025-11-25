package org.spacehq.mc.auth.response;

import java.util.UUID;
import org.spacehq.mc.auth.properties.PropertyMap;

public class MinecraftProfilePropertiesResponse extends Response {
   private UUID id;
   private String name;
   private PropertyMap properties;

   public UUID getId() {
      return this.id;
   }

   public String getName() {
      return this.name;
   }

   public PropertyMap getProperties() {
      return this.properties;
   }
}
