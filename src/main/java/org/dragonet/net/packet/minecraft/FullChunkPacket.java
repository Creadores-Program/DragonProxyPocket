package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class FullChunkPacket extends PEPacket {
   public int chunkX;
   public int chunkZ;
   public FullChunkPacket.ChunkOrder order;
   public byte[] chunkData;

   public int pid() {
      return 52;
   }

   public void encode() {
      try {
         this.setShouldSendImmidate(false);
         this.setChannel(NetworkChannel.CHANNEL_WORLD_CHUNKS);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeInt(this.chunkX);
         writer.writeInt(this.chunkZ);
         writer.writeByte(this.order != null ? this.order.getType() : 0);
         writer.writeInt(this.chunkData.length);
         writer.write(this.chunkData);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }

   public static enum ChunkOrder {
      COLUMNS((byte)0),
      LAYERS((byte)1);

      private byte type;

      private ChunkOrder(byte t) {
         this.type = t;
      }

      public byte getType() {
         return this.type;
      }
   }
}
