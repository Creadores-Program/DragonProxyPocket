package org.dragonet.raknet.protocol.packet;

import java.net.InetSocketAddress;
import org.dragonet.raknet.protocol.Packet;

public class SERVER_HANDSHAKE_DataPacket extends Packet {
   public static byte ID = 16;
   public String address;
   public int port;
   public InetSocketAddress[] systemAddresses = new InetSocketAddress[]{new InetSocketAddress("127.0.0.1", 0), new InetSocketAddress("0.0.0.0", 0), new InetSocketAddress("0.0.0.0", 0), new InetSocketAddress("0.0.0.0", 0), new InetSocketAddress("0.0.0.0", 0), new InetSocketAddress("0.0.0.0", 0), new InetSocketAddress("0.0.0.0", 0), new InetSocketAddress("0.0.0.0", 0), new InetSocketAddress("0.0.0.0", 0), new InetSocketAddress("0.0.0.0", 0)};
   public long sendPing;
   public long sendPong;

   public byte getID() {
      return ID;
   }

   public void encode() {
      super.encode();
      this.putAddress(new InetSocketAddress(this.address, this.port));
      this.putShort(0);

      for(int i = 0; i < 10; ++i) {
         this.putAddress(this.systemAddresses[i]);
      }

      this.putLong(this.sendPing);
      this.putLong(this.sendPong);
   }

   public void decode() {
      super.decode();
   }
}
