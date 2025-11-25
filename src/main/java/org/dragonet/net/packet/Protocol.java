package org.dragonet.net.packet;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import org.dragonet.net.packet.minecraft.BatchPacket;
import org.dragonet.net.packet.minecraft.ChatPacket;
import org.dragonet.net.packet.minecraft.ClientConnectPacket;
import org.dragonet.net.packet.minecraft.ClientHandshakePacket;
import org.dragonet.net.packet.minecraft.DisconnectPacket;
import org.dragonet.net.packet.minecraft.DropItemPacket;
import org.dragonet.net.packet.minecraft.EntityEventPacket;
import org.dragonet.net.packet.minecraft.InteractPacket;
import org.dragonet.net.packet.minecraft.LoginPacket;
import org.dragonet.net.packet.minecraft.MovePlayerPacket;
import org.dragonet.net.packet.minecraft.PEPacket;
import org.dragonet.net.packet.minecraft.PlayerActionPacket;
import org.dragonet.net.packet.minecraft.PlayerEquipmentPacket;
import org.dragonet.net.packet.minecraft.RemoveBlockPacket;
import org.dragonet.net.packet.minecraft.UnknownPacket;
import org.dragonet.net.packet.minecraft.UpdateBlockPacket;
import org.dragonet.net.packet.minecraft.UseItemPacket;
import org.dragonet.net.packet.minecraft.WindowSetSlotPacket;

public final class Protocol {
   private static final HashMap<Byte, Class<? extends PEPacket>> protocol = new HashMap();

   private static void registerDecoder(byte id, Class<? extends PEPacket> clazz) {
      if (!protocol.containsKey(id)) {
         try {
            clazz.getConstructor(byte[].class);
         } catch (SecurityException | NoSuchMethodException var3) {
            return;
         }

         protocol.put(id, clazz);
      }
   }

   public static PEPacket decode(byte[] data) {
      if (data != null && data.length >= 1) {
         byte pid = data[0];
         if (protocol.containsKey(pid)) {
            Class c = (Class)protocol.get(pid);

            try {
               PEPacket pk = (PEPacket)c.getDeclaredConstructor(byte[].class).newInstance(data);
               pk.decode();
               return pk;
            } catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException var4) {
            }
         }

         return new UnknownPacket(pid, data);
      } else {
         return null;
      }
   }

   static {
      registerDecoder((byte)9, ClientConnectPacket.class);
      registerDecoder((byte)19, ClientHandshakePacket.class);
      registerDecoder((byte)5, DisconnectPacket.class);
      registerDecoder((byte)6, BatchPacket.class);
      registerDecoder((byte)7, ChatPacket.class);
      registerDecoder((byte)5, DisconnectPacket.class);
      registerDecoder((byte)41, DropItemPacket.class);
      registerDecoder((byte)1, LoginPacket.class);
      registerDecoder((byte)16, MovePlayerPacket.class);
      registerDecoder((byte)27, PlayerEquipmentPacket.class);
      registerDecoder((byte)32, PlayerActionPacket.class);
      registerDecoder((byte)18, RemoveBlockPacket.class);
      registerDecoder((byte)19, UpdateBlockPacket.class);
      registerDecoder((byte)31, UseItemPacket.class);
      registerDecoder((byte)44, WindowSetSlotPacket.class);
      registerDecoder((byte)30, InteractPacket.class);
      registerDecoder((byte)24, EntityEventPacket.class);
   }
}
