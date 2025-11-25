package org.dragonet.proxy.network.translator.pe;

import org.dragonet.net.packet.minecraft.ChatPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.PEPacketTranslator;
import org.dragonet.proxy.utilities.PatternChecker;
import org.spacehq.mc.protocol.packet.ingame.client.ClientChatPacket;
import org.spacehq.packetlib.packet.Packet;

public class PEChatPacketTranslator implements PEPacketTranslator<ChatPacket> {
   public Packet[] translate(UpstreamSession session, ChatPacket packet) {
      if (session.getDataCache().get("auth_state") == null) {
         ClientChatPacket pk = new ClientChatPacket(packet.message);
         return new Packet[]{pk};
      } else {
         if (session.getDataCache().get("auth_state").equals("email")) {
            if (!PatternChecker.matchEmail(packet.message.trim())) {
               session.sendChat(session.getProxy().getLang().get("message_online_error"));
               session.disconnect(session.getProxy().getLang().get("message_online_error"));
               return null;
            }

            session.getDataCache().put("auth_mail", packet.message.trim());
            session.getDataCache().put("auth_state", "password");
            session.sendChat(session.getProxy().getLang().get("message_online_password"));
         } else if (session.getDataCache().get("auth_state").equals("password")) {
            if (session.getDataCache().get("auth_mail") == null || packet.message.equals(" ")) {
               session.sendChat(session.getProxy().getLang().get("message_online_error"));
               session.disconnect(session.getProxy().getLang().get("message_online_error"));
               return null;
            }

            session.sendChat(session.getProxy().getLang().get("message_online_logging_in"));
            session.getDataCache().remove("auth_state");
            session.authenticate(packet.message);
         }

         return null;
      }
   }
}
