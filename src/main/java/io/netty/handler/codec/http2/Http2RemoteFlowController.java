package io.netty.handler.codec.http2;

import io.netty.channel.ChannelHandlerContext;

public interface Http2RemoteFlowController extends Http2FlowController {
   void sendFlowControlled(ChannelHandlerContext var1, Http2Stream var2, Http2RemoteFlowController.FlowControlled var3);

   public interface FlowControlled {
      int size();

      void error(Throwable var1);

      boolean write(int var1);
   }
}
