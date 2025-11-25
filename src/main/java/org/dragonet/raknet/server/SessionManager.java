package org.dragonet.raknet.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.dragonet.proxy.utilities.Binary;
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

public class SessionManager {
   protected Map<Byte, Class<? extends Packet>> packetPool = new ConcurrentHashMap();
   protected RakNetServer server;
   protected UDPServerSocket socket;
   protected int receiveBytes = 0;
   protected int sendBytes = 0;
   protected Map<String, Session> sessions = new ConcurrentHashMap();
   protected String name = "";
   protected int packetLimit = 1000;
   protected boolean shutdown = false;
   protected long ticks = 0L;
   protected long lastMeasure;
   protected Map<String, Long> block = new ConcurrentHashMap();
   protected Map<String, Integer> ipSec = new ConcurrentHashMap();
   public boolean portChecking = true;
   public long serverId;

   public SessionManager(RakNetServer server, UDPServerSocket socket) throws Exception {
      this.server = server;
      this.socket = socket;
      this.registerPackets();
      this.serverId = (new Random()).nextLong();
      this.run();
   }

   public int getPort() {
      return this.server.port;
   }

   public void run() throws Exception {
      this.tickProcessor();
   }

   private void tickProcessor() throws Exception {
      this.lastMeasure = System.currentTimeMillis();

      while(!this.shutdown) {
         long start = System.currentTimeMillis();

         for(int max = 5000; max > 0 && this.receivePacket(); --max) {
         }

         while(this.receiveStream()) {
         }

         long time = System.currentTimeMillis() - start;
         if (time < 50L) {
            try {
               Thread.sleep(50L - time);
            } catch (InterruptedException var7) {
            }

            this.tick();
         }
      }

   }

   private void tick() throws Exception {
      long time = System.currentTimeMillis();
      Iterator var3 = this.sessions.values().iterator();

      while(var3.hasNext()) {
         Session session = (Session)var3.next();
         session.update(time);
      }

      var3 = this.ipSec.entrySet().iterator();

      while(var3.hasNext()) {
         Entry<String, Integer> entry = (Entry)var3.next();
         String address = (String)entry.getKey();
         int count = (Integer)entry.getValue();
         if (count >= this.packetLimit) {
            this.blockAddress(address);
         }
      }

      this.ipSec.clear();
      if ((this.ticks & 15L) == 0L) {
         double diff = Math.max(50.0D, (double)time - (double)this.lastMeasure);
         this.streamOption("bandwidth", (double)this.sendBytes / diff + ";" + (double)this.receiveBytes / diff);
         this.lastMeasure = time;
         this.sendBytes = 0;
         this.receiveBytes = 0;
         if (!this.block.isEmpty()) {
            long now = System.currentTimeMillis();
            Iterator var7 = this.block.entrySet().iterator();

            while(var7.hasNext()) {
               Entry<String, Long> entry = (Entry)var7.next();
               String address = (String)entry.getKey();
               long timeout = (Long)entry.getValue();
               if (timeout > now) {
                  break;
               }

               this.block.remove(address);
            }
         }
      }

      ++this.ticks;
   }

   private boolean receivePacket() throws Exception {
      DatagramPacket datagramPacket = this.socket.readPacket();
      if (datagramPacket != null) {
         int len = datagramPacket.getLength();
         byte[] buffer = datagramPacket.getData();
         String source = datagramPacket.getAddress().getHostAddress();
         int port = datagramPacket.getPort();
         if (len > 0) {
            this.receiveBytes += len;
            if (this.block.containsKey(source)) {
               return true;
            }

            if (this.ipSec.containsKey(source)) {
               this.ipSec.put(source, (Integer)this.ipSec.get(source) + 1);
            } else {
               this.ipSec.put(source, 1);
            }

            byte pid = buffer[0];
            if (pid == UNCONNECTED_PONG.ID) {
               return false;
            }

            Packet packet = this.getPacketFromPool(pid);
            if (packet != null) {
               packet.buffer = buffer;
               this.getSession(source, port).handlePacket(packet);
               return true;
            }

            if (pid != UNCONNECTED_PING.ID) {
               if (buffer.length != 0) {
                  this.streamRAW(source, port, buffer);
                  return true;
               }

               return false;
            }

            Packet packet2 = new UNCONNECTED_PING();
            packet2.buffer = buffer;
            packet2.decode();
            UNCONNECTED_PONG pk = new UNCONNECTED_PONG();
            pk.serverID = this.getID();
            pk.pingID = ((UNCONNECTED_PING)packet).pingID;
            pk.serverName = this.getName();
            this.sendPacket(pk, source, port);
         }
      }

      return false;
   }

