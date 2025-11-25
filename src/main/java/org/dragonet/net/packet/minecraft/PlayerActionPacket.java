package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryReader;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class PlayerActionPacket extends PEPacket {
   public static final byte ACTION_START_BREAK = 0;
   public static final byte ACTION_CANCEL_BREAK = 1;
   public static final byte ACTION_FINISH_BREAK = 2;
   public static final byte ACTION_RELEASE_ITEM = 5;
   public static final byte ACTION_STOP_SLEEPING = 6;
   public static final byte ACTION_RESPAWN = 7;
   public static final byte ACTION_JUMP = 8;
   public static final byte ACTION_START_SPRINT = 9;
   public static final byte ACTION_STOP_SPRINT = 10;
   public static final byte ACTION_START_SNEAK = 11;
   public static final byte ACTION_STOP_SNEAK = 12;
   public static final byte ACTION_DIMENSION_CHANGE = 13;
   public long eid;
   public int action;
   public int x;
   public int y;
   public int z;
   public int face;

   public PlayerActionPacket(byte[] data) {
      this.setData(data);
   }

   public int pid() {
      return 32;
   }

   public void encode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_WORLD_EVENTS);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeLong(this.eid);
         writer.writeInt(this.action);
         writer.writeInt(this.x);
         writer.writeInt(this.y);
         writer.writeInt(this.z);
         writer.writeInt(this.face);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
      try {
         PEBinaryReader reader = new PEBinaryReader(new ByteArrayInputStream(this.getData()));
         reader.readByte();
         this.eid = reader.readLong();
         this.action = reader.readInt();
         this.x = reader.readInt();
         this.y = reader.readInt();
         this.z = reader.readInt();
         this.face = reader.readInt();
         this.setLength(reader.totallyRead());
      } catch (IOException var2) {
      }

   }
}
