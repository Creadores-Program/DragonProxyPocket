package io.netty.channel.epoll;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.ServerChannel;
import io.netty.channel.unix.FileDescriptor;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public abstract class AbstractEpollServerChannel extends AbstractEpollChannel implements ServerChannel {
   protected AbstractEpollServerChannel(int fd) {
      super(fd, Native.EPOLLIN);
   }

   protected AbstractEpollServerChannel(FileDescriptor fd) {
      super((Channel)null, fd, Native.EPOLLIN, Native.getSoError(fd.intValue()) == 0);
   }

   protected boolean isCompatible(EventLoop loop) {
      return loop instanceof EpollEventLoop;
   }

   protected InetSocketAddress remoteAddress0() {
      return null;
   }

   protected AbstractEpollChannel.AbstractEpollUnsafe newUnsafe() {
      return new AbstractEpollServerChannel.EpollServerSocketUnsafe();
   }

   protected void doWrite(ChannelOutboundBuffer in) throws Exception {
      throw new UnsupportedOperationException();
   }

   protected Object filterOutboundMessage(Object msg) throws Exception {
      throw new UnsupportedOperationException();
   }

   abstract Channel newChildChannel(int var1, byte[] var2, int var3, int var4) throws Exception;

   final class EpollServerSocketUnsafe extends AbstractEpollChannel.AbstractEpollUnsafe {
      private final byte[] acceptedAddress = new byte[26];

      EpollServerSocketUnsafe() {
         super();
      }

      public void connect(SocketAddress socketAddress, SocketAddress socketAddress2, ChannelPromise channelPromise) {
         channelPromise.setFailure(new UnsupportedOperationException());
      }

      void epollInReady() {
         assert AbstractEpollServerChannel.this.eventLoop().inEventLoop();

         boolean edgeTriggered = AbstractEpollServerChannel.this.isFlagSet(Native.EPOLLET);
         ChannelConfig config = AbstractEpollServerChannel.this.config();
         if (!this.readPending && !edgeTriggered && !config.isAutoRead()) {
            this.clearEpollIn0();
         } else {
            ChannelPipeline pipeline = AbstractEpollServerChannel.this.pipeline();
            Throwable exception = null;

            try {
               try {
                  int maxMessagesPerRead = edgeTriggered ? Integer.MAX_VALUE : config.getMaxMessagesPerRead();
                  int messages = 0;

                  do {
                     int socketFd = Native.accept(AbstractEpollServerChannel.this.fd().intValue(), this.acceptedAddress);
                     if (socketFd == -1) {
                        break;
                     }

                     this.readPending = false;

                     try {
                        int len = this.acceptedAddress[0];
                        pipeline.fireChannelRead(AbstractEpollServerChannel.this.newChildChannel(socketFd, this.acceptedAddress, 1, len));
                     } catch (Throwable var19) {
                        pipeline.fireChannelReadComplete();
                        pipeline.fireExceptionCaught(var19);
                     } finally {
                        if (!edgeTriggered && !config.isAutoRead()) {
                           break;
                        }

                     }

                     ++messages;
                  } while(messages < maxMessagesPerRead);
               } catch (Throwable var21) {
                  exception = var21;
               }

               pipeline.fireChannelReadComplete();
               if (exception != null) {
                  pipeline.fireExceptionCaught(exception);
               }
            } finally {
               if (!this.readPending && !config.isAutoRead()) {
                  this.clearEpollIn0();
               }

            }

         }
      }
   }
}
