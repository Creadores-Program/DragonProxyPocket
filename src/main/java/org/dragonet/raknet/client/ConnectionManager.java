package org.dragonet.raknet.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.dragonet.raknet.protocol.EncapsulatedPacket;
import org.dragonet.raknet.protocol.Packet;
import org.dragonet.raknet.protocol.packet.ACK;
import org.dragonet.raknet.protocol.packet.ADVERTISE_SYSTEM;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_0;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_1;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_2;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_3;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_4;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_5;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_6;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_7;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_8;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_9;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_A;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_B;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_C;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_D;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_E;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_F;
import org.dragonet.raknet.protocol.packet.NACK;
import org.dragonet.raknet.protocol.packet.OPEN_CONNECTION_REPLY_1;
import org.dragonet.raknet.protocol.packet.OPEN_CONNECTION_REPLY_2;
import org.dragonet.raknet.protocol.packet.OPEN_CONNECTION_REQUEST_1;
import org.dragonet.raknet.protocol.packet.OPEN_CONNECTION_REQUEST_2;
import org.dragonet.raknet.protocol.packet.UNCONNECTED_PING;
import org.dragonet.raknet.protocol.packet.UNCONNECTED_PING_OPEN_CONNECTIONS;
import org.dragonet.raknet.protocol.packet.UNCONNECTED_PONG;

public class ConnectionManager {
   protected Map<Byte, Class<? extends Packet>> packetPool = new ConcurrentHashMap();
   protected JRakLibClient client;
   protected UDPClientSocket socket;
   protected int receiveBytes = 0;
   protected int sendBytes = 0;
   protected boolean shutdown = false;
   protected int ticks = 0;
   protected long lastMeasure;
   protected Connection connection;
   public boolean portChecking = false;

   public ConnectionManager(JRakLibClient client, UDPClientSocket socket) {
      this.client = client;
      this.socket = socket;
      this.registerPackets();

      try {
         if (!this.connect(1447, 4)) {
            client.pushMainToThreadPacket(new byte[]{126});
         } else {
            this.run();
         }
      } catch (IOException var4) {
         var4.printStackTrace();
         client.pushMainToThreadPacket(new byte[]{127});
      }

   }

   private boolean connect(int payloadSize, int packets) throws IOException {
      System.out.println("[DEBUG] Client connecting, PLOAD=" + payloadSize + ", TRIES=" + packets);

      for(int i = 0; i < packets; ++i) {
         System.out.println("[DEBUG] Trying to connect, TRY=" + i + "... ");
         OPEN_CONNECTION_REQUEST_1 request1 = new OPEN_CONNECTION_REQUEST_1();
         request1.protocol = 6;
         request1.mtuSize = (short)payloadSize;
         request1.encode();
         this.socket.writePacket(request1.buffer, this.client.getServerEndpoint());
         DatagramPacket response = this.socket.readPacketBlocking(500);
         if (response != null && response.getData()[0] == OPEN_CONNECTION_REPLY_1.ID) {
            this.connection = new Connection(this, payloadSize);
            Packet packet = this.getPacketFromPool(response.getData()[0]);
            packet.buffer = response.getData();
            this.connection.handlePacket(packet);
            return true;
         }
      }

      if (payloadSize == 1447) {
         return this.connect(1155, 4);
      } else if (payloadSize == 1155) {
         return this.connect(531, 5);
      } else {
         return false;
      }
   }

   public void run() {
      try {
         this.tickProcessor();
      } catch (Exception var2) {
         var2.printStackTrace();
      }

   }

   private void tickProcessor() throws IOException {
      for(this.lastMeasure = Instant.now().toEpochMilli(); !this.shutdown; this.tick()) {
         long start = Instant.now().toEpochMilli();

         for(int max = 5000; this.receivePacket(); --max) {
         }

         while(this.receiveStream()) {
         }

         long time = Instant.now().toEpochMilli() - start;
         if (time < 50L) {
            sleepUntil(Instant.now().toEpochMilli() + (50L - time));
         }
      }

   }

   private void tick() throws IOException {
      long time = Instant.now().toEpochMilli();
      this.connection.update(time);
      if ((this.ticks & 15) == 0) {
         double diff = Math.max(0.005D, (double)(time - this.lastMeasure));
         this.streamOption("bandwith", "up:" + (double)this.sendBytes / diff + ",down:" + (double)this.receiveBytes / diff);
         this.lastMeasure = time;
         this.sendBytes = 0;
         this.receiveBytes = 0;
      }

      ++this.ticks;
   }

