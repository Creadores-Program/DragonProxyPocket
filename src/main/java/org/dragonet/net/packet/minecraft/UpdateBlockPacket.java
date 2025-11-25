package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class UpdateBlockPacket extends PEPacket {
   public static final byte FLAG_NONE = 0;
   public static final byte FLAG_NEIGHBORS = 1;
   public static final byte FLAG_NETWORK = 2;
   public static final byte FLAG_NOGRAPHIC = 4;
   public static final byte FLAG_PRIORITY = 8;
   public static final byte FLAG_ALL = 3;
   public static final byte FLAG_ALL_PRIORITY = 11;
   public UpdateBlockPacket.UpdateBlockRecord[] records;

   public int pid() {
      return 19;
   }

   public void encode() {
      this.setShouldSendImmidate(true);

      try {
         this.setChannel(NetworkChannel.CHANNEL_BLOCKS);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         if (this.records == null) {
            writer.writeInt(0);
         } else {
            UpdateBlockPacket.UpdateBlockRecord[] var3 = this.records;
            int var4 = var3.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               UpdateBlockPacket.UpdateBlockRecord rec = var3[var5];
               writer.writeInt(rec.x);
               writer.writeInt(rec.z);
               writer.writeByte(rec.y);
               writer.writeByte(rec.block);
               writer.writeByte((byte)(rec.flags << 4 | rec.meta));
            }
         }

         this.setData(bos.toByteArray());
      } catch (IOException var7) {
      }

   }

   public void decode() {
   }

   public static class UpdateBlockRecord {
      public int x;
      public int z;
      public byte y;
      public byte block;
      public byte meta;
      public byte flags;

      public int getX() {
         return this.x;
      }

      public int getZ() {
         return this.z;
      }

      public byte getY() {
         return this.y;
      }

      public byte getBlock() {
         return this.block;
      }

      public byte getMeta() {
         return this.meta;
      }

      public byte getFlags() {
         return this.flags;
      }

      public void setX(int x) {
         this.x = x;
      }

      public void setZ(int z) {
         this.z = z;
      }

      public void setY(byte y) {
         this.y = y;
      }

      public void setBlock(byte block) {
         this.block = block;
      }

      public void setMeta(byte meta) {
         this.meta = meta;
      }

      public void setFlags(byte flags) {
         this.flags = flags;
      }

      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof UpdateBlockPacket.UpdateBlockRecord)) {
            return false;
         } else {
            UpdateBlockPacket.UpdateBlockRecord other = (UpdateBlockPacket.UpdateBlockRecord)o;
            if (!other.canEqual(this)) {
               return false;
            } else if (this.getX() != other.getX()) {
               return false;
            } else if (this.getZ() != other.getZ()) {
               return false;
            } else if (this.getY() != other.getY()) {
               return false;
            } else if (this.getBlock() != other.getBlock()) {
               return false;
            } else if (this.getMeta() != other.getMeta()) {
               return false;
            } else {
               return this.getFlags() == other.getFlags();
            }
         }
      }

      protected boolean canEqual(Object other) {
         return other instanceof UpdateBlockPacket.UpdateBlockRecord;
      }

      public int hashCode() {
         int result = 1;
         result = result * 59 + this.getX();
         result = result * 59 + this.getZ();
         result = result * 59 + this.getY();
         result = result * 59 + this.getBlock();
         result = result * 59 + this.getMeta();
         result = result * 59 + this.getFlags();
         return result;
      }

      public String toString() {
         return "UpdateBlockPacket.UpdateBlockRecord(x=" + this.getX() + ", z=" + this.getZ() + ", y=" + this.getY() + ", block=" + this.getBlock() + ", meta=" + this.getMeta() + ", flags=" + this.getFlags() + ")";
      }
   }
}
