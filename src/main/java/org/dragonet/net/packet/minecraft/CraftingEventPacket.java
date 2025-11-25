package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;
import org.dragonet.inventory.PEInventorySlot;
import org.dragonet.proxy.utilities.io.PEBinaryReader;

public class CraftingEventPacket extends PEPacket {
   public byte windowId;
   public int craftType;
   public UUID uuid;
   public PEInventorySlot[] input;
   public PEInventorySlot[] output;

   public int pid() {
      return 48;
   }

   public void encode() {
   }

   public void decode() {
      try {
         PEBinaryReader reader = new PEBinaryReader(new ByteArrayInputStream(this.getData()));
         reader.readByte();
         this.windowId = reader.readByte();
         this.craftType = reader.readInt();
         this.uuid = reader.readUUID();
         int size = reader.readInt();
         this.input = new PEInventorySlot[size > 128 ? 128 : size];

         int i;
         for(i = 0; i < size; ++i) {
            this.input[i] = PEInventorySlot.readSlot(reader);
         }

         size = reader.readInt();
         this.output = new PEInventorySlot[size > 128 ? 128 : size];

         for(i = 0; i < size; ++i) {
            this.output[i] = PEInventorySlot.readSlot(reader);
         }

         this.setLength(reader.totallyRead());
      } catch (IOException var4) {
      }

   }
}
