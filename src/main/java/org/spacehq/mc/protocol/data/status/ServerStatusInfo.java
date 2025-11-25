package org.spacehq.mc.protocol.data.status;

import java.awt.image.BufferedImage;
import org.spacehq.mc.protocol.data.message.Message;

public class ServerStatusInfo {
   private VersionInfo version;
   private PlayerInfo players;
   private Message description;
   private BufferedImage icon;

   public ServerStatusInfo(VersionInfo version, PlayerInfo players, Message description, BufferedImage icon) {
      this.version = version;
      this.players = players;
      this.description = description;
      this.icon = icon;
   }

   public VersionInfo getVersionInfo() {
      return this.version;
   }

   public PlayerInfo getPlayerInfo() {
      return this.players;
   }

   public Message getDescription() {
      return this.description;
   }

   public BufferedImage getIcon() {
      return this.icon;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ServerStatusInfo that = (ServerStatusInfo)o;
         if (!this.description.equals(that.description)) {
            return false;
         } else {
            label32: {
               if (this.icon != null) {
                  if (this.icon.equals(that.icon)) {
                     break label32;
                  }
               } else if (that.icon == null) {
                  break label32;
               }

               return false;
            }

            if (!this.players.equals(that.players)) {
               return false;
            } else {
               return this.version.equals(that.version);
            }
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.version.hashCode();
      result = 31 * result + this.players.hashCode();
      result = 31 * result + this.description.hashCode();
      result = 31 * result + (this.icon != null ? this.icon.hashCode() : 0);
      return result;
   }
}
