package org.spacehq.mc.protocol.data.game.values.world.block;

import org.spacehq.mc.protocol.data.game.Position;

public class BlockChangeRecord {
   private Position position;
   private int id;
   private int data;

   public BlockChangeRecord(Position position, int id, int data) {
      this.position = position;
      this.id = id;
      this.data = data;
   }

   public Position getPosition() {
      return this.position;
   }

   public int getId() {
      return this.id;
   }

   public int getData() {
      return this.data;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         BlockChangeRecord record = (BlockChangeRecord)o;
         if (this.id != record.id) {
            return false;
         } else if (this.data != record.data) {
            return false;
         } else {
            return this.position.equals(record.position);
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.position.hashCode();
      result = 31 * result + this.id;
      result = 31 * result + this.data;
      return result;
   }
}
