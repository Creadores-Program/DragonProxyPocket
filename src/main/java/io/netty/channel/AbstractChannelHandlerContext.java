package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.ResourceLeakHint;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.concurrent.PausableEventExecutor;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import java.net.SocketAddress;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

abstract class AbstractChannelHandlerContext implements ChannelHandlerContext, ResourceLeakHint {
   static final int MASK_HANDLER_ADDED = 1;
   static final int MASK_HANDLER_REMOVED = 2;
   private static final int MASK_EXCEPTION_CAUGHT = 4;
   private static final int MASK_CHANNEL_REGISTERED = 8;
   private static final int MASK_CHANNEL_UNREGISTERED = 16;
   private static final int MASK_CHANNEL_ACTIVE = 32;
   private static final int MASK_CHANNEL_INACTIVE = 64;
   private static final int MASK_CHANNEL_READ = 128;
   private static final int MASK_CHANNEL_READ_COMPLETE = 256;
   private static final int MASK_CHANNEL_WRITABILITY_CHANGED = 512;
   private static final int MASK_USER_EVENT_TRIGGERED = 1024;
   private static final int MASK_BIND = 2048;
   private static final int MASK_CONNECT = 4096;
   private static final int MASK_DISCONNECT = 8192;
   private static final int MASK_CLOSE = 16384;
   private static final int MASK_DEREGISTER = 32768;
   private static final int MASK_READ = 65536;
   private static final int MASK_WRITE = 131072;
   private static final int MASK_FLUSH = 262144;
   private static final int MASKGROUP_INBOUND = 2044;
   private static final int MASKGROUP_OUTBOUND = 522240;
   private static final FastThreadLocal<WeakHashMap<Class<?>, Integer>> skipFlagsCache = new FastThreadLocal<WeakHashMap<Class<?>, Integer>>() {
      protected WeakHashMap<Class<?>, Integer> initialValue() throws Exception {
         return new WeakHashMap();
      }
   };
   private static final AtomicReferenceFieldUpdater<AbstractChannelHandlerContext, PausableChannelEventExecutor> WRAPPED_EVENTEXECUTOR_UPDATER;
   volatile AbstractChannelHandlerContext next;
   volatile AbstractChannelHandlerContext prev;
   private final AbstractChannel channel;
   private final DefaultChannelPipeline pipeline;
   private final String name;
   boolean invokedThisChannelRead;
   private volatile boolean invokedNextChannelRead;
   private volatile boolean invokedPrevRead;
   private boolean removed;
   final int skipFlags;
   final ChannelHandlerInvoker invoker;
   private ChannelFuture succeededFuture;
   volatile Runnable invokeChannelReadCompleteTask;
   volatile Runnable invokeReadTask;
   volatile Runnable invokeFlushTask;
   volatile Runnable invokeChannelWritableStateChangedTask;
   private volatile PausableChannelEventExecutor wrappedEventLoop;

   static int skipFlags(ChannelHandler handler) {
      WeakHashMap<Class<?>, Integer> cache = (WeakHashMap)skipFlagsCache.get();
      Class<? extends ChannelHandler> handlerType = handler.getClass();
      Integer flags = (Integer)cache.get(handlerType);
      int flagsVal;
      if (flags != null) {
         flagsVal = flags;
      } else {
         flagsVal = skipFlags0(handlerType);
         cache.put(handlerType, flagsVal);
      }

      return flagsVal;
   }

