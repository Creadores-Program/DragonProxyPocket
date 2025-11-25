package org.dragonet.raknet.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.dragonet.raknet.protocol.DataPacket;
import org.dragonet.raknet.protocol.EncapsulatedPacket;
import org.dragonet.raknet.protocol.Packet;
import org.dragonet.raknet.protocol.packet.ACK;
import org.dragonet.raknet.protocol.packet.CLIENT_CONNECT_DataPacket;
import org.dragonet.raknet.protocol.packet.CLIENT_DISCONNECT_DataPacket;
import org.dragonet.raknet.protocol.packet.CLIENT_HANDSHAKE_DataPacket;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_0;
import org.dragonet.raknet.protocol.packet.DATA_PACKET_4;
import org.dragonet.raknet.protocol.packet.NACK;
import org.dragonet.raknet.protocol.packet.OPEN_CONNECTION_REPLY_1;
import org.dragonet.raknet.protocol.packet.OPEN_CONNECTION_REPLY_2;
import org.dragonet.raknet.protocol.packet.OPEN_CONNECTION_REQUEST_2;
import org.dragonet.raknet.protocol.packet.PING_DataPacket;
import org.dragonet.raknet.protocol.packet.PONG_DataPacket;
import org.dragonet.raknet.protocol.packet.SERVER_HANDSHAKE_DataPacket;

public class Connection {
   public static final int STATE_UNCONNECTED = 0;
   public static final int STATE_CONNECTING_1 = 1;
   public static final int STATE_CONNECTING_2 = 2;
   public static final int STATE_CONNECTING_3 = 3;
   public static final int STATE_CONNECTED = 4;
   public static long sessionID = (new Random()).nextLong();
   public static int WINDOW_SIZE = 2048;
   protected ConnectionManager manager;
   private int state = 0;
   private List<EncapsulatedPacket> preJoinQueue = new ArrayList();
   private int mtuSize = 548;
   private long id = 0L;
   private int splitID = 0;
   private int messageIndex = 0;
   private Map<Byte, Integer> channelIndex = new ConcurrentHashMap();
   private int sendSeqNumber = 0;
   private int lastSeqNumber = -1;
   private long lastUpdate;
   private long startTime;
   private List<DataPacket> packetToSend = new ArrayList();
   private boolean isActive;
   private Map<Integer, Integer> ACKQueue = new HashMap();
   private Map<Integer, Integer> NACKQueue = new HashMap();
   private Map<Integer, DataPacket> recoveryQueue = new ConcurrentHashMap();
   private Map<Short, Map<Integer, EncapsulatedPacket>> splitPackets = new ConcurrentHashMap();
   private Map<Integer, Map<Integer, Integer>> needACK = new ConcurrentHashMap();
   private DataPacket sendQueue;
   private int windowStart;
   private Map<Integer, Integer> receivedWindow = new ConcurrentHashMap();
   private int windowEnd;
   private int reliableWindowStart;
   private int reliableWindowEnd;
   private Map<Integer, EncapsulatedPacket> reliableWindow = new ConcurrentHashMap();
   private int lastReliableIndex = -1;
   private long lastPing;
   private long lastPong = Instant.now().toEpochMilli();

   public Connection(ConnectionManager manager, int mtuSize) {
      this.manager = manager;
      this.mtuSize = mtuSize;
      this.sendQueue = new DATA_PACKET_4();
      this.lastUpdate = Instant.now().toEpochMilli();
      this.startTime = Instant.now().toEpochMilli();
      this.isActive = false;
      this.windowStart = -1;
      this.windowEnd = WINDOW_SIZE;
      this.reliableWindowStart = 0;
      this.reliableWindowEnd = WINDOW_SIZE;

      for(byte i = 0; i < 32; ++i) {
         this.channelIndex.put(i, 0);
      }

   }

