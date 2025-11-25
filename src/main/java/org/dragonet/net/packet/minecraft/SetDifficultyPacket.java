package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class SetDifficultyPacket extends PEPacket {
   public int difficulty;

   public int pid() {
      return 53;
   }

   public void encode() {
      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeInt(this.difficulty);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
