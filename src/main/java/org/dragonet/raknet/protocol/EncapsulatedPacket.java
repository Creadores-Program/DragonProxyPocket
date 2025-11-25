package org.dragonet.raknet.protocol;

import java.nio.ByteBuffer;
import java.util.Arrays;
import org.dragonet.proxy.utilities.Binary;

public class EncapsulatedPacket implements Cloneable {
   public byte reliability;
   public boolean hasSplit = false;
   public int length = 0;
   public Integer messageIndex = null;
   public Integer orderIndex = null;
   public Integer orderChannel = null;
   public Integer splitCount = null;
   public Integer splitID = null;
   public Integer splitIndex = null;
   public byte[] buffer;
   public boolean needACK = false;
   public Integer identifierACK = null;
   private int offset;

   public int getOffset() {
      return this.offset;
   }

   public static EncapsulatedPacket fromBinary(byte[] binary) {
      return fromBinary(binary, false);
   }

   public static EncapsulatedPacket fromBinary(byte[] binary, boolean internal) {
      EncapsulatedPacket packet = new EncapsulatedPacket();
      byte flags = binary[0];
      packet.reliability = (byte)((flags & 224) >> 5);
      packet.hasSplit = (flags & 16) > 0;
      int length;
      int offset;
      if (internal) {
         length = Binary.readInt(Binary.subBytes(binary, 1, 4));
         packet.identifierACK = Binary.readInt(Binary.subBytes(binary, 5, 4));
         offset = 9;
      } else {
         length = (int)Math.ceil((double)Binary.readShort(Binary.subBytes(binary, 1, 2)) / 8.0D);
         offset = 3;
         packet.identifierACK = null;
      }

      if (packet.reliability > 0) {
         if (packet.reliability >= 2 && packet.reliability != 5) {
            packet.messageIndex = Binary.readLTriad(Binary.subBytes(binary, offset, 3));
            offset += 3;
         }

         if (packet.reliability <= 4 && packet.reliability != 2) {
            packet.orderIndex = Binary.readLTriad(Binary.subBytes(binary, offset, 3));
            offset += 3;
            packet.orderChannel = binary[offset++] & 255;
         }
      }

      if (packet.hasSplit) {
         packet.splitCount = Binary.readInt(Binary.subBytes(binary, offset, 4));
         offset += 4;
         packet.splitID = Binary.readShort(Binary.subBytes(binary, offset, 2));
         offset += 2;
         packet.splitIndex = Binary.readInt(Binary.subBytes(binary, offset, 4));
         offset += 4;
      }

      packet.buffer = Binary.subBytes(binary, offset, length);
      offset += length;
      packet.offset = offset;
      return packet;
   }

   public int getTotalLength() {
      return 3 + this.buffer.length + (this.messageIndex != null ? 3 : 0) + (this.orderIndex != null ? 4 : 0) + (this.hasSplit ? 10 : 0);
   }

   public byte[] toBinary() {
      return this.toBinary(false);
   }

   public byte[] toBinary(boolean internal) {
      ByteBuffer bb = ByteBuffer.allocate(23 + this.buffer.length);
      bb.put((byte)((byte)(this.reliability << 5) | (this.hasSplit ? 16 : 0)));
      if (internal) {
         bb.put(Binary.writeInt(this.buffer.length));
         bb.put(Binary.writeInt(this.identifierACK == null ? 0 : this.identifierACK));
      } else {
         bb.put(Binary.writeShort(this.buffer.length << 3));
      }

      if (this.reliability > 0) {
         if (this.reliability >= 2 && this.reliability != 5) {
            bb.put(Binary.writeLTriad(this.messageIndex == null ? 0 : this.messageIndex));
         }

         if (this.reliability <= 4 && this.reliability != 2) {
            bb.put(Binary.writeLTriad(this.orderIndex));
            bb.put(Binary.writeByte((byte)(this.orderChannel & 255)));
         }
      }

      if (this.hasSplit) {
         bb.put(Binary.writeInt(this.splitCount));
         bb.put(Binary.writeShort(this.splitID));
         bb.put(Binary.writeInt(this.splitIndex));
      }

      bb.put(this.buffer);
      return Arrays.copyOf(bb.array(), bb.position());
   }

   public String toString() {
      return Binary.bytesToHexString(this.toBinary());
   }

   public EncapsulatedPacket clone() throws CloneNotSupportedException {
      EncapsulatedPacket packet = (EncapsulatedPacket)super.clone();
      packet.buffer = (byte[])this.buffer.clone();
      return packet;
   }
}
