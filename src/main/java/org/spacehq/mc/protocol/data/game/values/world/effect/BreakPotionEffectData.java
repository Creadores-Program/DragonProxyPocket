package org.spacehq.mc.protocol.data.game.values.world.effect;

public class BreakPotionEffectData implements WorldEffectData {
   private int potionId;

   public BreakPotionEffectData(int potionId) {
      this.potionId = potionId;
   }

   public int getPotionId() {
      return this.potionId;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         BreakPotionEffectData that = (BreakPotionEffectData)o;
         return this.potionId == that.potionId;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.potionId;
   }
}
