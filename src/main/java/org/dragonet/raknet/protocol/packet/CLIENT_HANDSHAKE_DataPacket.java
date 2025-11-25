package org.dragonet.raknet.protocol.packet;

import java.net.InetSocketAddress;
import org.dragonet.raknet.protocol.Packet;

public class CLIENT_HANDSHAKE_DataPacket extends Packet {
   public static byte ID = 19;
   public String address;
   public int port;
   public InetSocketAddress[] systemAddresses = new InetSocketAddress[10];
   public long sendPing;
   public long sendPong;

   public byte getID() {
      return ID;
   }

   public void encode() {
   }

   public void decode() {
      super.decode();
      InetSocketAddress addr = this.getAddress();
      this.address = addr.getHostString();
      this.port = addr.getPort();

      for(int i = 0; i < 10; ++i) {
         this.systemAddresses[i] = this.getAddress();
      }

      this.sendPing = this.getLong();
      this.sendPong = this.getLong();
   }
}
