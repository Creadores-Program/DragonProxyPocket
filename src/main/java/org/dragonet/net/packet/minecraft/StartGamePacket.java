package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.proxy.utilities.io.PEBinaryReader;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class StartGamePacket extends PEPacket {
   public int seed;
   public byte dimension;
   public int generator;
   public int gamemode;
   public long eid;
   public int spawnX;
   public int spawnY;
   public int spawnZ;
   public float x;
   public float y;
   public float z;

   public int pid() {
      return 9;
   }

   public void encode() {
      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeInt(this.seed);
         writer.writeByte(this.dimension);
         writer.writeInt(this.generator);
         writer.writeInt(this.gamemode);
         writer.writeLong(this.eid);
         writer.writeInt(this.spawnX);
         writer.writeInt(this.spawnY);
         writer.writeInt(this.spawnZ);
         writer.writeFloat(this.x);
         writer.writeFloat(this.y + 1.62F);
         writer.writeFloat(this.z);
         writer.writeByte((byte)1);
         writer.writeByte((byte)1);
         writer.writeByte((byte)0);
         writer.writeString("");
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
      try {
         PEBinaryReader reader = new PEBinaryReader(new ByteArrayInputStream(this.getData()));
         reader.readByte();
         this.seed = reader.readInt();
         this.dimension = reader.readByte();
         this.generator = reader.readInt();
         this.gamemode = reader.readInt();
         this.eid = reader.readLong();
         this.spawnX = reader.readInt();
         this.spawnY = reader.readInt();
         this.spawnZ = reader.readInt();
         this.x = reader.readFloat();
         this.y = reader.readFloat() - 1.62F;
         this.z = reader.readFloat();
      } catch (IOException var2) {
      }

   }
}
