package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryReader;

public class RemoveBlockPacket extends PEPacket {
   public long eid;
   public int x;
   public int z;
   public int y;

   public RemoveBlockPacket(byte[] data) {
      this.setData(data);
   }

   public int pid() {
      return 18;
   }

   public void encode() {
   }

   public void decode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_WORLD_EVENTS);
         PEBinaryReader reader = new PEBinaryReader(new ByteArrayInputStream(this.getData()));
         reader.readByte();
         this.eid = reader.readLong();
         this.x = reader.readInt();
         this.z = reader.readInt();
         this.y = reader.readByte();
         this.setLength(reader.totallyRead());
      } catch (IOException var2) {
      }

   }
}
