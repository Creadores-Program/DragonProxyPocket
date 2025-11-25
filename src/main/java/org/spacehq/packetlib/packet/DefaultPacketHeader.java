package org.spacehq.packetlib.packet;

import java.io.IOException;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;

public class DefaultPacketHeader implements PacketHeader {
   public boolean isLengthVariable() {
      return true;
   }

   public int getLengthSize() {
      return 5;
   }

   public int getLengthSize(int length) {
      if ((length & -128) == 0) {
         return 1;
      } else if ((length & -16384) == 0) {
         return 2;
      } else if ((length & -2097152) == 0) {
         return 3;
      } else {
         return (length & -268435456) == 0 ? 4 : 5;
      }
   }

   public int readLength(NetInput in, int available) throws IOException {
      return in.readVarInt();
   }

   public void writeLength(NetOutput out, int length) throws IOException {
      out.writeVarInt(length);
   }

   public int readPacketId(NetInput in) throws IOException {
      return in.readVarInt();
   }

   public void writePacketId(NetOutput out, int packetId) throws IOException {
      out.writeVarInt(packetId);
   }
}
