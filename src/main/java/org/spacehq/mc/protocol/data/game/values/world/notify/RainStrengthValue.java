package org.spacehq.mc.protocol.data.game.values.world.notify;

public class RainStrengthValue implements ClientNotificationValue {
   private float strength;

   public RainStrengthValue(float strength) {
      if (strength > 1.0F) {
         strength = 1.0F;
      }

      if (strength < 0.0F) {
         strength = 0.0F;
      }

      this.strength = strength;
   }

   public float getStrength() {
      return this.strength;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         RainStrengthValue that = (RainStrengthValue)o;
         return Float.compare(that.strength, this.strength) == 0;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.strength != 0.0F ? Float.floatToIntBits(this.strength) : 0;
   }
}
