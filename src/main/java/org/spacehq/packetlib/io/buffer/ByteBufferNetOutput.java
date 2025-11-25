package org.spacehq.packetlib.io.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import org.spacehq.packetlib.io.NetOutput;

public class ByteBufferNetOutput implements NetOutput {
   private ByteBuffer buffer;

   public ByteBufferNetOutput(ByteBuffer buffer) {
      this.buffer = buffer;
   }

   public ByteBuffer getByteBuffer() {
      return this.buffer;
   }

   public void writeBoolean(boolean b) throws IOException {
      this.buffer.put((byte)(b ? 1 : 0));
   }

   public void writeByte(int b) throws IOException {
      this.buffer.put((byte)b);
   }

   public void writeShort(int s) throws IOException {
      this.buffer.putShort((short)s);
   }

   public void writeChar(int c) throws IOException {
      this.buffer.putChar((char)c);
   }

   public void writeInt(int i) throws IOException {
      this.buffer.putInt(i);
   }

   public void writeVarInt(int i) throws IOException {
      while((i & -128) != 0) {
         this.writeByte(i & 127 | 128);
         i >>>= 7;
      }

      this.writeByte(i);
   }

   public void writeLong(long l) throws IOException {
      this.buffer.putLong(l);
   }

   public void writeVarLong(long l) throws IOException {
      while((l & -128L) != 0L) {
         this.writeByte((int)(l & 127L) | 128);
         l >>>= 7;
      }

      this.writeByte((int)l);
   }

   public void writeFloat(float f) throws IOException {
      this.buffer.putFloat(f);
   }

   public void writeDouble(double d) throws IOException {
      this.buffer.putDouble(d);
   }

   public void writeBytes(byte[] b) throws IOException {
      this.buffer.put(b);
   }

   public void writeBytes(byte[] b, int length) throws IOException {
      this.buffer.put(b, 0, length);
   }

   public void writeShorts(short[] s) throws IOException {
      this.writeShorts(s, s.length);
   }

   public void writeShorts(short[] s, int length) throws IOException {
      for(int index = 0; index < length; ++index) {
         this.writeShort(s[index]);
      }

   }

   public void writeInts(int[] i) throws IOException {
      this.writeInts(i, i.length);
   }

   public void writeInts(int[] i, int length) throws IOException {
      for(int index = 0; index < length; ++index) {
         this.writeInt(i[index]);
      }

   }

   public void writeLongs(long[] l) throws IOException {
      this.writeLongs(l, l.length);
   }

   public void writeLongs(long[] l, int length) throws IOException {
      for(int index = 0; index < length; ++index) {
         this.writeLong(l[index]);
      }

   }

   public void writeString(String s) throws IOException {
      if (s == null) {
         throw new IllegalArgumentException("String cannot be null!");
      } else {
         byte[] bytes = s.getBytes("UTF-8");
         if (bytes.length > 32767) {
            throw new IOException("String too big (was " + s.length() + " bytes encoded, max " + 32767 + ")");
         } else {
            this.writeVarInt(bytes.length);
            this.writeBytes(bytes);
         }
      }
   }

   public void writeUUID(UUID uuid) throws IOException {
      this.writeLong(uuid.getMostSignificantBits());
      this.writeLong(uuid.getLeastSignificantBits());
   }

   public void flush() throws IOException {
   }
}
