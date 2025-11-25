package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class LoginStatusPacket extends PEPacket {
   public static final int LOGIN_SUCCESS = 0;
   public static final int LOGIN_FAILED_CLIENT = 1;
   public static final int LOGIN_FAILED_SERVER = 2;
   public static final int PLAYER_SPAWN = 3;
   public int status;

   public int pid() {
      return 2;
   }

   public void encode() {
      this.setShouldSendImmidate(true);

      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeInt(this.status);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
