package org.spacehq.mc.protocol.data.game;

import org.spacehq.opennbt.tag.builtin.CompoundTag;

public class ItemStack {
   private int id;
   private int amount;
   private int data;
   private CompoundTag nbt;

   public ItemStack(int id) {
      this(id, 1);
   }

   public ItemStack(int id, int amount) {
      this(id, amount, 0);
   }

   public ItemStack(int id, int amount, int data) {
      this(id, amount, data, (CompoundTag)null);
   }

   public ItemStack(int id, int amount, int data, CompoundTag nbt) {
      this.id = id;
      this.amount = amount;
      this.data = data;
      this.nbt = nbt;
   }

   public int getId() {
      return this.id;
   }

   public int getAmount() {
      return this.amount;
   }

   public int getData() {
      return this.data;
   }

   public CompoundTag getNBT() {
      return this.nbt;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ItemStack itemStack = (ItemStack)o;
         if (this.amount != itemStack.amount) {
            return false;
         } else if (this.data != itemStack.data) {
            return false;
         } else if (this.id != itemStack.id) {
            return false;
         } else {
            if (this.nbt != null) {
               if (!this.nbt.equals(itemStack.nbt)) {
                  return false;
               }
            } else if (itemStack.nbt != null) {
               return false;
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.id;
      result = 31 * result + this.amount;
      result = 31 * result + this.data;
      result = 31 * result + (this.nbt != null ? this.nbt.hashCode() : 0);
      return result;
   }
}
