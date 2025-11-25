package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class WindowClosePacket extends PEPacket {
   public byte windowID;

   public int pid() {
      return 43;
   }

   public void encode() {
      this.setShouldSendImmidate(true);

      try {
         this.setChannel(NetworkChannel.CHANNEL_PRIORITY);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeByte(this.windowID);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
