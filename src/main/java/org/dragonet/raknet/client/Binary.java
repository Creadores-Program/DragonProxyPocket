package org.dragonet.raknet.client;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Binary {
   public static int readLTriad(byte[] bytes) {
      return bytes[0] & 255 | (bytes[1] & 255) << 8 | (bytes[2] & 15) << 16;
   }

   public static byte[] writeLTriad(int triad) {
      byte b3 = (byte)(triad & 255);
      byte b2 = (byte)(triad >> 8 & 255);
      byte b1 = (byte)(triad >> 16 & 255);
      return new byte[]{b3, b2, b1};
   }

   public static boolean readBool(byte b) {
      return b == 0;
   }

   public static byte writeBool(boolean b) {
      return (byte)(b ? 1 : 0);
   }

   public static int readByte(byte b, boolean signed) {
      return signed ? b : b & 255;
   }

   public static byte writeByte(byte b) {
      return b;
   }

   public static int readShort(byte[] bytes) {
      return bytes[0] << 8 & '\uff00' | bytes[1] & 255;
   }

   public static short readSignedShort(byte[] bytes) {
      return ByteBuffer.wrap(bytes).getShort();
   }

   public static byte[] writeShort(short s) {
      return ByteBuffer.allocate(2).putShort(s).array();
   }

   public static byte[] writeUnsignedShort(int s) {
      ByteBuffer bb = ByteBuffer.allocate(2);
      bb.put((byte)(s >> 8 & 255));
      bb.put((byte)(s & 255));
      return bb.array();
   }

   public static int readInt(byte[] bytes) {
      return ByteBuffer.wrap(bytes).getInt();
   }

   public static byte[] writeInt(int i) {
      return ByteBuffer.allocate(4).putInt(i).array();
   }

   public static float readFloat(byte[] bytes) {
      return ByteBuffer.wrap(bytes).getFloat();
   }

   public static byte[] writeFloat(float f) {
      return ByteBuffer.allocate(4).putFloat(f).array();
   }

   public static double readDouble(byte[] bytes) {
      return (double)ByteBuffer.wrap(bytes).getFloat();
   }

   public static byte[] writeDouble(double d) {
      return ByteBuffer.allocate(8).putDouble(d).array();
   }

   public static long readLong(byte[] bytes) {
      return ByteBuffer.wrap(bytes).getLong();
   }

   public static byte[] writeLong(long l) {
      return ByteBuffer.allocate(8).putLong(l).array();
   }

   public static byte[] subbytes(byte[] bytes, int start, int length) {
      ByteBuffer bb = ByteBuffer.wrap(bytes);
      bb.position(start);
      byte[] bytes2 = new byte[length];
      bb.get(bytes2);
      return bytes2;
   }

   public static byte[] subbytes(byte[] bytes, int start) {
      return subbytes(bytes, start, bytes.length - start);
   }

   public static byte[][] splitbytes(byte[] bytes, int chunkSize) {
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
}
