package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class ChunkRadiusUpdatedPacket extends PEPacket {
   public int radius;

   public int pid() {
      return 62;
   }

   public void encode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_WORLD_EVENTS);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeInt(this.radius);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
