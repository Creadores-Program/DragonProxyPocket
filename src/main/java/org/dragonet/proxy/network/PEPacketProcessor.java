package org.dragonet.proxy.network;

import java.util.ArrayDeque;
import java.util.Deque;
import org.dragonet.net.packet.Protocol;
import org.dragonet.net.packet.minecraft.BatchPacket;
import org.dragonet.net.packet.minecraft.LoginPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.spacehq.packetlib.packet.Packet;

public class PEPacketProcessor implements Runnable {
   public static final int MAX_PACKETS_PER_CYCLE = 200;
   private final UpstreamSession client;
   private final Deque<byte[]> packets = new ArrayDeque();

   public PEPacketProcessor(UpstreamSession client) {
      this.client = client;
   }

   public void putPacket(byte[] packet) {
      this.packets.add(packet);
   }

   public void run() {
      int cnt = 0;

      while(cnt < 200 && !this.packets.isEmpty()) {
         ++cnt;
         byte[] bin = (byte[])this.packets.pop();
         PEPacket packet = Protocol.decode(bin);
         if (packet != null) {
            this.handlePacket(packet);
         }
      }

   }

   public void handlePacket(PEPacket packet) {
      if (packet != null) {
         if (BatchPacket.class.isAssignableFrom(packet.getClass())) {
            ((BatchPacket)packet).packets.stream().filter((pk) -> {
               return pk != null;
            }).forEach((pk) -> {
               this.handlePacket(pk);
            });
         } else {
            switch(packet.pid()) {
            case 1:
               this.client.onLogin((LoginPacket)packet);
               break;
            case 7:
               if (this.client.getDataCache().get("auth_state") != null) {
                  PacketTranslatorRegister.translateToPC(this.client, packet);
                  break;
               }
            default:
               if (this.client.getDownstream() != null && this.client.getDownstream().isConnected()) {
                  Packet[] translated = PacketTranslatorRegister.translateToPC(this.client, packet);
                  if (translated != null && translated.length != 0) {
                     this.client.getDownstream().send((Object[])translated);
                  }
               }
            }

         }
      }
   }

   public UpstreamSession getClient() {
      return this.client;
   }
}
