package org.spacehq.mc.protocol.packet.ingame.server.entity.player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.spacehq.mc.protocol.data.game.values.MagicValues;
import org.spacehq.mc.protocol.data.game.values.entity.player.PositionElement;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;
import org.spacehq.packetlib.packet.Packet;

public class ServerPlayerPositionRotationPacket implements Packet {
   private double x;
   private double y;
   private double z;
   private float yaw;
   private float pitch;
   private List<PositionElement> relative;

   private ServerPlayerPositionRotationPacket() {
   }

   public ServerPlayerPositionRotationPacket(double x, double y, double z, float yaw, float pitch) {
      this(x, y, z, yaw, pitch);
   }

   public ServerPlayerPositionRotationPacket(double x, double y, double z, float yaw, float pitch, PositionElement... relative) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.yaw = yaw;
      this.pitch = pitch;
      this.relative = Arrays.asList(relative);
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

   public float getYaw() {
      return this.yaw;
   }

   public float getPitch() {
      return this.pitch;
   }

   public List<PositionElement> getRelativeElements() {
      return this.relative;
   }

   public void read(NetInput in) throws IOException {
      this.x = in.readDouble();
      this.y = in.readDouble();
      this.z = in.readDouble();
      this.yaw = in.readFloat();
      this.pitch = in.readFloat();
      this.relative = new ArrayList();
      int flags = in.readUnsignedByte();
      PositionElement[] var3 = PositionElement.values();
      int var4 = var3.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         PositionElement element = var3[var5];
         int bit = 1 << (Integer)MagicValues.value(Integer.class, element);
         if ((flags & bit) == bit) {
            this.relative.add(element);
         }
      }

   }

   public void write(NetOutput out) throws IOException {
      out.writeDouble(this.x);
      out.writeDouble(this.y);
      out.writeDouble(this.z);
      out.writeFloat(this.yaw);
      out.writeFloat(this.pitch);
      int flags = 0;

      PositionElement element;
      for(Iterator var3 = this.relative.iterator(); var3.hasNext(); flags |= 1 << (Integer)MagicValues.value(Integer.class, element)) {
         element = (PositionElement)var3.next();
      }

      out.writeByte(flags);
   }

   public boolean isPriority() {
      return false;
   }
}
