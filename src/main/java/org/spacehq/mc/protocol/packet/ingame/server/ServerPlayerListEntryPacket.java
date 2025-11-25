package org.spacehq.mc.protocol.packet.ingame.server;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;
import org.spacehq.mc.auth.data.GameProfile;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.PlayerListEntry;
import org.spacehq.mc.protocol.data.game.values.PlayerListEntryAction;
import org.spacehq.mc.protocol.data.game.values.entity.player.GameMode;
import org.spacehq.mc.protocol.data.message.Message;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerPlayerListEntryPacket implements Packet {
   private PlayerListEntryAction action;
   private PlayerListEntry[] entries;

   private ServerPlayerListEntryPacket() {
   }

   public ServerPlayerListEntryPacket(PlayerListEntryAction action, PlayerListEntry[] entries) {
      this.action = action;
      this.entries = entries;
   }

   public PlayerListEntryAction getAction() {
      return this.action;
   }

   public PlayerListEntry[] getEntries() {
      return this.entries;
   }

   public void read(NetInput in) throws IOException {
      this.action = (PlayerListEntryAction)MagicValues.key(PlayerListEntryAction.class, in.readVarInt());
      this.entries = new PlayerListEntry[in.readVarInt()];

      for(int count = 0; count < this.entries.length; ++count) {
         UUID uuid = in.readUUID();
         GameProfile profile;
         if (this.action == PlayerListEntryAction.ADD_PLAYER) {
            profile = new GameProfile(uuid, in.readString());
         } else {
            profile = new GameProfile(uuid, (String)null);
         }

         PlayerListEntry entry = null;
         switch(this.action) {
         case ADD_PLAYER:
            int properties = in.readVarInt();

            for(int index = 0; index < properties; ++index) {
               String propertyName = in.readString();
               String value = in.readString();
               String signature = null;
               if (in.readBoolean()) {
                  signature = in.readString();
               }

               profile.getProperties().add(new GameProfile.Property(propertyName, value, signature));
            }

            GameMode gameMode = (GameMode)MagicValues.key(GameMode.class, in.readVarInt());
            int ping = in.readVarInt();
            Message displayName = null;
            if (in.readBoolean()) {
               displayName = Message.fromString(in.readString());
            }

            entry = new PlayerListEntry(profile, gameMode, ping, displayName);
            break;
         case UPDATE_GAMEMODE:
            GameMode mode = (GameMode)MagicValues.key(GameMode.class, in.readVarInt());
            entry = new PlayerListEntry(profile, mode);
            break;
         case UPDATE_LATENCY:
            int png = in.readVarInt();
            entry = new PlayerListEntry(profile, png);
            break;
         case UPDATE_DISPLAY_NAME:
            Message disp = null;
            if (in.readBoolean()) {
               disp = Message.fromString(in.readString());
            }

            new PlayerListEntry(profile, disp);
         case REMOVE_PLAYER:
            entry = new PlayerListEntry(profile);
         }

         this.entries[count] = entry;
      }

   }

   public void write(NetOutput out) throws IOException {
      out.writeVarInt((Integer)MagicValues.value(Integer.class, this.action));
      out.writeVarInt(this.entries.length);
      PlayerListEntry[] var2 = this.entries;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         PlayerListEntry entry = var2[var4];
         out.writeUUID(entry.getProfile().getId());
         switch(this.action) {
         case ADD_PLAYER:
            out.writeString(entry.getProfile().getName());
            out.writeVarInt(entry.getProfile().getProperties().size());
            Iterator var6 = entry.getProfile().getProperties().iterator();

            while(var6.hasNext()) {
               GameProfile.Property property = (GameProfile.Property)var6.next();
               out.writeString(property.getName());
               out.writeString(property.getValue());
               out.writeBoolean(property.hasSignature());
               if (property.hasSignature()) {
                  out.writeString(property.getSignature());
               }
            }

            out.writeVarInt((Integer)MagicValues.value(Integer.class, entry.getGameMode()));
            out.writeVarInt(entry.getPing());
            out.writeBoolean(entry.getDisplayName() != null);
            if (entry.getDisplayName() != null) {
               out.writeString(entry.getDisplayName().toJsonString());
            }
            break;
         case UPDATE_GAMEMODE:
            out.writeVarInt((Integer)MagicValues.value(Integer.class, entry.getGameMode()));
            break;
         case UPDATE_LATENCY:
            out.writeVarInt(entry.getPing());
            break;
         case UPDATE_DISPLAY_NAME:
            out.writeBoolean(entry.getDisplayName() != null);
            if (entry.getDisplayName() != null) {
               out.writeString(entry.getDisplayName().toJsonString());
            }
         case REMOVE_PLAYER:
         }
      }

   }

   public boolean isPriority() {
      return false;
   }
}
