package org.dragonet.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.spacehq.mc.protocol.data.game.ItemStack;
import org.spacehq.opennbt.tag.builtin.CompoundTag;

public class ItemList {
   private List<ItemStack> items;

   public ItemList() {
      this.items = new ArrayList();
   }

   public ItemList(ArrayList<ItemStack> items) {
      this.items = items;
   }

   public ItemList(ItemStack[] items) {
      this.items = Arrays.asList(items);
   }

   public boolean tryToRemove(ItemStack item) {
      ArrayList<ItemStack> original = this.cloneList();
      if (item != null && item.getId() != 0) {
         int toRemove = item.getAmount();

         for(int i = 0; i < this.items.size() && toRemove != 0; ++i) {
            if (this.items.get(i) != null) {
               int typeID = ((ItemStack)this.items.get(i)).getId();
               int damage = ((ItemStack)this.items.get(i)).getData();
               int amount = ((ItemStack)this.items.get(i)).getAmount();
               CompoundTag nbt = ((ItemStack)this.items.get(i)).getNBT();
               if (typeID == item.getId() && damage == item.getData()) {
                  if (amount > toRemove) {
                     this.items.set(i, new ItemStack(typeID, amount - toRemove, damage, nbt));
                     return true;
                  }

                  this.items.set(i, null);
                  toRemove -= amount;
               }
            }
         }

         if (toRemove <= 0) {
            return true;
         } else {
            this.items = original;
            return false;
         }
      } else {
         return true;
      }
   }

   private ArrayList<ItemStack> cloneList() {
      ArrayList<ItemStack> cloned = new ArrayList();
      Iterator var2 = this.items.iterator();

      while(var2.hasNext()) {
         ItemStack item = (ItemStack)var2.next();
         cloned.add(new ItemStack(item.getId(), item.getAmount(), item.getData(), item.getNBT()));
      }

      return cloned;
   }

   public List<ItemStack> getItems() {
      return this.items;
   }

   public ItemStack[] getContents() {
      return (ItemStack[])this.items.toArray(new ItemStack[0]);
   }
}
