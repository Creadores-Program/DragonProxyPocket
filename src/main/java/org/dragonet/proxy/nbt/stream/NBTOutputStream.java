package org.dragonet.proxy.nbt.stream;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class NBTOutputStream extends FilterOutputStream implements DataOutput {
   private final ByteOrder endianness;

   public NBTOutputStream(OutputStream stream) {
      this(stream, ByteOrder.BIG_ENDIAN);
   }

   public NBTOutputStream(OutputStream stream, ByteOrder endianness) {
      super((OutputStream)(stream instanceof DataOutputStream ? stream : new DataOutputStream(stream)));
      this.endianness = endianness;
   }

   public ByteOrder getEndianness() {
      return this.endianness;
   }

   protected DataOutputStream getStream() {
      return (DataOutputStream)super.out;
   }

   public void writeBoolean(boolean v) throws IOException {
      this.getStream().writeBoolean(v);
   }

   public void writeByte(int v) throws IOException {
      this.getStream().writeByte(v);
   }

   public void writeShort(int v) throws IOException {
      if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
         v = Integer.reverseBytes(v) >> 16;
      }

      this.getStream().writeShort(v);
   }

   public void writeChar(int v) throws IOException {
      if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
         v = Character.reverseBytes((char)v);
      }

      this.getStream().writeChar(v);
   }

   public void writeInt(int v) throws IOException {
      if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
         v = Integer.reverseBytes(v);
      }

      this.getStream().writeInt(v);
   }

   public void writeLong(long v) throws IOException {
      if (this.endianness == ByteOrder.LITTLE_ENDIAN) {
         v = Long.reverseBytes(v);
      }

      this.getStream().writeLong(v);
   }

   public void writeFloat(float v) throws IOException {
      this.writeInt(Float.floatToIntBits(v));
   }

   public void writeDouble(double v) throws IOException {
      this.writeLong(Double.doubleToLongBits(v));
   }

   public void writeBytes(String s) throws IOException {
      this.getStream().writeBytes(s);
   }

   public void writeChars(String s) throws IOException {
      this.getStream().writeChars(s);
   }

   public void writeUTF(String s) throws IOException {
      byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
      this.writeShort(bytes.length);
      this.getStream().write(bytes);
   }

   public void close() throws IOException {
      this.getStream().close();
   }
}
