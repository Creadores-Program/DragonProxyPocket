package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.data.game.values.PlayerListEntry;
import org.spacehq.mc.protocol.data.game.values.PlayerListEntryAction;
import org.spacehq.mc.protocol.packet.ingame.server.ServerPlayerListEntryPacket;

public class PCPlayerListItemPacketTranslator implements PCPacketTranslator<ServerPlayerListEntryPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerPlayerListEntryPacket packet) {
      PlayerListEntry[] entries;
      PlayerListEntry[] var4;
      int var5;
      int var6;
      PlayerListEntry entrie;
      if (packet.getAction() == PlayerListEntryAction.ADD_PLAYER) {
         entries = packet.getEntries();
         var4 = entries;
         var5 = entries.length;

         for(var6 = 0; var6 < var5; ++var6) {
            entrie = var4[var6];
            session.getPlayerInfoCache().put(entrie.getProfile().getId(), entrie);
         }
      } else if (packet.getAction() == PlayerListEntryAction.REMOVE_PLAYER) {
         entries = packet.getEntries();
         var4 = entries;
         var5 = entries.length;

         for(var6 = 0; var6 < var5; ++var6) {
            entrie = var4[var6];
            session.getPlayerInfoCache().remove(entrie.getProfile().getId());
         }
      }

      return null;
   }
}
