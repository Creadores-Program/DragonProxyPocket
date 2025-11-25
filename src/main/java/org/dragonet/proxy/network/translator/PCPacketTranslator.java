package org.dragonet.proxy.network.translator;

import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.spacehq.packetlib.packet.Packet;

public interface PCPacketTranslator<P extends Packet> {
   PEPacket[] translate(UpstreamSession var1, P var2);
}
