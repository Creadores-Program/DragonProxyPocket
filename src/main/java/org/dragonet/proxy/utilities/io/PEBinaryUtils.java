package org.dragonet.proxy.utilities.io;

public abstract class PEBinaryUtils {
   public static final boolean BIG_ENDIAN = false;
   public static final boolean LITTLE_ENDIAN = true;

   public static byte[] write(long x, int length, boolean endianness) {
      byte[] result = new byte[length];

      for(int i = 0; i < length; ++i) {
         result[!endianness ? length - 1 - i : i] = (byte)((int)(x & 255L));
         x >>= 8;
      }

      return result;
   }

   public static byte[] write(long x, int length) {
      return write(x, length, false);
   }

   public static long read(byte[] buffer, int start, int length, boolean endianness) {
      long x = 0L;

      for(int i = 0; i < length; ++i) {
         x <<= 8;
         x |= (long)(buffer[!endianness ? start + i : start + length - 1 - i] & 255);
      }

      return x;
   }

   public static long read(byte[] buffer, int start, int length) {
      return read(buffer, start, length, false);
   }

   public static long read(byte[] buffer, boolean endianness) {
      return read(buffer, 0, buffer.length, endianness);
   }

   public static long read(byte[] buffer) {
      return read(buffer, 0, buffer.length, false);
   }
}
