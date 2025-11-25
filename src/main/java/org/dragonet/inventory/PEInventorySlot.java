package org.dragonet.inventory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import org.dragonet.proxy.nbt.PENBT;
import org.dragonet.proxy.nbt.tag.CompoundTag;
import org.dragonet.proxy.network.translator.ItemBlockTranslator;
import org.dragonet.proxy.utilities.io.PEBinaryReader;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;
import org.spacehq.mc.protocol.data.game.ItemStack;

public class PEInventorySlot {
   public static final PEInventorySlot AIR = new PEInventorySlot();
   public short id;
   public byte count;
   public short meta;
   public CompoundTag nbt;

   public PEInventorySlot() {
      this((short)0, (byte)0, (short)0);
   }

   public PEInventorySlot(short id, byte count, short meta) {
      this.id = id;
      this.count = count;
      this.meta = meta;
      this.nbt = new CompoundTag("");
   }

   public PEInventorySlot(short id, byte count, short meta, CompoundTag nbt) {
      this.id = id;
      this.count = count;
      this.meta = meta;
      this.nbt = nbt;
   }

   public static PEInventorySlot readSlot(PEBinaryReader reader) throws IOException {
      short id = (short)(reader.readShort() & '\uffff');
      if (id <= 0) {
         return new PEInventorySlot((short)0, (byte)0, (short)0);
      } else {
         byte count = reader.readByte();
         short meta = reader.readShort();
         if (meta == -1) {
            meta = 0;
         }

         reader.switchEndianness();
         short lNbt = reader.readShort();
         reader.switchEndianness();
         if (lNbt <= 0) {
            return new PEInventorySlot(id, count, meta);
         } else {
            byte[] nbtData = reader.read(lNbt);
            CompoundTag nbt = PENBT.read((InputStream)(new DataInputStream(new ByteArrayInputStream(nbtData))), ByteOrder.LITTLE_ENDIAN);
            return new PEInventorySlot(id, count, meta, nbt);
         }
      }
   }

   public static void writeSlot(PEBinaryWriter writer, PEInventorySlot slot) throws IOException {
      if (slot != null && slot.id != 0) {
         writer.writeShort(slot.id);
         writer.writeByte(slot.count);
         writer.writeShort(slot.meta);
         if (slot.nbt == null) {
            writer.writeShort((short)0);
         } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PENBT.write(slot.nbt, (OutputStream)(new DataOutputStream(bos)), ByteOrder.LITTLE_ENDIAN);
            byte[] nbtdata = bos.toByteArray();
            writer.switchEndianness();
            writer.writeShort((short)(nbtdata.length & '\uffff'));
            writer.switchEndianness();
            writer.write(nbtdata);
         }

      } else {
         writer.writeShort((short)0);
      }
   }

   public static PEInventorySlot fromItemStack(ItemStack item) {
      return ItemBlockTranslator.translateToPE(item);
   }

   public String toString() {
      return "{PE Item: ID=" + this.id + ", Count=" + (this.count & 255) + ", Data=" + this.meta + "}";
   }
}
