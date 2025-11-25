package org.dragonet.proxy.utilities.io;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PEBinaryWriter implements Flushable, Closeable {
   protected OutputStream os;
   protected boolean endianness;

   public PEBinaryWriter(OutputStream os) {
      this(os, false);
   }

   public PEBinaryWriter(OutputStream os, boolean endianness) {
      this.os = os;
      this.endianness = endianness;
   }

   public boolean switchEndianness() {
      this.endianness = !this.endianness;
      return this.endianness;
   }

   public boolean getEndianness() {
      return this.endianness;
   }

   public void flush() throws IOException {
      this.os.flush();
   }

   public void close() throws IOException {
      this.os.close();
   }

   public void writeUUID(UUID uuid) throws IOException {
      this.writeLong(uuid.getMostSignificantBits());
      this.writeLong(uuid.getLeastSignificantBits());
   }

   public void writeString(String string) throws IOException {
      this.writeString(string, 2);
   }

   public void writeAddress(InetAddress addr, short port) throws IOException {
      if (addr instanceof Inet4Address) {
         this.writeByte((byte)4);
         this.writeInt(addr.getAddress()[0] << 24 | addr.getAddress()[1] << 16 | addr.getAddress()[2] << 8 | addr.getAddress()[3]);
         this.writeShort(port);
      } else {
         this.writeByte((byte)6);
         this.writeLong(0L);
      }

   }

   public void writeString(String string, int lenPrefix) throws IOException {
      byte[] bin = string.getBytes(StandardCharsets.UTF_8);
      this.write((long)bin.length, lenPrefix);
      this.os.write(bin);
   }

   public void writeByte(byte b) throws IOException {
      this.os.write(b);
   }

   public void writeShort(short s) throws IOException {
      this.write((long)s, 2);
   }

   public void writeTriad(int t) throws IOException {
      this.endianness = !this.endianness;
      this.write((long)t, 3);
      this.endianness = !this.endianness;
   }

   public void writeInt(int i) throws IOException {
      this.write((long)i, 4);
   }

   public void writeLong(long l) throws IOException {
      this.write(l, 8);
   }

   public void writeFloat(float f) throws IOException {
      ByteBuffer bb = ByteBuffer.allocate(4);
      bb.putFloat(f);
      this.os.write(bb.array());
   }

   public void writeDouble(double d) throws IOException {
      ByteBuffer bb = ByteBuffer.allocate(8);
      bb.putDouble(d);
      this.os.write(bb.array());
   }

   public void write(long x, int len) throws IOException {
      this.os.write(PEBinaryUtils.write(x, len, this.endianness));
   }

   public void write(byte[] bytes) throws IOException {
      this.os.write(bytes);
   }

   public void writeNat(int oneByte) throws IOException {
      this.os.write(oneByte);
   }

   public <T> void writeObject(T obj, Object[] args) throws IOException {
      if (obj instanceof CharSequence) {
         boolean written = false;
         if (args.length > 0 && args[0] instanceof Integer) {
            this.writeString(obj.toString(), (Integer)args[0]);
            written = true;
         }

         if (!written) {
            this.writeString(obj.toString());
         }
      } else if (obj instanceof Byte) {
         this.writeByte((Byte)obj);
      } else if (obj instanceof Short) {
         this.writeShort((Short)obj);
      } else if (obj instanceof Integer) {
         this.writeInt((Integer)obj);
      } else if (obj instanceof Long) {
         this.writeLong((Long)obj);
      } else if (obj instanceof Float) {
         this.writeFloat((Float)obj);
      } else if (obj instanceof Double) {
         this.writeDouble((Double)obj);
      } else {
         this.writeUnknownType(obj, args);
      }

   }

   protected <T> void writeUnknownType(T obj, Object[] args) throws IOException {
      throw new UnsupportedOperationException(String.format("Unknown object type %s", obj.getClass().getName()));
   }
}
