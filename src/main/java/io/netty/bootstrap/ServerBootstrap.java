package io.netty.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.util.AttributeKey;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel> {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(ServerBootstrap.class);
   private final Map<ChannelOption<?>, Object> childOptions = new LinkedHashMap();
   private final Map<AttributeKey<?>, Object> childAttrs = new LinkedHashMap();
   private volatile EventLoopGroup childGroup;
   private volatile ChannelHandler childHandler;

   public ServerBootstrap() {
   }

   private ServerBootstrap(ServerBootstrap bootstrap) {
      super(bootstrap);
      this.childGroup = bootstrap.childGroup;
      this.childHandler = bootstrap.childHandler;
      synchronized(bootstrap.childOptions) {
         this.childOptions.putAll(bootstrap.childOptions);
      }

      synchronized(bootstrap.childAttrs) {
         this.childAttrs.putAll(bootstrap.childAttrs);
      }
   }

   public ServerBootstrap group(EventLoopGroup group) {
      return this.group(group, group);
   }

   public ServerBootstrap group(EventLoopGroup parentGroup, EventLoopGroup childGroup) {
      super.group(parentGroup);
      if (childGroup == null) {
         throw new NullPointerException("childGroup");
      } else if (this.childGroup != null) {
         throw new IllegalStateException("childGroup set already");
      } else {
         this.childGroup = childGroup;
         return this;
      }
   }

   public <T> ServerBootstrap childOption(ChannelOption<T> childOption, T value) {
      if (childOption == null) {
         throw new NullPointerException("childOption");
      } else {
         if (value == null) {
            synchronized(this.childOptions) {
               this.childOptions.remove(childOption);
            }
         } else {
            synchronized(this.childOptions) {
               this.childOptions.put(childOption, value);
            }
         }

         return this;
      }
   }

   public <T> ServerBootstrap childAttr(AttributeKey<T> childKey, T value) {
      if (childKey == null) {
         throw new NullPointerException("childKey");
      } else {
         if (value == null) {
            this.childAttrs.remove(childKey);
         } else {
            this.childAttrs.put(childKey, value);
         }

         return this;
      }
   }

   public ServerBootstrap childHandler(ChannelHandler childHandler) {
      if (childHandler == null) {
         throw new NullPointerException("childHandler");
      } else {
         this.childHandler = childHandler;
         return this;
      }
   }

   public EventLoopGroup childGroup() {
      return this.childGroup;
   }

   void init(Channel channel) throws Exception {
      Map<ChannelOption<?>, Object> options = this.options();
      synchronized(options) {
         channel.config().setOptions(options);
      }

      Map<AttributeKey<?>, Object> attrs = this.attrs();
      synchronized(attrs) {
         Iterator i$ = attrs.entrySet().iterator();

         while(true) {
            if (!i$.hasNext()) {
               break;
            }

            Entry<AttributeKey<?>, Object> e = (Entry)i$.next();
            AttributeKey<Object> key = (AttributeKey)e.getKey();
            channel.attr(key).set(e.getValue());
         }
      }

      ChannelPipeline p = channel.pipeline();
      if (this.handler() != null) {
         p.addLast(this.handler());
      }

      final EventLoopGroup currentChildGroup = this.childGroup;
      final ChannelHandler currentChildHandler = this.childHandler;
      final Entry[] currentChildOptions;
      synchronized(this.childOptions) {
         currentChildOptions = (Entry[])this.childOptions.entrySet().toArray(newOptionArray(this.childOptions.size()));
      }

      final Entry[] currentChildAttrs;
      synchronized(this.childAttrs) {
         currentChildAttrs = (Entry[])this.childAttrs.entrySet().toArray(newAttrArray(this.childAttrs.size()));
      }

      p.addLast(new ChannelInitializer<Channel>() {
         public void initChannel(Channel ch) throws Exception {
            ch.pipeline().addLast(new ServerBootstrap.ServerBootstrapAcceptor(currentChildGroup, currentChildHandler, currentChildOptions, currentChildAttrs));
         }
      });
   }

   public ServerBootstrap validate() {
      super.validate();
      if (this.childHandler == null) {
         throw new IllegalStateException("childHandler not set");
      } else {
         if (this.childGroup == null) {
            logger.warn("childGroup is not set. Using parentGroup instead.");
            this.childGroup = this.group();
         }

         return this;
      }
   }

   private static Entry<ChannelOption<?>, Object>[] newOptionArray(int size) {
      return new Entry[size];
   }

   private static Entry<AttributeKey<?>, Object>[] newAttrArray(int size) {
      return new Entry[size];
   }

   public ServerBootstrap clone() {
      return new ServerBootstrap(this);
   }

   public String toString() {
      StringBuilder buf = new StringBuilder(super.toString());
      buf.setLength(buf.length() - 1);
      buf.append(", ");
      if (this.childGroup != null) {
         buf.append("childGroup: ");
         buf.append(StringUtil.simpleClassName((Object)this.childGroup));
         buf.append(", ");
      }

      synchronized(this.childOptions) {
         if (!this.childOptions.isEmpty()) {
            buf.append("childOptions: ");
            buf.append(this.childOptions);
            buf.append(", ");
         }
      }

      synchronized(this.childAttrs) {
         if (!this.childAttrs.isEmpty()) {
            buf.append("childAttrs: ");
            buf.append(this.childAttrs);
            buf.append(", ");
         }
      }

      if (this.childHandler != null) {
         buf.append("childHandler: ");
         buf.append(this.childHandler);
         buf.append(", ");
      }

      if (buf.charAt(buf.length() - 1) == '(') {
         buf.append(')');
      } else {
         buf.setCharAt(buf.length() - 2, ')');
         buf.setLength(buf.length() - 1);
      }

      return buf.toString();
   }

   private static class ServerBootstrapAcceptor extends ChannelHandlerAdapter {
      private final EventLoopGroup childGroup;
      private final ChannelHandler childHandler;
      private final Entry<ChannelOption<?>, Object>[] childOptions;
      private final Entry<AttributeKey<?>, Object>[] childAttrs;

      ServerBootstrapAcceptor(EventLoopGroup childGroup, ChannelHandler childHandler, Entry<ChannelOption<?>, Object>[] childOptions, Entry<AttributeKey<?>, Object>[] childAttrs) {
         this.childGroup = childGroup;
         this.childHandler = childHandler;
         this.childOptions = childOptions;
         this.childAttrs = childAttrs;
      }

      public void channelRead(ChannelHandlerContext ctx, Object msg) {
         final Channel child = (Channel)msg;
         child.pipeline().addLast(this.childHandler);
         Entry[] arr$ = this.childOptions;
         int len$ = arr$.length;

         int i$;
         Entry e;
         for(i$ = 0; i$ < len$; ++i$) {
            e = arr$[i$];

            try {
               if (!child.config().setOption((ChannelOption)e.getKey(), e.getValue())) {
                  ServerBootstrap.logger.warn("Unknown channel option: " + e);
               }
            } catch (Throwable var10) {
               ServerBootstrap.logger.warn("Failed to set a channel option: " + child, var10);
            }
         }

         arr$ = this.childAttrs;
         len$ = arr$.length;

         for(i$ = 0; i$ < len$; ++i$) {
            e = arr$[i$];
            child.attr((AttributeKey)e.getKey()).set(e.getValue());
         }

         try {
            this.childGroup.register(child).addListener(new ChannelFutureListener() {
               public void operationComplete(ChannelFuture future) throws Exception {
                  if (!future.isSuccess()) {
                     ServerBootstrap.ServerBootstrapAcceptor.forceClose(child, future.cause());
                  }

               }
            });
         } catch (Throwable var9) {
            forceClose(child, var9);
         }

      }

      private static void forceClose(Channel child, Throwable t) {
         child.unsafe().closeForcibly();
         ServerBootstrap.logger.warn("Failed to register an accepted channel: " + child, t);
      }

      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
         final ChannelConfig config = ctx.channel().config();
         if (config.isAutoRead()) {
            config.setAutoRead(false);
            ctx.channel().eventLoop().schedule(new Runnable() {
               public void run() {
                  config.setAutoRead(true);
               }
            }, 1L, TimeUnit.SECONDS);
         }

         ctx.fireExceptionCaught(cause);
      }
   }
}
