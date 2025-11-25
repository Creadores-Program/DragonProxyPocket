package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.dragonet.proxy.utilities.io.PEBinaryReader;

public class ClientHandshakePacket extends PEPacket {
   public PEBinaryReader.BinaryAddress address;
   public long session;
   public long session2;

   public ClientHandshakePacket(byte[] data) {
      this.setData(data);
   }

   public int pid() {
      return 19;
   }

   public void encode() {
   }

   public void decode() {
      try {
         PEBinaryReader reader = new PEBinaryReader(new ByteArrayInputStream(this.getData()));
         reader.readByte();
         this.address = reader.readAddress();
         reader.readAddress();
         reader.readAddress();
         reader.readAddress();
         reader.readAddress();
         reader.readAddress();
         reader.readAddress();
         reader.readAddress();
         reader.readAddress();
         reader.readAddress();
         reader.readAddress();
         this.session2 = reader.readLong();
         this.session = reader.readLong();
         this.setLength(reader.totallyRead());
      } catch (IOException var2) {
      }

   }

   public static byte[][] getDataArray(PEBinaryReader reader, int len) throws IOException {
      byte[][] dataArray = new byte[len][];

      for(int i = 0; i < len; ++i) {
         reader.switchEndianness();
         int arrayLen = reader.readTriad();
         reader.switchEndianness();
         dataArray[i] = reader.read(arrayLen);
      }

      return dataArray;
   }
}
