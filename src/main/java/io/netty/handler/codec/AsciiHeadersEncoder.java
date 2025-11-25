package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import java.util.Map.Entry;

public final class AsciiHeadersEncoder implements TextHeaders.EntryVisitor {
   private final ByteBuf buf;
   private final AsciiHeadersEncoder.SeparatorType separatorType;
   private final AsciiHeadersEncoder.NewlineType newlineType;

   public AsciiHeadersEncoder(ByteBuf buf) {
      this(buf, AsciiHeadersEncoder.SeparatorType.COLON_SPACE, AsciiHeadersEncoder.NewlineType.CRLF);
   }

   public AsciiHeadersEncoder(ByteBuf buf, AsciiHeadersEncoder.SeparatorType separatorType, AsciiHeadersEncoder.NewlineType newlineType) {
      if (buf == null) {
         throw new NullPointerException("buf");
      } else if (separatorType == null) {
         throw new NullPointerException("separatorType");
      } else if (newlineType == null) {
         throw new NullPointerException("newlineType");
      } else {
         this.buf = buf;
         this.separatorType = separatorType;
         this.newlineType = newlineType;
      }
   }

   public boolean visit(Entry<CharSequence, CharSequence> entry) throws Exception {
      CharSequence name = (CharSequence)entry.getKey();
      CharSequence value = (CharSequence)entry.getValue();
      ByteBuf buf = this.buf;
      int nameLen = name.length();
      int valueLen = value.length();
      int entryLen = nameLen + valueLen + 4;
      int offset = buf.writerIndex();
      buf.ensureWritable(entryLen);
      writeAscii(buf, offset, name, nameLen);
      offset += nameLen;
      switch(this.separatorType) {
      case COLON:
         buf.setByte(offset++, 58);
         break;
      case COLON_SPACE:
         buf.setByte(offset++, 58);
         buf.setByte(offset++, 32);
         break;
      default:
         throw new Error();
      }

      writeAscii(buf, offset, value, valueLen);
      offset += valueLen;
      switch(this.newlineType) {
      case LF:
         buf.setByte(offset++, 10);
         break;
      case CRLF:
         buf.setByte(offset++, 13);
         buf.setByte(offset++, 10);
         break;
      default:
         throw new Error();
      }

      buf.writerIndex(offset);
      return true;
   }

   private static void writeAscii(ByteBuf buf, int offset, CharSequence value, int valueLen) {
      if (value instanceof AsciiString) {
         writeAsciiString(buf, offset, (AsciiString)value, valueLen);
      } else {
         writeCharSequence(buf, offset, value, valueLen);
      }

   }

   private static void writeAsciiString(ByteBuf buf, int offset, AsciiString value, int valueLen) {
      value.copy(0, (ByteBuf)buf, offset, valueLen);
   }

   private static void writeCharSequence(ByteBuf buf, int offset, CharSequence value, int valueLen) {
      for(int i = 0; i < valueLen; ++i) {
         buf.setByte(offset++, c2b(value.charAt(i)));
      }

   }

   private static int c2b(char ch) {
      return ch < 256 ? (byte)ch : 63;
   }

   public static enum NewlineType {
      LF,
      CRLF;
   }

   public static enum SeparatorType {
      COLON,
      COLON_SPACE;
   }
}