   public void sendPacket(Packet packet, String dest, int port) throws IOException {
      packet.encode();
      this.sendBytes += this.socket.writePacket(packet.buffer, dest, port);
   }

   public void sendPacket(Packet packet, InetSocketAddress dest) throws IOException {
      packet.encode();
      this.sendBytes += this.socket.writePacket(packet.buffer, dest);
   }

   public void streamEncapsulated(Session session, EncapsulatedPacket packet) {
      this.streamEncapsulated(session, packet, 0);
   }

   public void streamEncapsulated(Session session, EncapsulatedPacket packet, int flags) {
      String id = session.getAddress() + ":" + session.getPort();
      byte[] buffer = Binary.appendBytes((byte)1, new byte[]{(byte)(id.length() & 255)}, id.getBytes(StandardCharsets.UTF_8), new byte[]{(byte)(flags & 255)}, packet.toBinary(true));
      this.server.pushThreadToMainPacket(buffer);
   }

   public void streamRAW(String address, int port, byte[] payload) {
      byte[] buffer = Binary.appendBytes((byte)8, new byte[]{(byte)(address.length() & 255)}, address.getBytes(StandardCharsets.UTF_8), Binary.writeShort(port), payload);
      this.server.pushThreadToMainPacket(buffer);
   }

   protected void streamClose(String identifier, String reason) {
      byte[] buffer = Binary.appendBytes((byte)3, new byte[]{(byte)(identifier.length() & 255)}, identifier.getBytes(StandardCharsets.UTF_8), new byte[]{(byte)(reason.length() & 255)}, reason.getBytes(StandardCharsets.UTF_8));
      this.server.pushThreadToMainPacket(buffer);
   }

   protected void streamInvalid(String identifier) {
      byte[] buffer = Binary.appendBytes((byte)4, new byte[]{(byte)(identifier.length() & 255)}, identifier.getBytes(StandardCharsets.UTF_8));
      this.server.pushThreadToMainPacket(buffer);
   }

   protected void streamOpen(Session session) {
      String identifier = session.getAddress() + ":" + session.getPort();
      byte[] buffer = Binary.appendBytes((byte)2, new byte[]{(byte)(identifier.length() & 255)}, identifier.getBytes(StandardCharsets.UTF_8), new byte[]{(byte)(session.getAddress().length() & 255)}, session.getAddress().getBytes(StandardCharsets.UTF_8), Binary.writeShort(session.getPort()), Binary.writeLong(session.getID()));
      this.server.pushThreadToMainPacket(buffer);
   }

   protected void streamACK(String identifier, int identifierACK) {
      byte[] buffer = Binary.appendBytes((byte)6, new byte[]{(byte)(identifier.length() & 255)}, identifier.getBytes(StandardCharsets.UTF_8), Binary.writeInt(identifierACK));
      this.server.pushThreadToMainPacket(buffer);
   }

