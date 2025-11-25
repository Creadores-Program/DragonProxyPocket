package org.spacehq.mc.protocol.data.game.values.world.map;

public class MapPlayer {
   private int centerX;
   private int centerZ;
   private int iconSize;
   private int iconRotation;

   public MapPlayer(int centerX, int centerZ, int iconSize, int iconRotation) {
      this.centerX = centerX;
      this.centerZ = centerZ;
      this.iconSize = iconSize;
      this.iconRotation = iconRotation;
   }

   public int getCenterX() {
      return this.centerX;
   }

   public int getCenterZ() {
      return this.centerZ;
   }

   public int getIconSize() {
      return this.iconSize;
   }

   public int getIconRotation() {
      return this.iconRotation;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         MapPlayer mapPlayer = (MapPlayer)o;
         if (this.centerX != mapPlayer.centerX) {
            return false;
         } else if (this.centerZ != mapPlayer.centerZ) {
            return false;
         } else if (this.iconRotation != mapPlayer.iconRotation) {
            return false;
         } else {
            return this.iconSize == mapPlayer.iconSize;
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.centerX;
      result = 31 * result + this.centerZ;
      result = 31 * result + this.iconSize;
      result = 31 * result + this.iconRotation;
      return result;
   }
}
