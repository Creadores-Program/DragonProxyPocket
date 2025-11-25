package org.dragonet.inventory;

public final class InventoryType {
   public static final class SlotSize {
      public static final int CHEST = 27;
      public static final int DOUBLE_CHEST = 54;
      public static final int PLAYER = 36;
      public static final int FURNACE = 3;
      public static final int CRAFTING = 5;
      public static final int WORKBENCH = 10;
      public static final int STONECUTTER = 10;
      public static final int ANVIL = 3;
   }

   public static final class PCInventory {
      public static final byte CHEST = 0;
      public static final byte WORKBENCH = 1;
      public static final byte FURNANCE = 2;
      public static final byte DISPENSER = 3;
      public static final byte ENCHANTING_TABLE = 4;
      public static final byte BREWING_STAND = 5;
      public static final byte NPC_TRADE = 6;
      public static final byte BEACON = 7;
      public static final byte ANVIL = 8;
      public static final byte HOPPER = 9;
      public static final byte DROPPER = 16;
      public static final byte HORSE = 10;

      public static final byte fromString(String str) {
         if (str.equals("minecraft:chest")) {
            return 0;
         } else if (str.equals("minecraft:crafting_table")) {
            return 1;
         } else if (str.equals("minecraft:furnance")) {
            return 2;
         } else if (str.equals("minecraft:dispenser")) {
            return 3;
         } else if (str.equals("minecraft:enchanting_table")) {
            return 4;
         } else if (str.equals("minecraft:brewing_stand")) {
            return 5;
         } else if (str.equals("minecraft:villager")) {
            return 6;
         } else if (str.equals("minecraft:beacon")) {
            return 7;
         } else if (str.equals("minecraft:hopper")) {
            return 9;
         } else if (str.equals("minecraft:dropper")) {
            return 16;
         } else {
            return (byte)(str.equals("EntityHorse") ? 10 : -1);
         }
      }
   }

   public static final class PEInventory {
      public static final byte CHEST = 0;
      public static final byte DOUBLE_CHEST = 0;
      public static final byte PLAYER = 0;
      public static final byte FURNACE = 2;
      public static final byte CRAFTING = 1;
      public static final byte WORKBENCH = 1;
      public static final byte STONECUTTER = 1;
      public static final byte BREWING_STAND = 5;
      public static final byte ANVIL = 6;
      public static final byte ENCHANT_TABLE = 4;

      public static final byte toPEInventory(byte bytePC, int slots) {
         switch(bytePC) {
         case 0:
            if (slots > 64) {
               return 0;
            }

            return 0;
         case 1:
            return 1;
         case 2:
            return 2;
         default:
            return -1;
         }
      }
   }
}
