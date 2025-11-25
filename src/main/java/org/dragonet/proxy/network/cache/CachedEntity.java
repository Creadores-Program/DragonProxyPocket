package org.dragonet.proxy.network.cache;

import java.beans.ConstructorProperties;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.dragonet.proxy.entity.EntityType;
import org.spacehq.mc.protocol.data.game.EntityMetadata;
import org.spacehq.mc.protocol.data.game.values.entity.ObjectType;

public class CachedEntity {
   public final int eid;
   public final int pcType;
   public final EntityType peType;
   public final ObjectType objType;
   public final boolean player;
   public final UUID playerUniqueId;
   public double x;
   public double y;
   public double z;
   public double motionX;
   public double motionY;
   public double motionZ;
   public float yaw;
   public float pitch;
   public EntityMetadata[] pcMeta;
   public boolean spawned;
   public final Set<Integer> effects = Collections.synchronizedSet(new HashSet());

   public CachedEntity relativeMove(double rx, double ry, double rz, float yaw, float pitch) {
      this.x += rx;
      this.y += ry;
      this.z += rz;
      this.yaw = yaw;
      this.pitch = pitch;
      return this;
   }

   public CachedEntity relativeMove(double rx, double ry, double rz) {
      this.x += rx;
      this.y += ry;
      this.z += rz;
      return this;
   }

   @ConstructorProperties({"eid", "pcType", "peType", "objType", "player", "playerUniqueId"})
   public CachedEntity(int eid, int pcType, EntityType peType, ObjectType objType, boolean player, UUID playerUniqueId) {
      this.eid = eid;
      this.pcType = pcType;
      this.peType = peType;
      this.objType = objType;
      this.player = player;
      this.playerUniqueId = playerUniqueId;
   }

   public int getEid() {
      return this.eid;
   }

   public int getPcType() {
      return this.pcType;
   }

   public EntityType getPeType() {
      return this.peType;
   }

   public ObjectType getObjType() {
      return this.objType;
   }

   public boolean isPlayer() {
      return this.player;
   }