   public void update(long time) throws IOException {
      this.isActive = false;
      if (Instant.now().toEpochMilli() - this.lastPong >= 8000L) {
         this.disconnect("connection timed out.");
      }

      if (Instant.now().toEpochMilli() - this.lastPing >= 5000L) {
         this.sendPing();
      }

      if (!this.ACKQueue.isEmpty()) {
         ACK pk = new ACK();
         pk.packets = new TreeMap(this.ACKQueue);
         this.sendPacket(pk);
         this.ACKQueue = new HashMap();
      }

      if (!this.NACKQueue.isEmpty()) {
         NACK pk = new NACK();
         pk.packets = new TreeMap(this.NACKQueue);
         this.sendPacket(pk);
         this.NACKQueue = new HashMap();
      }

      DataPacket pk;
      if (!this.packetToSend.isEmpty()) {
         int limit = 16;

         for(int i = 0; i < this.packetToSend.size(); ++i) {
            pk = (DataPacket)this.packetToSend.get(i);
            pk.sendTime = time;
            pk.encode();
            this.recoveryQueue.put(pk.seqNumber, pk);
            this.packetToSend.remove(pk);
            this.sendPacket(pk);
            if (limit-- <= 0) {
               break;
            }
         }
      }

      if (this.packetToSend.size() > WINDOW_SIZE) {
         this.packetToSend.clear();
      }

      Iterator var9;
      Integer seq;
      if (this.needACK.values().size() > 0) {
         var9 = this.needACK.keySet().iterator();

         while(var9.hasNext()) {
            seq = (Integer)var9.next();
            Map<Integer, Integer> indexes = (Map)this.needACK.get(seq);
            if (indexes.values().size() == 0) {
               this.needACK.remove(indexes);
            }
         }
      }

      var9 = this.recoveryQueue.keySet().iterator();

      while(var9.hasNext()) {
         seq = (Integer)var9.next();
         pk = (DataPacket)this.recoveryQueue.get(seq);
         if (pk.sendTime >= Instant.now().toEpochMilli() - 6000L) {
            break;
         }

         this.packetToSend.add(pk);
         this.recoveryQueue.remove(seq);
      }

      var9 = this.receivedWindow.keySet().iterator();

      while(var9.hasNext()) {
         seq = (Integer)var9.next();
         if (seq >= this.windowStart) {
            break;
         }

         this.receivedWindow.remove(seq);
      }

      try {
         this.sendQueue();
      } catch (IOException var6) {
         throw new RuntimeException(var6);
      }
   }

   private void sendPing() throws IOException {
      PING_DataPacket ping = new PING_DataPacket();
      ping.pingID = Instant.now().toEpochMilli();
      ping.encode();
      EncapsulatedPacket pk = new EncapsulatedPacket();
      pk.reliability = 0;
      pk.buffer = ping.buffer;
      this.addToQueue(pk);
      this.lastPing = Instant.now().toEpochMilli();
   }

   public void disconnect(String reason) {
      this.manager.streamClose("0.0.0.0:" + this.manager.getSocket().getSocket().getLocalPort(), reason);
      this.manager.shutdown = true;
   }

   public void disconnect() {
      this.disconnect("disconnected by client.");
   }

   private void sendPacket(Packet pk) throws IOException {
      System.out.println("[DEBUG] Sending to remote " + pk.getClass().getSimpleName() + " (@Connection.java:223)");
      this.manager.sendPacket(pk, this.manager.getClient().getServerIP(), this.manager.getClient().getServerPort());
   }

   public void sendQueue() throws IOException {
      if (!this.sendQueue.packets.isEmpty()) {
         this.sendQueue.seqNumber = this.sendSeqNumber++;
         this.sendPacket(this.sendQueue);
         this.sendQueue.sendTime = Instant.now().toEpochMilli();
         this.recoveryQueue.put(this.sendQueue.seqNumber, this.sendQueue);
         this.sendQueue = new DATA_PACKET_4();
      }

   }

   private void addToQueue(EncapsulatedPacket pk) throws IOException {
      this.addToQueue(pk, 0);
   }

   private void addToQueue(EncapsulatedPacket pk, int flags) throws IOException {
      int priority = flags & 7;
      if (pk.needACK && pk.messageIndex != -1) {
         Object map;
         if (this.needACK.get(pk.needACK) != null) {
            map = (Map)this.needACK.get(pk.needACK);
            ((Map)map).put(pk.messageIndex, pk.messageIndex);
         } else {
            map = new ConcurrentHashMap();
            ((Map)map).put(pk.messageIndex, pk.messageIndex);
         }

         this.needACK.put(pk.identifierACK, (Map<Integer, Integer>) map);
      }

      if (priority == 1) {
         DataPacket packet = new DATA_PACKET_0();
         packet.seqNumber = this.sendSeqNumber++;
         if (pk.needACK) {
            packet.packets.add(pk);
            pk.needACK = false;
         } else {
            packet.packets.add(pk.toBinary());
         }

         this.sendPacket(packet);
         packet.sendTime = Instant.now().toEpochMilli();
         this.recoveryQueue.put(packet.seqNumber, packet);
      } else {
         int length = this.sendQueue.length();
         if (length + pk.getTotalLength() > this.mtuSize) {
            this.sendQueue();
         }

         if (pk.needACK) {
            this.sendQueue.packets.add(pk);
            pk.needACK = false;
         } else {
            this.sendQueue.packets.add(pk.toBinary());
         }

      }
   }

