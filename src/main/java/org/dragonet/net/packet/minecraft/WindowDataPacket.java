package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class WindowDataPacket extends PEPacket {
   private byte windowID;
   private short property;
   private short value;

   public int pid() {
      return 45;
   }

   public void encode() {
      this.setShouldSendImmidate(true);

      try {
         this.setChannel(NetworkChannel.CHANNEL_PRIORITY);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeByte(this.windowID);
         writer.writeShort(this.property);
         writer.writeShort(this.value);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
