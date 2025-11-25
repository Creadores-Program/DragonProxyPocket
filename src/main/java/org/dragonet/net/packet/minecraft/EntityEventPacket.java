package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryReader;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class EntityEventPacket extends PEPacket {
   public long eid;
   public byte event;

   public EntityEventPacket() {
   }

   public EntityEventPacket(long eid, byte event) {
      this.eid = eid;
      this.event = event;
   }

   public EntityEventPacket(byte[] data) {
      this.setData(data);
   }

   public int pid() {
      return 24;
   }

   public void encode() {
      this.setShouldSendImmidate(true);

      try {
         this.setChannel(NetworkChannel.CHANNEL_WORLD_EVENTS);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeLong(this.eid);
         writer.writeByte(this.event);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_ENTITY_SPAWNING);
         PEBinaryReader reader = new PEBinaryReader(new ByteArrayInputStream(this.getData()));
         reader.readByte();
         this.eid = reader.readLong();
         this.event = reader.readByte();
         this.setLength(reader.totallyRead());
      } catch (IOException var2) {
      }

   }
}
