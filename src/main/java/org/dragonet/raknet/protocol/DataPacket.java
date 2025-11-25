package org.dragonet.raknet.protocol;

import java.util.ArrayList;
import java.util.Iterator;
import org.dragonet.proxy.utilities.Binary;

public abstract class DataPacket extends Packet {
   public ArrayList<Object> packets = new ArrayList();
   public Integer seqNumber;

   public void encode() {
      super.encode();
      this.putLTriad(this.seqNumber);
      Iterator var1 = this.packets.iterator();

      while(var1.hasNext()) {
         Object packet = var1.next();
         this.put(packet instanceof EncapsulatedPacket ? ((EncapsulatedPacket)packet).toBinary() : (byte[])((byte[])packet));
      }

   }

   public int length() {
      int length = 4;

      Object packet;
      for(Iterator var2 = this.packets.iterator(); var2.hasNext(); length += packet instanceof EncapsulatedPacket ? ((EncapsulatedPacket)packet).getTotalLength() : ((byte[])((byte[])packet)).length) {
         packet = var2.next();
      }

      return length;
   }

   public void decode() {
      super.decode();
      this.seqNumber = this.getLTriad();

      while(!this.feof()) {
         byte[] data = Binary.subBytes(this.buffer, this.offset);
         EncapsulatedPacket packet = EncapsulatedPacket.fromBinary(data, false);
         this.offset += packet.getOffset();
         if (packet.buffer.length == 0) {
            break;
         }

         this.packets.add(packet);
      }

   }

   public Packet clean() {
      this.packets.clear();
      this.seqNumber = null;
      return super.clean();
   }

   public DataPacket clone() throws CloneNotSupportedException {
      DataPacket packet = (DataPacket)super.clone();
      packet.packets = new ArrayList(this.packets);
      return packet;
   }
}
