package io.netty.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.resolver.DefaultNameResolverGroup;
import io.netty.resolver.NameResolver;
import io.netty.resolver.NameResolverGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class Bootstrap extends AbstractBootstrap<Bootstrap, Channel> {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(Bootstrap.class);
   private static final NameResolverGroup<?> DEFAULT_RESOLVER;
   private volatile NameResolverGroup<SocketAddress> resolver;
   private volatile SocketAddress remoteAddress;

   public Bootstrap() {
      this.resolver = DEFAULT_RESOLVER;
   }

   private Bootstrap(Bootstrap bootstrap) {
      super(bootstrap);
      this.resolver = DEFAULT_RESOLVER;
      this.resolver = bootstrap.resolver;
      this.remoteAddress = bootstrap.remoteAddress;
   }

   public Bootstrap resolver(NameResolverGroup<?> resolver) {
      if (resolver == null) {
         throw new NullPointerException("resolver");
      } else {
         this.resolver = resolver;
         return this;
      }
   }

   public Bootstrap remoteAddress(SocketAddress remoteAddress) {
      this.remoteAddress = remoteAddress;
      return this;
   }

   public Bootstrap remoteAddress(String inetHost, int inetPort) {
      this.remoteAddress = InetSocketAddress.createUnresolved(inetHost, inetPort);
      return this;
   }

   public Bootstrap remoteAddress(InetAddress inetHost, int inetPort) {
      this.remoteAddress = new InetSocketAddress(inetHost, inetPort);
      return this;
   }

   public ChannelFuture connect() {
      this.validate();
      SocketAddress remoteAddress = this.remoteAddress;
      if (remoteAddress == null) {
         throw new IllegalStateException("remoteAddress not set");
      } else {
         return this.doResolveAndConnect(remoteAddress, this.localAddress());
      }
   }

   public ChannelFuture connect(String inetHost, int inetPort) {
      return this.connect(InetSocketAddress.createUnresolved(inetHost, inetPort));
   }

   public ChannelFuture connect(InetAddress inetHost, int inetPort) {
      return this.connect(new InetSocketAddress(inetHost, inetPort));
   }

   public ChannelFuture connect(SocketAddress remoteAddress) {
      if (remoteAddress == null) {
         throw new NullPointerException("remoteAddress");
      } else {
         this.validate();
         return this.doResolveAndConnect(remoteAddress, this.localAddress());
      }
   }

   public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
      if (remoteAddress == null) {
         throw new NullPointerException("remoteAddress");
      } else {
         this.validate();
         return this.doResolveAndConnect(remoteAddress, localAddress);
      }
   }

   private ChannelFuture doResolveAndConnect(SocketAddress remoteAddress, final SocketAddress localAddress) {
      final ChannelFuture regFuture = this.initAndRegister();
      if (regFuture.cause() != null) {
         return regFuture;
      } else {
         final Channel channel = regFuture.channel();
         EventLoop eventLoop = channel.eventLoop();
         NameResolver<SocketAddress> resolver = this.resolver.getResolver(eventLoop);
         if (resolver.isSupported(remoteAddress) && !resolver.isResolved(remoteAddress)) {
            Future<SocketAddress> resolveFuture = resolver.resolve(remoteAddress);
            Throwable resolveFailureCause = resolveFuture.cause();
            if (resolveFailureCause != null) {
               channel.close();
               return channel.newFailedFuture(resolveFailureCause);
            } else if (resolveFuture.isDone()) {
               return doConnect((SocketAddress)resolveFuture.getNow(), localAddress, regFuture, channel.newPromise());
            } else {
               final ChannelPromise connectPromise = channel.newPromise();
               resolveFuture.addListener(new FutureListener<SocketAddress>() {
                  public void operationComplete(Future<SocketAddress> future) throws Exception {
                     if (future.cause() != null) {
                        channel.close();
                        connectPromise.setFailure(future.cause());
                     } else {
                        Bootstrap.doConnect((SocketAddress)future.getNow(), localAddress, regFuture, connectPromise);
                     }

                  }
               });
               return connectPromise;
            }
         } else {
            return doConnect(remoteAddress, localAddress, regFuture, channel.newPromise());
         }
      }
   }

   private static ChannelFuture doConnect(final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelFuture regFuture, final ChannelPromise connectPromise) {
      if (regFuture.isDone()) {
         doConnect0(remoteAddress, localAddress, regFuture, connectPromise);
      } else {
         regFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) throws Exception {
               Bootstrap.doConnect0(remoteAddress, localAddress, regFuture, connectPromise);
            }
         });
      }

      return connectPromise;
   }

   private static void doConnect0(final SocketAddress remoteAddress, final SocketAddress localAddress, final ChannelFuture regFuture, final ChannelPromise connectPromise) {
      final Channel channel = connectPromise.channel();
      channel.eventLoop().execute(new Runnable() {
         public void run() {
            if (regFuture.isSuccess()) {
               if (localAddress == null) {
                  channel.connect(remoteAddress, connectPromise);
               } else {
                  channel.connect(remoteAddress, localAddress, connectPromise);
               }

               connectPromise.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            } else {
               connectPromise.setFailure(regFuture.cause());
            }

         }
      });
   }

   void init(Channel channel) throws Exception {
      ChannelPipeline p = channel.pipeline();
      p.addLast(this.handler());
      Map<ChannelOption<?>, Object> options = this.options();
      synchronized(options) {
         Iterator i$ = options.entrySet().iterator();

         while(true) {
            if (!i$.hasNext()) {
               break;
            }

            Entry e = (Entry)i$.next();

            try {
               if (!channel.config().setOption((ChannelOption)e.getKey(), e.getValue())) {
                  logger.warn("Unknown channel option: " + e);
               }
            } catch (Throwable var10) {
               logger.warn("Failed to set a channel option: " + channel, var10);
            }
         }
      }

      Map<AttributeKey<?>, Object> attrs = this.attrs();
      synchronized(attrs) {
         Iterator i$ = attrs.entrySet().iterator();

         while(i$.hasNext()) {
            Entry<AttributeKey<?>, Object> e = (Entry)i$.next();
            channel.attr((AttributeKey)e.getKey()).set(e.getValue());
         }

      }
   }

   public Bootstrap validate() {
      super.validate();
      if (this.handler() == null) {
         throw new IllegalStateException("handler not set");
      } else {
         return this;
      }
   }

   public Bootstrap clone() {
      return new Bootstrap(this);
   }

   public String toString() {
      if (this.remoteAddress == null) {
         return super.toString();
      } else {
         StringBuilder buf = new StringBuilder(super.toString());
         buf.setLength(buf.length() - 1);
         return buf.append(", remoteAddress: ").append(this.remoteAddress).append(')').toString();
      }
   }

   static {
      DEFAULT_RESOLVER = DefaultNameResolverGroup.INSTANCE;
   }
}
