package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class SetTimePacket extends PEPacket {
   public int time;
   public boolean started;

   public SetTimePacket() {
   }

   public SetTimePacket(int time, boolean started) {
      this.time = time;
      this.started = started;
   }

   public int pid() {
      return 8;
   }

   public void encode() {
      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)this.pid());
         writer.writeInt(this.time);
         if (!this.started) {
            writer.writeByte((byte)0);
         } else {
            writer.writeByte((byte)1);
         }

         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
