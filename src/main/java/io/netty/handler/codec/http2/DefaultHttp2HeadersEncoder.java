package io.netty.handler.codec.http2;

import com.twitter.hpack.Encoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.BinaryHeaders;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

public class DefaultHttp2HeadersEncoder implements Http2HeadersEncoder, Http2HeadersEncoder.Configuration {
   private final Encoder encoder;
   private final ByteArrayOutputStream tableSizeChangeOutput;
   private final Set<String> sensitiveHeaders;
   private final Http2HeaderTable headerTable;

   public DefaultHttp2HeadersEncoder() {
      this(4096, Collections.emptySet());
   }

   public DefaultHttp2HeadersEncoder(int maxHeaderTableSize, Set<String> sensitiveHeaders) {
      this.tableSizeChangeOutput = new ByteArrayOutputStream();
      this.sensitiveHeaders = new TreeSet(String.CASE_INSENSITIVE_ORDER);
      this.encoder = new Encoder(maxHeaderTableSize);
      this.sensitiveHeaders.addAll(sensitiveHeaders);
      this.headerTable = new DefaultHttp2HeadersEncoder.Http2HeaderTableEncoder();
   }

   public void encodeHeaders(Http2Headers headers, ByteBuf buffer) throws Http2Exception {
      final ByteBufOutputStream stream = new ByteBufOutputStream(buffer);

      try {
         if (headers.size() > this.headerTable.maxHeaderListSize()) {
            throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Number of headers (%d) exceeds maxHeaderListSize (%d)", headers.size(), this.headerTable.maxHeaderListSize());
         }

         if (this.tableSizeChangeOutput.size() > 0) {
            buffer.writeBytes(this.tableSizeChangeOutput.toByteArray());
            this.tableSizeChangeOutput.reset();
         }

         Http2Headers.PseudoHeaderName[] arr$ = Http2Headers.PseudoHeaderName.values();
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            Http2Headers.PseudoHeaderName pseudoHeader = arr$[i$];
            AsciiString name = pseudoHeader.value();
            AsciiString value = (AsciiString)headers.get(name);
            if (value != null) {
               this.encodeHeader(name, value, stream);
            }
         }

         headers.forEachEntry(new BinaryHeaders.EntryVisitor() {
            public boolean visit(Entry<AsciiString, AsciiString> entry) throws Exception {
               AsciiString name = (AsciiString)entry.getKey();
               AsciiString value = (AsciiString)entry.getValue();
               if (!Http2Headers.PseudoHeaderName.isPseudoHeader(name)) {
                  DefaultHttp2HeadersEncoder.this.encodeHeader(name, value, stream);
               }

               return true;
            }
         });
      } catch (Http2Exception var18) {
         throw var18;
      } catch (Throwable var19) {
         throw Http2Exception.connectionError(Http2Error.COMPRESSION_ERROR, var19, "Failed encoding headers block: %s", var19.getMessage());
      } finally {
         try {
            stream.close();
         } catch (IOException var17) {
            throw Http2Exception.connectionError(Http2Error.INTERNAL_ERROR, var17, var17.getMessage());
         }
      }

   }

   public Http2HeaderTable headerTable() {
      return this.headerTable;
   }

   public Http2HeadersEncoder.Configuration configuration() {
      return this;
   }

   private void encodeHeader(AsciiString key, AsciiString value, OutputStream stream) throws IOException {
      boolean sensitive = this.sensitiveHeaders.contains(key.toString());
      this.encoder.encodeHeader(stream, key.array(), value.array(), sensitive);
   }

   private final class Http2HeaderTableEncoder extends DefaultHttp2HeaderTableListSize implements Http2HeaderTable {
      private Http2HeaderTableEncoder() {
      }

      public void maxHeaderTableSize(int max) throws Http2Exception {
         if (max < 0) {
            throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Header Table Size must be non-negative but was %d", max);
         } else {
            try {
               DefaultHttp2HeadersEncoder.this.encoder.setMaxHeaderTableSize(DefaultHttp2HeadersEncoder.this.tableSizeChangeOutput, max);
            } catch (IOException var3) {
               throw new Http2Exception(Http2Error.COMPRESSION_ERROR, var3.getMessage(), var3);
            } catch (Throwable var4) {
               throw new Http2Exception(Http2Error.PROTOCOL_ERROR, var4.getMessage(), var4);
            }
         }
      }

      public int maxHeaderTableSize() {
         return DefaultHttp2HeadersEncoder.this.encoder.getMaxHeaderTableSize();
      }

      // $FF: synthetic method
      Http2HeaderTableEncoder(Object x1) {
         this();
      }
   }
}
