package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.LoginStatusPacket;
import org.dragonet.net.packet.minecraft.MovePlayerPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.net.packet.minecraft.StartGamePacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.data.game.values.entity.player.GameMode;
import org.spacehq.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerSpawnPositionPacket;

public class PCSpawnPositionPacketTranslator implements PCPacketTranslator<ServerSpawnPositionPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerSpawnPositionPacket packet) {
      if (session.getDataCache().get("achedJoinGamePacket") == null) {
         if (session.getProxy().getAuthMode().equals("online")) {
            session.sendChat(session.getProxy().getLang().get("message_teleport_to_spawn"));
            MovePlayerPacket pkMovePlayer = new MovePlayerPacket(0L, (float)packet.getPosition().getX(), (float)packet.getPosition().getY(), (float)packet.getPosition().getZ(), 0.0F, 0.0F, 0.0F, false);
            pkMovePlayer.mode = 1;
            return new PEPacket[]{pkMovePlayer};
         } else {
            session.disconnect(session.getProxy().getLang().get("message_remote_error"));
            return null;
         }
      } else {
         ServerJoinGamePacket restored = (ServerJoinGamePacket)session.getDataCache().remove("achedJoinGamePacket");
         StartGamePacket ret = new StartGamePacket();
         ret.eid = 0L;
         ret.dimension = (byte)(restored.getDimension() & 255);
         ret.seed = 0;
         ret.generator = 1;
         ret.gamemode = restored.getGameMode() == GameMode.CREATIVE ? 1 : 0;
         ret.spawnX = packet.getPosition().getX();
         ret.spawnY = packet.getPosition().getY();
         ret.spawnZ = packet.getPosition().getZ();
         ret.x = (float)packet.getPosition().getX();
         ret.y = (float)packet.getPosition().getY();
         ret.z = (float)packet.getPosition().getZ();
         LoginStatusPacket stat = new LoginStatusPacket();
         stat.status = 3;
         return new PEPacket[]{ret, stat};
      }
   }
}
