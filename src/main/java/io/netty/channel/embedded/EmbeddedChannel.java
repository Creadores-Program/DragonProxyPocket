package io.netty.channel.embedded;

import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.EventLoop;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.RecyclableArrayList;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Queue;

public class EmbeddedChannel extends AbstractChannel {
   private static final SocketAddress LOCAL_ADDRESS = new EmbeddedSocketAddress();
   private static final SocketAddress REMOTE_ADDRESS = new EmbeddedSocketAddress();
   private static final ChannelHandler[] EMPTY_HANDLERS = new ChannelHandler[0];
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(EmbeddedChannel.class);
   private static final ChannelMetadata METADATA = new ChannelMetadata(false);
   private final EmbeddedEventLoop loop;
   private final ChannelConfig config;
   private final Queue<Object> inboundMessages;
   private final Queue<Object> outboundMessages;
   private Throwable lastException;
   private EmbeddedChannel.State state;

   public EmbeddedChannel() {
      this(EMPTY_HANDLERS);
   }

   public EmbeddedChannel(ChannelHandler... handlers) {
      super((Channel)null, EmbeddedChannelId.INSTANCE);
      this.loop = new EmbeddedEventLoop();
      this.config = new DefaultChannelConfig(this);
      this.inboundMessages = new ArrayDeque();
      this.outboundMessages = new ArrayDeque();
      if (handlers == null) {
         throw new NullPointerException("handlers");
      } else {
         ChannelPipeline p = this.pipeline();
         ChannelHandler[] arr$ = handlers;
         int len$ = handlers.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            ChannelHandler h = arr$[i$];
            if (h == null) {
               break;
            }

            p.addLast(h);
         }

         this.loop.register(this);
         p.addLast(new EmbeddedChannel.LastInboundHandler());
      }
   }

   public ChannelMetadata metadata() {
      return METADATA;
   }

   public ChannelConfig config() {
      return this.config;
   }

   public boolean isOpen() {
      return this.state != EmbeddedChannel.State.CLOSED;
   }

   public boolean isActive() {
      return this.state == EmbeddedChannel.State.ACTIVE;
   }

   public Queue<Object> inboundMessages() {
      return this.inboundMessages;
   }

   /** @deprecated */
   @Deprecated
   public Queue<Object> lastInboundBuffer() {
      return this.inboundMessages();
   }

   public Queue<Object> outboundMessages() {
      return this.outboundMessages;
   }

   /** @deprecated */
   @Deprecated
   public Queue<Object> lastOutboundBuffer() {
      return this.outboundMessages();
   }

   public <T> T readInbound() {
      return this.inboundMessages.poll();
   }

   public <T> T readOutbound() {
      return this.outboundMessages.poll();
   }

   public boolean writeInbound(Object... msgs) {
      this.ensureOpen();
      if (msgs.length == 0) {
         return !this.inboundMessages.isEmpty();
      } else {
         ChannelPipeline p = this.pipeline();
         Object[] arr$ = msgs;
         int len$ = msgs.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            Object m = arr$[i$];
            p.fireChannelRead(m);
         }

         p.fireChannelReadComplete();
         this.runPendingTasks();
         this.checkException();
         return !this.inboundMessages.isEmpty();
      }
   }

   public boolean writeOutbound(Object... msgs) {
      this.ensureOpen();
      if (msgs.length == 0) {
         return !this.outboundMessages.isEmpty();
      } else {
         RecyclableArrayList futures = RecyclableArrayList.newInstance(msgs.length);

         try {
            Object[] arr$ = msgs;
            int i = msgs.length;

            for(int i$ = 0; i$ < i; ++i$) {
               Object m = arr$[i$];
               if (m == null) {
                  break;
               }

               futures.add(this.write(m));
            }

            this.flush();
            int size = futures.size();

            for(i = 0; i < size; ++i) {
               ChannelFuture future = (ChannelFuture)futures.get(i);

               assert future.isDone();

               if (future.cause() != null) {
                  this.recordException(future.cause());
               }
            }

            this.runPendingTasks();
            this.checkException();
            boolean var11 = !this.outboundMessages.isEmpty();
            return var11;
         } finally {
            futures.recycle();
         }
      }
   }

   public boolean finish() {
      this.close();
      this.runPendingTasks();
      this.loop.cancelScheduledTasks();
      this.checkException();
      return !this.inboundMessages.isEmpty() || !this.outboundMessages.isEmpty();
   }

   public void runPendingTasks() {
      try {
         this.loop.runTasks();
      } catch (Exception var3) {
         this.recordException(var3);
      }

      try {
         this.loop.runScheduledTasks();
      } catch (Exception var2) {
         this.recordException(var2);
      }

   }

   public long runScheduledPendingTasks() {
      try {
         return this.loop.runScheduledTasks();
      } catch (Exception var2) {
         this.recordException(var2);
         return this.loop.nextScheduledTask();
      }
   }

   private void recordException(Throwable cause) {
      if (this.lastException == null) {
         this.lastException = cause;
      } else {
         logger.warn("More than one exception was raised. Will report only the first one and log others.", cause);
      }

   }

   public void checkException() {
      Throwable t = this.lastException;
      if (t != null) {
         this.lastException = null;
         PlatformDependent.throwException(t);
      }
   }

   protected final void ensureOpen() {
      if (!this.isOpen()) {
         this.recordException(new ClosedChannelException());
         this.checkException();
      }

   }

   protected boolean isCompatible(EventLoop loop) {
      return loop instanceof EmbeddedEventLoop;
   }

   protected SocketAddress localAddress0() {
      return this.isActive() ? LOCAL_ADDRESS : null;
   }

   protected SocketAddress remoteAddress0() {
      return this.isActive() ? REMOTE_ADDRESS : null;
   }

   protected void doRegister() throws Exception {
      this.state = EmbeddedChannel.State.ACTIVE;
   }

   protected void doBind(SocketAddress localAddress) throws Exception {
   }

   protected void doDisconnect() throws Exception {
      this.doClose();
   }

   protected void doClose() throws Exception {
      this.state = EmbeddedChannel.State.CLOSED;
   }

   protected void doBeginRead() throws Exception {
   }

   protected AbstractChannel.AbstractUnsafe newUnsafe() {
      return new EmbeddedChannel.DefaultUnsafe();
   }

   protected void doWrite(ChannelOutboundBuffer in) throws Exception {
      while(true) {
         Object msg = in.current();
         if (msg == null) {
            return;
         }

         ReferenceCountUtil.retain(msg);
         this.outboundMessages.add(msg);
         in.remove();
      }
   }

   private final class LastInboundHandler extends ChannelHandlerAdapter {
      private LastInboundHandler() {
      }

      public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
         EmbeddedChannel.this.inboundMessages.add(msg);
      }

      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
         EmbeddedChannel.this.recordException(cause);
      }

      // $FF: synthetic method
      LastInboundHandler(Object x1) {
         this();
      }
   }

   private class DefaultUnsafe extends AbstractChannel.AbstractUnsafe {
      private DefaultUnsafe() {
         super();
      }

      public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
         this.safeSetSuccess(promise);
      }

      // $FF: synthetic method
      DefaultUnsafe(Object x1) {
         this();
      }
   }

   private static enum State {
      OPEN,
      ACTIVE,
      CLOSED;
   }
}
