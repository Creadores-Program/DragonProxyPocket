package io.netty.channel.epoll;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.unix.FileDescriptor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.OneTimeTask;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;

public final class EpollSocketChannel extends AbstractEpollStreamChannel implements SocketChannel {
   private final EpollSocketChannelConfig config = new EpollSocketChannelConfig(this);
   private volatile InetSocketAddress local;
   private volatile InetSocketAddress remote;

   EpollSocketChannel(Channel parent, int fd, InetSocketAddress remote) {
      super(parent, fd);
      this.remote = remote;
      this.local = Native.localAddress(fd);
   }

   public EpollSocketChannel() {
      super(Native.socketStreamFd());
   }

   public EpollSocketChannel(FileDescriptor fd) {
      super(fd);
      this.remote = Native.remoteAddress(fd.intValue());
      this.local = Native.localAddress(fd.intValue());
   }

   public EpollTcpInfo tcpInfo() {
      return this.tcpInfo(new EpollTcpInfo());
   }

   public EpollTcpInfo tcpInfo(EpollTcpInfo info) {
      Native.tcpInfo(this.fd().intValue(), info);
      return info;
   }

   public InetSocketAddress remoteAddress() {
      return (InetSocketAddress)super.remoteAddress();
   }

   public InetSocketAddress localAddress() {
      return (InetSocketAddress)super.localAddress();
   }

   protected SocketAddress localAddress0() {
      return this.local;
   }

   protected SocketAddress remoteAddress0() {
      if (this.remote == null) {
         InetSocketAddress address = Native.remoteAddress(this.fd().intValue());
         if (address != null) {
            this.remote = address;
         }

         return address;
      } else {
         return this.remote;
      }
   }

   protected void doBind(SocketAddress local) throws Exception {
      InetSocketAddress localAddress = (InetSocketAddress)local;
      int fd = this.fd().intValue();
      Native.bind(fd, localAddress);
      this.local = Native.localAddress(fd);
   }

   public EpollSocketChannelConfig config() {
      return this.config;
   }

   public boolean isInputShutdown() {
      return this.isInputShutdown0();
   }

   public boolean isOutputShutdown() {
      return this.isOutputShutdown0();
   }

   public ChannelFuture shutdownOutput() {
      return this.shutdownOutput(this.newPromise());
   }

   public ChannelFuture shutdownOutput(final ChannelPromise promise) {
      Executor closeExecutor = ((EpollSocketChannel.EpollSocketChannelUnsafe)this.unsafe()).closeExecutor();
      if (closeExecutor != null) {
         closeExecutor.execute(new OneTimeTask() {
            public void run() {
               EpollSocketChannel.this.shutdownOutput0(promise);
            }
         });
      } else {
         EventLoop loop = this.eventLoop();
         if (loop.inEventLoop()) {
            this.shutdownOutput0(promise);
         } else {
            loop.execute(new OneTimeTask() {
               public void run() {
                  EpollSocketChannel.this.shutdownOutput0(promise);
               }
            });
         }
      }

      return promise;
   }

   public ServerSocketChannel parent() {
      return (ServerSocketChannel)super.parent();
   }

   protected AbstractEpollChannel.AbstractEpollUnsafe newUnsafe() {
      return new EpollSocketChannel.EpollSocketChannelUnsafe();
   }

   protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
      if (localAddress != null) {
         checkResolvable((InetSocketAddress)localAddress);
      }

      checkResolvable((InetSocketAddress)remoteAddress);
      if (super.doConnect(remoteAddress, localAddress)) {
         int fd = this.fd().intValue();
         this.local = Native.localAddress(fd);
         this.remote = (InetSocketAddress)remoteAddress;
         return true;
      } else {
         return false;
      }
   }

   private final class EpollSocketChannelUnsafe extends AbstractEpollStreamChannel.EpollStreamUnsafe {
      private EpollSocketChannelUnsafe() {
         super();
      }

      protected Executor closeExecutor() {
         return EpollSocketChannel.this.config().getSoLinger() > 0 ? GlobalEventExecutor.INSTANCE : null;
      }

      // $FF: synthetic method
      EpollSocketChannelUnsafe(Object x1) {
         this();
      }
   }
}
