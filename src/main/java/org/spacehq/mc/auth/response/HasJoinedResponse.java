package org.spacehq.mc.auth.response;

import java.util.UUID;
import org.spacehq.mc.auth.properties.PropertyMap;

public class HasJoinedResponse extends Response {
   private UUID id;
   private PropertyMap properties;

   public UUID getId() {
      return this.id;
   }

   public PropertyMap getProperties() {
      return this.properties;
   }
}
