package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class MoveEntitiesPacket extends PEPacket {
   public MoveEntitiesPacket.MoveEntityData[] entities;

   public MoveEntitiesPacket(MoveEntitiesPacket.MoveEntityData[] entities) {
      this.entities = entities;
   }

   public int pid() {
      return 15;
   }

   public void encode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_WORLD_EVENTS);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeInt(this.entities.length);
         MoveEntitiesPacket.MoveEntityData[] var3 = this.entities;
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            MoveEntitiesPacket.MoveEntityData d = var3[var5];
            writer.writeLong(d.eid);
            writer.writeFloat(d.x);
            writer.writeFloat(d.y);
            writer.writeFloat(d.z);
            writer.writeFloat(d.yaw);
            writer.writeFloat(d.headYaw);
            writer.writeFloat(d.pitch);
         }

         this.setData(bos.toByteArray());
      } catch (IOException var7) {
      }

   }

   public void decode() {
   }

   public static class MoveEntityData {
      public long eid;
      public float x;
      public float y;
      public float z;
      public float yaw;
      public float headYaw;
      public float pitch;

      public long getEid() {
         return this.eid;
      }

      public float getX() {
         return this.x;
      }

      public float getY() {
         return this.y;
      }

      public float getZ() {
         return this.z;
      }

      public float getYaw() {
         return this.yaw;
      }

      public float getHeadYaw() {
         return this.headYaw;
      }

      public float getPitch() {
         return this.pitch;
      }

      public void setEid(long eid) {
         this.eid = eid;
      }

      public void setX(float x) {
         this.x = x;
      }

      public void setY(float y) {
         this.y = y;
      }

      public void setZ(float z) {
         this.z = z;
      }

      public void setYaw(float yaw) {
         this.yaw = yaw;
      }

      public void setHeadYaw(float headYaw) {
         this.headYaw = headYaw;
      }

      public void setPitch(float pitch) {
         this.pitch = pitch;
      }

      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof MoveEntitiesPacket.MoveEntityData)) {
            return false;
         } else {
            MoveEntitiesPacket.MoveEntityData other = (MoveEntitiesPacket.MoveEntityData)o;
            if (!other.canEqual(this)) {
               return false;
            } else if (this.getEid() != other.getEid()) {
               return false;
            } else if (Float.compare(this.getX(), other.getX()) != 0) {
               return false;
            } else if (Float.compare(this.getY(), other.getY()) != 0) {
               return false;
            } else if (Float.compare(this.getZ(), other.getZ()) != 0) {
               return false;
            } else if (Float.compare(this.getYaw(), other.getYaw()) != 0) {
               return false;
            } else if (Float.compare(this.getHeadYaw(), other.getHeadYaw()) != 0) {
               return false;
            } else {
               return Float.compare(this.getPitch(), other.getPitch()) == 0;
            }
         }
      }

      protected boolean canEqual(Object other) {
         return other instanceof MoveEntitiesPacket.MoveEntityData;
      }

      public int hashCode() {
         int PRIME = true;
         int result = 1;
         long $eid = this.getEid();
         int result = result * 59 + (int)($eid >>> 32 ^ $eid);
         result = result * 59 + Float.floatToIntBits(this.getX());
         result = result * 59 + Float.floatToIntBits(this.getY());
         result = result * 59 + Float.floatToIntBits(this.getZ());
         result = result * 59 + Float.floatToIntBits(this.getYaw());
         result = result * 59 + Float.floatToIntBits(this.getHeadYaw());
         result = result * 59 + Float.floatToIntBits(this.getPitch());
         return result;
      }

      public String toString() {
         return "MoveEntitiesPacket.MoveEntityData(eid=" + this.getEid() + ", x=" + this.getX() + ", y=" + this.getY() + ", z=" + this.getZ() + ", yaw=" + this.getYaw() + ", headYaw=" + this.getHeadYaw() + ", pitch=" + this.getPitch() + ")";
      }
   }
}
