package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.dragonet.proxy.utilities.io.PEBinaryReader;

public class ClientConnectPacket extends PEPacket {
   public long clientID;
   public long sessionID;
   public boolean useSecurity;

   public ClientConnectPacket(byte[] data) {
      this.setData(data);
   }

   public int pid() {
      return 9;
   }

   public void encode() {
   }

   public void decode() {
      try {
         PEBinaryReader reader = new PEBinaryReader(new ByteArrayInputStream(this.getData()));
         reader.readByte();
         this.clientID = reader.readLong();
         this.sessionID = reader.readLong();
         this.useSecurity = (reader.readByte() & 255) > 0;
         this.setLength(reader.totallyRead());
      } catch (IOException var2) {
      }

   }
}
