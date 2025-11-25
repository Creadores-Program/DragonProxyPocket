package org.spacehq.mc.protocol.util;

public class NetworkChunkData {
   private int mask;
   private boolean fullChunk;
   private boolean sky;
   private byte[] data;

   public NetworkChunkData(int mask, boolean fullChunk, boolean sky, byte[] data) {
      this.mask = mask;
      this.fullChunk = fullChunk;
      this.sky = sky;
      this.data = data;
   }

   public int getMask() {
      return this.mask;
   }

   public boolean isFullChunk() {
      return this.fullChunk;
   }

   public boolean hasSkyLight() {
      return this.sky;
   }

   public byte[] getData() {
      return this.data;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         NetworkChunkData that = (NetworkChunkData)o;
         if (this.fullChunk != that.fullChunk) {
            return false;
         } else if (this.mask != that.mask) {
            return false;
         } else {
            return this.sky == that.sky;
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.mask;
      result = 31 * result + (this.fullChunk ? 1 : 0);
      result = 31 * result + (this.sky ? 1 : 0);
      return result;
   }
}
