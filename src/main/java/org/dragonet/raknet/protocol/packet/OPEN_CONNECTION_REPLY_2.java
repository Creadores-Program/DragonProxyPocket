package org.dragonet.raknet.protocol.packet;

import java.net.InetSocketAddress;
import org.dragonet.raknet.RakNet;
import org.dragonet.raknet.protocol.Packet;

public class OPEN_CONNECTION_REPLY_2 extends Packet {
   public static byte ID = 8;
   public long serverID;
   public String clientAddress;
   public int clientPort;
   public short mtuSize;

   public byte getID() {
      return ID;
   }

   public void encode() {
      super.encode();
      this.put(RakNet.MAGIC);
      this.putLong(this.serverID);
      this.putAddress(this.clientAddress, this.clientPort);
      this.putShort(this.mtuSize);
      this.putByte((byte)0);
   }

   public void decode() {
      super.decode();
      this.offset += 16;
      this.serverID = this.getLong();
      InetSocketAddress address = this.getAddress();
      this.clientAddress = address.getHostString();
      this.clientPort = address.getPort();
      this.mtuSize = this.getSignedShort();
   }
}
