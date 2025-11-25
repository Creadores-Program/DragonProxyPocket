package org.dragonet.raknet.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.dragonet.proxy.utilities.Binary;
import org.dragonet.proxy.utilities.BinaryStream;
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
import org.dragonet.raknet.protocol.packet.OPEN_CONNECTION_REQUEST_1;
import org.dragonet.raknet.protocol.packet.OPEN_CONNECTION_REQUEST_2;
import org.dragonet.raknet.protocol.packet.PING_DataPacket;
import org.dragonet.raknet.protocol.packet.PONG_DataPacket;
import org.dragonet.raknet.protocol.packet.SERVER_HANDSHAKE_DataPacket;

public class Session {
   public static final int STATE_UNCONNECTED = 0;
   public static final int STATE_CONNECTING_1 = 1;
   public static final int STATE_CONNECTING_2 = 2;
   public static final int STATE_CONNECTED = 3;
   public static final int MAX_SPLIT_SIZE = 128;
   public static final int MAX_SPLIT_COUNT = 4;
   public static int WINDOW_SIZE = 2048;
   private int messageIndex = 0;
   private Map<Integer, Integer> channelIndex = new ConcurrentHashMap();
   private SessionManager sessionManager;
   private String address;
   private int port;
   private int state = 0;
   private int mtuSize = 548;
   private long id = 0L;
   private int splitID = 0;
   private int sendSeqNumber = 0;
   private int lastSeqNumber = -1;
   private long lastUpdate;
   private long startTime;
   private boolean isTemporal = true;
   private List<DataPacket> packetToSend = new ArrayList();
   private boolean isActive;
   private Map<Integer, Integer> ACKQueue = new HashMap();
   private Map<Integer, Integer> NACKQueue = new HashMap();
   private Map<Integer, DataPacket> recoveryQueue = new HashMap();
   private Map<Integer, Map<Integer, EncapsulatedPacket>> splitPackets = new HashMap();
   private Map<Integer, Map<Integer, Integer>> needACK = new HashMap();
   private DataPacket sendQueue;
   private int windowStart;
   private Map<Integer, Integer> receivedWindow = new HashMap();
   private int windowEnd;
   private int reliableWindowStart;
   private int reliableWindowEnd;
   private Map<Integer, EncapsulatedPacket> reliableWindow = new HashMap();
   private int lastReliableIndex = -1;

   public Session(SessionManager sessionManager, String address, int port) {
      this.sessionManager = sessionManager;
      this.address = address;
      this.port = port;
      this.sendQueue = new DATA_PACKET_4();
      this.lastUpdate = System.currentTimeMillis();
      this.startTime = System.currentTimeMillis();
      this.isActive = false;
      this.windowStart = -1;
      this.windowEnd = WINDOW_SIZE;
      this.reliableWindowStart = 0;
      this.reliableWindowEnd = WINDOW_SIZE;

      for(int i = 0; i < 32; ++i) {
         this.channelIndex.put(i, 0);
      }

   }

   public String getAddress() {
      return this.address;
   }

   public int getPort() {
      return this.port;
   }

   public long getID() {
      return this.id;
   }

