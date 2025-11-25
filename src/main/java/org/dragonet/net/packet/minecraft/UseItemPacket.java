package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.dragonet.inventory.PEInventorySlot;
import org.dragonet.proxy.utilities.io.PEBinaryReader;

public class UseItemPacket extends PEPacket {
   public int x;
   public int y;
   public int z;
   public int face;
   public PEInventorySlot item;
   public float fx;
   public float fy;
   public float fz;
   public float posX;
   public float posY;
   public float posZ;
   public int slot;

   public UseItemPacket(byte[] data) {
      this.setData(data);
   }

   public int pid() {
      return 31;
   }

   public void encode() {
   }

   public void decode() {
      try {
         PEBinaryReader reader = new PEBinaryReader(new ByteArrayInputStream(this.getData()));
         reader.readByte();
         this.x = reader.readInt();
         this.y = reader.readInt();
         this.z = reader.readInt();
         this.face = reader.readByte() & 255;
         this.fx = reader.readFloat();
         this.fy = reader.readFloat();
         this.fz = reader.readFloat();
         this.posX = reader.readFloat();
         this.posY = reader.readFloat();
         this.posZ = reader.readFloat();
         this.slot = reader.readInt();
         this.item = PEInventorySlot.readSlot(reader);
      } catch (IOException var2) {
      }

   }
}
