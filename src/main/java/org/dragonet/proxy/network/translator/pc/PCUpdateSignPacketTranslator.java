package org.dragonet.proxy.network.translator.pc;

import org.dragonet.net.packet.minecraft.BlockEntityDataPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.nbt.tag.CompoundTag;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.MessageTranslator;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerUpdateSignPacket;

public class PCUpdateSignPacketTranslator implements PCPacketTranslator<ServerUpdateSignPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerUpdateSignPacket packet) {
      CompoundTag root = new CompoundTag();
      root.putString("id", "Sign");
      root.putInt("x", packet.getPosition().getX());
      root.putInt("y", packet.getPosition().getY());
      root.putInt("z", packet.getPosition().getZ());
      root.putString("Text1", MessageTranslator.translate(packet.getLines()[0]));
      root.putString("Text2", MessageTranslator.translate(packet.getLines()[1]));
      root.putString("Text3", MessageTranslator.translate(packet.getLines()[2]));
      root.putString("Text4", MessageTranslator.translate(packet.getLines()[3]));
      return new PEPacket[]{new BlockEntityDataPacket(packet.getPosition().getX(), packet.getPosition().getY(), packet.getPosition().getZ(), root)};
   }
}