   private boolean receivePacket() throws IOException {
      DatagramPacket packet = this.socket.readPacket();
      if (packet == null) {
         return false;
      } else {
         int len = packet.getLength();
         if (len > 0) {
            SocketAddress source = packet.getSocketAddress();
            this.receiveBytes += len;
            Packet pkt = this.getPacketFromPool(packet.getData()[0]);
            if (pkt != null) {
               pkt.buffer = packet.getData();
               this.connection.handlePacket(pkt);
               return true;
            } else if (packet.getData() != null) {
               this.streamRaw(source, packet.getData());
               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public void streamEncapsulated(EncapsulatedPacket packet) {
      this.streamEncapsulated(packet, (byte)0);
   }

   public void streamEncapsulated(EncapsulatedPacket packet, byte flags) {
      String id = this.client.getServerIP() + ":" + this.client.getServerPort();
      ByteBuffer bb = ByteBuffer.allocate(packet.getTotalLength());
      bb.put((byte)1).put((byte)id.getBytes().length).put(id.getBytes()).put(flags).put(packet.toBinary(true));
      this.client.pushThreadToMainPacket(bb.array());
   }

   public void streamRaw(SocketAddress address, byte[] payload) {
      String dest;
      int port;
      if (address.toString().contains("/")) {
         dest = address.toString().split(Pattern.quote("/"))[1].split(Pattern.quote(":"))[0];
         port = Integer.parseInt(address.toString().split(Pattern.quote("/"))[1].split(Pattern.quote(":"))[1]);
      } else {
         dest = address.toString().split(Pattern.quote(":"))[0];
         port = Integer.parseInt(address.toString().split(Pattern.quote(":"))[1]);
      }

      this.streamRaw(dest, port, payload);
   }

   public void streamRaw(String address, int port, byte[] payload) {
      ByteBuffer bb = ByteBuffer.allocate(4 + address.getBytes().length + payload.length);
      bb.put((byte)8).put((byte)address.getBytes().length).put(address.getBytes()).put(Binary.writeShort((short)port)).put(payload);
      this.client.pushThreadToMainPacket(bb.array());
   }

   protected void streamClose(String identifier, String reason) {
      ByteBuffer bb = ByteBuffer.allocate(3 + identifier.getBytes().length + reason.getBytes().length);
      bb.put((byte)3).put((byte)identifier.getBytes().length).put(identifier.getBytes()).put((byte)reason.getBytes().length).put(reason.getBytes());
      this.client.pushThreadToMainPacket(bb.array());
   }

   protected void streamInvalid(String identifier) {
      ByteBuffer bb = ByteBuffer.allocate(2 + identifier.getBytes().length);
      bb.put((byte)4).put((byte)identifier.getBytes().length).put(identifier.getBytes());
      this.client.pushThreadToMainPacket(bb.array());
   }

   protected void streamOpen(long serverId) {
      String identifier = this.client.getServerIP() + ":" + this.client.getServerPort();
      ByteBuffer bb = ByteBuffer.allocate(10 + identifier.getBytes().length);
      bb.put((byte)2).put((byte)identifier.getBytes().length).put(identifier.getBytes()).put(Binary.writeLong(serverId));
      this.client.pushThreadToMainPacket(bb.array());
   }

   protected void streamACK(String identifier, int identifierACK) {
      ByteBuffer bb = ByteBuffer.allocate(6 + identifier.getBytes().length);
      bb.put((byte)6).put((byte)identifier.getBytes().length).put(identifier.getBytes()).put(Binary.writeInt(identifierACK));
      this.client.pushThreadToMainPacket(bb.array());
   }

   protected void streamOption(String name, String value) {
      ByteBuffer bb = ByteBuffer.allocate(2 + name.getBytes().length + value.getBytes().length);
      bb.put((byte)7).put((byte)name.getBytes().length).put(name.getBytes()).put(value.getBytes());
      this.client.pushThreadToMainPacket(bb.array());
   }

   public void sendPacket(Packet packet, String dest, int port) throws IOException {
      packet.encode();
      this.sendBytes += packet.buffer.length;
      this.socket.writePacket(packet.buffer, new InetSocketAddress(dest, port));
   }

   public boolean receiveStream() throws IOException {
      byte[] packet = this.client.readMainToThreadPacket();
      if (packet == null) {
         return false;
      } else if (packet.length > 0) {
         byte id = packet[0];
         int offset = 1;
         byte len;
         byte[] payload;
         int offset;
         if (id == 1) {
            offset = offset + 1;
            len = packet[offset];
            new String(Binary.subbytes(packet, offset, len));
            offset += len;
            byte flags = packet[offset++];
            payload = Binary.subbytes(packet, offset);
            this.connection.addEncapsulatedToQueue(EncapsulatedPacket.fromBinary(payload, true), flags);
         } else {
            String name;
            if (id == 8) {
               offset = offset + 1;
               len = packet[offset];
               name = new String(Binary.subbytes(packet, offset, len));
               offset += len;
               int port = Binary.readShort(Binary.subbytes(packet, offset, 2));
               offset += 2;
               payload = Binary.subbytes(packet, offset);
               this.socket.writePacket(payload, new InetSocketAddress(name, port));
            } else if (id == 3) {
               this.client.pushThreadToMainPacket(packet);
            } else if (id == 7) {
               offset = offset + 1;
               len = packet[offset];
               name = new String(Binary.subbytes(packet, offset, len));
               offset += len;
               String value = new String(Binary.subbytes(packet, offset));
               byte var8 = -1;
               switch(name.hashCode()) {
               case 1141776763:
                  if (name.equals("portChecking")) {
                     var8 = 0;
                  }
               default:
                  switch(var8) {
                  case 0:
                     this.portChecking = Boolean.parseBoolean(value);
                  }
               }
            } else if (id == 126) {
               this.connection.onShutdown();
               this.socket.close();
               this.shutdown = true;
            } else {
               if (id != 127) {
                  return false;
               }

               this.shutdown = true;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private void registerPacket(byte id, Class<? extends Packet> clazz) {
      this.packetPool.put(id, clazz);
   }

   public Packet getPacketFromPool(byte id) {
      if (this.packetPool.containsKey(id)) {
         try {
            return (Packet)((Class)this.packetPool.get(id)).newInstance();
         } catch (InstantiationException var3) {
            var3.printStackTrace();
         } catch (IllegalAccessException var4) {
            var4.printStackTrace();
         }
      }

      return null;
   }

   private void registerPackets() {
      this.registerPacket(UNCONNECTED_PING.ID, UNCONNECTED_PING.class);
      this.registerPacket(UNCONNECTED_PING_OPEN_CONNECTIONS.ID, UNCONNECTED_PING_OPEN_CONNECTIONS.class);
      this.registerPacket(OPEN_CONNECTION_REQUEST_1.ID, OPEN_CONNECTION_REQUEST_1.class);
      this.registerPacket(OPEN_CONNECTION_REPLY_1.ID, OPEN_CONNECTION_REPLY_1.class);
      this.registerPacket(OPEN_CONNECTION_REQUEST_2.ID, OPEN_CONNECTION_REQUEST_2.class);
      this.registerPacket(OPEN_CONNECTION_REPLY_2.ID, OPEN_CONNECTION_REPLY_2.class);
      this.registerPacket(UNCONNECTED_PONG.ID, UNCONNECTED_PONG.class);
      this.registerPacket(ADVERTISE_SYSTEM.ID, ADVERTISE_SYSTEM.class);
      this.registerPacket(DATA_PACKET_0.ID, DATA_PACKET_0.class);
      this.registerPacket(DATA_PACKET_1.ID, DATA_PACKET_1.class);
      this.registerPacket(DATA_PACKET_2.ID, DATA_PACKET_2.class);
      this.registerPacket(DATA_PACKET_3.ID, DATA_PACKET_3.class);
      this.registerPacket(DATA_PACKET_4.ID, DATA_PACKET_4.class);
      this.registerPacket(DATA_PACKET_5.ID, DATA_PACKET_5.class);
      this.registerPacket(DATA_PACKET_6.ID, DATA_PACKET_6.class);
      this.registerPacket(DATA_PACKET_7.ID, DATA_PACKET_7.class);
      this.registerPacket(DATA_PACKET_8.ID, DATA_PACKET_8.class);
      this.registerPacket(DATA_PACKET_9.ID, DATA_PACKET_9.class);
      this.registerPacket(DATA_PACKET_A.ID, DATA_PACKET_A.class);
      this.registerPacket(DATA_PACKET_B.ID, DATA_PACKET_B.class);
      this.registerPacket(DATA_PACKET_C.ID, DATA_PACKET_C.class);
      this.registerPacket(DATA_PACKET_D.ID, DATA_PACKET_D.class);
      this.registerPacket(DATA_PACKET_E.ID, DATA_PACKET_E.class);
      this.registerPacket(DATA_PACKET_F.ID, DATA_PACKET_F.class);
      this.registerPacket(NACK.ID, NACK.class);
      this.registerPacket(ACK.ID, ACK.class);
   }

   public JRakLibClient getClient() {
      return this.client;
   }

   public UDPClientSocket getSocket() {
      return this.socket;
   }

   public static void sleepUntil(long time) {
      while(Instant.now().toEpochMilli() < time) {
      }

   }
}
