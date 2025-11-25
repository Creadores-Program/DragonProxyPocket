package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class RemoveEntityPacket extends PEPacket {
   public long eid;

   public RemoveEntityPacket() {
   }

   public RemoveEntityPacket(long eid) {
      this.eid = eid;
   }

   public int pid() {
      return 12;
   }

   public void encode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_ENTITY_SPAWNING);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeLong(this.eid);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
