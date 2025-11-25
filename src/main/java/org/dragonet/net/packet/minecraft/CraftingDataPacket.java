package org.dragonet.net.packet.minecraft;

import java.util.ArrayList;

public class CraftingDataPacket extends PEPacket {
   public static final int ENTRY_SHAPELESS = 0;
   public static final int ENTRY_SHAPED = 1;
   public static final int ENTRY_FURNACE = 2;
   public static final int ENTRY_FURNACE_DATA = 3;
   public static final int ENTRY_ENCHANT = 4;
   public ArrayList<Object> recipies;
   public boolean cleanRecipies;
   private int enchants;

   public CraftingDataPacket(boolean cleanRecipies) {
      this.cleanRecipies = cleanRecipies;
   }

   public int pid() {
      return 47;
   }

   public void encode() {
   }

   public void decode() {
   }
}