   public void addEncapsulatedToQueue(EncapsulatedPacket packet) throws IOException {
      this.addEncapsulatedToQueue(packet, (byte)0);
   }

   public void addEncapsulatedToQueue(EncapsulatedPacket packet, byte flags) throws IOException {
      if (packet.needACK = (flags & 8) > 0) {
         this.needACK.put(packet.identifierACK, new ConcurrentHashMap());
      }

      if (packet.reliability == 2 || packet.reliability == 3 || packet.reliability == 4 || packet.reliability == 6 || packet.reliability == 7) {
         packet.messageIndex = this.messageIndex++;
         if (packet.reliability == 3) {
            this.channelIndex.put((byte)(packet.orderChannel & 255), (Integer)this.channelIndex.get((byte)(packet.orderChannel & 255)) + 1);
            packet.orderIndex = (Integer)this.channelIndex.get((byte)(packet.orderChannel & 255));
         }
      }

      if (packet.getTotalLength() + 4 > this.mtuSize) {
         byte[][] buffers = Binary.splitbytes(packet.buffer, this.mtuSize - 34);
         int splitID = this.splitID++;
         splitID %= 65536;

         for(int count = 0; count < buffers.length; ++count) {
            byte[] buffer = buffers[count];
            EncapsulatedPacket pk = new EncapsulatedPacket();
            pk.splitID = splitID;
            pk.hasSplit = true;
            pk.splitCount = buffers.length;
            pk.reliability = packet.reliability;
            pk.splitIndex = count;
            pk.buffer = buffer;
            if (count > 0) {
               pk.messageIndex = this.messageIndex++;
            } else {
               pk.messageIndex = packet.messageIndex;
            }

            if (pk.reliability == 3) {
               pk.orderChannel = packet.orderChannel;
               pk.orderIndex = packet.orderIndex;
            }

            this.addToQueue(pk, flags | 1);
         }
      } else {
         this.addToQueue(packet, flags);
      }

   }

   private void handleSplit(EncapsulatedPacket packet) throws IOException {
      if (packet.splitCount < 128) {
         if (!this.splitPackets.containsKey((short)(packet.splitID & '\uffff'))) {
            Map<Integer, EncapsulatedPacket> map = new ConcurrentHashMap();
            map.put(packet.splitIndex, packet);
            this.splitPackets.put((short)(packet.splitID & '\uffff'), map);
         } else {
            Map<Integer, EncapsulatedPacket> map = (Map)this.splitPackets.get((short)(packet.splitID & '\uffff'));
            map.put(packet.splitIndex, packet);
            this.splitPackets.put((short)(packet.splitID & '\uffff'), map);
         }

         if (((Map)this.splitPackets.get((short)(packet.splitID & '\uffff'))).values().size() == packet.splitCount) {
            EncapsulatedPacket pk = new EncapsulatedPacket();
            ByteBuffer bb = ByteBuffer.allocate(262144);

            for(int i = 0; i < packet.splitCount; ++i) {
               bb.put(((EncapsulatedPacket)((Map)this.splitPackets.get((short)(packet.splitID & '\uffff'))).get(i)).buffer);
            }

            pk.buffer = Arrays.copyOf(bb.array(), bb.position());
            bb = null;
            pk.length = pk.buffer.length;
            this.splitPackets.remove((short)(packet.splitID & '\uffff'));
            this.handleEncapsulatedPacketRoute(pk);
         }

      }
   }

   private void handleEncapsulatedPacket(EncapsulatedPacket packet) throws IOException {
      if (packet == null) {
         System.out.println("[ERROR] NULL ENCAPSULATED PACKET! ");
      } else {
         System.out.println("Recieved encapulated packet INDEX=" + packet.messageIndex);
         if (packet.messageIndex == -1) {
            this.handleEncapsulatedPacketRoute(packet);
         } else {
            if (packet.messageIndex < this.reliableWindowStart || packet.messageIndex > this.reliableWindowEnd) {
               return;
            }

            if (packet.messageIndex - this.lastReliableIndex == 1) {
               ++this.lastReliableIndex;
               ++this.reliableWindowStart;
               ++this.reliableWindowEnd;
               this.handleEncapsulatedPacketRoute(packet);
               if (!this.reliableWindow.values().isEmpty()) {
                  Iterator var2 = this.reliableWindow.keySet().iterator();

                  while(var2.hasNext()) {
                     Integer index = (Integer)var2.next();
                     EncapsulatedPacket pk = (EncapsulatedPacket)this.reliableWindow.get(index);
                     if (index - this.lastReliableIndex != 1) {
                        break;
                     }

                     ++this.lastReliableIndex;
                     ++this.reliableWindowStart;
                     ++this.reliableWindowEnd;
                     this.handleEncapsulatedPacketRoute(packet);
                     this.reliableWindow.remove(index);
                  }
               }
            } else {
               this.reliableWindow.put(packet.messageIndex, packet);
            }
         }

      }
   }

