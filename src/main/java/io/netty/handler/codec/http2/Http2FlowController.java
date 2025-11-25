package io.netty.handler.codec.http2;

import io.netty.channel.ChannelHandlerContext;

public interface Http2FlowController {
   void initialWindowSize(int var1) throws Http2Exception;

   int initialWindowSize();

   int windowSize(Http2Stream var1);

   void incrementWindowSize(ChannelHandlerContext var1, Http2Stream var2, int var3) throws Http2Exception;
}
