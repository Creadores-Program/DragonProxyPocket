package org.dragonet.proxy.network.translator.pc;

import java.lang.reflect.Field;
import org.dragonet.net.packet.minecraft.LevelEventPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.proxy.network.UpstreamSession;
import org.dragonet.proxy.network.translator.PCPacketTranslator;
import org.spacehq.mc.protocol.data.game.values.world.CustomSound;
import org.spacehq.mc.protocol.data.game.values.world.GenericSound;
import org.spacehq.mc.protocol.packet.ingame.server.world.ServerPlaySoundPacket;

public class PCPlaySoundPacketTranslator implements PCPacketTranslator<ServerPlaySoundPacket> {
   public PEPacket[] translate(UpstreamSession session, ServerPlaySoundPacket packet) {
      try {
         String soundName = null;
         Field[] var5;
         int var6;
         int var7;
         Field f;
         if (GenericSound.class.isAssignableFrom(packet.getSound().getClass())) {
            GenericSound sound = (GenericSound)packet.getSound();
            var5 = GenericSound.class.getDeclaredFields();
            var6 = var5.length;

            for(var7 = 0; var7 < var6; ++var7) {
               f = var5[var7];
               boolean saved = f.isAccessible();
               f.setAccessible(true);
               if (f.get((Object)null).equals(sound)) {
                  soundName = f.getName();
               }

               f.setAccessible(saved);
            }
         } else {
            soundName = ((CustomSound)packet.getSound()).getName();
         }

         if (soundName == null) {
            return null;
         } else {
            short ev = 0;
            var5 = LevelEventPacket.Events.class.getDeclaredFields();
            var6 = var5.length;

            for(var7 = 0; var7 < var6; ++var7) {
               f = var5[var7];
               if (f.getType().equals(Short.TYPE) && f.getName().equalsIgnoreCase("EVENT_SOUND_" + soundName)) {
                  ev = (Short)f.get((Object)null);
               }
            }

            if (ev == 0) {
               return null;
            } else {
               LevelEventPacket pkSound = new LevelEventPacket();
               pkSound.eventID = (short)(16384 | ev);
               pkSound.x = (float)packet.getX();
               pkSound.y = (float)packet.getY();
               pkSound.z = (float)packet.getZ();
               return new PEPacket[]{pkSound};
            }
         }
      } catch (Exception var10) {
         var10.printStackTrace();
         return null;
      }
   }
}
