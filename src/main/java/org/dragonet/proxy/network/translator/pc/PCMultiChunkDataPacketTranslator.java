package org.dragonet.proxy.network.translator.pc;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import org.dragonet.net.packet.minecraft.FullChunkPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.ItemBlockTranslator;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.data.game.Chunk;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerMultiChunkDataPacket;

public class PCMultiChunkDataPacketTranslator implements PCPacketTranslator<ServerMultiChunkDataPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerMultiChunkDataPacket packet) {
      session.getProxy().getGeneralThreadPool().execute(() -> {
         ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
         DataOutputStream dos1 = new DataOutputStream(bos1);
         ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
         new DataOutputStream(bos2);
         ByteArrayOutputStream bosTiles = new ByteArrayOutputStream();
         new DataOutputStream(bosTiles);

         try {
            for(int col = 0; col < packet.getColumns(); ++col) {
               bos1.reset();
               bos2.reset();
               bosTiles.reset();
               FullChunkPacket pePacket = new FullChunkPacket();
               pePacket.chunkX = packet.getX(col);
               pePacket.chunkZ = packet.getZ(col);
               pePacket.order = FullChunkPacket.ChunkOrder.COLUMNS;
               Chunk[] pcChunks = packet.getChunks(col);

               int x;
               int z;
               int y;
               for(x = 0; x < 16; ++x) {
                  for(z = 0; z < 16; ++z) {
                     for(y = 0; y < 128; ++y) {
                        if (pcChunks[y >> 4] != null && !pcChunks[y >> 4].isEmpty()) {
                           int pcBlock = pcChunks[y >> 4].getBlocks().getBlock(x, y % 16, z);
                           int peBlock = ItemBlockTranslator.translateToPE(pcBlock);
                           dos1.writeByte((byte)(peBlock & 255));
                        } else {
                           dos1.writeByte(0);
                        }
                     }
                  }
               }

               byte data;
               for(x = 0; x < 16; ++x) {
                  for(z = 0; z < 16; ++z) {
                     for(y = 0; y < 128; y += 2) {
                        data = 0;
                        byte data2 = 0;

                        try {
                           data = (byte)pcChunks[y >> 4].getBlocks().getData(x, y % 16, z);
                        } catch (Exception var19) {
                        }

                        try {
                           data2 = (byte)pcChunks[y >> 4].getBlocks().getData(x, (y + 1) % 16, z);
                        } catch (Exception var18) {
                        }

                        data = (byte)(data | (data2 & 15) << 4);
                        dos1.writeByte(data);
                     }
                  }
               }

               for(x = 0; x < 16; ++x) {
                  for(z = 0; z < 16; ++z) {
                     for(y = 0; y < 128; y += 2) {
                        data = 0;

                        try {
                           data = (byte)(pcChunks[y >> 4].getSkyLight().get(x, y & 15, z) & 15);
                           data = (byte)(data | pcChunks[y >> 4].getSkyLight().get(x, y + 1 & 15, z) & 15);
                        } catch (Exception var17) {
                        }

                        dos1.writeByte(data);
                     }
                  }
               }

               dos1.write(bos2.toByteArray());
               bos2.reset();

               for(x = 0; x < 16; ++x) {
                  for(z = 0; z < 16; ++z) {
                     for(y = 0; y < 128; y += 2) {
                        data = pcChunks[y >> 4] != null && !pcChunks[y >> 4].isEmpty() ? (byte)((pcChunks[y >> 4].getBlockLight().get(x, y % 16, z) & 15) << 4) : 0;
                        data |= pcChunks[y + 1 >> 4] != null && !pcChunks[y + 1 >> 4].isEmpty() ? (byte)(pcChunks[y + 1 >> 4].getBlockLight().get(x, (y + 1) % 16, z) & 15) : 0;
                        dos1.writeByte(data);
                     }
                  }
               }

               for(x = 0; x < 256; ++x) {
                  dos1.writeByte(-1);
               }

               for(x = 0; x < 256; ++x) {
                  dos1.writeByte(1);
                  dos1.writeByte(-123);
                  dos1.writeByte(-78);
                  dos1.writeByte(74);
               }

               bos2.reset();
               dos1.writeInt(0);
               dos1.write(bosTiles.toByteArray());
               pePacket.chunkData = bos1.toByteArray();
               session.sendPacket(pePacket, true);
            }
         } catch (Exception var20) {
         }

      });
      return null;
   }
}
