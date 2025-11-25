package org.spacehq.mc.protocol.data.status;

import java.util.Arrays;
import org.spacehq.mc.auth.data.GameProfile;

public class PlayerInfo {
   private int max;
   private int online;
   private GameProfile[] players;

   public PlayerInfo(int max, int online, GameProfile[] players) {
      this.max = max;
      this.online = online;
      this.players = players;
   }

   public int getMaxPlayers() {
      return this.max;
   }

   public int getOnlinePlayers() {
      return this.online;
   }

   public GameProfile[] getPlayers() {
      return this.players;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         PlayerInfo that = (PlayerInfo)o;
         if (this.max != that.max) {
            return false;
         } else if (this.online != that.online) {
            return false;
         } else {
            return Arrays.equals(this.players, that.players);
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.max;
      result = 31 * result + this.online;
      result = 31 * result + Arrays.hashCode(this.players);
      return result;
   }
}
