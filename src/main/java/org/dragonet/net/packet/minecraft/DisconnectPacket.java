package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.proxy.utilities.io.PEBinaryReader;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class DisconnectPacket extends PEPacket {
   public String message;

   public DisconnectPacket(String message) {
      this.message = message;
   }

   public DisconnectPacket(byte[] data) {
      this.setData(data);
   }

   public int pid() {
      return 5;
   }

   public void encode() {
      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeString(this.message);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
      try {
         PEBinaryReader reader = new PEBinaryReader(new ByteArrayInputStream(this.getData()));
         reader.readByte();
         this.message = reader.readString();
         this.setLength(reader.totallyRead());
      } catch (IOException var2) {
      }

   }
}
