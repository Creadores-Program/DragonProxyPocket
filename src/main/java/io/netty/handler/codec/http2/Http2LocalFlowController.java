package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface Http2LocalFlowController extends Http2FlowController {
   void receiveFlowControlledFrame(ChannelHandlerContext var1, Http2Stream var2, ByteBuf var3, int var4, boolean var5) throws Http2Exception;

   void consumeBytes(ChannelHandlerContext var1, Http2Stream var2, int var3) throws Http2Exception;

   int unconsumedBytes(Http2Stream var1);
}
