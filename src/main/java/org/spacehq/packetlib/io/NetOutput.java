package org.spacehq.packetlib.io;

import java.io.IOException;
import java.util.UUID;

public interface NetOutput {
   void writeBoolean(boolean var1) throws IOException;

   void writeByte(int var1) throws IOException;

   void writeShort(int var1) throws IOException;

   void writeChar(int var1) throws IOException;

   void writeInt(int var1) throws IOException;

   void writeVarInt(int var1) throws IOException;

   void writeLong(long var1) throws IOException;

   void writeVarLong(long var1) throws IOException;

   void writeFloat(float var1) throws IOException;

   void writeDouble(double var1) throws IOException;

   void writeBytes(byte[] var1) throws IOException;

   void writeBytes(byte[] var1, int var2) throws IOException;

   void writeShorts(short[] var1) throws IOException;

   void writeShorts(short[] var1, int var2) throws IOException;

   void writeInts(int[] var1) throws IOException;

   void writeInts(int[] var1, int var2) throws IOException;

   void writeLongs(long[] var1) throws IOException;

   void writeLongs(long[] var1, int var2) throws IOException;

   void writeString(String var1) throws IOException;

   void writeUUID(UUID var1) throws IOException;

   void flush() throws IOException;
}