   public void update(long time) throws Exception {
      if (!this.isActive && this.lastUpdate + 10000L < time) {
         this.disconnect("timeout");
      } else {
         this.isActive = false;
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

         int seq;
         DataPacket pk;
         if (!this.packetToSend.isEmpty()) {
            int limit = 16;

            for(seq = 0; seq < this.packetToSend.size(); ++seq) {
               pk = (DataPacket)this.packetToSend.get(seq);
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

         Iterator var8;
         if (!this.needACK.isEmpty()) {
            var8 = (new ArrayList(this.needACK.keySet())).iterator();

            while(var8.hasNext()) {
               seq = (Integer)var8.next();
               Map<Integer, Integer> indexes = (Map)this.needACK.get(seq);
               if (indexes.isEmpty()) {
                  this.needACK.remove(seq);
                  this.sessionManager.notifyACK(this, seq);
               }
            }
         }

         var8 = (new ArrayList(this.recoveryQueue.keySet())).iterator();

         while(var8.hasNext()) {
            seq = (Integer)var8.next();
            pk = (DataPacket)this.recoveryQueue.get(seq);
            if (pk.sendTime >= System.currentTimeMillis() - 8000L) {
               break;
            }

            this.packetToSend.add(pk);
            this.recoveryQueue.remove(seq);
         }

         var8 = (new ArrayList(this.receivedWindow.keySet())).iterator();

         while(var8.hasNext()) {
            seq = (Integer)var8.next();
            if (seq >= this.windowStart) {
               break;
            }

            this.receivedWindow.remove(seq);
         }

         this.sendQueue();
      }
   }

   public void disconnect() throws Exception {
      this.disconnect("unknown");
   }

   public void disconnect(String reason) throws Exception {
      this.sessionManager.removeSession(this, reason);
   }

   private void sendPacket(Packet packet) throws IOException {
      this.sessionManager.sendPacket(packet, this.address, this.port);
   }

   public void sendQueue() throws IOException {
      if (!this.sendQueue.packets.isEmpty()) {
         this.sendQueue.seqNumber = this.sendSeqNumber++;
         this.sendPacket(this.sendQueue);
         this.sendQueue.sendTime = System.currentTimeMillis();
         this.recoveryQueue.put(this.sendQueue.seqNumber, this.sendQueue);
         this.sendQueue = new DATA_PACKET_4();
      }

   }

   private void addToQueue(EncapsulatedPacket pk) throws Exception {
      this.addToQueue(pk, 0);
   }

   private void addToQueue(EncapsulatedPacket pk, int flags) throws Exception {
      int priority = flags & 7;
      if (pk.needACK && pk.messageIndex != null) {
         if (!this.needACK.containsKey(pk.identifierACK)) {
            this.needACK.put(pk.identifierACK, new HashMap());
         }

         ((Map)this.needACK.get(pk.identifierACK)).put(pk.messageIndex, pk.messageIndex);
      }

      if (priority == 1) {
         DataPacket packet = new DATA_PACKET_0();
         packet.seqNumber = this.sendSeqNumber++;
         if (pk.needACK) {
            packet.packets.add(pk.clone());
            pk.needACK = false;
         } else {
            packet.packets.add(pk.toBinary());
         }

         this.sendPacket(packet);
         packet.sendTime = System.currentTimeMillis();
         this.recoveryQueue.put(packet.seqNumber, packet);
      } else {
         int length = this.sendQueue.length();
         if (length + pk.getTotalLength() > this.mtuSize) {
            this.sendQueue();
         }

         if (pk.needACK) {
            this.sendQueue.packets.add(pk.clone());
            pk.needACK = false;
         } else {
            this.sendQueue.packets.add(pk.toBinary());
         }

      }
   }

   public void addEncapsulatedToQueue(EncapsulatedPacket packet) throws Exception {
      this.addEncapsulatedToQueue(packet, 0);
   }

   public void addEncapsulatedToQueue(EncapsulatedPacket packet, int flags) throws Exception {
      if (packet.needACK = (flags & 8) > 0) {
         this.needACK.put(packet.identifierACK, new HashMap());
      }

      if (packet.reliability == 2 || packet.reliability == 3 || packet.reliability == 4 || packet.reliability == 6 || packet.reliability == 7) {
         packet.messageIndex = this.messageIndex++;
         if (packet.reliability == 3) {
            int index = (Integer)this.channelIndex.get(packet.orderChannel) + 1;
            packet.orderIndex = index;
            this.channelIndex.put(packet.orderChannel, index);
         }
      }

      if (packet.getTotalLength() + 4 > this.mtuSize) {
         byte[][] buffers = Binary.splitBytes(packet.buffer, this.mtuSize - 34);
         int splitID = ++this.splitID % 65536;

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

   private void handleSplit(final EncapsulatedPacket packet) throws Exception {
      if (packet.splitCount < 128 && packet.splitIndex < 128 && packet.splitIndex >= 0) {
         if (!this.splitPackets.containsKey(packet.splitID)) {
            if (this.splitPackets.size() >= 4) {
               return;
            }

            this.splitPackets.put(packet.splitID, new HashMap<Integer, EncapsulatedPacket>() {
               {
                  this.put(packet.splitIndex, packet);
               }
            });
         } else {
            ((Map)this.splitPackets.get(packet.splitID)).put(packet.splitIndex, packet);
         }

         if (((Map)this.splitPackets.get(packet.splitID)).size() == packet.splitCount) {
            EncapsulatedPacket pk = new EncapsulatedPacket();
            BinaryStream stream = new BinaryStream();

            for(int i = 0; i < packet.splitCount; ++i) {
               stream.put(((EncapsulatedPacket)((Map)this.splitPackets.get(packet.splitID)).get(i)).buffer);
            }

            pk.buffer = stream.getBuffer();
            pk.length = pk.buffer.length;
            this.splitPackets.remove(packet.splitID);
            this.handleEncapsulatedPacketRoute(pk);
         }

      }
   }

   private void handleEncapsulatedPacket(EncapsulatedPacket packet) throws Exception {
      if (packet.messageIndex == null) {
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
            if (!this.reliableWindow.isEmpty()) {
               TreeMap<Integer, EncapsulatedPacket> sortedMap = new TreeMap(this.reliableWindow);
               Iterator var3 = sortedMap.keySet().iterator();

               while(var3.hasNext()) {
                  int index = (Integer)var3.next();
                  EncapsulatedPacket pk = (EncapsulatedPacket)this.reliableWindow.get(index);
                  if (index - this.lastReliableIndex != 1) {
                     break;
                  }

                  ++this.lastReliableIndex;
                  ++this.reliableWindowStart;
                  ++this.reliableWindowEnd;
                  this.handleEncapsulatedPacketRoute(pk);
                  this.reliableWindow.remove(index);
               }
            }
         } else {
            this.reliableWindow.put(packet.messageIndex, packet);
         }
      }

   }

   public int getState() {
      return this.state;
   }

   public boolean isTemporal() {
      return this.isTemporal;
   }

   private void handleEncapsulatedPacketRoute(EncapsulatedPacket packet) throws Exception {
      if (this.sessionManager != null) {
         if (packet.hasSplit) {
            if (this.state == 3) {
               this.handleSplit(packet);
            }

         } else {
            byte id = packet.buffer[0];
            if ((id & 255) < 128) {
               EncapsulatedPacket sendPacket;
               if (this.state == 2) {
                  if (id == CLIENT_CONNECT_DataPacket.ID) {
                     CLIENT_CONNECT_DataPacket dataPacket = new CLIENT_CONNECT_DataPacket();
                     dataPacket.buffer = packet.buffer;
                     dataPacket.decode();
                     SERVER_HANDSHAKE_DataPacket pk = new SERVER_HANDSHAKE_DataPacket();
                     pk.address = this.address;
                     pk.port = this.port;
                     pk.sendPing = dataPacket.sendPing;
                     pk.sendPong = dataPacket.sendPing + 1000L;
                     pk.encode();
                     sendPacket = new EncapsulatedPacket();
                     sendPacket.reliability = 0;
                     sendPacket.buffer = pk.buffer;
                     this.addToQueue(sendPacket, 1);
                  } else if (id == CLIENT_HANDSHAKE_DataPacket.ID) {
                     CLIENT_HANDSHAKE_DataPacket dataPacket = new CLIENT_HANDSHAKE_DataPacket();
                     dataPacket.buffer = packet.buffer;
                     dataPacket.decode();
                     if (dataPacket.port == this.sessionManager.getPort() || !this.sessionManager.portChecking) {
                        this.state = 3;
                        this.isTemporal = false;
                        this.sessionManager.openSession(this);
                     }
                  }
               } else if (id == CLIENT_DISCONNECT_DataPacket.ID) {
                  this.disconnect("client disconnect");
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
               }
            } else if (this.state == 3) {
               this.sessionManager.streamEncapsulated(this, packet);
            }

         }
      }
   }

   public void handlePacket(Packet packet) throws Exception {
      this.isActive = true;
      this.lastUpdate = System.currentTimeMillis();
      if (this.state != 3 && this.state != 2) {
         if ((packet.buffer[0] & 255) > 0 || (packet.buffer[0] & 255) < -128) {
            packet.decode();
            if (packet instanceof OPEN_CONNECTION_REQUEST_1) {
               OPEN_CONNECTION_REPLY_1 pk = new OPEN_CONNECTION_REPLY_1();
               pk.mtuSize = ((OPEN_CONNECTION_REQUEST_1)packet).mtuSize;
               pk.serverID = this.sessionManager.getID();
               this.sendPacket(pk);
               this.state = 1;
            } else if (this.state == 1 && packet instanceof OPEN_CONNECTION_REQUEST_2) {
               this.id = ((OPEN_CONNECTION_REQUEST_2)packet).clientID;
               if (((OPEN_CONNECTION_REQUEST_2)packet).serverPort == this.sessionManager.getPort() || !this.sessionManager.portChecking) {
                  this.mtuSize = Math.min(Math.abs(((OPEN_CONNECTION_REQUEST_2)packet).mtuSize), 1464);
                  OPEN_CONNECTION_REPLY_2 pk = new OPEN_CONNECTION_REPLY_2();
                  pk.mtuSize = (short)this.mtuSize;
                  pk.serverID = this.sessionManager.getID();
                  pk.clientAddress = this.address;
                  pk.clientPort = this.port;
                  this.sendPacket(pk);
                  this.state = 2;
               }
            }
         }
      } else {
         int seq;
         Iterator var4;
         Object pk;
         if (((packet.buffer[0] & 255) >= 128 || (packet.buffer[0] & 255) <= 143) && packet instanceof DataPacket) {
            DataPacket dp = (DataPacket)packet;
            dp.decode();
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
                     if (pk instanceof EncapsulatedPacket && ((EncapsulatedPacket)pk).needACK && ((EncapsulatedPacket)pk).messageIndex != null && this.needACK.containsKey(((EncapsulatedPacket)pk).identifierACK)) {
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

   public void close() throws Exception {
      byte[] data = new byte[]{0, 0, 8, 21};
      this.addEncapsulatedToQueue(EncapsulatedPacket.fromBinary(data), 1);
      this.sessionManager = null;
   }
}
