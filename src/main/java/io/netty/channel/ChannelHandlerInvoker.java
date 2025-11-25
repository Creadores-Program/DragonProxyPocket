package io.netty.channel;

import io.netty.util.concurrent.EventExecutor;
import java.net.SocketAddress;

public interface ChannelHandlerInvoker {
   EventExecutor executor();

   void invokeChannelRegistered(ChannelHandlerContext var1);

   void invokeChannelUnregistered(ChannelHandlerContext var1);

   void invokeChannelActive(ChannelHandlerContext var1);

   void invokeChannelInactive(ChannelHandlerContext var1);

   void invokeExceptionCaught(ChannelHandlerContext var1, Throwable var2);

   void invokeUserEventTriggered(ChannelHandlerContext var1, Object var2);

   void invokeChannelRead(ChannelHandlerContext var1, Object var2);

   void invokeChannelReadComplete(ChannelHandlerContext var1);

   void invokeChannelWritabilityChanged(ChannelHandlerContext var1);

   void invokeBind(ChannelHandlerContext var1, SocketAddress var2, ChannelPromise var3);

   void invokeConnect(ChannelHandlerContext var1, SocketAddress var2, SocketAddress var3, ChannelPromise var4);

   void invokeDisconnect(ChannelHandlerContext var1, ChannelPromise var2);

   void invokeClose(ChannelHandlerContext var1, ChannelPromise var2);

   void invokeDeregister(ChannelHandlerContext var1, ChannelPromise var2);

   void invokeRead(ChannelHandlerContext var1);

   void invokeWrite(ChannelHandlerContext var1, Object var2, ChannelPromise var3);

   void invokeFlush(ChannelHandlerContext var1);
}
