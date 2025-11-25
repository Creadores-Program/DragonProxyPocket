package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class SetEntityMotionPacket extends PEPacket {
   public SetEntityMotionPacket.EntityMotionData[] motions;

   public int pid() {
      return 35;
   }

   public void encode() {
      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeInt(this.motions.length);
         SetEntityMotionPacket.EntityMotionData[] var3 = this.motions;
         int var4 = var3.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            SetEntityMotionPacket.EntityMotionData d = var3[var5];
            if (d != null) {
               writer.writeLong(d.eid);
               writer.writeFloat(d.motionX);
               writer.writeFloat(d.motionY);
               writer.writeFloat(d.motionZ);
            }
         }

         this.setData(bos.toByteArray());
      } catch (IOException var7) {
      }

   }

   public void decode() {
   }

   public static class EntityMotionData {
      public long eid;
      public float motionX;
      public float motionY;
      public float motionZ;
   }
}
