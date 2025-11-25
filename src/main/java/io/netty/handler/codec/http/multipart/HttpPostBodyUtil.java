package io.netty.handler.codec.http.multipart;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderValues;

final class HttpPostBodyUtil {
   public static final int chunkSize = 8096;
   public static final String DEFAULT_BINARY_CONTENT_TYPE;
   public static final String DEFAULT_TEXT_CONTENT_TYPE;

   private HttpPostBodyUtil() {
   }

   static int findNonWhitespace(String sb, int offset) {
      int result;
      for(result = offset; result < sb.length() && Character.isWhitespace(sb.charAt(result)); ++result) {
      }

      return result;
   }

   static int findWhitespace(String sb, int offset) {
      int result;
      for(result = offset; result < sb.length() && !Character.isWhitespace(sb.charAt(result)); ++result) {
      }

      return result;
   }

   static int findEndOfString(String sb) {
      int result;
      for(result = sb.length(); result > 0 && Character.isWhitespace(sb.charAt(result - 1)); --result) {
      }

      return result;
   }

   static {
      DEFAULT_BINARY_CONTENT_TYPE = HttpHeaderValues.APPLICATION_OCTET_STREAM.toString();
      DEFAULT_TEXT_CONTENT_TYPE = HttpHeaderValues.TEXT_PLAIN.toString();
   }

   static class SeekAheadOptimize {
      byte[] bytes;
      int readerIndex;
      int pos;
      int origPos;
      int limit;
      ByteBuf buffer;

      SeekAheadOptimize(ByteBuf buffer) throws HttpPostBodyUtil.SeekAheadNoBackArrayException {
         if (!buffer.hasArray()) {
            throw new HttpPostBodyUtil.SeekAheadNoBackArrayException();
         } else {
            this.buffer = buffer;
            this.bytes = buffer.array();
            this.readerIndex = buffer.readerIndex();
            this.origPos = this.pos = buffer.arrayOffset() + this.readerIndex;
            this.limit = buffer.arrayOffset() + buffer.writerIndex();
         }
      }

      void setReadPosition(int minus) {
         this.pos -= minus;
         this.readerIndex = this.getReadPosition(this.pos);
         this.buffer.readerIndex(this.readerIndex);
      }

      int getReadPosition(int index) {
         return index - this.origPos + this.readerIndex;
      }

      void clear() {
         this.buffer = null;
         this.bytes = null;
         this.limit = 0;
         this.pos = 0;
         this.readerIndex = 0;
      }
   }

   static class SeekAheadNoBackArrayException extends Exception {
      private static final long serialVersionUID = -630418804938699495L;
   }

   public static enum TransferEncodingMechanism {
      BIT7("7bit"),
      BIT8("8bit"),
      BINARY("binary");

      private final String value;

      private TransferEncodingMechanism(String value) {
         this.value = value;
      }

      private TransferEncodingMechanism() {
         this.value = this.name();
      }

      public String value() {
         return this.value;
      }

      public String toString() {
         return this.value;
      }
   }
}