   static int skipFlags0(Class<? extends ChannelHandler> handlerType) {
      int flags = 0;

      try {
         if (isSkippable(handlerType, "handlerAdded")) {
            flags |= 1;
         }

         if (isSkippable(handlerType, "handlerRemoved")) {
            flags |= 2;
         }

         if (isSkippable(handlerType, "exceptionCaught", Throwable.class)) {
            flags |= 4;
         }

         if (isSkippable(handlerType, "channelRegistered")) {
            flags |= 8;
         }

         if (isSkippable(handlerType, "channelUnregistered")) {
            flags |= 16;
         }

         if (isSkippable(handlerType, "channelActive")) {
            flags |= 32;
         }

         if (isSkippable(handlerType, "channelInactive")) {
            flags |= 64;
         }

         if (isSkippable(handlerType, "channelRead", Object.class)) {
            flags |= 128;
         }

         if (isSkippable(handlerType, "channelReadComplete")) {
            flags |= 256;
         }

         if (isSkippable(handlerType, "channelWritabilityChanged")) {
            flags |= 512;
         }

         if (isSkippable(handlerType, "userEventTriggered", Object.class)) {
            flags |= 1024;
         }

         if (isSkippable(handlerType, "bind", SocketAddress.class, ChannelPromise.class)) {
            flags |= 2048;
         }

         if (isSkippable(handlerType, "connect", SocketAddress.class, SocketAddress.class, ChannelPromise.class)) {
            flags |= 4096;
         }

         if (isSkippable(handlerType, "disconnect", ChannelPromise.class)) {
            flags |= 8192;
         }

         if (isSkippable(handlerType, "close", ChannelPromise.class)) {
            flags |= 16384;
         }

         if (isSkippable(handlerType, "deregister", ChannelPromise.class)) {
            flags |= 32768;
         }

         if (isSkippable(handlerType, "read")) {
            flags |= 65536;
         }

         if (isSkippable(handlerType, "write", Object.class, ChannelPromise.class)) {
            flags |= 131072;
         }

         if (isSkippable(handlerType, "flush")) {
            flags |= 262144;
         }
      } catch (Exception var3) {
         PlatformDependent.throwException(var3);
      }

      return flags;
   }

   private static boolean isSkippable(Class<?> handlerType, String methodName, Class<?>... paramTypes) throws Exception {
      Class[] newParamTypes = new Class[paramTypes.length + 1];
      newParamTypes[0] = ChannelHandlerContext.class;
      System.arraycopy(paramTypes, 0, newParamTypes, 1, paramTypes.length);
      return handlerType.getMethod(methodName, newParamTypes).isAnnotationPresent(ChannelHandler.Skip.class);
   }

   AbstractChannelHandlerContext(DefaultChannelPipeline pipeline, ChannelHandlerInvoker invoker, String name, int skipFlags) {
      if (name == null) {
         throw new NullPointerException("name");
      } else {
         this.channel = pipeline.channel;
         this.pipeline = pipeline;
         this.name = name;
         this.invoker = invoker;
         this.skipFlags = skipFlags;
      }
   }

   public final Channel channel() {
      return this.channel;
   }

   public ChannelPipeline pipeline() {
      return this.pipeline;
   }

   public ByteBufAllocator alloc() {
      return this.channel().config().getAllocator();
   }

   public final EventExecutor executor() {
      return (EventExecutor)(this.invoker == null ? this.channel().eventLoop() : this.wrappedEventLoop());
   }

   public final ChannelHandlerInvoker invoker() {
      return (ChannelHandlerInvoker)(this.invoker == null ? this.channel().eventLoop().asInvoker() : this.wrappedEventLoop());
   }

   private PausableChannelEventExecutor wrappedEventLoop() {
      PausableChannelEventExecutor wrapped = this.wrappedEventLoop;
      if (wrapped == null) {
         wrapped = new AbstractChannelHandlerContext.PausableChannelEventExecutor0();
         if (!WRAPPED_EVENTEXECUTOR_UPDATER.compareAndSet(this, (Object)null, wrapped)) {
            return this.wrappedEventLoop;
         }
      }

      return (PausableChannelEventExecutor)wrapped;
   }

   public String name() {
      return this.name;
   }

   public <T> Attribute<T> attr(AttributeKey<T> key) {
      return this.channel.attr(key);
   }

   public <T> boolean hasAttr(AttributeKey<T> key) {
      return this.channel.hasAttr(key);
   }

   public ChannelHandlerContext fireChannelRegistered() {
      AbstractChannelHandlerContext next = this.findContextInbound();
      next.invoker().invokeChannelRegistered(next);
      return this;
   }

