package org.spacehq.packetlib.packet;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;

public interface PacketHeader {
   boolean isLengthVariable();

   int getLengthSize();

   int getLengthSize(int var1);

   int readLength(NetInput var1, int var2) throws IOException;

   void writeLength(NetOutput var1, int var2) throws IOException;

   int readPacketId(NetInput var1) throws IOException;

   void writePacketId(NetOutput var1, int var2) throws IOException;
}
