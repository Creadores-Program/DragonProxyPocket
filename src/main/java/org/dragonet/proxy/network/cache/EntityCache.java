package org.dragonet.proxy.network.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.dragonet.proxy.entity.EntityType;
import org.dragonet.proxy.network.UpstreamSession;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.ObjectType;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnMobPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnObjectPacket;
import org.spacehq.mc.protocol.packet.ingame.server.entity.spawn.ServerSpawnPlayerPacket;

public final class EntityCache {
   private final UpstreamSession upstream;
   private final Map<Integer, CachedEntity> entities = Collections.synchronizedMap(new HashMap());
   private final List<Integer> playerEntities = Collections.synchronizedList(new ArrayList());

   public EntityCache(UpstreamSession upstream) {
      this.upstream = upstream;
      this.reset(false);
   }

   public void reset(boolean clear) {
      if (clear) {
         this.entities.clear();
      }

      CachedEntity clientEntity = new CachedEntity(0, -1, (EntityType)null, (ObjectType)null, true, (UUID)null);
      this.entities.put(0, clientEntity);
   }

   public CachedEntity getClientEntity() {
      return (CachedEntity)this.entities.get(0);
   }

   public CachedEntity get(int eid) {
      return (CachedEntity)this.entities.get(eid);
   }

   public CachedEntity remove(int eid) {
      CachedEntity e = (CachedEntity)this.entities.remove(eid);
      if (e == null) {
         return null;
      } else {
         this.playerEntities.remove(eid);
         return e;
      }
   }

   public CachedEntity newEntity(ServerSpawnMobPacket packet) {
      EntityType peType = EntityType.convertToPE(packet.getType());
      if (peType == null) {
         return null;
      } else {
         CachedEntity e = new CachedEntity(packet.getEntityId(), (Integer)MagicValues.value(Integer.class, packet.getType()), peType, (ObjectType)null, false, (UUID)null);
         e.x = packet.getX();
         e.y = packet.getY();
         e.z = packet.getZ();
         e.motionX = packet.getMotionX();
         e.motionY = packet.getMotionY();
         e.motionZ = packet.getMotionZ();
         e.yaw = packet.getYaw();
         e.pitch = packet.getPitch();
         e.pcMeta = packet.getMetadata();
         e.spawned = true;
         this.entities.put(e.eid, e);
         return e;
      }
   }

   public CachedEntity newPlayer(ServerSpawnPlayerPacket packet) {
      CachedEntity e = new CachedEntity(packet.getEntityId(), -1, (EntityType)null, (ObjectType)null, true, packet.getUUID());
      e.x = packet.getX();
      e.y = packet.getY();
      e.z = packet.getZ();
      e.yaw = packet.getYaw();
      e.pitch = packet.getPitch();
      e.pcMeta = packet.getMetadata();
      e.spawned = true;
      this.entities.put(e.eid, e);
      this.playerEntities.add(e.eid);
      return e;
   }

   public CachedEntity newObject(ServerSpawnObjectPacket packet) {
      CachedEntity e = new CachedEntity(packet.getEntityId(), -1, (EntityType)null, packet.getType(), false, (UUID)null);
      e.x = packet.getX();
      e.y = packet.getY();
      e.z = packet.getZ();
      e.motionX = packet.getMotionX();
      e.motionY = packet.getMotionY();
      e.motionZ = packet.getMotionZ();
      e.yaw = packet.getYaw();
      e.pitch = packet.getPitch();
      e.spawned = false;
      this.entities.put(e.eid, e);
      return e;
   }

   public boolean isPlayerEntity(int eid) {
      return this.playerEntities.contains(eid);
   }

   public void onTick() {
   }

   public UpstreamSession getUpstream() {
      return this.upstream;
   }

   public Map<Integer, CachedEntity> getEntities() {
      return this.entities;
   }
}
