package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.entity.meta.EntityMetaData;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class AddEntityPacket extends PEPacket {
   public long eid;
   public int type;
   public float x;
   public float y;
   public float z;
   public float speedX;
   public float speedY;
   public float speedZ;
   public float yaw;
   public float pitch;
   public EntityMetaData meta;
   public AddEntityPacket.EntityLink[] links;

   public int pid() {
      return 11;
   }

   public void encode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_ENTITY_SPAWNING);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeLong(this.eid);
         writer.writeInt(this.type);
         writer.writeFloat(this.x);
         writer.writeFloat(this.y);
         writer.writeFloat(this.z);
         writer.writeFloat(this.speedX);
         writer.writeFloat(this.speedY);
         writer.writeFloat(this.speedZ);
         writer.writeFloat(this.yaw);
         writer.writeFloat(this.pitch);
         writer.write(this.meta.encode());
         writer.writeShort((short)(this.links == null ? 0 : this.links.length));
         if (this.links != null) {
            AddEntityPacket.EntityLink[] var3 = this.links;
            int var4 = var3.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               AddEntityPacket.EntityLink link = var3[var5];
               link.writeTo(writer);
            }
         }

         this.setData(bos.toByteArray());
      } catch (IOException var7) {
      }

   }

   public void decode() {
   }

   public static class EntityLink {
      public long eid1;
      public long eid2;
      public byte flag;

      public void writeTo(PEBinaryWriter writer) throws IOException {
         writer.writeLong(this.eid1);
         writer.writeLong(this.eid2);
         writer.writeByte(this.flag);
      }

      public long getEid1() {
         return this.eid1;
      }

      public long getEid2() {
         return this.eid2;
      }

      public byte getFlag() {
         return this.flag;
      }

      public void setEid1(long eid1) {
         this.eid1 = eid1;
      }

      public void setEid2(long eid2) {
         this.eid2 = eid2;
      }

      public void setFlag(byte flag) {
         this.flag = flag;
      }

      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof AddEntityPacket.EntityLink)) {
            return false;
         } else {
            AddEntityPacket.EntityLink other = (AddEntityPacket.EntityLink)o;
            if (!other.canEqual(this)) {
               return false;
            } else if (this.getEid1() != other.getEid1()) {
               return false;
            } else if (this.getEid2() != other.getEid2()) {
               return false;
            } else {
               return this.getFlag() == other.getFlag();
            }
         }
      }

      protected boolean canEqual(Object other) {
         return other instanceof AddEntityPacket.EntityLink;
      }

      public int hashCode() {
         int result = 1;
         long $eid1 = this.getEid1();
         result = result * 59 + (int)($eid1 >>> 32 ^ $eid1);
         long $eid2 = this.getEid2();
         result = result * 59 + (int)($eid2 >>> 32 ^ $eid2);
         result = result * 59 + this.getFlag();
         return result;
      }

      public String toString() {
         return "AddEntityPacket.EntityLink(eid1=" + this.getEid1() + ", eid2=" + this.getEid2() + ", flag=" + this.getFlag() + ")";
      }
   }
}
