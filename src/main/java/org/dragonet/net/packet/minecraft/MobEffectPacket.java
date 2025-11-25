package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.PocketPotionEffect;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class MobEffectPacket extends PEPacket {
   public long eid;
   public MobEffectPacket.EffectAction action;
   public PocketPotionEffect effect;

   public int pid() {
      return 25;
   }

   public void encode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_WORLD_EVENTS);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeLong(this.eid);
         writer.writeByte(this.action.getActionID());
         writer.writeByte((byte)(this.effect.getEffect() & 255));
         writer.writeByte((byte)(this.effect.getAmpilifier() & 255));
         writer.writeByte((byte)(this.effect.isParticles() ? 1 : 0));
         writer.writeInt(this.effect.getDuration());
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }

   public static enum EffectAction {
      ADD(1),
      MODIFY(2),
      REMOVE(3);

      private int id;

      private EffectAction(int id) {
         this.id = id;
      }

      public byte getActionID() {
         return (byte)(this.id & 255);
      }
   }
}
