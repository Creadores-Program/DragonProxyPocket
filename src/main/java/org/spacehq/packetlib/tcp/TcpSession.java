package org.spacehq.packetlib.tcp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutException;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.event.session.ConnectedEvent;
import org.spacehq.packetlib.event.session.DisconnectedEvent;
import org.spacehq.packetlib.event.session.DisconnectingEvent;
import org.spacehq.packetlib.event.session.PacketReceivedEvent;
import org.spacehq.packetlib.event.session.PacketSentEvent;
import org.spacehq.packetlib.event.session.SessionEvent;
import org.spacehq.packetlib.event.session.SessionListener;
import org.spacehq.packetlib.packet.Packet;
import org.spacehq.packetlib.packet.PacketProtocol;

public abstract class TcpSession extends SimpleChannelInboundHandler<Packet> implements Session {
   private String host;
   private int port;
   private PacketProtocol protocol;
   private int compressionThreshold = -1;
   private int connectTimeout = 30;
   private int readTimeout = 30;
   private int writeTimeout = 0;
   private Map<String, Object> flags = new HashMap();
   private List<SessionListener> listeners = new CopyOnWriteArrayList();
   private Channel channel;
   protected boolean disconnected = false;
   private BlockingQueue<Packet> packets = new LinkedBlockingQueue();
   private Thread packetHandleThread;

   public TcpSession(String host, int port, PacketProtocol protocol) {
      this.host = host;
      this.port = port;
      this.protocol = protocol;
   }

   public void connect() {
      this.connect(true);
   }

   public void connect(boolean wait) {
   }

   public String getHost() {
      return this.host;
   }

   public int getPort() {
      return this.port;
   }

   public PacketProtocol getPacketProtocol() {
      return this.protocol;
   }

   public Map<String, Object> getFlags() {
      return new HashMap(this.flags);
   }

   public boolean hasFlag(String key) {
      return this.getFlags().containsKey(key);
   }

   public <T> T getFlag(String key) {
      Object value = this.getFlags().get(key);
      if (value == null) {
         return null;
      } else {
         try {
            return value;
         } catch (ClassCastException var4) {
            throw new IllegalStateException("Tried to get flag \"" + key + "\" as the wrong type. Actual type: " + value.getClass().getName());
         }
      }
   }

   public void setFlag(String key, Object value) {
      this.flags.put(key, value);
   }

   public List<SessionListener> getListeners() {
      return new ArrayList(this.listeners);
   }

   public void addListener(SessionListener listener) {
      this.listeners.add(listener);
   }

   public void removeListener(SessionListener listener) {
      this.listeners.remove(listener);
   }

   public void callEvent(SessionEvent event) {
      try {
         Iterator var2 = this.listeners.iterator();

         while(var2.hasNext()) {
            SessionListener listener = (SessionListener)var2.next();
            event.call(listener);
         }
      } catch (Throwable var4) {
         this.exceptionCaught((ChannelHandlerContext)null, var4);
      }

   }

   public int getCompressionThreshold() {
      return this.compressionThreshold;
   }

   public void setCompressionThreshold(int threshold) {
      this.compressionThreshold = threshold;
      if (this.channel != null) {
         if (this.compressionThreshold >= 0) {
            if (this.channel.pipeline().get("compression") == null) {
               this.channel.pipeline().addBefore("codec", "compression", new TcpPacketCompression(this));
            }
         } else if (this.channel.pipeline().get("compression") != null) {
            this.channel.pipeline().remove("compression");
         }
      }

   }

   public int getConnectTimeout() {
      return this.connectTimeout;
   }

   public void setConnectTimeout(int timeout) {
      this.connectTimeout = timeout;
   }

   public int getReadTimeout() {
      return this.readTimeout;
   }

   public void setReadTimeout(int timeout) {
      this.readTimeout = timeout;
      this.refreshReadTimeoutHandler();
   }

   public int getWriteTimeout() {
      return this.writeTimeout;
   }

   public void setWriteTimeout(int timeout) {
      this.writeTimeout = timeout;
      this.refreshWriteTimeoutHandler();
   }

   public boolean isConnected() {
      return this.channel != null && this.channel.isOpen() && !this.disconnected;
   }

