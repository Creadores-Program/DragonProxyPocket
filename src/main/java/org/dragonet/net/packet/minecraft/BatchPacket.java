package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.dragonet.net.packet.Protocol;
import org.dragonet.proxy.utilities.io.PEBinaryReader;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class BatchPacket extends PEPacket {
   public ArrayList<PEPacket> packets;

   public BatchPacket() {
      this.packets = new ArrayList();
   }

   public BatchPacket(byte[] data) {
      this.setData(data);
   }

   public int pid() {
      return 6;
   }

   public void encode() {
      try {
         this.setShouldSendImmidate(true);
         ByteArrayOutputStream packetCombinerData = new ByteArrayOutputStream();
         PEBinaryWriter packetCombiner = new PEBinaryWriter(packetCombinerData);
         Iterator var3 = this.packets.iterator();

         while(var3.hasNext()) {
            PEPacket pk = (PEPacket)var3.next();
            pk.encode();
            packetCombiner.writeInt(pk.getData().length);
            packetCombiner.write(pk.getData());
         }

         Deflater def = new Deflater(7);
         def.reset();
         def.setInput(packetCombinerData.toByteArray());
         def.finish();
         byte[] deflateBuffer = new byte['\uffff'];
         int size = def.deflate(deflateBuffer);
         deflateBuffer = Arrays.copyOfRange(deflateBuffer, 0, size);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeInt(deflateBuffer.length);
         writer.write(deflateBuffer);
         this.setData(bos.toByteArray());
      } catch (IOException var8) {
      }

   }

   public void decode() {
      try {
         this.packets = new ArrayList();
         PEBinaryReader reader = new PEBinaryReader(new ByteArrayInputStream(this.getData()));
         reader.readByte();
         int size = reader.readInt();
         byte[] payload = reader.read(size);
         Inflater inf = new Inflater();
         inf.setInput(payload);
         byte[] decompressedPayload = new byte[67108864];
         boolean var6 = false;

         int decompressedSize;
         try {
            decompressedSize = inf.inflate(decompressedPayload);
         } catch (DataFormatException var12) {
            this.setLength(reader.totallyRead());
            return;
         }

         inf.end();
         PEBinaryReader dataReader = new PEBinaryReader(new ByteArrayInputStream(decompressedPayload));
         int offset = 0;

         while(offset < decompressedSize) {
            int pkLen = dataReader.readInt();
            offset += 4;
            byte[] pkData = dataReader.read(pkLen);
            offset += pkLen;
            PEPacket pk = Protocol.decode(pkData);
            if (pk == null) {
               this.packets.clear();
               return;
            }

            this.packets.add(pk);
         }

         this.setLength(reader.totallyRead());
      } catch (IOException var13) {
         var13.printStackTrace();
      }

   }
}
