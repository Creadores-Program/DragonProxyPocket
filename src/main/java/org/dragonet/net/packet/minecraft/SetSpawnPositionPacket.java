package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class SetSpawnPositionPacket extends PEPacket {
   public int x;
   public int y;
   public int z;

   public int pid() {
      return 38;
   }

   public void encode() {
      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeInt(this.x);
         writer.writeInt(this.y);
         writer.writeInt(this.z);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
