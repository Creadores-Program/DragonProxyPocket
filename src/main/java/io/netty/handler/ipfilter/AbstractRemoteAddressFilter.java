package io.netty.handler.ipfilter;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import java.net.SocketAddress;

public abstract class AbstractRemoteAddressFilter<T extends SocketAddress> extends ChannelHandlerAdapter {
   public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
      this.handleNewChannel(ctx);
      ctx.fireChannelRegistered();
   }

   public void channelActive(ChannelHandlerContext ctx) throws Exception {
      if (!this.handleNewChannel(ctx)) {
         throw new IllegalStateException("cannot determine to accept or reject a channel: " + ctx.channel());
      } else {
         ctx.fireChannelActive();
      }
   }

   private boolean handleNewChannel(ChannelHandlerContext ctx) throws Exception {
      T remoteAddress = ctx.channel().remoteAddress();
      if (remoteAddress == null) {
         return false;
      } else {
         ctx.pipeline().remove((ChannelHandler)this);
         if (this.accept(ctx, remoteAddress)) {
            this.channelAccepted(ctx, remoteAddress);
         } else {
            ChannelFuture rejectedFuture = this.channelRejected(ctx, remoteAddress);
            if (rejectedFuture != null) {
               rejectedFuture.addListener(ChannelFutureListener.CLOSE);
            } else {
               ctx.close();
            }
         }

         return true;
      }
   }

   protected abstract boolean accept(ChannelHandlerContext var1, T var2) throws Exception;

   protected void channelAccepted(ChannelHandlerContext ctx, T remoteAddress) {
   }

   protected ChannelFuture channelRejected(ChannelHandlerContext ctx, T remoteAddress) {
      return null;
   }
}
