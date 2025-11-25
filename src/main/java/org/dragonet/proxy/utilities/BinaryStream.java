package org.dragonet.proxy.utilities;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class BinaryStream {
   public int offset;
   private byte[] buffer;
   private int count;
   private static final int MAX_ARRAY_SIZE = 2147483639;

   public BinaryStream() {
      this.buffer = new byte[32];
      this.buffer = new byte[32];
      this.offset = 0;
      this.count = 0;
   }

   public BinaryStream(byte[] buffer) {
      this(buffer, 0);
   }

   public BinaryStream(byte[] buffer, int offset) {
      this.buffer = new byte[32];
      this.buffer = buffer;
      this.offset = offset;
      this.count = buffer.length;
   }

   public void reset() {
      this.buffer = new byte[32];
      this.offset = 0;
      this.count = 0;
   }

   public void setBuffer(byte[] buffer) {
      this.buffer = buffer;
      this.count = buffer == null ? -1 : buffer.length;
   }

   public void setBuffer(byte[] buffer, int offset) {
      this.setBuffer(buffer);
      this.setOffset(offset);
   }

   public void setOffset(int offset) {
      this.offset = offset;
   }

   public int getOffset() {
      return this.offset;
   }

   public byte[] getBuffer() {
      return Arrays.copyOf(this.buffer, this.count);
   }

   public int getCount() {
      return this.count;
   }

   public byte[] get() {
      return Arrays.copyOfRange(this.buffer, this.offset, this.count - 1);
   }

   public byte[] get(int len) {
      if (len < 0) {
         this.offset = this.count - 1;
         return new byte[0];
      } else {
         this.offset += len;
         return Arrays.copyOfRange(this.buffer, this.offset - len, this.offset);
      }
   }

   public void put(byte[] bytes) {
      if (bytes != null) {
         this.ensureCapacity(this.count + bytes.length);
         System.arraycopy(bytes, 0, this.buffer, this.count, bytes.length);
         this.count += bytes.length;
      }
   }

   public long getLong() {
      return Binary.readLong(this.get(8));
   }

   public void putLong(long l) {
      this.put(Binary.writeLong(l));
   }

   public int getInt() {
      return Binary.readInt(this.get(4));
   }

   public void putInt(int i) {
      this.put(Binary.writeInt(i));
   }

   public long getLLong() {
      return Binary.readLLong(this.get(8));
   }

   public void putLLong(long l) {
      this.put(Binary.writeLLong(l));
   }

   public int getLInt() {
      return Binary.readLInt(this.get(4));
   }

   public void putLInt(int i) {
      this.put(Binary.writeLInt(i));
   }

   public int getShort() {
      return Binary.readShort(this.get(2));
   }

   public void putShort(int s) {
      this.put(Binary.writeShort(s));
   }

   public short getSignedShort() {
      return Binary.readSignedShort(this.get(2));
   }

   public void putSignedShort(short s) {
      this.put(Binary.writeShort(s));
   }

   public int getLShort() {
      return Binary.readLShort(this.get(2));
   }

   public void putLShort(int s) {
      this.put(Binary.writeLShort(s));
   }

   public short getSignedLShort() {
      return Binary.readSignedLShort(this.get(2));
   }

   public void putSignedLShort(short s) {
      this.put(Binary.writeLShort(s));
   }

   public float getFloat() {
      return Binary.readFloat(this.get(4));
   }

   public void putFloat(float v) {
      this.put(Binary.writeFloat(v));
   }

   public float getLFloat() {
      return Binary.readLFloat(this.get(4));
   }

   public void putLFloat(float v) {
      this.put(Binary.writeLFloat(v));
   }

   public int getTriad() {
      return Binary.readTriad(this.get(3));
   }

   public void putTriad(int triad) {
      this.put(Binary.writeTriad(triad));
   }

   public int getLTriad() {
      return Binary.readLTriad(this.get(3));
   }

   public void putLTriad(int triad) {
      this.put(Binary.writeLTriad(triad));
   }

   public byte getSignedByte() {
      return this.buffer[this.offset++];
   }

   public boolean getBoolean() {
      return this.getByte() == 1;
   }

   public void putBoolean(boolean bool) {
      this.putByte((byte)(bool ? 1 : 0));
   }

   public int getByte() {
      return this.buffer[this.offset++] & 255;
   }

   public void putByte(byte b) {
      this.put(new byte[]{b});
   }

   public byte[][] getDataArray() {
      return this.getDataArray(10);
   }

   public byte[][] getDataArray(int len) {
      byte[][] data = new byte[len][];

      for(int i = 0; i < len && !this.feof(); ++i) {
         data[i] = this.get(this.getTriad());
      }

      return data;
   }

   public void putDataArray(byte[][] data) {
      byte[][] var2 = data;
      int var3 = data.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         byte[] v = var2[var4];
         this.putTriad(v.length);
         this.put(v);
      }

   }

   public void putUUID(UUID uuid) {
      this.put(Binary.writeUUID(uuid));
   }

   public UUID getUUID() {
      return Binary.readUUID(this.get(16));
   }

   public String getString() {
      return new String(this.get(this.getShort()), StandardCharsets.UTF_8);
   }

   public void putString(String string) {
      byte[] b = string.getBytes(StandardCharsets.UTF_8);
      this.putShort(b.length);
      this.put(b);
   }

   public boolean feof() {
      return this.offset < 0 || this.offset >= this.buffer.length;
   }

   private void ensureCapacity(int minCapacity) {
      if (minCapacity - this.buffer.length > 0) {
         this.grow(minCapacity);
      }

   }

   private void grow(int minCapacity) {
      int oldCapacity = this.buffer.length;
      int newCapacity = oldCapacity << 1;
      if (newCapacity - minCapacity < 0) {
         newCapacity = minCapacity;
      }

      if (newCapacity - 2147483639 > 0) {
         newCapacity = hugeCapacity(minCapacity);
      }

      this.buffer = Arrays.copyOf(this.buffer, newCapacity);
   }

   private static int hugeCapacity(int minCapacity) {
      if (minCapacity < 0) {
         throw new OutOfMemoryError();
      } else {
         return minCapacity > 2147483639 ? Integer.MAX_VALUE : 2147483639;
      }
   }
}
