package org.spacehq.mc.protocol.packet.ingame.server.window;

import java.io.IOException;
import org.spacehq.mc.protocol.data.game.ItemStack;
import org.spacehq.mc.protocol.util.NetUtil;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerWindowItemsPacket implements Packet {
   private int windowId;
   private ItemStack[] items;

   private ServerWindowItemsPacket() {
   }

   public ServerWindowItemsPacket(int windowId, ItemStack[] items) {
      this.windowId = windowId;
      this.items = items;
   }

   public int getWindowId() {
      return this.windowId;
   }

   public ItemStack[] getItems() {
      return this.items;
   }

   public void read(NetInput in) throws IOException {
      this.windowId = in.readUnsignedByte();
      this.items = new ItemStack[in.readShort()];

      for(int index = 0; index < this.items.length; ++index) {
         this.items[index] = NetUtil.readItem(in);
      }

   }

   public void write(NetOutput out) throws IOException {
      out.writeByte(this.windowId);
      out.writeShort(this.items.length);
      ItemStack[] var2 = this.items;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         ItemStack item = var2[var4];
         NetUtil.writeItem(out, item);
      }

   }

   public boolean isPriority() {
      return false;
   }
}
