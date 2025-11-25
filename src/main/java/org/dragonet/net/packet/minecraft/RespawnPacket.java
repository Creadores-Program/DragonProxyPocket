package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class RespawnPacket extends PEPacket {
   public float x;
   public float y;
   public float z;

   public int pid() {
      return 40;
   }

   public void encode() {
      this.setShouldSendImmidate(true);

      try {
         this.setChannel(NetworkChannel.CHANNEL_MOVEMENT);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeFloat(this.x);
         writer.writeFloat(this.y + 1.62F);
         writer.writeFloat(this.z);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
