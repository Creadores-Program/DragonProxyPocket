package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import org.dragonet.inventory.PEInventorySlot;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.entity.meta.EntityMetaData;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class AddPlayerPacket extends PEPacket {
   public UUID uuid;
   public String username;
   public long eid;
   public float x;
   public float y;
   public float z;
   public float speedX;
   public float speedY;
   public float speedZ;
   public float yaw;
   public float pitch;
   public PEInventorySlot item;
   public EntityMetaData metadata;

   public int pid() {
      return 10;
   }

   public void encode() {
      this.setShouldSendImmidate(true);
      this.setChannel(NetworkChannel.CHANNEL_ENTITY_SPAWNING);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      PEBinaryWriter writer = new PEBinaryWriter(bos);

      try {
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeUUID(this.uuid);
         writer.writeString(this.username);
         writer.writeLong(this.eid);
         writer.writeFloat(this.x);
         writer.writeFloat(this.y);
         writer.writeFloat(this.z);
         writer.writeFloat(this.speedX);
         writer.writeFloat(this.speedY);
         writer.writeFloat(this.speedZ);
         writer.writeFloat(this.yaw);
         writer.writeFloat(this.yaw);
         writer.writeFloat(this.pitch);
         PEInventorySlot.writeSlot(writer, this.item);
         writer.writeByte((byte)0);
         this.setData(bos.toByteArray());
      } catch (IOException var4) {
      }

   }

   public void decode() {
   }
}
