package org.spacehq.mc.protocol.data.game;

public class Chunk {
   private ShortArray3d blocks;
   private NibbleArray3d blocklight;
   private NibbleArray3d skylight;

   public Chunk(boolean skylight) {
      this(new ShortArray3d(4096), new NibbleArray3d(4096), skylight ? new NibbleArray3d(4096) : null);
   }

   public Chunk(ShortArray3d blocks, NibbleArray3d blocklight, NibbleArray3d skylight) {
      this.blocks = blocks;
      this.blocklight = blocklight;
      this.skylight = skylight;
   }

   public ShortArray3d getBlocks() {
      return this.blocks;
   }

   public NibbleArray3d getBlockLight() {
      return this.blocklight;
   }

   public NibbleArray3d getSkyLight() {
      return this.skylight;
   }

   public boolean isEmpty() {
      short[] var1 = this.blocks.getData();
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         short block = var1[var3];
         if (block != 0) {
            return false;
         }
      }

      return true;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         Chunk chunk = (Chunk)o;
         if (!this.blocklight.equals(chunk.blocklight)) {
            return false;
         } else if (!this.blocks.equals(chunk.blocks)) {
            return false;
         } else {
            if (this.skylight != null) {
               if (!this.skylight.equals(chunk.skylight)) {
                  return false;
               }
            } else if (chunk.skylight != null) {
               return false;
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.blocks.hashCode();
      result = 31 * result + this.blocklight.hashCode();
      result = 31 * result + (this.skylight != null ? this.skylight.hashCode() : 0);
      return result;
   }
}
