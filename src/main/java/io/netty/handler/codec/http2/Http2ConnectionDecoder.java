package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.io.Closeable;
import java.util.List;

public interface Http2ConnectionDecoder extends Closeable {
   Http2Connection connection();

   Http2LocalFlowController flowController();

   Http2FrameListener listener();

   void decodeFrame(ChannelHandlerContext var1, ByteBuf var2, List<Object> var3) throws Http2Exception;

   Http2Settings localSettings();

   void localSettings(Http2Settings var1) throws Http2Exception;

   boolean prefaceReceived();

   void close();

   public interface Builder {
      Http2ConnectionDecoder.Builder connection(Http2Connection var1);

      Http2ConnectionDecoder.Builder lifecycleManager(Http2LifecycleManager var1);

      Http2LifecycleManager lifecycleManager();

      Http2ConnectionDecoder.Builder frameReader(Http2FrameReader var1);

      Http2ConnectionDecoder.Builder listener(Http2FrameListener var1);

      Http2ConnectionDecoder.Builder encoder(Http2ConnectionEncoder var1);

      Http2ConnectionDecoder build();
   }
}
