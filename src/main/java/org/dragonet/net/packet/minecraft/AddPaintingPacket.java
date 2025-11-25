package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class AddPaintingPacket extends PEPacket {
   public long eid;
   public int x;
   public int y;
   public int z;
   public int direction;
   public String title;

   public int pid() {
      return 20;
   }

   public void encode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_ENTITY_SPAWNING);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeLong(this.eid);
         writer.writeInt(this.x);
         writer.writeInt(this.y);
         writer.writeInt(this.z);
         writer.writeInt(this.direction);
         writer.writeString(this.title == null ? "" : this.title);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
