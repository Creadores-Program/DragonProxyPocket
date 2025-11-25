package org.dragonet.raknet.protocol.packet;

import java.net.InetSocketAddress;
import org.dragonet.raknet.RakNet;
import org.dragonet.raknet.protocol.Packet;

public class OPEN_CONNECTION_REQUEST_2 extends Packet {
   public static byte ID = 7;
   public long clientID;
   public String serverAddress;
   public int serverPort;
   public short mtuSize;

   public byte getID() {
      return ID;
   }

   public void encode() {
      super.encode();
      this.put(RakNet.MAGIC);
      this.putAddress(this.serverAddress, this.serverPort);
      this.putShort(this.mtuSize);
      this.putLong(this.clientID);
   }

   public void decode() {
      super.decode();
      this.offset += 16;
      InetSocketAddress address = this.getAddress();
      this.serverAddress = address.getHostString();
      this.serverPort = address.getPort();
      this.mtuSize = this.getSignedShort();
      this.clientID = this.getLong();
   }
}
