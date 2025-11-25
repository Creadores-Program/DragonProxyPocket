package org.dragonet.raknet.server;

import java.nio.charset.StandardCharsets;
import org.dragonet.proxy.utilities.Binary;
import org.dragonet.raknet.protocol.EncapsulatedPacket;

public class ServerHandler {
   protected RakNetServer server;
   protected ServerInstance instance;

   public ServerHandler(RakNetServer server, ServerInstance instance) {
      this.server = server;
      this.instance = instance;
   }

   public void sendEncapsulated(String identifier, EncapsulatedPacket packet) {
      this.sendEncapsulated(identifier, packet, 0);
   }

   public void sendEncapsulated(String identifier, EncapsulatedPacket packet, int flags) {
      byte[] buffer = Binary.appendBytes((byte)1, new byte[]{(byte)(identifier.length() & 255)}, identifier.getBytes(StandardCharsets.UTF_8), new byte[]{(byte)(flags & 255)}, packet.toBinary(true));
      this.server.pushMainToThreadPacket(buffer);
   }

   public void sendRaw(String address, int port, byte[] payload) {
      byte[] buffer = Binary.appendBytes((byte)8, new byte[]{(byte)(address.length() & 255)}, address.getBytes(StandardCharsets.UTF_8), Binary.writeShort(port), payload);
      this.server.pushMainToThreadPacket(buffer);
   }

   public void closeSession(String identifier, String reason) {
      byte[] buffer = Binary.appendBytes((byte)3, new byte[]{(byte)(identifier.length() & 255)}, identifier.getBytes(StandardCharsets.UTF_8), new byte[]{(byte)(reason.length() & 255)}, reason.getBytes(StandardCharsets.UTF_8));
      this.server.pushMainToThreadPacket(buffer);
   }

   public void sendOption(String name, String value) {
      byte[] buffer = Binary.appendBytes((byte)7, new byte[]{(byte)(name.length() & 255)}, name.getBytes(StandardCharsets.UTF_8), value.getBytes(StandardCharsets.UTF_8));
      this.server.pushMainToThreadPacket(buffer);
   }

   public void blockAddress(String address, int timeout) {
      byte[] buffer = Binary.appendBytes((byte)9, new byte[]{(byte)(address.length() & 255)}, address.getBytes(StandardCharsets.UTF_8), Binary.writeInt(timeout));
      this.server.pushMainToThreadPacket(buffer);
   }

   public void shutdown() {
      this.server.pushMainToThreadPacket(new byte[]{126});
      this.server.shutdown();
      synchronized(this) {
         try {
            this.wait(20L);
         } catch (InterruptedException var5) {
         }
      }

      try {
         this.server.join();
      } catch (InterruptedException var4) {
      }

   }

   public void emergencyShutdown() {
      this.server.shutdown();
      this.server.pushMainToThreadPacket(new byte[]{127});
   }

   protected void invalidSession(String identifier) {
      byte[] buffer = Binary.appendBytes((byte)4, new byte[]{(byte)(identifier.length() & 255)}, identifier.getBytes(StandardCharsets.UTF_8));
      this.server.pushMainToThreadPacket(buffer);
   }

   public boolean handlePacket() {
      byte[] packet = this.server.readThreadToMainPacket();
      if (packet != null && packet.length > 0) {
         byte id = packet[0];
         int offset = 1;
         byte len;
         String identifier;
         byte[] payload;
         int offset;
         if (id == 1) {
            offset = offset + 1;
            len = packet[offset];
            identifier = new String(Binary.subBytes(packet, offset, len), StandardCharsets.UTF_8);
            offset += len;
            int flags = packet[offset++];
            payload = Binary.subBytes(packet, offset);
            this.instance.handleEncapsulated(identifier, EncapsulatedPacket.fromBinary(payload, true), flags);
         } else {
            int identifierACK;
            if (id == 8) {
               offset = offset + 1;
               len = packet[offset];
               identifier = new String(Binary.subBytes(packet, offset, len), StandardCharsets.UTF_8);
               offset += len;
               identifierACK = Binary.readShort(Binary.subBytes(packet, offset, 2)) & '\uffff';
               offset += 2;
               payload = Binary.subBytes(packet, offset);
               this.instance.handleRaw(identifier, identifierACK, payload);
            } else {
               String reason;
               if (id == 7) {
                  offset = offset + 1;
                  len = packet[offset];
                  identifier = new String(Binary.subBytes(packet, offset, len), StandardCharsets.UTF_8);
                  offset += len;
                  reason = new String(Binary.subBytes(packet, offset), StandardCharsets.UTF_8);
                  this.instance.handleOption(identifier, reason);
               } else if (id == 2) {
                  offset = offset + 1;
                  len = packet[offset];
                  identifier = new String(Binary.subBytes(packet, offset, len), StandardCharsets.UTF_8);
                  offset += len;
                  len = packet[offset++];
                  reason = new String(Binary.subBytes(packet, offset, len), StandardCharsets.UTF_8);
                  offset += len;
                  int port = Binary.readShort(Binary.subBytes(packet, offset, 2)) & '\uffff';
                  offset += 2;
                  long clientID = Binary.readLong(Binary.subBytes(packet, offset, 8));
                  this.instance.openSession(identifier, reason, port, clientID);
               } else if (id == 3) {
                  offset = offset + 1;
                  len = packet[offset];
                  identifier = new String(Binary.subBytes(packet, offset, len), StandardCharsets.UTF_8);
                  offset += len;
                  len = packet[offset++];
                  reason = new String(Binary.subBytes(packet, offset, len), StandardCharsets.UTF_8);
                  this.instance.closeSession(identifier, reason);
               } else if (id == 4) {
                  offset = offset + 1;
                  len = packet[offset];
                  identifier = new String(Binary.subBytes(packet, offset, len), StandardCharsets.UTF_8);
                  this.instance.closeSession(identifier, "Invalid session");
               } else if (id == 6) {
                  offset = offset + 1;
                  len = packet[offset];
                  identifier = new String(Binary.subBytes(packet, offset, len), StandardCharsets.UTF_8);
                  offset += len;
                  identifierACK = Binary.readInt(Binary.subBytes(packet, offset, 4));
                  this.instance.notifyACK(identifier, identifierACK);
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }
}
