package org.spacehq.packetlib.tcp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.InetSocketAddress;
import org.spacehq.packetlib.ConnectionListener;
import org.spacehq.packetlib.Server;
import org.spacehq.packetlib.packet.PacketProtocol;

public class TcpConnectionListener implements ConnectionListener {
   private String host;
   private int port;
   private Server server;
   private EventLoopGroup group;
   private Channel channel;

   public TcpConnectionListener(String host, int port, Server server) {
      this.host = host;
      this.port = port;
      this.server = server;
   }

   public String getHost() {
      return this.host;
   }

   public int getPort() {
      return this.port;
   }

   public boolean isListening() {
      return this.channel != null && this.channel.isOpen();
   }

   public void bind() {
      this.bind(true);
   }

   public void bind(boolean wait) {
      this.bind(wait, (Runnable)null);
   }

   public void bind(boolean wait, final Runnable callback) {
      if (this.group == null && this.channel == null) {
         this.group = new NioEventLoopGroup();
         ChannelFuture future = ((ServerBootstrap)((ServerBootstrap)(new ServerBootstrap()).channel(NioServerSocketChannel.class)).childHandler(new ChannelInitializer<Channel>() {
            public void initChannel(Channel channel) throws Exception {
               InetSocketAddress address = (InetSocketAddress)channel.remoteAddress();
               PacketProtocol protocol = TcpConnectionListener.this.server.createPacketProtocol();
               TcpSession session = new TcpServerSession(address.getHostName(), address.getPort(), protocol, TcpConnectionListener.this.server);
               session.getPacketProtocol().newServerSession(TcpConnectionListener.this.server, session);
               channel.config().setOption(ChannelOption.IP_TOS, 24);
               channel.config().setOption(ChannelOption.TCP_NODELAY, false);
               ChannelPipeline pipeline = channel.pipeline();
               session.refreshReadTimeoutHandler(channel);
               session.refreshWriteTimeoutHandler(channel);
               pipeline.addLast((String)"encryption", (ChannelHandler)(new TcpPacketEncryptor(session)));
               pipeline.addLast((String)"sizer", (ChannelHandler)(new TcpPacketSizer(session)));
               pipeline.addLast((String)"codec", (ChannelHandler)(new TcpPacketCodec(session)));
               pipeline.addLast((String)"manager", (ChannelHandler)session);
            }
         }).group(this.group).localAddress(this.host, this.port)).bind();
         if (wait) {
            try {
               future.sync();
            } catch (InterruptedException var5) {
            }

            this.channel = future.channel();
            if (callback != null) {
               callback.run();
            }
         } else {
            future.addListener(new ChannelFutureListener() {
               public void operationComplete(ChannelFuture future) throws Exception {
                  if (future.isSuccess()) {
                     TcpConnectionListener.this.channel = future.channel();
                     if (callback != null) {
                        callback.run();
                     }
                  } else {
                     System.err.println("[ERROR] Failed to asynchronously bind connection listener.");
                     if (future.cause() != null) {
                        future.cause().printStackTrace();
                     }
                  }

               }
            });
         }

      }
   }

   public void close() {
      this.close(false);
   }

   public void close(boolean wait) {
      this.close(wait, (Runnable)null);
   }

   public void close(boolean wait, final Runnable callback) {
      if (this.channel != null) {
         if (this.channel.isOpen()) {
            ChannelFuture future = this.channel.close();
            if (wait) {
               try {
                  future.sync();
               } catch (InterruptedException var6) {
               }

               if (callback != null) {
                  callback.run();
               }
            } else {
               future.addListener(new ChannelFutureListener() {
                  public void operationComplete(ChannelFuture future) throws Exception {
                     if (future.isSuccess()) {
                        if (callback != null) {
                           callback.run();
                        }
                     } else {
                        System.err.println("[ERROR] Failed to asynchronously close connection listener.");
                        if (future.cause() != null) {
                           future.cause().printStackTrace();
                        }
                     }

                  }
               });
            }
         }

         this.channel = null;
      }

      if (this.group != null) {
         Future<?> future = this.group.shutdownGracefully();
         if (wait) {
            try {
               future.sync();
            } catch (InterruptedException var5) {
            }
         } else {
            future.addListener(new GenericFutureListener() {
               public void operationComplete(Future future) throws Exception {
                  if (!future.isSuccess()) {
                     System.err.println("[ERROR] Failed to asynchronously close connection listener.");
                     if (future.cause() != null) {
                        future.cause().printStackTrace();
                     }
                  }

               }
            });
         }

         this.group = null;
      }

   }
}
