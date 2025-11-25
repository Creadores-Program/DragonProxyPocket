package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;

public interface Http2HeadersDecoder {
   Http2Headers decodeHeaders(ByteBuf var1) throws Http2Exception;

   Http2HeadersDecoder.Configuration configuration();

   public interface Configuration {
      Http2HeaderTable headerTable();
   }
}
