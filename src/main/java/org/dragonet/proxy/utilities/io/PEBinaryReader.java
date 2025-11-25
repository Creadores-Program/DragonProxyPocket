package org.dragonet.proxy.utilities.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PEBinaryReader implements Closeable {
   protected InputStream is;
   protected boolean endianness;
   private int totallyRead;

   public PEBinaryReader(InputStream is) {
      this(is, false);
   }

   public PEBinaryReader(InputStream is, boolean endianness) {
      this.is = is;
      this.endianness = endianness;
   }

   public boolean switchEndianness() {
      this.endianness = !this.endianness;
      return this.endianness;
   }

   public boolean getEndianness() {
      return this.endianness;
   }

   public void close() throws IOException {
      this.is.close();
   }

   public PEBinaryReader.BinaryAddress readAddress() throws IOException {
      PEBinaryReader.BinaryAddress addr = new PEBinaryReader.BinaryAddress();
      addr.type = this.readByte();
      if ((addr.type & 255) == 4) {
         addr.address = this.read(4);
      } else {
         addr.address = this.read(8);
      }

      addr.port = this.readShort();
      return addr;
   }

   public UUID readUUID() throws IOException {
      long first = this.readLong();
      long last = this.readLong();
      return new UUID(first, last);
   }

   public String readString() throws IOException {
      this.falloc(2);
      return this.readString(2);
   }

   public String readString(int lenLen) throws IOException {
      this.falloc(lenLen);
      int length = (int)this.readNat(lenLen);
      this.falloc(length);
      return new String(this.read(length), StandardCharsets.UTF_8);
   }

   public byte readByte() throws IOException {
      this.falloc(1);
      ++this.totallyRead;
      return (byte)this.is.read();
   }

   public short readShort() throws IOException {
      this.falloc(2);
      return (short)((int)(this.readNat(2) & 65535L));
   }

   public int readTriad() throws IOException {
      this.falloc(3);
      this.endianness = !this.endianness;
      int triad = (int)(this.readNat(3) & 16777215L);
      this.endianness = !this.endianness;
      return triad;
   }

   public int readInt() throws IOException {
      this.falloc(4);
      return (int)(this.readNat(4) & -1L);
   }

   public long readLong() throws IOException {
      this.falloc(8);
      return this.readNat(8);
   }

   public float readFloat() throws IOException {
      this.falloc(4);
      ByteBuffer bb = ByteBuffer.wrap(this.read(4));
      return bb.getFloat();
   }

   public double readDouble() throws IOException {
      this.falloc(8);
      ByteBuffer bb = ByteBuffer.wrap(this.read(8));
      return bb.getDouble();
   }

   public byte[] read(int length) throws IOException {
      this.falloc(length);
      this.totallyRead += length;
      byte[] buffer = new byte[length];
      this.is.read(buffer, 0, length);
      return buffer;
   }

   public long readNat(int length) throws IOException {
      this.falloc(length);
      return PEBinaryUtils.read(this.read(length), 0, length, this.endianness);
   }

   public <T> T readType(Class<T> clazz, Object[] args) throws IOException {
      if (clazz.equals(String.class)) {
         if (args.length > 0) {
            Object arg = args[0];
            if (arg instanceof Integer) {
               return (T) this.readString((Integer)arg);
            }
         }

         return (T) this.readString();
      } else if (clazz.equals(Byte.class)) {
         return (T) Byte.valueOf(this.readByte());
      } else if (clazz.equals(Short.class)) {
         return (T) Short.valueOf(this.readShort());
      } else if (clazz.equals(Integer.class)) {
         return (T) Integer.valueOf(this.readInt());
      } else if (clazz.equals(Long.class)) {
         return (T) Long.valueOf(this.readLong());
      } else if (clazz.equals(Float.class)) {
         return (T) Float.valueOf(this.readFloat());
      } else {
         return clazz.equals(Double.class) ? (T) Double.valueOf(this.readDouble()) : this.getUnknownTypeValue(clazz, args);
      }
   }

   protected <T> T getUnknownTypeValue(Class<T> clazz, Object[] args) throws IOException {
      throw new UnsupportedOperationException(String.format("Trying to read unknown type %s from class %s", clazz.getSimpleName(), this.getClass().getName()));
   }

   protected void falloc(int l) throws IOException {
      int lack = l - this.is.available();
      if (lack > 0) {
         throw this.getUEOFException(lack);
      }
   }

   protected IOException getUEOFException(int needed) {
      return new IOException(String.format("Unexpected end of file: %d more bytes expected", needed));
   }

   public int available() throws IOException {
      return this.is.available();
   }

   public int totallyRead() {
      return this.totallyRead;
   }

   public static class BinaryAddress {
      public byte type;
      public byte[] address;
      public short port;
   }
}
