package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.ChatPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.MessageTranslator;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.packet.ingame.server.ServerChatPacket;

public class PCChatPacketTranslator implements PCPacketTranslator<ServerChatPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerChatPacket packet) {
      ChatPacket ret = new ChatPacket();
      ret.source = "";
      ret.message = MessageTranslator.translate(packet.getMessage());
      switch(packet.getType()) {
      case CHAT:
         ret.type = ChatPacket.TextType.CHAT;
         break;
      case NOTIFICATION:
      case SYSTEM:
      default:
         ret.type = ChatPacket.TextType.CHAT;
      }

      return new PEPacket[]{ret};
   }
}
