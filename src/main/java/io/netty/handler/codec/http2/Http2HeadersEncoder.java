package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;

public interface Http2HeadersEncoder {
   void encodeHeaders(Http2Headers var1, ByteBuf var2) throws Http2Exception;

   Http2HeadersEncoder.Configuration configuration();

   public interface Configuration {
      Http2HeaderTable headerTable();
   }
}