   private void handleEncapsulatedPacketRoute(EncapsulatedPacket packet) throws IOException {
      if (this.manager != null) {
         if (packet.hasSplit) {
            if (this.state == 4) {
               this.handleSplit(packet);
            }

         } else {
            byte id = packet.buffer[0];
            if (id < 128) {
               EncapsulatedPacket sendPacket;
               if (this.state == 3) {
                  if (id == SERVER_HANDSHAKE_DataPacket.ID) {
                     SERVER_HANDSHAKE_DataPacket pk = new SERVER_HANDSHAKE_DataPacket();
                     pk.buffer = packet.buffer;
                     pk.decode();
                     CLIENT_HANDSHAKE_DataPacket response = new CLIENT_HANDSHAKE_DataPacket();
                     response.address = "0.0.0.0";
                     response.port = 0;
                     response.systemAddresses = new InetSocketAddress[]{new InetSocketAddress("0.0.0.0", 0), new InetSocketAddress("0.0.0.0", 0), new InetSocketAddress("0.0.0.0", 0), new InetSocketAddress("0.0.0.0", 0), new InetSocketAddress("0.0.0.0", 0), new InetSocketAddress("0.0.0.0", this.manager.getSocket().getSocket().getLocalPort()), new InetSocketAddress("0.0.0.0", 0), new InetSocketAddress("0.0.0.0", this.manager.getSocket().getSocket().getLocalPort()), new InetSocketAddress("0.0.0.0", 0), new InetSocketAddress("0.0.0.0", this.manager.getSocket().getSocket().getLocalPort())};
                     response.sendPing = Instant.now().toEpochMilli();
                     response.sendPong = Instant.now().toEpochMilli();
                     response.encode();
                     sendPacket = new EncapsulatedPacket();
                     sendPacket.reliability = 0;
                     sendPacket.buffer = response.buffer;
                     this.addToQueue(sendPacket, 1);
                     if (!this.manager.portChecking) {
                        this.state = 4;
                        this.manager.streamOpen(this.id);
                        Iterator var6 = this.preJoinQueue.iterator();

                        while(var6.hasNext()) {
                           EncapsulatedPacket p = (EncapsulatedPacket)var6.next();
                           this.manager.streamEncapsulated(p);
                        }

                        this.preJoinQueue.clear();
                     }
                  }
               } else if (id == CLIENT_DISCONNECT_DataPacket.ID) {
                  this.disconnect("disconnected by server.");
               } else if (id == PING_DataPacket.ID) {
                  PING_DataPacket dataPacket = new PING_DataPacket();
                  dataPacket.buffer = packet.buffer;
                  dataPacket.decode();
                  PONG_DataPacket pk = new PONG_DataPacket();
                  pk.pingID = dataPacket.pingID;
                  pk.encode();
                  sendPacket = new EncapsulatedPacket();
                  sendPacket.reliability = 0;
                  sendPacket.buffer = pk.buffer;
                  this.addToQueue(sendPacket);
               } else if (id == PONG_DataPacket.ID) {
                  this.lastPong = Instant.now().toEpochMilli();
               } else if (this.state == 4) {
                  this.manager.streamEncapsulated(packet);
               }
            } else {
               this.preJoinQueue.add(packet);
            }

         }
      }
   }

