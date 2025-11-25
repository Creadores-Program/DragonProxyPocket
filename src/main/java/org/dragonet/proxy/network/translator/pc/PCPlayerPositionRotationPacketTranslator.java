package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.MovePlayerPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.cache.CachedEntity;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;

public class PCPlayerPositionRotationPacketTranslator implements PCPacketTranslator<ServerPlayerPositionRotationPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerPlayerPositionRotationPacket packet) {
      MovePlayerPacket pk = new MovePlayerPacket(0L, (float)packet.getX(), (float)packet.getY(), (float)packet.getZ(), packet.getYaw(), packet.getPitch(), packet.getYaw(), false);
      CachedEntity cliEntity = session.getEntityCache().getClientEntity();
      cliEntity.x = packet.getX();
      cliEntity.y = packet.getY();
      cliEntity.z = packet.getZ();
      return new PEPacket[]{pk};
   }
}
