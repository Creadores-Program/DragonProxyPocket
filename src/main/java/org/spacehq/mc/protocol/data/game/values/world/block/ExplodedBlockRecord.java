package org.spacehq.mc.protocol.data.game.values.world.block;

public class ExplodedBlockRecord {
   private int x;
   private int y;
   private int z;

   public ExplodedBlockRecord(int x, int y, int z) {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public int getZ() {
      return this.z;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ExplodedBlockRecord that = (ExplodedBlockRecord)o;
         if (this.x != that.x) {
            return false;
         } else if (this.y != that.y) {
            return false;
         } else {
            return this.z == that.z;
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.x;
      result = 31 * result + this.y;
      result = 31 * result + this.z;
      return result;
   }
}
