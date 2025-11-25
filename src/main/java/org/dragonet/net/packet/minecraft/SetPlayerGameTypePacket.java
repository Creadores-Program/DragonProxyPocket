package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class SetPlayerGameTypePacket extends PEPacket {
   public int gamemode;

   public SetPlayerGameTypePacket() {
   }

   public SetPlayerGameTypePacket(int gamemode) {
      this.gamemode = gamemode;
   }

   public int pid() {
      return 55;
   }

   public void encode() {
      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeInt(this.gamemode);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
