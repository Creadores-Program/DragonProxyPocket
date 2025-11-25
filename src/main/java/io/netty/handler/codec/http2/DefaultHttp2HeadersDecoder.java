package io.netty.handler.codec.http2;

import com.twitter.hpack.Decoder;
import com.twitter.hpack.HeaderListener;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.AsciiString;
import java.io.IOException;

public class DefaultHttp2HeadersDecoder implements Http2HeadersDecoder, Http2HeadersDecoder.Configuration {
   private final Decoder decoder;
   private final Http2HeaderTable headerTable;

   public DefaultHttp2HeadersDecoder() {
      this(8192, 4096);
   }

   public DefaultHttp2HeadersDecoder(int maxHeaderSize, int maxHeaderTableSize) {
      this.decoder = new Decoder(maxHeaderSize, maxHeaderTableSize);
      this.headerTable = new DefaultHttp2HeadersDecoder.Http2HeaderTableDecoder();
   }

   public Http2HeaderTable headerTable() {
      return this.headerTable;
   }

   public Http2HeadersDecoder.Configuration configuration() {
      return this;
   }

   public Http2Headers decodeHeaders(ByteBuf headerBlock) throws Http2Exception {
      ByteBufInputStream in = new ByteBufInputStream(headerBlock);

      DefaultHttp2Headers var6;
      try {
         final Http2Headers headers = new DefaultHttp2Headers();
         HeaderListener listener = new HeaderListener() {
            public void addHeader(byte[] key, byte[] value, boolean sensitive) {
               headers.add(new AsciiString(key, false), new AsciiString(value, false));
            }
         };
         this.decoder.decode(in, listener);
         boolean truncated = this.decoder.endHeaderBlock();
         if (truncated) {
         }

         if (headers.size() > this.headerTable.maxHeaderListSize()) {
            throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Number of headers (%d) exceeds maxHeaderListSize (%d)", headers.size(), this.headerTable.maxHeaderListSize());
         }

         var6 = headers;
      } catch (IOException var17) {
         throw Http2Exception.connectionError(Http2Error.COMPRESSION_ERROR, var17, var17.getMessage());
      } catch (Http2Exception var18) {
         throw var18;
      } catch (Throwable var19) {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, var19, var19.getMessage());
      } finally {
         try {
            in.close();
         } catch (IOException var16) {
            throw Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, var16, var16.getMessage());
         }
      }

      return var6;
   }

   private final class Http2HeaderTableDecoder extends DefaultHttp2HeaderTableListSize implements Http2HeaderTable {
      private Http2HeaderTableDecoder() {
      }

      public void maxHeaderTableSize(int max) throws Http2Exception {
         if (max < 0) {
            throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Header Table Size must be non-negative but was %d", max);
         } else {
            try {
               DefaultHttp2HeadersDecoder.this.decoder.setMaxHeaderTableSize(max);
            } catch (Throwable var3) {
               throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, var3.getMessage(), var3);
            }
         }
      }

      public int maxHeaderTableSize() {
         return DefaultHttp2HeadersDecoder.this.decoder.getMaxHeaderTableSize();
      }

      // $FF: synthetic method
      Http2HeaderTableDecoder(Object x1) {
         this();
      }
   }
}
