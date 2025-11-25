package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class SetHealthPacket extends PEPacket {
   public int health;

   public SetHealthPacket() {
   }

   public SetHealthPacket(int health) {
      this.health = health;
   }

   public int pid() {
      return 37;
   }

   public void encode() {
      this.setShouldSendImmidate(true);

      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeInt(this.health);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
