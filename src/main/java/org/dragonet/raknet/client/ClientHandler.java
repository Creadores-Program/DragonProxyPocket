package org.dragonet.raknet.client;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.dragonet.raknet.protocol.EncapsulatedPacket;

public class ClientHandler {
   protected JRakLibClient client;
   protected ClientInstance instance;

   public ClientHandler(JRakLibClient client, ClientInstance instance) {
      this.client = client;
      this.instance = instance;
   }

   public void sendEncapsulated(EncapsulatedPacket packet) {
      byte flags = 0;
      this.sendEncapsulated("", packet, flags);
   }

   public void sendEncapsulated(String identifier, EncapsulatedPacket packet, int flags) {
      ByteBuffer bb = ByteBuffer.allocate(packet.getTotalLength());
      bb.put((byte)1).put((byte)identifier.getBytes().length).put(identifier.getBytes()).put((byte)(flags & 255)).put(packet.toBinary(true));
      this.client.pushMainToThreadPacket(Arrays.copyOf(bb.array(), bb.position()));
      bb = null;
   }

   public void sendRaw(byte[] payload) {
      this.sendRaw(this.client.getServerIP(), (short)this.client.getServerPort(), payload);
   }

   public void sendRaw(String address, short port, byte[] payload) {
      ByteBuffer bb = ByteBuffer.allocate(4 + address.getBytes().length + payload.length);
      bb.put((byte)8).put((byte)address.getBytes().length).put(address.getBytes()).put(Binary.writeShort(port)).put(payload);
      this.client.pushMainToThreadPacket(bb.array());
   }

   public void sendOption(String name, String value) {
      ByteBuffer bb = ByteBuffer.allocate(2 + name.getBytes().length + value.getBytes().length);
      bb.put((byte)7).put((byte)name.getBytes().length).put(name.getBytes()).put(value.getBytes());
      this.client.pushMainToThreadPacket(bb.array());
   }

   public void disconnectFromServer() {
      this.shutdown();
   }

   public void shutdown() {
      this.client.shutdown();
      this.client.pushMainToThreadPacket(new byte[]{126});
   }

   public void emergencyShutdown() {
      this.client.shutdown();
      this.client.pushMainToThreadPacket(new byte[]{127});
   }

   public boolean handlePacket() {
      byte[] packet = this.client.readThreadToMainPacket();
      if (packet == null) {
         return false;
      } else if (packet.length > 0) {
         byte id = packet[0];
         int offset = 1;
         byte len;
         int offset;
         if (id == 1) {
            offset = offset + 1;
            len = packet[offset];
            offset += len;
            byte flags = packet[offset++];
            byte[] buffer = Binary.subbytes(packet, offset);
            this.instance.handleEncapsulated(EncapsulatedPacket.fromBinary(buffer, true), flags);
         } else if (id == 8) {
            offset = offset + 1;
            len = packet[offset];
            new String(Binary.subbytes(packet, offset, len));
            offset += len;
            int port = Binary.readShort(Binary.subbytes(packet, offset, 2));
            offset += 2;
            byte[] payload = Binary.subbytes(packet, offset);
            this.instance.handleRaw(payload);
         } else {
            String reason;
            if (id == 7) {
               offset = offset + 1;
               len = packet[offset];
               String name = new String(Binary.subbytes(packet, offset, len));
               offset += len;
               reason = new String(Binary.subbytes(packet, offset));
               this.instance.handleOption(name, reason);
            } else if (id == 2) {
               offset = offset + 1;
               len = packet[offset];
               new String(Binary.subbytes(packet, offset, len));
               offset += len;
               long serverID = Binary.readLong(Binary.subbytes(packet, offset, 8));
               this.instance.connectionOpened(serverID);
            } else if (id == 3) {
               offset = offset + 1;
               len = packet[offset];
               new String(Binary.subbytes(packet, offset, len));
               offset += len;
               len = packet[offset++];
               reason = new String(Binary.subbytes(packet, offset, len));
               this.instance.connectionClosed(reason);
            }
         }

         return true;
      } else {
         return false;
      }
   }
}