   protected void streamOption(String name, String value) {
      byte[] buffer = Binary.appendBytes((byte)7, new byte[]{(byte)(name.length() & 255)}, name.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
      this.server.pushThreadToMainPacket(buffer);
   }

   private void checkSessions() {
      int size = this.sessions.size();
      if (size > 4096) {
         List<String> keyToRemove = new ArrayList();
         Iterator var3 = this.sessions.keySet().iterator();

         String i;
         while(var3.hasNext()) {
            i = (String)var3.next();
            Session s = (Session)this.sessions.get(i);
            if (s.isTemporal()) {
               keyToRemove.add(i);
               --size;
               if (size <= 4096) {
                  break;
               }
            }
         }

         var3 = keyToRemove.iterator();

         while(var3.hasNext()) {
            i = (String)var3.next();
            this.sessions.remove(i);
         }
      }

   }

   public boolean receiveStream() throws Exception {
      byte[] packet = this.server.readMainToThreadPacket();
      if (packet != null && packet.length > 0) {
         byte id = packet[0];
         int offset = 1;
         byte len;
         String identifier;
         String address;
         switch(id) {
         case 1:
            offset = offset + 1;
            len = packet[offset];
            identifier = new String(Binary.subBytes(packet, offset, len), StandardCharsets.UTF_8);
            offset += len;
            if (this.sessions.containsKey(identifier)) {
               byte flags = packet[offset++];
               byte[] buffer = Binary.subBytes(packet, offset);
               ((Session)this.sessions.get(identifier)).addEncapsulatedToQueue(EncapsulatedPacket.fromBinary(buffer, true), flags);
            } else {
               this.streamInvalid(identifier);
            }
            break;
         case 3:
            offset = offset + 1;
            len = packet[offset];
            identifier = new String(Binary.subBytes(packet, offset, len), StandardCharsets.UTF_8);
            if (this.sessions.containsKey(identifier)) {
               this.removeSession((Session)this.sessions.get(identifier));
            } else {
               this.streamInvalid(identifier);
            }
            break;
         case 4:
            offset = offset + 1;
            len = packet[offset];
            identifier = new String(Binary.subBytes(packet, offset, len), StandardCharsets.UTF_8);
            if (this.sessions.containsKey(identifier)) {
               this.removeSession((Session)this.sessions.get(identifier));
            }
            break;
         case 7:
            offset = offset + 1;
            len = packet[offset];
            String name = new String(Binary.subBytes(packet, offset, len), StandardCharsets.UTF_8);
            offset += len;
            String value = new String(Binary.subBytes(packet, offset), StandardCharsets.UTF_8);
            byte var17 = -1;
            switch(name.hashCode()) {
            case 3373707:
               if (name.equals("name")) {
                  var17 = 0;
               }
               break;
            case 513713171:
               if (name.equals("packetLimit")) {
                  var17 = 2;
               }
               break;
            case 1141776763:
               if (name.equals("portChecking")) {
                  var17 = 1;
               }
            }

            switch(var17) {
            case 0:
               this.name = value;
               return true;
            case 1:
               this.portChecking = Boolean.valueOf(value);
               return true;
            case 2:
               this.packetLimit = Integer.valueOf(value);
               return true;
            default:
               return true;
            }
         case 8:
            offset = offset + 1;
            len = packet[offset];
            address = new String(Binary.subBytes(packet, offset, len), StandardCharsets.UTF_8);
            offset += len;
            int port = Binary.readShort(Binary.subBytes(packet, offset, 2));
            offset += 2;
            byte[] payload = Binary.subBytes(packet, offset);
            this.socket.writePacket(payload, address, port);
            break;
         case 9:
            offset = offset + 1;
            len = packet[offset];
            address = new String(Binary.subBytes(packet, offset, len), StandardCharsets.UTF_8);
            offset += len;
            int timeout = Binary.readInt(Binary.subBytes(packet, offset, 4));
            this.blockAddress(address, timeout);
            break;
         case 126:
            Iterator var12 = this.sessions.values().iterator();

            while(var12.hasNext()) {
               Session session = (Session)var12.next();
               this.removeSession(session);
            }

            this.socket.close();
            this.shutdown = true;
            break;
         case 127:
            this.shutdown = true;
         default:
            return false;
         }

         return true;
      } else {
         return false;
      }
   }

   public void blockAddress(String address) {
      this.blockAddress(address, 300);
   }

   public void blockAddress(String address, int timeout) {
      long finalTime = System.currentTimeMillis() + 300000L;
      if (this.block.containsKey(address) && timeout != -1) {
         if ((Long)this.block.get(address) < finalTime) {
            this.block.put(address, finalTime);
         }
      } else {
         if (timeout == -1) {
            finalTime = Long.MAX_VALUE;
         }

         this.block.put(address, finalTime);
      }

   }

   public Session getSession(String ip, int port) {
      String id = ip + ":" + port;
      if (!this.sessions.containsKey(id)) {
         this.checkSessions();
         Session session = new Session(this, ip, port);
         this.sessions.put(id, session);
         return session;
      } else {
         return (Session)this.sessions.get(id);
      }
   }

   public void removeSession(Session session) throws Exception {
      this.removeSession(session, "unknown");
   }

   public void removeSession(Session session, String reason) throws Exception {
      String id = session.getAddress() + ":" + session.getPort();
      if (this.sessions.containsKey(id)) {
         ((Session)this.sessions.get(id)).close();
         this.sessions.remove(id);
         this.streamClose(id, reason);
      }

   }

   public void openSession(Session session) {
      this.streamOpen(session);
   }

   public void notifyACK(Session session, int identifierACK) {
      this.streamACK(session.getAddress() + ":" + session.getPort(), identifierACK);
   }

   public String getName() {
      return this.name;
   }

   public long getID() {
      return this.serverId;
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
}
