package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.dragonet.inventory.PEInventorySlot;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryReader;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class WindowItemsPacket extends PEPacket {
   public static final WindowItemsPacket CREATIVE_INVENTORY = new WindowItemsPacket();
   public byte windowID;
   public PEInventorySlot[] slots;
   public int[] hotbar;

   public int pid() {
      return 46;
   }

   public void encode() {
      this.setShouldSendImmidate(true);

      try {
         this.setChannel(NetworkChannel.CHANNEL_PRIORITY);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeByte(this.windowID);
         writer.writeShort((short)(this.slots.length & '\uffff'));
         PEInventorySlot[] var3 = this.slots;
         int var4 = var3.length;

         int var5;
         for(var5 = 0; var5 < var4; ++var5) {
            PEInventorySlot slot = var3[var5];
            PEInventorySlot.writeSlot(writer, slot);
         }

         if (this.hotbar != null && this.windowID == 0 && this.hotbar.length > 0) {
            writer.writeShort((short)(this.hotbar.length & '\uffff'));
            int[] var8 = this.hotbar;
            var4 = var8.length;

            for(var5 = 0; var5 < var4; ++var5) {
               int slot = var8[var5];
               writer.writeInt(slot);
            }
         } else {
            writer.writeShort((short)0);
         }

         this.setData(bos.toByteArray());
      } catch (IOException var7) {
      }

   }

   public void decode() {
      try {
         PEBinaryReader reader = new PEBinaryReader(new ByteArrayInputStream(this.getData()));
         reader.readByte();
         this.windowID = reader.readByte();
         short cnt = reader.readShort();
         this.slots = new PEInventorySlot[cnt];

         for(int i = 0; i < cnt; ++i) {
            this.slots[i] = PEInventorySlot.readSlot(reader);
         }

         if (this.windowID == 0) {
            short hcnt = reader.readShort();
            this.hotbar = new int[hcnt];

            for(int i = 0; i < hcnt; ++i) {
               this.hotbar[i] = reader.readInt();
            }
         }

         this.setLength(reader.totallyRead());
      } catch (IOException var5) {
      }

   }

   static {
      CREATIVE_INVENTORY.windowID = 121;
      ArrayList<PEInventorySlot> slots = new ArrayList();
      slots.add(new PEInventorySlot((short)1, (byte)1, (short)0));
      CREATIVE_INVENTORY.slots = (PEInventorySlot[])slots.toArray(new PEInventorySlot[0]);
   }
}
