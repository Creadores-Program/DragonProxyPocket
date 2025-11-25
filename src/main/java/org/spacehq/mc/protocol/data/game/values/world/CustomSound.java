package org.spacehq.mc.protocol.data.game.values.world;

public class CustomSound implements Sound {
   private String name;

   public CustomSound(String name) {
      this.name = name;
   }

   public String getName() {
      return this.name;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         CustomSound that = (CustomSound)o;
         return this.name.equals(that.name);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.name.hashCode();
   }
}
