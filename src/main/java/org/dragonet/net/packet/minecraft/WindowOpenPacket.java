package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class WindowOpenPacket extends PEPacket {
   public byte windowID;
   public byte type;
   public short slots;
   public int x;
   public int y;
   public int z;

   public int pid() {
      return 42;
   }

   public void encode() {
      this.setShouldSendImmidate(true);

      try {
         this.setChannel(NetworkChannel.CHANNEL_PRIORITY);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeByte(this.windowID);
         writer.writeByte(this.type);
         writer.writeShort(this.slots);
         writer.writeInt(this.x);
         writer.writeInt(this.y);
         writer.writeInt(this.z);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