   public void handlePacket(Packet packet) throws IOException {
      System.out.println("[DEBUG] Recieved " + packet.getClass().getSimpleName() + " (@ Connection.java:484)");
      this.isActive = true;
      this.lastUpdate = Instant.now().toEpochMilli();
      if (this.state != 4 && this.state != 3) {
         if (packet.buffer[0] > 0 || packet.buffer[0] < 128) {
            packet.decode();
            if (packet instanceof OPEN_CONNECTION_REPLY_1) {
               OPEN_CONNECTION_REPLY_1 reply1 = (OPEN_CONNECTION_REPLY_1)packet;
               this.id = reply1.serverID;
               OPEN_CONNECTION_REQUEST_2 request2 = new OPEN_CONNECTION_REQUEST_2();
               request2.mtuSize = (short)this.mtuSize;
               request2.clientID = JRakLibClient.getClientID();
               request2.serverAddress = this.manager.getClient().getServerEndpoint().getHostString();
               request2.serverPort = this.manager.getClient().getServerEndpoint().getPort();
               request2.encode();
               this.sendPacket(request2);
               if (reply1.mtuSize == this.mtuSize) {
                  this.state = 2;
               }
            } else if (this.state == 2 && packet instanceof OPEN_CONNECTION_REPLY_2) {
               OPEN_CONNECTION_REPLY_2 reply2 = (OPEN_CONNECTION_REPLY_2)packet;
               if (((OPEN_CONNECTION_REPLY_2)packet).clientPort == this.manager.getSocket().getSocket().getLocalPort() || !this.manager.portChecking) {
                  CLIENT_CONNECT_DataPacket connect = new CLIENT_CONNECT_DataPacket();
                  connect.clientID = JRakLibClient.getClientID();
                  connect.sendPing = sessionID;
                  connect.encode();
                  EncapsulatedPacket pk = new EncapsulatedPacket();
                  pk.reliability = 0;
                  pk.buffer = connect.buffer;
                  this.addToQueue(pk, 1);
                  this.sendPing();
                  this.state = 3;
               }
            }
         }
      } else {
         int seq;
         Iterator var4;
         Object pk;
         if (packet.buffer[0] >= 128 || packet.buffer[0] <= 143 && packet instanceof DataPacket) {
            packet.decode();
            DataPacket dp = (DataPacket)packet;
            if (dp.seqNumber < this.windowStart || dp.seqNumber > this.windowEnd || this.receivedWindow.containsKey(dp.seqNumber)) {
               return;
            }

            seq = dp.seqNumber - this.lastSeqNumber;
            this.NACKQueue.remove(dp.seqNumber);
            this.ACKQueue.put(dp.seqNumber, dp.seqNumber);
            this.receivedWindow.put(dp.seqNumber, dp.seqNumber);
            if (seq != 1) {
               for(int i = this.lastSeqNumber + 1; i < dp.seqNumber; ++i) {
                  if (!this.receivedWindow.containsKey(i)) {
                     this.NACKQueue.put(i, i);
                  }
               }
            }

            if (seq >= 1) {
               this.lastSeqNumber = dp.seqNumber;
               this.windowStart += seq;
               this.windowEnd += seq;
            }

            var4 = dp.packets.iterator();

            while(var4.hasNext()) {
               pk = var4.next();
               if (pk instanceof EncapsulatedPacket) {
                  this.handleEncapsulatedPacket((EncapsulatedPacket)pk);
               }
            }
         } else {
            Iterator var2;
            if (packet instanceof ACK) {
               packet.decode();
               var2 = (new ArrayList(((ACK)packet).packets.values())).iterator();

               while(true) {
                  do {
                     if (!var2.hasNext()) {
                        return;
                     }

                     seq = (Integer)var2.next();
                  } while(!this.recoveryQueue.containsKey(seq));

                  var4 = ((DataPacket)this.recoveryQueue.get(seq)).packets.iterator();

                  while(var4.hasNext()) {
                     pk = var4.next();
                     if (EncapsulatedPacket.class.isAssignableFrom(pk.getClass()) && ((EncapsulatedPacket)pk).needACK && ((EncapsulatedPacket)pk).messageIndex != null && this.needACK.containsKey(((EncapsulatedPacket)pk).identifierACK)) {
                        ((Map)this.needACK.get(((EncapsulatedPacket)pk).identifierACK)).remove(((EncapsulatedPacket)pk).messageIndex);
                     }
                  }

                  this.recoveryQueue.remove(seq);
               }
            } else if (packet instanceof NACK) {
               packet.decode();
               var2 = (new ArrayList(((NACK)packet).packets.values())).iterator();

               while(var2.hasNext()) {
                  seq = (Integer)var2.next();
                  if (this.recoveryQueue.containsKey(seq)) {
                     DataPacket pk2 = (DataPacket)this.recoveryQueue.get(seq);
                     pk2.seqNumber = this.sendSeqNumber++;
                     this.packetToSend.add(pk2);
                     this.recoveryQueue.remove(seq);
                  }
               }
            }
         }
      }

   }

   protected void onShutdown() {
      try {
         this.close();
      } catch (IOException var2) {
         var2.printStackTrace();
      }

   }

   public void close() throws IOException {
      byte[] data = new byte[]{0, 0, 8, 21};
      this.addEncapsulatedToQueue(EncapsulatedPacket.fromBinary(data), (byte)1);
      this.sendQueue();
      this.manager = null;
   }
}
