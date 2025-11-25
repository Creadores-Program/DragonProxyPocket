package io.netty.channel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.SocketAddress;

public interface ChannelHandler {
   void handlerAdded(ChannelHandlerContext var1) throws Exception;

   void handlerRemoved(ChannelHandlerContext var1) throws Exception;

   void exceptionCaught(ChannelHandlerContext var1, Throwable var2) throws Exception;

   void channelRegistered(ChannelHandlerContext var1) throws Exception;

   void channelUnregistered(ChannelHandlerContext var1) throws Exception;

   void channelActive(ChannelHandlerContext var1) throws Exception;

   void channelInactive(ChannelHandlerContext var1) throws Exception;

   void channelRead(ChannelHandlerContext var1, Object var2) throws Exception;

   void channelReadComplete(ChannelHandlerContext var1) throws Exception;

   void userEventTriggered(ChannelHandlerContext var1, Object var2) throws Exception;

   void channelWritabilityChanged(ChannelHandlerContext var1) throws Exception;

   void bind(ChannelHandlerContext var1, SocketAddress var2, ChannelPromise var3) throws Exception;

   void connect(ChannelHandlerContext var1, SocketAddress var2, SocketAddress var3, ChannelPromise var4) throws Exception;

   void disconnect(ChannelHandlerContext var1, ChannelPromise var2) throws Exception;

   void close(ChannelHandlerContext var1, ChannelPromise var2) throws Exception;

   void deregister(ChannelHandlerContext var1, ChannelPromise var2) throws Exception;

   void read(ChannelHandlerContext var1) throws Exception;

   void write(ChannelHandlerContext var1, Object var2, ChannelPromise var3) throws Exception;

   void flush(ChannelHandlerContext var1) throws Exception;

   @Target({ElementType.METHOD})
   @Retention(RetentionPolicy.RUNTIME)
   public @interface Skip {
   }

   @Inherited
   @Documented
   @Target({ElementType.TYPE})
   @Retention(RetentionPolicy.RUNTIME)
   public @interface Sharable {
   }
}
