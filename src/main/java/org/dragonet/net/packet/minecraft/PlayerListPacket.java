package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.UUID;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryReader;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class PlayerListPacket extends PEPacket {
   public boolean isAdding = true;
   public ArrayList<PlayerListPacket.PlayerInfo> players = new ArrayList();

   public PlayerListPacket() {
   }

   public PlayerListPacket(PlayerListPacket.PlayerInfo... playerArray) {
      PlayerListPacket.PlayerInfo[] var2 = playerArray;
      int var3 = playerArray.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         PlayerListPacket.PlayerInfo info = var2[var4];
         this.players.add(info);
      }

   }

   public int pid() {
      return 56;
   }

   public void encode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_TEXT);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         if (this.isAdding) {
            writer.writeByte((byte)0);
         } else {
            writer.writeByte((byte)1);
         }

         writer.writeInt(this.players.size());
         Iterator var3 = this.players.iterator();

         while(var3.hasNext()) {
            PlayerListPacket.PlayerInfo info = (PlayerListPacket.PlayerInfo)var3.next();
            info.encode(writer, this.isAdding);
         }

         this.setData(bos.toByteArray());
      } catch (IOException var5) {
      }

   }

   public void decode() {
   }

   public static class PlayerInfo {
      public UUID uuid;
      public long eid;
      public String name;
      public String skinName;
      public byte[] skin;

      public PlayerInfo(UUID uuid, long eid, String name, String skinName, byte[] skin) {
         this.uuid = uuid;
         this.eid = eid;
         this.name = name;
         this.skinName = skinName;
         this.skin = skin;
      }

      public void encode(PEBinaryWriter out, boolean isAdding) throws IOException {
         out.writeUUID(this.uuid);
         if (isAdding) {
            out.writeLong(this.eid);
            out.writeString(this.name);
            out.writeString(this.skinName);
            out.writeShort((short)(this.skin.length & '\uffff'));
            out.write(this.skin);
         }
      }

      public static PlayerListPacket.PlayerInfo decode(PEBinaryReader reader) throws IOException {
         UUID uuid = reader.readUUID();
         long eid = reader.readLong();
         String name = reader.readString();
         String skinName = reader.readString();
         short skinLen = reader.readShort();
         byte[] skin = reader.read(skinLen);
         return new PlayerListPacket.PlayerInfo(uuid, eid, name, skinName, skin);
      }

      public UUID getUuid() {
         return this.uuid;
      }

      public long getEid() {
         return this.eid;
      }

      public String getName() {
         return this.name;
      }

      public String getSkinName() {
         return this.skinName;
      }

      public byte[] getSkin() {
         return this.skin;
      }

      public void setUuid(UUID uuid) {
         this.uuid = uuid;
      }

      public void setEid(long eid) {
         this.eid = eid;
      }

      public void setName(String name) {
         this.name = name;
      }

      public void setSkinName(String skinName) {
         this.skinName = skinName;
      }

      public void setSkin(byte[] skin) {
         this.skin = skin;
      }

      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof PlayerListPacket.PlayerInfo)) {
            return false;
         } else {
            PlayerListPacket.PlayerInfo other = (PlayerListPacket.PlayerInfo)o;
            if (!other.canEqual(this)) {
               return false;
            } else {
               label55: {
                  Object this$uuid = this.getUuid();
                  Object other$uuid = other.getUuid();
                  if (this$uuid == null) {
                     if (other$uuid == null) {
                        break label55;
                     }
                  } else if (this$uuid.equals(other$uuid)) {
                     break label55;
                  }

                  return false;
               }

               if (this.getEid() != other.getEid()) {
                  return false;
               } else {
                  Object this$name = this.getName();
                  Object other$name = other.getName();
                  if (this$name == null) {
                     if (other$name != null) {
                        return false;
                     }
                  } else if (!this$name.equals(other$name)) {
                     return false;
                  }

                  Object this$skinName = this.getSkinName();
                  Object other$skinName = other.getSkinName();
                  if (this$skinName == null) {
                     if (other$skinName != null) {
                        return false;
                     }
                  } else if (!this$skinName.equals(other$skinName)) {
                     return false;
                  }

                  if (!Arrays.equals(this.getSkin(), other.getSkin())) {
                     return false;
                  } else {
                     return true;
                  }
               }
            }
         }
      }

      protected boolean canEqual(Object other) {
         return other instanceof PlayerListPacket.PlayerInfo;
      }

      public int hashCode() {
         int PRIME = true;
         int result = 1;
         Object $uuid = this.getUuid();
         int result = result * 59 + ($uuid == null ? 0 : $uuid.hashCode());
         long $eid = this.getEid();
         result = result * 59 + (int)($eid >>> 32 ^ $eid);
         Object $name = this.getName();
         result = result * 59 + ($name == null ? 0 : $name.hashCode());
         Object $skinName = this.getSkinName();
         result = result * 59 + ($skinName == null ? 0 : $skinName.hashCode());
         result = result * 59 + Arrays.hashCode(this.getSkin());
         return result;
      }

      public String toString() {
         return "PlayerListPacket.PlayerInfo(uuid=" + this.getUuid() + ", eid=" + this.getEid() + ", name=" + this.getName() + ", skinName=" + this.getSkinName() + ", skin=" + Arrays.toString(this.getSkin()) + ")";
      }
   }
}
