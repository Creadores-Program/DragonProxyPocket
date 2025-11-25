package org.dragonet.proxy.nbt.stream;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class NBTInputStream extends FilterInputStream implements DataInput {
   private final ByteOrder endianness;

   public NBTInputStream(InputStream stream) {
      this(stream, ByteOrder.BIG_ENDIAN);
   }

   public NBTInputStream(InputStream stream, ByteOrder endianness) {
      super((InputStream)(stream instanceof DataInputStream ? stream : new DataInputStream(stream)));
      this.endianness = endianness;
   }

   public ByteOrder getEndianness() {
      return this.endianness;
   }

   protected DataInputStream getStream() {
      return (DataInputStream)super.in;
   }

   public void readFully(byte[] b) throws IOException {
      this.getStream().readFully(b);
   }

   public void readFully(byte[] b, int off, int len) throws IOException {
      this.getStream().readFully(b, off, len);
   }

   public int skipBytes(int n) throws IOException {
      return this.getStream().skipBytes(n);
   }

   public boolean readBoolean() throws IOException {
      return this.getStream().readBoolean();
   }

   public byte readByte() throws IOException {
      return this.getStream().readByte();
   }

   public int readUnsignedByte() throws IOException {
      return this.getStream().readUnsignedByte();
   }

   public short readShort() throws IOException {
      short s = this.getStream().readShort();
      if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
         s = Short.reverseBytes(s);
      }

      return s;
   }

   public int readUnsignedShort() throws IOException {
      int s = this.getStream().readUnsignedShort();
      if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
         s = Integer.reverseBytes(s) >> 16;
      }

      return s;
   }

   public char readChar() throws IOException {
      char c = this.getStream().readChar();
      if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
         c = Character.reverseBytes(c);
      }

      return c;
   }

   public int readInt() throws IOException {
      int i = this.getStream().readInt();
      if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
         i = Integer.reverseBytes(i);
      }

      return i;
   }

   public long readLong() throws IOException {
      long l = this.getStream().readLong();
      if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
         l = Long.reverseBytes(l);
      }

      return l;
   }

   public float readFloat() throws IOException {
      return Float.intBitsToFloat(this.readInt());
   }

   public double readDouble() throws IOException {
      return Double.longBitsToDouble(this.readLong());
   }

   /** @deprecated */
   @Deprecated
   public String readLine() throws IOException {
      return this.getStream().readLine();
   }

   public String readUTF() throws IOException {
      int length = this.readUnsignedShort();
      byte[] bytes = new byte[length];
      this.read(bytes);
      return new String(bytes, StandardCharsets.UTF_8);
   }

   public void close() throws IOException {
      this.getStream().close();
   }
}
