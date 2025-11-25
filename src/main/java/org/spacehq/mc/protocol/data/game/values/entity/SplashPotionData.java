package org.spacehq.mc.protocol.data.game.values.entity;

public class SplashPotionData implements ObjectData {
   private int potionData;

   public SplashPotionData(int potionData) {
      this.potionData = potionData;
   }

   public int getPotionData() {
      return this.potionData;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         SplashPotionData that = (SplashPotionData)o;
         return this.potionData == that.potionData;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.potionData;
   }
}
