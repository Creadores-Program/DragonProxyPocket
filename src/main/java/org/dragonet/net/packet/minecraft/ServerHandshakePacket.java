package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class ServerHandshakePacket extends PEPacket {
   public InetAddress addr;
   public short port;
   public long session;
   public long session2;

   public int pid() {
      return 16;
   }

   public void encode() {
      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeAddress(this.addr, this.port);
         writer.writeShort((short)0);
         writer.writeAddress(Inet4Address.getByName("127.0.0.1"), (short)0);
         writer.writeAddress(Inet4Address.getByName("0.0.0.0"), (short)0);
         writer.writeAddress(Inet4Address.getByName("0.0.0.0"), (short)0);
         writer.writeAddress(Inet4Address.getByName("0.0.0.0"), (short)0);
         writer.writeAddress(Inet4Address.getByName("0.0.0.0"), (short)0);
         writer.writeAddress(Inet4Address.getByName("0.0.0.0"), (short)0);
         writer.writeAddress(Inet4Address.getByName("0.0.0.0"), (short)0);
         writer.writeAddress(Inet4Address.getByName("0.0.0.0"), (short)0);
         writer.writeAddress(Inet4Address.getByName("0.0.0.0"), (short)0);
         writer.writeAddress(Inet4Address.getByName("0.0.0.0"), (short)0);
         writer.writeLong(this.session);
         writer.writeLong(this.session2);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }
}
