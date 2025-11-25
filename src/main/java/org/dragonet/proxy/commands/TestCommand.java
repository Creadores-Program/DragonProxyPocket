package org.dragonet.proxy.commands;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import org.dragonet.net.packet.minecraft.FullChunkPacket;
import org.dragonet.proxy.DragonProxy;
import org.dragonet.proxy.network.UpstreamSession;

public class TestCommand implements ConsoleCommand {
   public void execute(DragonProxy proxy, String[] args) {
      UpstreamSession cli = ((UpstreamSession[])proxy.getSessionRegister().getAll().values().toArray(new UpstreamSession[0]))[0];
      cli.sendChat("Initiating... ");
   }

   private void sendFarChunk(UpstreamSession cli, int x, int z) {
      try {
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         byte[] yPreset = new byte[128];
         Arrays.fill(yPreset, 0, 64, (byte)1);
         Arrays.fill(yPreset, 64, 128, (byte)0);

         for(int xz = 0; xz < 256; ++xz) {
            bos.write(yPreset);
         }

         bos.write(new byte[16384]);
         byte[] lightPreset = new byte[16384];
         Arrays.fill(lightPreset, (byte)-1);
         bos.write(lightPreset);
         bos.write(lightPreset);

         int i;
         for(i = 0; i < 256; ++i) {
            bos.write(-1);
         }

         for(i = 0; i < 256; ++i) {
            bos.write(1);
            bos.write(-123);
            bos.write(-78);
            bos.write(74);
         }

         bos.write(new byte[4]);
         FullChunkPacket pkChunk = new FullChunkPacket();
         pkChunk.chunkX = x;
         pkChunk.chunkZ = z;
         pkChunk.chunkData = bos.toByteArray();
         cli.sendPacket(pkChunk, true);
      } catch (Exception var8) {
         var8.printStackTrace();
      }

   }
}
