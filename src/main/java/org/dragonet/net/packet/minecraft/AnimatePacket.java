package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class AnimatePacket extends PEPacket {
   public byte action;
   public long eid;

   public int pid() {
      return 39;
   }

   public void encode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_WORLD_EVENTS);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeByte(this.action);
         writer.writeLong(this.eid);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
