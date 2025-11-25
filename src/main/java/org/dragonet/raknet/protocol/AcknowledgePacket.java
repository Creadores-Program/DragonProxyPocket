package org.dragonet.raknet.protocol;

import java.util.Iterator;
import java.util.TreeMap;
import org.dragonet.proxy.utilities.Binary;
import org.dragonet.proxy.utilities.BinaryStream;

public abstract class AcknowledgePacket extends Packet {
   public TreeMap<Integer, Integer> packets;

   public void encode() {
      super.encode();
      int count = this.packets.size();
      int[] packets = new int[count];
      int index = 0;

      int i;
      for(Iterator var4 = this.packets.values().iterator(); var4.hasNext(); packets[index++] = i) {
         i = (Integer)var4.next();
      }

      short records = 0;
      BinaryStream payload = new BinaryStream();
      if (count > 0) {
         int pointer = 1;
         int start = packets[0];
         int last = packets[0];

         while(pointer < count) {
            int current = packets[pointer++];
            int diff = current - last;
            if (diff == 1) {
               last = current;
            } else if (diff > 1) {
               if (start == last) {
                  payload.putByte((byte)1);
                  payload.put(Binary.writeLTriad(start));
                  last = current;
                  start = current;
               } else {
                  payload.putByte((byte)0);
                  payload.put(Binary.writeLTriad(start));
                  payload.put(Binary.writeLTriad(last));
                  last = current;
                  start = current;
               }

               ++records;
            }
         }

         if (start == last) {
            payload.putByte((byte)1);
            payload.put(Binary.writeLTriad(start));
         } else {
            payload.putByte((byte)0);
            payload.put(Binary.writeLTriad(start));
            payload.put(Binary.writeLTriad(last));
         }

         ++records;
      }

      this.putShort(records);
      this.buffer = Binary.appendBytes(this.buffer, payload.getBuffer());
   }

   public void decode() {
      super.decode();
      short count = this.getSignedShort();
      this.packets = new TreeMap();
      int cnt = 0;

      for(int i = 0; i < count && !this.feof() && cnt < 4096; ++i) {
         if (this.getByte() == 0) {
            int start = this.getLTriad();
            int end = this.getLTriad();
            if (end - start > 512) {
               end = start + 512;
            }

            for(int c = start; c <= end; ++c) {
               this.packets.put(cnt++, c);
            }
         } else {
            this.packets.put(cnt++, this.getLTriad());
         }
      }

   }

   public Packet clean() {
      this.packets = new TreeMap();
      return super.clean();
   }

   public AcknowledgePacket clone() throws CloneNotSupportedException {
      AcknowledgePacket packet = (AcknowledgePacket)super.clone();
      packet.packets = new TreeMap(this.packets);
      return packet;
   }
}
