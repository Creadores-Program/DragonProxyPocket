package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class LevelEventPacket extends PEPacket {
   public static final byte TAME_FAIL = 6;
   public static final byte TAME_SUCCESS = 7;
   public static final byte SHAKE_WET = 8;
   public static final byte USE_ITEM = 9;
   public static final byte EAT_GRASS_ANIMATION = 10;
   public static final byte FISH_HOOK_BUBBLE = 11;
   public static final byte FISH_HOOK_POSITION = 12;
   public static final byte FISH_HOOK_HOOK = 13;
   public static final byte FISH_HOOK_TEASE = 14;
   public static final byte SQUID_INK_CLOUD = 15;
   public static final byte AMBIENT_SOUND = 16;
   public static final byte RESPAWN = 17;
   public short eventID;
   public float x;
   public float y;
   public float z;
   public int data;

   public int pid() {
      return 22;
   }

   public void encode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_WORLD_EVENTS);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeShort(this.eventID);
         writer.writeFloat(this.x);
         writer.writeFloat(this.y);
         writer.writeFloat(this.z);
         writer.writeInt(this.data);
         this.setData(bos.toByteArray());
      } catch (IOException var3) {
      }

   }

   public void decode() {
   }

   public static class Events {
      public static final short EVENT_SOUND_CLICK = 1000;
      public static final short EVENT_SOUND_CLICK_FAIL = 1001;
      public static final short EVENT_SOUND_SHOOT = 1002;
      public static final short EVENT_SOUND_DOOR = 1003;
      public static final short EVENT_SOUND_DOOR_OPEN = 1003;
      public static final short EVENT_SOUND_DOOR_CLOSE = 1003;
      public static final short EVENT_SOUND_FIZZ = 1004;
      public static final short EVENT_SOUND_GHAST = 1007;
      public static final short EVENT_SOUND_GHAST_SHOOT = 1008;
      public static final short EVENT_SOUND_BLAZE_SHOOT = 1009;
      public static final short EVENT_SOUND_DOOR_BUMP = 1010;
      public static final short EVENT_SOUND_POUND_WOODEN_DOOR = 1010;
      public static final short EVENT_SOUND_POUND_METAL_DOOR = 1010;
      public static final short EVENT_SOUND_BREAK_WOODEN_DOOR = 1012;
      public static final short EVENT_SOUND_BAT_FLY = 1015;
      public static final short EVENT_SOUND_ZOMBIE_INFECT = 1016;
      public static final short EVENT_SOUND_ZOMBIE_HEAL = 1017;
      public static final short EVENT_SOUND_ANVIL_BREAK = 1020;
      public static final short EVENT_SOUND_ANVIL_USE = 1021;
      public static final short EVENT_SOUND_ANVIL_LAND = 1022;
      public static final short EVENT_PARTICLE_SHOOT = 2000;
      public static final short EVENT_PARTICLE_DESTROY = 2001;
      public static final short EVENT_PARTICLE_SPLASH = 2002;
      public static final short EVENT_PARTICLE_EYE_DESPAWN = 2003;
      public static final short EVENT_PARTICLE_SPAWN = 2004;
      public static final short EVENT_START_RAIN = 3001;
      public static final short EVENT_START_THUNDER = 3002;
      public static final short EVENT_STOP_RAIN = 3003;
      public static final short EVENT_STOP_THUNDER = 3004;
      public static final short EVENT_SET_DATA = 4000;
      public static final short EVENT_PLAYERS_SLEEPING = 9800;
      public static final short EVENT_ADD_PARTICLE_MASK = 16384;
   }
}
