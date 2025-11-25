package org.spacehq.mc.protocol.data.game.values.entity;

public class FallingBlockData implements ObjectData {
   private int id;
   private int metadata;

   public FallingBlockData(int id, int metadata) {
      this.id = id;
      this.metadata = metadata;
   }

   public int getId() {
      return this.id;
   }

   public int getMetadata() {
      return this.metadata;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         FallingBlockData that = (FallingBlockData)o;
         if (this.id != that.id) {
            return false;
         } else {
            return this.metadata == that.metadata;
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.id;
      result = 31 * result + this.metadata;
      return result;
   }
}
