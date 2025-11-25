package org.spacehq.mc.protocol.util;

import java.util.Arrays;
import org.spacehq.mc.protocol.data.game.Chunk;

public class ParsedChunkData {
   private Chunk[] chunks;
   private byte[] biomes;

   public ParsedChunkData(Chunk[] chunks, byte[] biomes) {
      this.chunks = chunks;
      this.biomes = biomes;
   }

   public Chunk[] getChunks() {
      return this.chunks;
   }

   public byte[] getBiomes() {
      return this.biomes;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ParsedChunkData that = (ParsedChunkData)o;
         if (!Arrays.equals(this.biomes, that.biomes)) {
            return false;
         } else {
            return Arrays.equals(this.chunks, that.chunks);
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = Arrays.hashCode(this.chunks);
      result = 31 * result + (this.biomes != null ? Arrays.hashCode(this.biomes) : 0);
      return result;
   }
}
