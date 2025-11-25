package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.nbt.PENBT;
import org.dragonet.proxy.nbt.tag.CompoundTag;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class BlockEntityDataPacket extends PEPacket {
   public int x;
   public int y;
   public int z;
   public CompoundTag peTag;

   public BlockEntityDataPacket() {
   }

   public BlockEntityDataPacket(int x, int y, int z, CompoundTag peTag) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.peTag = peTag;
   }

   public int pid() {
      return 50;
   }

   public void encode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_WORLD_EVENTS);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeInt(this.x);
         writer.writeInt(this.y);
         writer.writeInt(this.z);
         PENBT.write(this.peTag, (OutputStream)bos, ByteOrder.LITTLE_ENDIAN);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
