package org.spacehq.mc.protocol.data.game.values.world.block.value;

public class GenericBlockValue implements BlockValue {
   private int value;

   public GenericBlockValue(int value) {
      this.value = value;
   }

   public int getValue() {
      return this.value;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         GenericBlockValue that = (GenericBlockValue)o;
         return this.value == that.value;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.value;
   }
}
