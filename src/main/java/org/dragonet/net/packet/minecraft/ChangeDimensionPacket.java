package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class ChangeDimensionPacket extends PEPacket {
   public byte dimension;
   public float x;
   public float y;
   public float z;
   public byte unknown;

   public ChangeDimensionPacket(byte dimension, int x, int y, int z, byte unknown) {
      this.dimension = dimension;
      this.x = (float)x;
      this.y = (float)y;
      this.z = (float)z;
      this.unknown = unknown;
   }

   public int pid() {
      return 54;
   }

   public void encode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_WORLD_EVENTS);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeByte(this.dimension);
         writer.writeFloat(this.x);
         writer.writeFloat(this.y);
         writer.writeFloat(this.z);
         writer.writeByte(this.unknown);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
