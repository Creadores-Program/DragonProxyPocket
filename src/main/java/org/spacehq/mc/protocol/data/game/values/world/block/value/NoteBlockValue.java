package org.spacehq.mc.protocol.data.game.values.world.block.value;

public class NoteBlockValue implements BlockValue {
   private int pitch;

   public NoteBlockValue(int pitch) {
      if (pitch >= 0 && pitch <= 24) {
         this.pitch = pitch;
      } else {
         throw new IllegalArgumentException("Pitch must be between 0 and 24.");
      }
   }

   public int getPitch() {
      return this.pitch;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         NoteBlockValue that = (NoteBlockValue)o;
         return this.pitch == that.pitch;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.pitch;
   }
}
