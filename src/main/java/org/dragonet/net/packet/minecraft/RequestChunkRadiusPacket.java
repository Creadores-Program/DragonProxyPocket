package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryReader;

public class RequestChunkRadiusPacket extends PEPacket {
   public int radius;

   public int pid() {
      return 61;
   }

   public void encode() {
   }

   public void decode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_ENTITY_SPAWNING);
         PEBinaryReader reader = new PEBinaryReader(new ByteArrayInputStream(this.getData()));
         reader.readByte();
         this.radius = reader.readInt();
         this.setLength(reader.totallyRead());
      } catch (IOException var2) {
      }

   }
}
