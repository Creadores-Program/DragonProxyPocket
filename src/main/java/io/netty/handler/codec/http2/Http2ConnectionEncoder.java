package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public interface Http2ConnectionEncoder extends Http2FrameWriter {
   Http2Connection connection();

   Http2RemoteFlowController flowController();

   Http2FrameWriter frameWriter();

   Http2Settings pollSentSettings();

   void remoteSettings(Http2Settings var1) throws Http2Exception;

   ChannelFuture writeFrame(ChannelHandlerContext var1, byte var2, int var3, Http2Flags var4, ByteBuf var5, ChannelPromise var6);

   public interface Builder {
      Http2ConnectionEncoder.Builder connection(Http2Connection var1);

      Http2ConnectionEncoder.Builder lifecycleManager(Http2LifecycleManager var1);

      Http2LifecycleManager lifecycleManager();

      Http2ConnectionEncoder.Builder frameWriter(Http2FrameWriter var1);

      Http2ConnectionEncoder build();
   }
}
