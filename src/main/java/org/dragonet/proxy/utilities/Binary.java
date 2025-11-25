package org.dragonet.proxy.utilities;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

public class Binary {
   public static int readTriad(byte[] bytes) {
      return readInt(new byte[]{0, bytes[0], bytes[1], bytes[2]});
   }

   public static byte[] writeTriad(int value) {
      return new byte[]{(byte)(value >>> 16 & 255), (byte)(value >>> 8 & 255), (byte)(value & 255)};
   }

   public static int readLTriad(byte[] bytes) {
      return readLInt(new byte[]{bytes[0], bytes[1], bytes[2], 0});
   }

   public static byte[] writeLTriad(int value) {
      return new byte[]{(byte)(value & 255), (byte)(value >>> 8 & 255), (byte)(value >>> 16 & 255)};
   }

   public static UUID readUUID(byte[] bytes) {
      return new UUID(readLong(bytes), readLong(new byte[]{bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15]}));
   }

   public static byte[] writeUUID(UUID uuid) {
      return appendBytes(writeLong(uuid.getMostSignificantBits()), writeLong(uuid.getLeastSignificantBits()));
   }

   public static boolean readBool(byte b) {
      return b == 0;
   }

   public static byte writeBool(boolean b) {
      return (byte)(b ? 1 : 0);
   }

   public static int readSignedByte(byte b) {
      return b & 255;
   }

   public static byte writeByte(byte b) {
      return b;
   }

   public static int readShort(byte[] bytes) {
      return ((bytes[0] & 255) << 8) + (bytes[1] & 255);
   }

   public static short readSignedShort(byte[] bytes) {
      return (short)readShort(bytes);
   }

   public static byte[] writeShort(int s) {
      return new byte[]{(byte)(s >>> 8 & 255), (byte)(s & 255)};
   }

   public static int readLShort(byte[] bytes) {
      return (bytes[1] & '\uff00') + (bytes[0] & 255);
   }

   public static short readSignedLShort(byte[] bytes) {
      return (short)readLShort(bytes);
   }

   public static byte[] writeLShort(int s) {
      s &= 65535;
      return new byte[]{(byte)(s & 255), (byte)(s >>> 8 & 255)};
   }

   public static int readInt(byte[] bytes) {
      return ((bytes[0] & 255) << 24) + ((bytes[1] & 255) << 16) + ((bytes[2] & 255) << 8) + (bytes[3] & 255);
   }

   public static byte[] writeInt(int i) {
      return new byte[]{(byte)(i >>> 24 & 255), (byte)(i >>> 16 & 255), (byte)(i >>> 8 & 255), (byte)(i & 255)};
   }

   public static int readLInt(byte[] bytes) {
      return ((bytes[3] & 255) << 24) + ((bytes[2] & 255) << 16) + ((bytes[1] & 255) << 8) + (bytes[0] & 255);
   }

   public static byte[] writeLInt(int i) {
      return new byte[]{(byte)(i & 255), (byte)(i >>> 8 & 255), (byte)(i >>> 16 & 255), (byte)(i >>> 24 & 255)};
   }

   public static float readFloat(byte[] bytes) {
      return Float.intBitsToFloat(readInt(bytes));
   }

   public static byte[] writeFloat(float f) {
      return writeInt(Float.floatToIntBits(f));
   }

   public static float readLFloat(byte[] bytes) {
      return Float.intBitsToFloat(readLInt(bytes));
   }

   public static byte[] writeLFloat(float f) {
      return writeLInt(Float.floatToIntBits(f));
   }

   public static double readDouble(byte[] bytes) {
      return Double.longBitsToDouble(readLong(bytes));
   }

   public static byte[] writeDouble(double d) {
      return writeLong(Double.doubleToLongBits(d));
   }

   public static double readLDouble(byte[] bytes) {
      return Double.longBitsToDouble(readLLong(bytes));
   }

   public static byte[] writeLDouble(double d) {
      return writeLLong(Double.doubleToLongBits(d));
   }

   public static long readLong(byte[] bytes) {
      return ((long)bytes[0] << 56) + ((long)(bytes[1] & 255) << 48) + ((long)(bytes[2] & 255) << 40) + ((long)(bytes[3] & 255) << 32) + ((long)(bytes[4] & 255) << 24) + (long)((bytes[5] & 255) << 16) + (long)((bytes[6] & 255) << 8) + (long)(bytes[7] & 255);
   }

   public static byte[] writeLong(long l) {
      return new byte[]{(byte)((int)(l >>> 56)), (byte)((int)(l >>> 48)), (byte)((int)(l >>> 40)), (byte)((int)(l >>> 32)), (byte)((int)(l >>> 24)), (byte)((int)(l >>> 16)), (byte)((int)(l >>> 8)), (byte)((int)l)};
   }

   public static long readLLong(byte[] bytes) {
      return ((long)bytes[7] << 56) + ((long)(bytes[6] & 255) << 48) + ((long)(bytes[5] & 255) << 40) + ((long)(bytes[4] & 255) << 32) + ((long)(bytes[3] & 255) << 24) + (long)((bytes[2] & 255) << 16) + (long)((bytes[1] & 255) << 8) + (long)(bytes[0] & 255);
   }

   public static byte[] writeLLong(long l) {
      return new byte[]{(byte)((int)l), (byte)((int)(l >>> 8)), (byte)((int)(l >>> 16)), (byte)((int)(l >>> 24)), (byte)((int)(l >>> 32)), (byte)((int)(l >>> 40)), (byte)((int)(l >>> 48)), (byte)((int)(l >>> 56))};
   }

   public static byte[] reserveBytes(byte[] bytes) {
      byte[] newBytes = new byte[bytes.length];

      for(int i = 0; i < bytes.length; ++i) {
         newBytes[bytes.length - 1 - i] = bytes[i];
      }

      return newBytes;
   }

   public static String bytesToHexString(byte[] src) {
      return bytesToHexString(src, false);
   }

   public static String bytesToHexString(byte[] src, boolean blank) {
      StringBuilder stringBuilder = new StringBuilder("");
      if (src != null && src.length > 0) {
         byte[] var3 = src;
         int var4 = src.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            byte b = var3[var5];
            if (stringBuilder.length() != 0 && blank) {
               stringBuilder.append(" ");
            }

            int v = b & 255;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
               stringBuilder.append(0);
            }

            stringBuilder.append(hv);
         }

         return stringBuilder.toString().toUpperCase();
      } else {
         return null;
      }
   }

   public static byte[] hexStringToBytes(String hexString) {
      if (hexString != null && !hexString.equals("")) {
         String str = "0123456789ABCDEF";
         hexString = hexString.toUpperCase();
         int length = hexString.length() / 2;
         char[] hexChars = hexString.toCharArray();
         byte[] d = new byte[length];

         for(int i = 0; i < length; ++i) {
            int pos = i * 2;
            d[i] = (byte)((byte)str.indexOf(hexChars[pos]) << 4 | (byte)str.indexOf(hexChars[pos + 1]));
         }

         return d;
      } else {
         return null;
      }
   }

   public static byte[] subBytes(byte[] bytes, int start, int length) {
      int len = Math.min(bytes.length, start + length);
      return Arrays.copyOfRange(bytes, start, len);
   }

   public static byte[] subBytes(byte[] bytes, int start) {
      return subBytes(bytes, start, bytes.length - start);
   }

   public static byte[][] splitBytes(byte[] bytes, int chunkSize) {
      byte[][] splits = new byte[1024][chunkSize];
      int chunks = 0;

      for(int i = 0; i < bytes.length; i += chunkSize) {
         if (bytes.length - i > chunkSize) {
            splits[chunks] = Arrays.copyOfRange(bytes, i, i + chunkSize);
         } else {
            splits[chunks] = Arrays.copyOfRange(bytes, i, bytes.length);
         }

         ++chunks;
      }

      splits = (byte[][])Arrays.copyOf(splits, chunks);
      return splits;
   }

   public static byte[] appendBytes(byte byte1, byte[]... bytes2) {
      int length = 1;
      byte[][] var3 = bytes2;
      int var4 = bytes2.length;

      int var5;
      for(var5 = 0; var5 < var4; ++var5) {
         byte[] bytes = var3[var5];
         length += bytes.length;
      }

      ByteBuffer buffer = ByteBuffer.allocate(length);
      buffer.put(byte1);
      byte[][] var9 = bytes2;
      var5 = bytes2.length;

      for(int var10 = 0; var10 < var5; ++var10) {
         byte[] bytes = var9[var10];
         buffer.put(bytes);
      }

      return buffer.array();
   }

   public static byte[] appendBytes(byte[] bytes1, byte[]... bytes2) {
      int length = bytes1.length;
      byte[][] var3 = bytes2;
      int var4 = bytes2.length;

      int var5;
      for(var5 = 0; var5 < var4; ++var5) {
         byte[] bytes = var3[var5];
         length += bytes.length;
      }

      ByteBuffer buffer = ByteBuffer.allocate(length);
      buffer.put(bytes1);
      byte[][] var9 = bytes2;
      var5 = bytes2.length;

      for(int var10 = 0; var10 < var5; ++var10) {
         byte[] bytes = var9[var10];
         buffer.put(bytes);
      }

      return buffer.array();
   }
}
