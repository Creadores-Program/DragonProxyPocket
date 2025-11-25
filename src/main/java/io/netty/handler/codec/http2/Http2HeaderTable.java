package io.netty.handler.codec.http2;

public interface Http2HeaderTable {
   void maxHeaderTableSize(int var1) throws Http2Exception;

   int maxHeaderTableSize();

   void maxHeaderListSize(int var1) throws Http2Exception;

   int maxHeaderListSize();
}
