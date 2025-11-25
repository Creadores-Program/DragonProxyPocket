package org.spacehq.mc.protocol.data.game.values.world.block.value;

public class ChestValue implements BlockValue {
   private int viewers;

   public ChestValue(int viewers) {
      this.viewers = viewers;
   }

   public int getViewers() {
      return this.viewers;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ChestValue that = (ChestValue)o;
         return this.viewers == that.viewers;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.viewers;
   }
}
