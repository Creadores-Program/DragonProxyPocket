package org.dragonet.net.packet.minecraft;

import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.net.packet.BinaryPacket;

public abstract class PEPacket extends BinaryPacket {
   private int length;
   private NetworkChannel channel;
   private boolean shouldSendImmidate;

   public PEPacket() {
      this.channel = NetworkChannel.CHANNEL_NONE;
   }

   public abstract int pid();

   public abstract void encode();

   public abstract void decode();

   public final void setLength(int length) {
      this.length = length;
   }

   public final int getLength() {
      return this.length;
   }

   public NetworkChannel getChannel() {
      return this.channel;
   }

   public void setChannel(NetworkChannel channel) {
      this.channel = channel;
   }

   public boolean isShouldSendImmidate() {
      return this.shouldSendImmidate;
   }

   public void setShouldSendImmidate(boolean shouldSendImmidate) {
      this.shouldSendImmidate = shouldSendImmidate;
   }
}
