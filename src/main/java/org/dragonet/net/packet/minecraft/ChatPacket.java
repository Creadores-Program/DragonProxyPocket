package org.dragonet.net.packet.minecraft;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.dragonet.net.inf.mcpe.NetworkChannel;
import org.dragonet.proxy.utilities.io.PEBinaryReader;
import org.dragonet.proxy.utilities.io.PEBinaryWriter;

public class ChatPacket extends PEPacket {
   public ChatPacket.TextType type;
   public String source;
   public String message;
   public String[] params;

   public ChatPacket() {
   }

   public ChatPacket(byte[] data) {
      this.setData(data);
   }

   public int pid() {
      return 7;
   }

   public void encode() {
      try {
         this.setChannel(NetworkChannel.CHANNEL_TEXT);
         ByteArrayOutputStream bos = new ByteArrayOutputStream();
         PEBinaryWriter writer = new PEBinaryWriter(bos);
         writer.writeByte((byte)(this.pid() & 255));
         writer.writeByte((byte)(this.type.getType() & 255));
         switch(this.type) {
         case POPUP:
         case CHAT:
            writer.writeString(this.source);
         case RAW:
         case TIP:
         case SYSTEM:
            writer.writeString(this.message);
            break;
         case TRANSLATION:
            writer.writeString(this.message);
            if (this.params == null) {
               writer.writeByte((byte)0);
            } else {
               writer.writeByte((byte)(this.params.length & 255));

               for(int i = 0; i < this.params.length; ++i) {
                  writer.writeString(this.params[i]);
               }
            }
         }

         this.setData(bos.toByteArray());
      } catch (IOException var4) {
      }

   }

   public void decode() {
      try {
         PEBinaryReader reader = new PEBinaryReader(new ByteArrayInputStream(this.getData()));
         reader.readByte();
         this.type = ChatPacket.TextType.fromNum(reader.readByte());
         switch(this.type) {
         case POPUP:
         case CHAT:
            this.source = reader.readString();
         case RAW:
         case TIP:
         case SYSTEM:
            this.message = reader.readString();
            break;
         case TRANSLATION:
            this.message = reader.readString();
            int cnt = reader.readByte();
            this.params = new String[cnt];

            for(int i = 0; i < cnt; ++i) {
               this.params[i] = reader.readString();
            }
         }

         this.setLength(reader.totallyRead());
      } catch (IOException var4) {
      }

   }

   public static enum TextType {
      RAW(0),
      CHAT(1),
      TRANSLATION(2),
      POPUP(3),
      TIP(4),
      SYSTEM(5);

      private int type;

      private TextType(int type) {
         this.type = type;
      }

      public int getType() {
         return this.type;
      }

      public static ChatPacket.TextType fromNum(int num) {
         switch(num) {
         case 0:
            return RAW;
         case 1:
            return CHAT;
         case 2:
            return TRANSLATION;
         case 3:
            return POPUP;
         case 4:
            return TIP;
         case 5:
            return SYSTEM;
         default:
            return RAW;
         }
      }
   }
}
