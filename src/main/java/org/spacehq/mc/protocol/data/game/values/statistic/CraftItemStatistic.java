package org.spacehq.mc.protocol.data.game.values.statistic;

public class CraftItemStatistic implements Statistic {
   private int id;

   public CraftItemStatistic(int id) {
      this.id = id;
   }

   public int getId() {
      return this.id;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         CraftItemStatistic that = (CraftItemStatistic)o;
         return this.id == that.id;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.id;
   }
}
