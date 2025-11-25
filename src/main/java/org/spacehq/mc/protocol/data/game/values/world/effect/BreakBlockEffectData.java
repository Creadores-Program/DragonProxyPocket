package org.spacehq.mc.protocol.data.game.values.world.effect;

public class BreakBlockEffectData implements WorldEffectData {
   private int blockId;

   public BreakBlockEffectData(int blockId) {
      this.blockId = blockId;
   }

   public int getBlockId() {
      return this.blockId;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         BreakBlockEffectData that = (BreakBlockEffectData)o;
         return this.blockId == that.blockId;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.blockId;
   }
}