   public ChannelHandlerContext fireChannelUnregistered() {
      AbstractChannelHandlerContext next = this.findContextInbound();
      next.invoker().invokeChannelUnregistered(next);
      return this;
   }

   public ChannelHandlerContext fireChannelActive() {
      AbstractChannelHandlerContext next = this.findContextInbound();
      next.invoker().invokeChannelActive(next);
      return this;
   }

   public ChannelHandlerContext fireChannelInactive() {
      AbstractChannelHandlerContext next = this.findContextInbound();
      next.invoker().invokeChannelInactive(next);
      return this;
   }

   public ChannelHandlerContext fireExceptionCaught(Throwable cause) {
      AbstractChannelHandlerContext next = this.findContextInbound();
      next.invoker().invokeExceptionCaught(next, cause);
      return this;
   }

   public ChannelHandlerContext fireUserEventTriggered(Object event) {
      AbstractChannelHandlerContext next = this.findContextInbound();
      next.invoker().invokeUserEventTriggered(next, event);
      return this;
   }

   public ChannelHandlerContext fireChannelRead(Object msg) {
      AbstractChannelHandlerContext next = this.findContextInbound();
      ReferenceCountUtil.touch(msg, next);
      this.invokedNextChannelRead = true;
      next.invoker().invokeChannelRead(next, msg);
      return this;
   }

   public ChannelHandlerContext fireChannelReadComplete() {
      if (!this.invokedNextChannelRead && this.invokedThisChannelRead) {
         if (this.invokedPrevRead && !this.channel().config().isAutoRead()) {
            this.read();
         } else {
            this.invokedPrevRead = false;
         }

         return this;
      } else {
         this.invokedNextChannelRead = false;
         this.invokedPrevRead = false;
         AbstractChannelHandlerContext next = this.findContextInbound();
         next.invoker().invokeChannelReadComplete(next);
         return this;
      }
   }

   public ChannelHandlerContext fireChannelWritabilityChanged() {
      AbstractChannelHandlerContext next = this.findContextInbound();
      next.invoker().invokeChannelWritabilityChanged(next);
      return this;
   }

   public ChannelFuture bind(SocketAddress localAddress) {
      return this.bind(localAddress, this.newPromise());
   }

   public ChannelFuture connect(SocketAddress remoteAddress) {
      return this.connect(remoteAddress, this.newPromise());
   }

