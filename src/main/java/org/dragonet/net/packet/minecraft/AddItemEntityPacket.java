package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.inventory.PEInventorySlot;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class AddItemEntityPacket extends PEPacket {
   public long eid;
   public PEInventorySlot item;
   public float x;
   public float y;
   public float z;
   public float speedX;
   public float speedY;
   public float speedZ;

   public int pid() {
      return 13;
   }

   public void encode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_ENTITY_SPAWNING);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeLong(this.eid);
         PEInventorySlot.writeSlot(writer, this.item);
         writer.writeFloat(this.x);
         writer.writeFloat(this.y);
         writer.writeFloat(this.z);
         writer.writeFloat(this.speedX);
         writer.writeFloat(this.speedY);
         writer.writeFloat(this.speedZ);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
