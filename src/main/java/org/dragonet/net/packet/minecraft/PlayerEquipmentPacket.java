package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.inventory.PEInventorySlot;
import org.dragonet.proxy.utilities.io.PEBinaryReader;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class PlayerEquipmentPacket extends PEPacket {
   public long eid;
   public PEInventorySlot item;
   public int slot;
   public int selectedSlot;

   public PlayerEquipmentPacket() {
   }

   public PlayerEquipmentPacket(byte[] data) {
      this.setData(data);
   }

   public int pid() {
      return 27;
   }

   public void encode() {
      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeLong(this.eid);
         PEInventorySlot.writeSlot(writer, this.item);
         writer.writeByte((byte)(this.slot & 255));
         writer.writeByte((byte)(this.selectedSlot & 255));
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
      try {
         PEBinaryReader reader = new PEBinaryReader(new ByteArrayInputStream(this.getData()));
         reader.readByte();
         this.eid = reader.readLong();
         this.item = PEInventorySlot.readSlot(reader);
         this.slot = reader.readByte() & 255;
         this.selectedSlot = reader.readByte() & 255;
         this.setLength(reader.totallyRead());
      } catch (IOException var2) {
      }

   }
}