   public void send(final Packet packet) {
      if (this.channel != null) {
         ChannelFuture future = this.channel.writeAndFlush(packet).addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
               if (future.isSuccess()) {
                  TcpSession.this.callEvent(new PacketSentEvent(TcpSession.this, packet));
               } else {
                  TcpSession.this.exceptionCaught((ChannelHandlerContext)null, future.cause());
               }

            }
         });
         if (packet.isPriority()) {
            try {
               future.await();
            } catch (InterruptedException var4) {
            }
         }

      }
   }

   public void disconnect(String reason) {
      this.disconnect(reason, false);
   }

   public void disconnect(String reason, boolean wait) {
      this.disconnect(reason, (Throwable)null, wait);
   }

   public void disconnect(String reason, Throwable cause) {
      this.disconnect(reason, cause, false);
   }

   public void disconnect(final String reason, final Throwable cause, boolean wait) {
      if (!this.disconnected) {
         this.disconnected = true;
         if (this.packetHandleThread != null) {
            this.packetHandleThread.interrupt();
            this.packetHandleThread = null;
         }

         if (this.channel != null && this.channel.isOpen()) {
            this.callEvent(new DisconnectingEvent(this, reason, cause));
            ChannelFuture future = this.channel.flush().close().addListener(new ChannelFutureListener() {
               public void operationComplete(ChannelFuture future) throws Exception {
                  TcpSession.this.callEvent(new DisconnectedEvent(TcpSession.this, reason != null ? reason : "Connection closed.", cause));
               }
            });
            if (wait) {
               try {
                  future.await();
               } catch (InterruptedException var6) {
               }
            }
         } else {
            this.callEvent(new DisconnectedEvent(this, reason != null ? reason : "Connection closed.", cause));
         }

         this.channel = null;
      }
   }

   protected void refreshReadTimeoutHandler() {
      this.refreshReadTimeoutHandler(this.channel);
   }

   protected void refreshReadTimeoutHandler(Channel channel) {
      if (channel != null) {
         if (this.readTimeout <= 0) {
            if (channel.pipeline().get("readTimeout") != null) {
               channel.pipeline().remove("readTimeout");
            }
         } else if (channel.pipeline().get("readTimeout") == null) {
            channel.pipeline().addFirst((String)"readTimeout", (ChannelHandler)(new ReadTimeoutHandler(this.readTimeout)));
         } else {
            channel.pipeline().replace((String)"readTimeout", "readTimeout", new ReadTimeoutHandler(this.readTimeout));
         }
      }

   }

   protected void refreshWriteTimeoutHandler() {
      this.refreshWriteTimeoutHandler(this.channel);
   }

   protected void refreshWriteTimeoutHandler(Channel channel) {
      if (channel != null) {
         if (this.writeTimeout <= 0) {
            if (channel.pipeline().get("writeTimeout") != null) {
               channel.pipeline().remove("writeTimeout");
            }
         } else if (channel.pipeline().get("writeTimeout") == null) {
            channel.pipeline().addFirst((String)"writeTimeout", (ChannelHandler)(new WriteTimeoutHandler(this.writeTimeout)));
         } else {
            channel.pipeline().replace((String)"writeTimeout", "writeTimeout", new WriteTimeoutHandler(this.writeTimeout));
         }
      }

   }

   public void channelActive(ChannelHandlerContext ctx) throws Exception {
      if (!this.disconnected && this.channel == null) {
         this.channel = ctx.channel();
         this.packetHandleThread = new Thread(new Runnable() {
            public void run() {
               while(true) {
                  try {
                     Packet packet;
                     if ((packet = (Packet)TcpSession.this.packets.take()) != null) {
                        TcpSession.this.callEvent(new PacketReceivedEvent(TcpSession.this, packet));
                        continue;
                     }
                  } catch (InterruptedException var2) {
                  } catch (Throwable var3) {
                     TcpSession.this.exceptionCaught((ChannelHandlerContext)null, var3);
                  }

                  return;
               }
            }
         });
         this.packetHandleThread.start();
         this.callEvent(new ConnectedEvent(this));
      } else {
         ctx.channel().close();
      }
   }

   public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      if (ctx.channel() == this.channel) {
         this.disconnect("Connection closed.");
      }

   }

   public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      String message = null;
      if (!(cause instanceof ConnectTimeoutException) && (!(cause instanceof ConnectException) || !cause.getMessage().contains("connection timed out"))) {
         if (cause instanceof ReadTimeoutException) {
            message = "Read timed out.";
         } else if (cause instanceof WriteTimeoutException) {
            message = "Write timed out.";
         } else {
            message = "Internal network exception.";
         }
      } else {
         message = "Connection timed out.";
      }

      this.disconnect(message, cause);
   }

   protected void messageReceived(ChannelHandlerContext ctx, Packet packet) throws Exception {
      if (!packet.isPriority()) {
         this.packets.add(packet);
      }

   }
}
