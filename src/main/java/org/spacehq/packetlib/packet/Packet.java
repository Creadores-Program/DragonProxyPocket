package org.spacehq.packetlib.packet;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;

public interface Packet {
   void read(NetInput var1) throws IOException;

   void write(NetOutput var1) throws IOException;

   boolean isPriority();
}
