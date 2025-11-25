package io.netty.channel.epoll;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.DomainSocketChannel;
import io.netty.channel.unix.FileDescriptor;
import java.net.SocketAddress;

public final class EpollDomainSocketChannel extends AbstractEpollStreamChannel implements DomainSocketChannel {
   private final EpollDomainSocketChannelConfig config = new EpollDomainSocketChannelConfig(this);
   private volatile DomainSocketAddress local;
   private volatile DomainSocketAddress remote;

   public EpollDomainSocketChannel() {
      super(Native.socketDomainFd());
   }

   public EpollDomainSocketChannel(Channel parent, FileDescriptor fd) {
      super(parent, fd.intValue());
   }

   public EpollDomainSocketChannel(FileDescriptor fd) {
      super(fd);
   }

   EpollDomainSocketChannel(Channel parent, int fd) {
      super(parent, fd);
   }

   protected AbstractEpollChannel.AbstractEpollUnsafe newUnsafe() {
      return new EpollDomainSocketChannel.EpollDomainUnsafe();
   }

   protected DomainSocketAddress localAddress0() {
      return this.local;
   }

   protected DomainSocketAddress remoteAddress0() {
      return this.remote;
   }

   protected void doBind(SocketAddress localAddress) throws Exception {
      Native.bind(this.fd().intValue(), localAddress);
      this.local = (DomainSocketAddress)localAddress;
   }

   public EpollDomainSocketChannelConfig config() {
      return this.config;
   }

   protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
      if (super.doConnect(remoteAddress, localAddress)) {
         this.local = (DomainSocketAddress)localAddress;
         this.remote = (DomainSocketAddress)remoteAddress;
         return true;
      } else {
         return false;
      }
   }

   public DomainSocketAddress remoteAddress() {
      return (DomainSocketAddress)super.remoteAddress();
   }

   public DomainSocketAddress localAddress() {
      return (DomainSocketAddress)super.localAddress();
   }

   protected boolean doWriteSingle(ChannelOutboundBuffer in, int writeSpinCount) throws Exception {
      Object msg = in.current();
      if (msg instanceof FileDescriptor && Native.sendFd(this.fd().intValue(), ((FileDescriptor)msg).intValue()) > 0) {
         in.remove();
         return true;
      } else {
         return super.doWriteSingle(in, writeSpinCount);
      }
   }

   protected Object filterOutboundMessage(Object msg) {
      return msg instanceof FileDescriptor ? msg : super.filterOutboundMessage(msg);
   }

   private final class EpollDomainUnsafe extends AbstractEpollStreamChannel.EpollStreamUnsafe {
      private EpollDomainUnsafe() {
         super();
      }

      void epollInReady() {
         switch(EpollDomainSocketChannel.this.config().getReadMode()) {
         case BYTES:
            super.epollInReady();
            break;
         case FILE_DESCRIPTORS:
            this.epollInReadFd();
            break;
         default:
            throw new Error();
         }

      }

      private void epollInReadFd() {
         boolean edgeTriggered = EpollDomainSocketChannel.this.isFlagSet(Native.EPOLLET);
         ChannelConfig config = EpollDomainSocketChannel.this.config();
         if (!this.readPending && !edgeTriggered && !config.isAutoRead()) {
            this.clearEpollIn0();
         } else {
            ChannelPipeline pipeline = EpollDomainSocketChannel.this.pipeline();

            try {
               try {
                  int maxMessagesPerRead = edgeTriggered ? Integer.MAX_VALUE : config.getMaxMessagesPerRead();
                  int messages = 0;

                  do {
                     int socketFd = Native.recvFd(EpollDomainSocketChannel.this.fd().intValue());
                     if (socketFd == 0) {
                        break;
                     }

                     if (socketFd == -1) {
                        this.close(this.voidPromise());
                        return;
                     }

                     this.readPending = false;

                     try {
                        pipeline.fireChannelRead(new FileDescriptor(socketFd));
                     } catch (Throwable var18) {
                        pipeline.fireChannelReadComplete();
                        pipeline.fireExceptionCaught(var18);
                     } finally {
                        if (!edgeTriggered && !config.isAutoRead()) {
                           break;
                        }

                     }

                     ++messages;
                  } while(messages < maxMessagesPerRead);

                  pipeline.fireChannelReadComplete();
               } catch (Throwable var20) {
                  pipeline.fireChannelReadComplete();
                  pipeline.fireExceptionCaught(var20);
                  EpollDomainSocketChannel.this.eventLoop().execute(new Runnable() {
                     public void run() {
                        EpollDomainUnsafe.this.epollInReady();
                     }
                  });
               }

            } finally {
               if (!this.readPending && !config.isAutoRead()) {
                  this.clearEpollIn0();
               }

            }
         }
      }

      // $FF: synthetic method
      EpollDomainUnsafe(Object x1) {
         this();
      }
   }
}
