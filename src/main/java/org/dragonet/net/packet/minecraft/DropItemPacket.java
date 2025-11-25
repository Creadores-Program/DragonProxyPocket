package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.dragonet.inventory.PEInventorySlot;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryReader;

public class DropItemPacket extends PEPacket {
   public byte type;
   public PEInventorySlot slot;

   public DropItemPacket(byte[] data) {
      this.setData(data);
   }

   public int pid() {
      return 41;
   }

   public void encode() {
   }

   public void decode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_ENTITY_SPAWNING);
         PEBinaryReader reader = new PEBinaryReader(new ByteArrayInputStream(this.getData()));
         reader.readByte();
         this.type = reader.readByte();
         this.slot = PEInventorySlot.readSlot(reader);
         this.setLength(reader.totallyRead());
      } catch (IOException var2) {
      }

   }
}
