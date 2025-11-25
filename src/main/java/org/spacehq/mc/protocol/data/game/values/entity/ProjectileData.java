package org.spacehq.mc.protocol.data.game.values.entity;

public class ProjectileData implements ObjectData {
   private int ownerId;

   public ProjectileData(int ownerId) {
      this.ownerId = ownerId;
   }

   public int getOwnerId() {
      return this.ownerId;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ProjectileData that = (ProjectileData)o;
         return this.ownerId == that.ownerId;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.ownerId;
   }
}
