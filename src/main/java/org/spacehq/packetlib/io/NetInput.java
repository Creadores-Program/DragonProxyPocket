package org.spacehq.packetlib.io;

import java.io.IOException;
import java.util.UUID;

public interface NetInput {
   boolean readBoolean() throws IOException;

   byte readByte() throws IOException;

   int readUnsignedByte() throws IOException;

   short readShort() throws IOException;

   int readUnsignedShort() throws IOException;

   char readChar() throws IOException;

   int readInt() throws IOException;

   int readVarInt() throws IOException;

   long readLong() throws IOException;

   long readVarLong() throws IOException;

   float readFloat() throws IOException;

   double readDouble() throws IOException;

   byte[] readBytes(int var1) throws IOException;

   int readBytes(byte[] var1) throws IOException;

   int readBytes(byte[] var1, int var2, int var3) throws IOException;

   short[] readShorts(int var1) throws IOException;

   int readShorts(short[] var1) throws IOException;

   int readShorts(short[] var1, int var2, int var3) throws IOException;

   int[] readInts(int var1) throws IOException;

   int readInts(int[] var1) throws IOException;

   int readInts(int[] var1, int var2, int var3) throws IOException;

   long[] readLongs(int var1) throws IOException;

   int readLongs(long[] var1) throws IOException;

   int readLongs(long[] var1, int var2, int var3) throws IOException;

   String readString() throws IOException;

   UUID readUUID() throws IOException;

   int available() throws IOException;
}
