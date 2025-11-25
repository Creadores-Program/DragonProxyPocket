package org.spacehq.packetlib.packet;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.Server;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.crypt.PacketEncryption;

public abstract class PacketProtocol {
   private final Map<Integer, Class<? extends Packet>> incoming = new HashMap();
   private final Map<Class<? extends Packet>, Integer> outgoing = new HashMap();

   public abstract String getSRVRecordPrefix();

   public abstract PacketHeader getPacketHeader();

   public abstract PacketEncryption getEncryption();

   public abstract void newClientSession(Client var1, Session var2);

   public abstract void newServerSession(Server var1, Session var2);

   public final void clearPackets() {
      this.incoming.clear();
      this.outgoing.clear();
   }

   public final void register(int id, Class<? extends Packet> packet) {
      this.registerIncoming(id, packet);
      this.registerOutgoing(id, packet);
   }

   public final void registerIncoming(int id, Class<? extends Packet> packet) {
      this.incoming.put(id, packet);

      try {
         this.createIncomingPacket(id);
      } catch (IllegalStateException var4) {
         this.incoming.remove(id);
         throw new IllegalArgumentException(var4.getMessage(), var4.getCause());
      }
   }

   public final void registerOutgoing(int id, Class<? extends Packet> packet) {
      this.outgoing.put(packet, id);
   }

   public final Packet createIncomingPacket(int id) {
      if (id >= 0 && this.incoming.containsKey(id) && this.incoming.get(id) != null) {
         Class packet = (Class)this.incoming.get(id);

         try {
            Constructor<? extends Packet> constructor = packet.getDeclaredConstructor();
            if (!constructor.isAccessible()) {
               constructor.setAccessible(true);
            }

            return (Packet)constructor.newInstance();
         } catch (NoSuchMethodError var4) {
            throw new IllegalStateException("Packet \"" + id + ", " + packet.getName() + "\" does not have a no-params constructor for instantiation.");
         } catch (Exception var5) {
            throw new IllegalStateException("Failed to instantiate packet \"" + id + ", " + packet.getName() + "\".", var5);
         }
      } else {
         throw new IllegalArgumentException("Invalid packet id: " + id);
      }
   }

   public final int getOutgoingId(Class<? extends Packet> packet) {
      if (this.outgoing.containsKey(packet) && this.outgoing.get(packet) != null) {
         return (Integer)this.outgoing.get(packet);
      } else {
         throw new IllegalArgumentException("Unregistered outgoing packet class: " + packet.getName());
      }
   }
}
