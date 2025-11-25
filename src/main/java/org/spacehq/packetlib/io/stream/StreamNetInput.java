package org.spacehq.packetlib.io.stream;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.spacehq.packetlib.io.NetInput;

public class StreamNetInput implements NetInput {
   private InputStream in;

   public StreamNetInput(InputStream in) {
      this.in = in;
   }

   public boolean readBoolean() throws IOException {
      return this.readByte() == 1;
   }

   public byte readByte() throws IOException {
      return (byte)this.readUnsignedByte();
   }

   public int readUnsignedByte() throws IOException {
      int b = this.in.read();
      if (b < 0) {
         throw new EOFException();
      } else {
         return b;
      }
   }

   public short readShort() throws IOException {
      return (short)this.readUnsignedShort();
   }

   public int readUnsignedShort() throws IOException {
      int ch1 = this.readUnsignedByte();
      int ch2 = this.readUnsignedByte();
      return (ch1 << 8) + (ch2 << 0);
   }

   public char readChar() throws IOException {
      int ch1 = this.readUnsignedByte();
      int ch2 = this.readUnsignedByte();
      return (char)((ch1 << 8) + (ch2 << 0));
   }

   public int readInt() throws IOException {
      int ch1 = this.readUnsignedByte();
      int ch2 = this.readUnsignedByte();
      int ch3 = this.readUnsignedByte();
      int ch4 = this.readUnsignedByte();
      return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0);
   }

   public int readVarInt() throws IOException {
      int value = 0;
      int size = 0;

      do {
         byte b;
         if (((b = this.readByte()) & 128) != 128) {
            return value | (b & 127) << size * 7;
         }

         value |= (b & 127) << size++ * 7;
      } while(size <= 5);

      throw new IOException("VarInt too long (length must be <= 5)");
   }

   public long readLong() throws IOException {
      byte[] read = this.readBytes(8);
      return ((long)read[0] << 56) + ((long)(read[1] & 255) << 48) + ((long)(read[2] & 255) << 40) + ((long)(read[3] & 255) << 32) + ((long)(read[4] & 255) << 24) + (long)((read[5] & 255) << 16) + (long)((read[6] & 255) << 8) + (long)((read[7] & 255) << 0);
   }

   public long readVarLong() throws IOException {
      int value = 0;
      int size = 0;

      do {
         byte b;
         if (((b = this.readByte()) & 128) != 128) {
            return (long)(value | (b & 127) << size * 7);
         }

         value |= (b & 127) << size++ * 7;
      } while(size <= 10);

      throw new IOException("VarLong too long (length must be <= 10)");
   }

   public float readFloat() throws IOException {
      return Float.intBitsToFloat(this.readInt());
   }

   public double readDouble() throws IOException {
      return Double.longBitsToDouble(this.readLong());
   }

   public byte[] readBytes(int length) throws IOException {
      if (length < 0) {
         throw new IllegalArgumentException("Array cannot have length less than 0.");
      } else {
         byte[] b = new byte[length];

         int count;
         for(int n = 0; n < length; n += count) {
            count = this.in.read(b, n, length - n);
            if (count < 0) {
               throw new EOFException();
            }
         }

         return b;
      }
   }

   public int readBytes(byte[] b) throws IOException {
      return this.in.read(b);
   }

   public int readBytes(byte[] b, int offset, int length) throws IOException {
      return this.in.read(b, offset, length);
   }

   public short[] readShorts(int length) throws IOException {
      if (length < 0) {
         throw new IllegalArgumentException("Array cannot have length less than 0.");
      } else {
         short[] s = new short[length];
         int read = this.readShorts(s);
         if (read < length) {
            throw new EOFException();
         } else {
            return s;
         }
      }
   }

   public int readShorts(short[] s) throws IOException {
      return this.readShorts(s, 0, s.length);
   }

   public int readShorts(short[] s, int offset, int length) throws IOException {
      for(int index = offset; index < offset + length; ++index) {
         try {
            s[index] = this.readShort();
         } catch (EOFException var6) {
            return index - offset;
         }
      }

      return length;
   }

   public int[] readInts(int length) throws IOException {
      if (length < 0) {
         throw new IllegalArgumentException("Array cannot have length less than 0.");
      } else {
         int[] i = new int[length];
         int read = this.readInts(i);
         if (read < length) {
            throw new EOFException();
         } else {
            return i;
         }
      }
   }

   public int readInts(int[] i) throws IOException {
      return this.readInts(i, 0, i.length);
   }

   public int readInts(int[] i, int offset, int length) throws IOException {
      for(int index = offset; index < offset + length; ++index) {
         try {
            i[index] = this.readInt();
         } catch (EOFException var6) {
            return index - offset;
         }
      }

      return length;
   }

   public long[] readLongs(int length) throws IOException {
      if (length < 0) {
         throw new IllegalArgumentException("Array cannot have length less than 0.");
      } else {
         long[] l = new long[length];
         int read = this.readLongs(l);
         if (read < length) {
            throw new EOFException();
         } else {
            return l;
         }
      }
   }

   public int readLongs(long[] l) throws IOException {
      return this.readLongs(l, 0, l.length);
   }

   public int readLongs(long[] l, int offset, int length) throws IOException {
      for(int index = offset; index < offset + length; ++index) {
         try {
            l[index] = this.readLong();
         } catch (EOFException var6) {
            return index - offset;
         }
      }

      return length;
   }

   public String readString() throws IOException {
      int length = this.readVarInt();
      byte[] bytes = this.readBytes(length);
      return new String(bytes, "UTF-8");
   }

   public UUID readUUID() throws IOException {
      return new UUID(this.readLong(), this.readLong());
   }

   public int available() throws IOException {
      return this.in.available();
   }
}