   public UUID getPlayerUniqueId() {
      return this.playerUniqueId;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public double getMotionX() {
      return this.motionX;
   }

   public double getMotionY() {
      return this.motionY;
   }

   public double getMotionZ() {
      return this.motionZ;
   }

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public EntityMetadata[] getPcMeta() {
      return this.pcMeta;
   }

   public boolean isSpawned() {
      return this.spawned;
   }

   public Set<Integer> getEffects() {
      return this.effects;
   }

   public void setX(double x) {
      this.x = x;
   }

   public void setY(double y) {
      this.y = y;
   }

   public void setZ(double z) {
      this.z = z;
   }

   public void setMotionX(double motionX) {
      this.motionX = motionX;
   }

   public void setMotionY(double motionY) {
      this.motionY = motionY;
   }

   public void setMotionZ(double motionZ) {
      this.motionZ = motionZ;
   }

   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   public void setPcMeta(EntityMetadata[] pcMeta) {
      this.pcMeta = pcMeta;
   }

   public void setSpawned(boolean spawned) {
      this.spawned = spawned;
   }

   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof CachedEntity)) {
         return false;
      } else {
         CachedEntity other = (CachedEntity)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (this.getEid() != other.getEid()) {
            return false;
         } else if (this.getPcType() != other.getPcType()) {
            return false;
         } else {
            label108: {
               Object this$peType = this.getPeType();
               Object other$peType = other.getPeType();
               if (this$peType == null) {
                  if (other$peType == null) {
                     break label108;
                  }
               } else if (this$peType.equals(other$peType)) {
                  break label108;
               }

               return false;
            }

            Object this$objType = this.getObjType();
            Object other$objType = other.getObjType();
            if (this$objType == null) {
               if (other$objType != null) {
                  return false;
               }
            } else if (!this$objType.equals(other$objType)) {
               return false;
            }

            if (this.isPlayer() != other.isPlayer()) {
               return false;
            } else {
               label93: {
                  Object this$playerUniqueId = this.getPlayerUniqueId();
                  Object other$playerUniqueId = other.getPlayerUniqueId();
                  if (this$playerUniqueId == null) {
                     if (other$playerUniqueId == null) {
                        break label93;
                     }
                  } else if (this$playerUniqueId.equals(other$playerUniqueId)) {
                     break label93;
                  }

                  return false;
               }

               if (Double.compare(this.getX(), other.getX()) != 0) {
                  return false;
               } else if (Double.compare(this.getY(), other.getY()) != 0) {
                  return false;
               } else if (Double.compare(this.getZ(), other.getZ()) != 0) {
                  return false;
               } else if (Double.compare(this.getMotionX(), other.getMotionX()) != 0) {
                  return false;
               } else if (Double.compare(this.getMotionY(), other.getMotionY()) != 0) {
                  return false;
               } else if (Double.compare(this.getMotionZ(), other.getMotionZ()) != 0) {
                  return false;
               } else if (Float.compare(this.getYaw(), other.getYaw()) != 0) {
                  return false;
               } else if (Float.compare(this.getPitch(), other.getPitch()) != 0) {
                  return false;
               } else if (!Arrays.deepEquals(this.getPcMeta(), other.getPcMeta())) {
                  return false;
               } else if (this.isSpawned() != other.isSpawned()) {
                  return false;
               } else {
                  Object this$effects = this.getEffects();
                  Object other$effects = other.getEffects();
                  if (this$effects == null) {
                     if (other$effects != null) {
                        return false;
                     }
                  } else if (!this$effects.equals(other$effects)) {
                     return false;
                  }

                  return true;
               }
            }
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof CachedEntity;
   }

   public int hashCode() {
      int result = 1;
      result = result * 59 + this.getEid();
      result = result * 59 + this.getPcType();
      Object $peType = this.getPeType();
      result = result * 59 + ($peType == null ? 0 : $peType.hashCode());
      Object $objType = this.getObjType();
      result = result * 59 + ($objType == null ? 0 : $objType.hashCode());
      result = result * 59 + (this.isPlayer() ? 79 : 97);
      Object $playerUniqueId = this.getPlayerUniqueId();
      result = result * 59 + ($playerUniqueId == null ? 0 : $playerUniqueId.hashCode());
      long $x = Double.doubleToLongBits(this.getX());
      result = result * 59 + (int)($x >>> 32 ^ $x);
      long $y = Double.doubleToLongBits(this.getY());
      result = result * 59 + (int)($y >>> 32 ^ $y);
      long $z = Double.doubleToLongBits(this.getZ());
      result = result * 59 + (int)($z >>> 32 ^ $z);
      long $motionX = Double.doubleToLongBits(this.getMotionX());
      result = result * 59 + (int)($motionX >>> 32 ^ $motionX);
      long $motionY = Double.doubleToLongBits(this.getMotionY());
      result = result * 59 + (int)($motionY >>> 32 ^ $motionY);
      long $motionZ = Double.doubleToLongBits(this.getMotionZ());
      result = result * 59 + (int)($motionZ >>> 32 ^ $motionZ);
      result = result * 59 + Float.floatToIntBits(this.getYaw());
      result = result * 59 + Float.floatToIntBits(this.getPitch());
      result = result * 59 + Arrays.deepHashCode(this.getPcMeta());
      result = result * 59 + (this.isSpawned() ? 79 : 97);
      Object $effects = this.getEffects();
      result = result * 59 + ($effects == null ? 0 : $effects.hashCode());
      return result;
   }

   public String toString() {
      return "CachedEntity(eid=" + this.getEid() + ", pcType=" + this.getPcType() + ", peType=" + this.getPeType() + ", objType=" + this.getObjType() + ", player=" + this.isPlayer() + ", playerUniqueId=" + this.getPlayerUniqueId() + ", x=" + this.getX() + ", y=" + this.getY() + ", z=" + this.getZ() + ", motionX=" + this.getMotionX() + ", motionY=" + this.getMotionY() + ", motionZ=" + this.getMotionZ() + ", yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ", pcMeta=" + Arrays.deepToString(this.getPcMeta()) + ", spawned=" + this.isSpawned() + ", effects=" + this.getEffects() + ")";
   }
}
