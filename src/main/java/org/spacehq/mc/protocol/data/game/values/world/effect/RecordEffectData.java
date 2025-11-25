package org.spacehq.mc.protocol.data.game.values.world.effect;

public class RecordEffectData implements WorldEffectData {
   private int recordId;

   public RecordEffectData(int recordId) {
      this.recordId = recordId;
   }

   public int getRecordId() {
      return this.recordId;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         RecordEffectData that = (RecordEffectData)o;
         return this.recordId == that.recordId;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.recordId;
   }
}
