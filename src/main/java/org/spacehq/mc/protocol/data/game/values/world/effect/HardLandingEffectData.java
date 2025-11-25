package org.spacehq.mc.protocol.data.game.values.world.effect;

public class HardLandingEffectData implements WorldEffectData {
   private int damagingDistance;

   public HardLandingEffectData(int damagingDistance) {
      this.damagingDistance = damagingDistance;
   }

   public int getDamagingDistance() {
      return this.damagingDistance;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         HardLandingEffectData that = (HardLandingEffectData)o;
         return this.damagingDistance == that.damagingDistance;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.damagingDistance;
   }
}