   public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
      return this.connect(remoteAddress, localAddress, this.newPromise());
   }

   public ChannelFuture disconnect() {
      return this.disconnect(this.newPromise());
   }

   public ChannelFuture close() {
      return this.close(this.newPromise());
   }

   public ChannelFuture deregister() {
      return this.deregister(this.newPromise());
   }

   public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
      AbstractChannelHandlerContext next = this.findContextOutbound();
      next.invoker().invokeBind(next, localAddress, promise);
      return promise;
   }

   public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
      return this.connect(remoteAddress, (SocketAddress)null, promise);
   }

   public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
      AbstractChannelHandlerContext next = this.findContextOutbound();
      next.invoker().invokeConnect(next, remoteAddress, localAddress, promise);
      return promise;
   }

   public ChannelFuture disconnect(ChannelPromise promise) {
      if (!this.channel().metadata().hasDisconnect()) {
         return this.close(promise);
      } else {
         AbstractChannelHandlerContext next = this.findContextOutbound();
         next.invoker().invokeDisconnect(next, promise);
         return promise;
      }
   }

   public ChannelFuture close(ChannelPromise promise) {
      AbstractChannelHandlerContext next = this.findContextOutbound();
      next.invoker().invokeClose(next, promise);
      return promise;
   }

   public ChannelFuture deregister(ChannelPromise promise) {
      AbstractChannelHandlerContext next = this.findContextOutbound();
      next.invoker().invokeDeregister(next, promise);
      return promise;
   }

   public ChannelHandlerContext read() {
      AbstractChannelHandlerContext next = this.findContextOutbound();
      this.invokedPrevRead = true;
      next.invoker().invokeRead(next);
      return this;
   }

   public ChannelFuture write(Object msg) {
      return this.write(msg, this.newPromise());
   }

   public ChannelFuture write(Object msg, ChannelPromise promise) {
      AbstractChannelHandlerContext next = this.findContextOutbound();
      ReferenceCountUtil.touch(msg, next);
      next.invoker().invokeWrite(next, msg, promise);
      return promise;
   }

   public ChannelHandlerContext flush() {
      AbstractChannelHandlerContext next = this.findContextOutbound();
      next.invoker().invokeFlush(next);
      return this;
   }

   public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
      AbstractChannelHandlerContext next = this.findContextOutbound();
      ReferenceCountUtil.touch(msg, next);
      next.invoker().invokeWrite(next, msg, promise);
      next = this.findContextOutbound();
      next.invoker().invokeFlush(next);
      return promise;
   }

   public ChannelFuture writeAndFlush(Object msg) {
      return this.writeAndFlush(msg, this.newPromise());
   }

   public ChannelPromise newPromise() {
      return new DefaultChannelPromise(this.channel(), this.executor());
   }

   public ChannelProgressivePromise newProgressivePromise() {
      return new DefaultChannelProgressivePromise(this.channel(), this.executor());
   }

   public ChannelFuture newSucceededFuture() {
      ChannelFuture succeededFuture = this.succeededFuture;
      if (succeededFuture == null) {
         this.succeededFuture = (ChannelFuture)(succeededFuture = new SucceededChannelFuture(this.channel(), this.executor()));
      }

      return (ChannelFuture)succeededFuture;
   }

   public ChannelFuture newFailedFuture(Throwable cause) {
      return new FailedChannelFuture(this.channel(), this.executor(), cause);
   }

   private AbstractChannelHandlerContext findContextInbound() {
      AbstractChannelHandlerContext ctx = this;

      do {
         ctx = ctx.next;
      } while((ctx.skipFlags & 2044) == 2044);

      return ctx;
   }

   private AbstractChannelHandlerContext findContextOutbound() {
      AbstractChannelHandlerContext ctx = this;

      do {
         ctx = ctx.prev;
      } while((ctx.skipFlags & 522240) == 522240);

      return ctx;
   }

   public ChannelPromise voidPromise() {
      return this.channel.voidPromise();
   }

   void setRemoved() {
      this.removed = true;
   }

   public boolean isRemoved() {
      return this.removed;
   }

   public String toHintString() {
      return '\'' + this.name + "' will handle the message from this point.";
   }

   public String toString() {
      return StringUtil.simpleClassName(ChannelHandlerContext.class) + '(' + this.name + ", " + this.channel + ')';
   }

   static {
      AtomicReferenceFieldUpdater<AbstractChannelHandlerContext, PausableChannelEventExecutor> updater = PlatformDependent.newAtomicReferenceFieldUpdater(AbstractChannelHandlerContext.class, "wrappedEventLoop");
      if (updater == null) {
         updater = AtomicReferenceFieldUpdater.newUpdater(AbstractChannelHandlerContext.class, PausableChannelEventExecutor.class, "wrappedEventLoop");
      }

      WRAPPED_EVENTEXECUTOR_UPDATER = updater;
   }

   private final class PausableChannelEventExecutor0 extends PausableChannelEventExecutor {
      private PausableChannelEventExecutor0() {
      }

      public void rejectNewTasks() {
         ((PausableEventExecutor)this.channel().eventLoop()).rejectNewTasks();
      }

      public void acceptNewTasks() {
         ((PausableEventExecutor)this.channel().eventLoop()).acceptNewTasks();
      }

      public boolean isAcceptingNewTasks() {
         return ((PausableEventExecutor)this.channel().eventLoop()).isAcceptingNewTasks();
      }

      public Channel channel() {
         return AbstractChannelHandlerContext.this.channel();
      }

      public EventExecutor unwrap() {
         return this.unwrapInvoker().executor();
      }

      public ChannelHandlerInvoker unwrapInvoker() {
         return AbstractChannelHandlerContext.this.invoker;
      }

      // $FF: synthetic method
      PausableChannelEventExecutor0(Object x1) {
         this();
      }
   }
}
