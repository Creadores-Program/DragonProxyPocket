package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.DefaultAttributeMap;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.OneTimeTask;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public abstract class AbstractChannel extends DefaultAttributeMap implements Channel {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractChannel.class);
   static final ClosedChannelException CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();
   static final NotYetConnectedException NOT_YET_CONNECTED_EXCEPTION = new NotYetConnectedException();
   private MessageSizeEstimator.Handle estimatorHandle;
   private final Channel parent;
   private final ChannelId id;
   private final Channel.Unsafe unsafe;
   private final DefaultChannelPipeline pipeline;
   private final ChannelFuture succeededFuture = new SucceededChannelFuture(this, (EventExecutor)null);
   private final VoidChannelPromise voidPromise = new VoidChannelPromise(this, true);
   private final VoidChannelPromise unsafeVoidPromise = new VoidChannelPromise(this, false);
   private final AbstractChannel.CloseFuture closeFuture = new AbstractChannel.CloseFuture(this);
   private volatile SocketAddress localAddress;
   private volatile SocketAddress remoteAddress;
   private volatile AbstractChannel.PausableChannelEventLoop eventLoop;
   private volatile boolean registered;
   private boolean strValActive;
   private String strVal;

   protected AbstractChannel(Channel parent) {
      this.parent = parent;
      this.id = DefaultChannelId.newInstance();
      this.unsafe = this.newUnsafe();
      this.pipeline = new DefaultChannelPipeline(this);
   }

   protected AbstractChannel(Channel parent, ChannelId id) {
      this.parent = parent;
      this.id = id;
      this.unsafe = this.newUnsafe();
      this.pipeline = new DefaultChannelPipeline(this);
   }

   public final ChannelId id() {
      return this.id;
   }

   public boolean isWritable() {
      ChannelOutboundBuffer buf = this.unsafe.outboundBuffer();
      return buf != null && buf.isWritable();
   }

   public Channel parent() {
      return this.parent;
   }

   public ChannelPipeline pipeline() {
      return this.pipeline;
   }

   public ByteBufAllocator alloc() {
      return this.config().getAllocator();
   }

   public final EventLoop eventLoop() {
      EventLoop eventLoop = this.eventLoop;
      if (eventLoop == null) {
         throw new IllegalStateException("channel not registered to an event loop");
      } else {
         return eventLoop;
      }
   }

   public SocketAddress localAddress() {
      SocketAddress localAddress = this.localAddress;
      if (localAddress == null) {
         try {
            this.localAddress = localAddress = this.unsafe().localAddress();
         } catch (Throwable var3) {
            return null;
         }
      }

      return localAddress;
   }

   protected void invalidateLocalAddress() {
      this.localAddress = null;
   }

   public SocketAddress remoteAddress() {
      SocketAddress remoteAddress = this.remoteAddress;
      if (remoteAddress == null) {
         try {
            this.remoteAddress = remoteAddress = this.unsafe().remoteAddress();
         } catch (Throwable var3) {
            return null;
         }
      }

      return remoteAddress;
   }

   protected void invalidateRemoteAddress() {
      this.remoteAddress = null;
   }

   public boolean isRegistered() {
      return this.registered;
   }

   public ChannelFuture bind(SocketAddress localAddress) {
      return this.pipeline.bind(localAddress);
   }

   public ChannelFuture connect(SocketAddress remoteAddress) {
      return this.pipeline.connect(remoteAddress);
   }

   public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
      return this.pipeline.connect(remoteAddress, localAddress);
   }

   public ChannelFuture disconnect() {
      return this.pipeline.disconnect();
   }

   public ChannelFuture close() {
      return this.pipeline.close();
   }

   public ChannelFuture deregister() {
      this.eventLoop.rejectNewTasks();
      return this.pipeline.deregister();
   }

   public Channel flush() {
      this.pipeline.flush();
      return this;
   }

   public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
      return this.pipeline.bind(localAddress, promise);
   }

   public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
      return this.pipeline.connect(remoteAddress, promise);
   }

   public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
      return this.pipeline.connect(remoteAddress, localAddress, promise);
   }

   public ChannelFuture disconnect(ChannelPromise promise) {
      return this.pipeline.disconnect(promise);
   }

   public ChannelFuture close(ChannelPromise promise) {
      return this.pipeline.close(promise);
   }

   public ChannelFuture deregister(ChannelPromise promise) {
      this.eventLoop.rejectNewTasks();
      return this.pipeline.deregister(promise);
   }

   public Channel read() {
      this.pipeline.read();
      return this;
   }

   public ChannelFuture write(Object msg) {
      return this.pipeline.write(msg);
   }

   public ChannelFuture write(Object msg, ChannelPromise promise) {
      return this.pipeline.write(msg, promise);
   }

   public ChannelFuture writeAndFlush(Object msg) {
      return this.pipeline.writeAndFlush(msg);
   }

   public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
      return this.pipeline.writeAndFlush(msg, promise);
   }

   public ChannelPromise newPromise() {
      return new DefaultChannelPromise(this);
   }

   public ChannelProgressivePromise newProgressivePromise() {
      return new DefaultChannelProgressivePromise(this);
   }

   public ChannelFuture newSucceededFuture() {
      return this.succeededFuture;
   }

   public ChannelFuture newFailedFuture(Throwable cause) {
      return new FailedChannelFuture(this, (EventExecutor)null, cause);
   }

   public ChannelFuture closeFuture() {
      return this.closeFuture;
   }

   public Channel.Unsafe unsafe() {
      return this.unsafe;
   }

   protected abstract AbstractChannel.AbstractUnsafe newUnsafe();

   public final int hashCode() {
      return this.id.hashCode();
   }

   public final boolean equals(Object o) {
      return this == o;
   }

   public final int compareTo(Channel o) {
      return this == o ? 0 : this.id().compareTo(o.id());
   }

   public String toString() {
      boolean active = this.isActive();
      if (this.strValActive == active && this.strVal != null) {
         return this.strVal;
      } else {
         SocketAddress remoteAddr = this.remoteAddress();
         SocketAddress localAddr = this.localAddress();
         if (remoteAddr != null) {
            SocketAddress srcAddr;
            SocketAddress dstAddr;
            if (this.parent == null) {
               srcAddr = localAddr;
               dstAddr = remoteAddr;
            } else {
               srcAddr = remoteAddr;
               dstAddr = localAddr;
            }

            StringBuilder buf = (new StringBuilder(96)).append("[id: 0x").append(this.id.asShortText()).append(", ").append(srcAddr).append(active ? " => " : " :> ").append(dstAddr).append(']');
            this.strVal = buf.toString();
         } else {
            StringBuilder buf;
            if (localAddr != null) {
               buf = (new StringBuilder(64)).append("[id: 0x").append(this.id.asShortText()).append(", ").append(localAddr).append(']');
               this.strVal = buf.toString();
            } else {
               buf = (new StringBuilder(16)).append("[id: 0x").append(this.id.asShortText()).append(']');
               this.strVal = buf.toString();
            }
         }

         this.strValActive = active;
         return this.strVal;
      }
   }

   public final ChannelPromise voidPromise() {
      return this.voidPromise;
   }

   final MessageSizeEstimator.Handle estimatorHandle() {
      if (this.estimatorHandle == null) {
         this.estimatorHandle = this.config().getMessageSizeEstimator().newHandle();
      }

      return this.estimatorHandle;
   }

   protected abstract boolean isCompatible(EventLoop var1);

   protected abstract SocketAddress localAddress0();

   protected abstract SocketAddress remoteAddress0();

   protected void doRegister() throws Exception {
   }

   protected abstract void doBind(SocketAddress var1) throws Exception;

   protected abstract void doDisconnect() throws Exception;

   protected abstract void doClose() throws Exception;

   protected void doDeregister() throws Exception {
   }

   protected abstract void doBeginRead() throws Exception;

   protected abstract void doWrite(ChannelOutboundBuffer var1) throws Exception;

   protected Object filterOutboundMessage(Object msg) throws Exception {
      return msg;
   }

   static {
      CLOSED_CHANNEL_EXCEPTION.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
      NOT_YET_CONNECTED_EXCEPTION.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
   }

   private final class PausableChannelEventLoop extends PausableChannelEventExecutor implements EventLoop {
      volatile boolean isAcceptingNewTasks = true;
      volatile EventLoop unwrapped;

      PausableChannelEventLoop(EventLoop unwrapped) {
         this.unwrapped = unwrapped;
      }

      public void rejectNewTasks() {
         this.isAcceptingNewTasks = false;
      }

      public void acceptNewTasks() {
         this.isAcceptingNewTasks = true;
      }

      public boolean isAcceptingNewTasks() {
         return this.isAcceptingNewTasks;
      }

      public EventLoopGroup parent() {
         return this.unwrap().parent();
      }

      public EventLoop next() {
         return this.unwrap().next();
      }

      public EventLoop unwrap() {
         return this.unwrapped;
      }

      public ChannelHandlerInvoker asInvoker() {
         return this;
      }

      public ChannelFuture register(Channel channel) {
         return this.unwrap().register(channel);
      }

      public ChannelFuture register(Channel channel, ChannelPromise promise) {
         return this.unwrap().register(channel, promise);
      }

      Channel channel() {
         return AbstractChannel.this;
      }

      ChannelHandlerInvoker unwrapInvoker() {
         return this.unwrapped.asInvoker();
      }
   }

   static final class CloseFuture extends DefaultChannelPromise {
      CloseFuture(AbstractChannel ch) {
         super(ch);
      }

      public ChannelPromise setSuccess() {
         throw new IllegalStateException();
      }

      public ChannelPromise setFailure(Throwable cause) {
         throw new IllegalStateException();
      }

      public boolean trySuccess() {
         throw new IllegalStateException();
      }

      public boolean tryFailure(Throwable cause) {
         throw new IllegalStateException();
      }

      boolean setClosed() {
         return super.trySuccess();
      }
   }

   protected abstract class AbstractUnsafe implements Channel.Unsafe {
      private ChannelOutboundBuffer outboundBuffer = new ChannelOutboundBuffer(AbstractChannel.this);
      private RecvByteBufAllocator.Handle recvHandle;
      private boolean inFlush0;
      private boolean neverRegistered = true;

      public RecvByteBufAllocator.Handle recvBufAllocHandle() {
         if (this.recvHandle == null) {
            this.recvHandle = AbstractChannel.this.config().getRecvByteBufAllocator().newHandle();
         }

         return this.recvHandle;
      }

      public final ChannelHandlerInvoker invoker() {
         return ((PausableChannelEventExecutor)AbstractChannel.this.eventLoop().asInvoker()).unwrapInvoker();
      }

      public final ChannelOutboundBuffer outboundBuffer() {
         return this.outboundBuffer;
      }

      public final SocketAddress localAddress() {
         return AbstractChannel.this.localAddress0();
      }

      public final SocketAddress remoteAddress() {
         return AbstractChannel.this.remoteAddress0();
      }

      public final void register(EventLoop eventLoop, final ChannelPromise promise) {
         if (eventLoop == null) {
            throw new NullPointerException("eventLoop");
         } else if (promise == null) {
            throw new NullPointerException("promise");
         } else if (AbstractChannel.this.isRegistered()) {
            promise.setFailure(new IllegalStateException("registered to an event loop already"));
         } else if (!AbstractChannel.this.isCompatible(eventLoop)) {
            promise.setFailure(new IllegalStateException("incompatible event loop type: " + eventLoop.getClass().getName()));
         } else {
            if (AbstractChannel.this.eventLoop == null) {
               AbstractChannel.this.eventLoop = AbstractChannel.this.new PausableChannelEventLoop(eventLoop);
            } else {
               AbstractChannel.this.eventLoop.unwrapped = eventLoop;
            }

            if (eventLoop.inEventLoop()) {
               this.register0(promise);
            } else {
               try {
                  eventLoop.execute(new OneTimeTask() {
                     public void run() {
                        AbstractUnsafe.this.register0(promise);
                     }
                  });
               } catch (Throwable var4) {
                  AbstractChannel.logger.warn("Force-closing a channel whose registration task was not accepted by an event loop: {}", AbstractChannel.this, var4);
                  this.closeForcibly();
                  AbstractChannel.this.closeFuture.setClosed();
                  this.safeSetFailure(promise, var4);
               }
            }

         }
      }

      private void register0(ChannelPromise promise) {
         try {
            if (!promise.setUncancellable() || !this.ensureOpen(promise)) {
               return;
            }

            boolean firstRegistration = this.neverRegistered;
            AbstractChannel.this.doRegister();
            this.neverRegistered = false;
            AbstractChannel.this.registered = true;
            AbstractChannel.this.eventLoop.acceptNewTasks();
            this.safeSetSuccess(promise);
            AbstractChannel.this.pipeline.fireChannelRegistered();
            if (firstRegistration && AbstractChannel.this.isActive()) {
               AbstractChannel.this.pipeline.fireChannelActive();
            }
         } catch (Throwable var3) {
            this.closeForcibly();
            AbstractChannel.this.closeFuture.setClosed();
            this.safeSetFailure(promise, var3);
         }

      }

      public final void bind(SocketAddress localAddress, ChannelPromise promise) {
         if (promise.setUncancellable() && this.ensureOpen(promise)) {
            if (Boolean.TRUE.equals(AbstractChannel.this.config().getOption(ChannelOption.SO_BROADCAST)) && localAddress instanceof InetSocketAddress && !((InetSocketAddress)localAddress).getAddress().isAnyLocalAddress() && !PlatformDependent.isWindows() && !PlatformDependent.isRoot()) {
               AbstractChannel.logger.warn("A non-root user can't receive a broadcast packet if the socket is not bound to a wildcard address; binding to a non-wildcard address (" + localAddress + ") anyway as requested.");
            }

            boolean wasActive = AbstractChannel.this.isActive();

            try {
               AbstractChannel.this.doBind(localAddress);
            } catch (Throwable var5) {
               this.safeSetFailure(promise, var5);
               this.closeIfClosed();
               return;
            }

            if (!wasActive && AbstractChannel.this.isActive()) {
               this.invokeLater(new OneTimeTask() {
                  public void run() {
                     AbstractChannel.this.pipeline.fireChannelActive();
                  }
               });
            }

            this.safeSetSuccess(promise);
         }
      }

      public final void disconnect(ChannelPromise promise) {
         if (promise.setUncancellable()) {
            boolean wasActive = AbstractChannel.this.isActive();

            try {
               AbstractChannel.this.doDisconnect();
            } catch (Throwable var4) {
               this.safeSetFailure(promise, var4);
               this.closeIfClosed();
               return;
            }

            if (wasActive && !AbstractChannel.this.isActive()) {
               this.invokeLater(new OneTimeTask() {
                  public void run() {
                     AbstractChannel.this.pipeline.fireChannelInactive();
                  }
               });
            }

            this.safeSetSuccess(promise);
            this.closeIfClosed();
         }
      }

      public final void close(final ChannelPromise promise) {
         if (promise.setUncancellable()) {
            if (this.inFlush0) {
               this.invokeLater(new OneTimeTask() {
                  public void run() {
                     AbstractUnsafe.this.close(promise);
                  }
               });
            } else if (this.outboundBuffer == null) {
               AbstractChannel.this.closeFuture.addListener(new ChannelFutureListener() {
                  public void operationComplete(ChannelFuture future) throws Exception {
                     promise.setSuccess();
                  }
               });
            } else if (AbstractChannel.this.closeFuture.isDone()) {
               this.safeSetSuccess(promise);
            } else {
               final boolean wasActive = AbstractChannel.this.isActive();
               final ChannelOutboundBuffer buffer = this.outboundBuffer;
               this.outboundBuffer = null;
               Executor closeExecutor = this.closeExecutor();
               if (closeExecutor != null) {
                  closeExecutor.execute(new OneTimeTask() {
                     public void run() {
                        final Throwable cause = null;

                        try {
                           AbstractChannel.this.doClose();
                        } catch (Throwable var3) {
                           cause = var3;
                        }

                        AbstractUnsafe.this.invokeLater(new OneTimeTask() {
                           public void run() {
                              AbstractUnsafe.this.closeAndDeregister(buffer, wasActive, promise, cause);
                           }
                        });
                     }
                  });
               } else {
                  Throwable error = null;

                  try {
                     AbstractChannel.this.doClose();
                  } catch (Throwable var7) {
                     error = var7;
                  }

                  this.closeAndDeregister(buffer, wasActive, promise, error);
               }

            }
         }
      }

      private void closeAndDeregister(ChannelOutboundBuffer outboundBuffer, boolean wasActive, ChannelPromise promise, Throwable error) {
         try {
            outboundBuffer.failFlushed(AbstractChannel.CLOSED_CHANNEL_EXCEPTION);
            outboundBuffer.close(AbstractChannel.CLOSED_CHANNEL_EXCEPTION);
         } finally {
            if (wasActive && !AbstractChannel.this.isActive()) {
               this.invokeLater(new OneTimeTask() {
                  public void run() {
                     AbstractChannel.this.pipeline.fireChannelInactive();
                     AbstractUnsafe.this.deregister(AbstractUnsafe.this.voidPromise());
                  }
               });
            } else {
               this.invokeLater(new OneTimeTask() {
                  public void run() {
                     AbstractUnsafe.this.deregister(AbstractUnsafe.this.voidPromise());
                  }
               });
            }

            AbstractChannel.this.closeFuture.setClosed();
            if (error != null) {
               this.safeSetFailure(promise, error);
            } else {
               this.safeSetSuccess(promise);
            }

         }

      }

      public final void closeForcibly() {
         try {
            AbstractChannel.this.doClose();
         } catch (Exception var2) {
            AbstractChannel.logger.warn("Failed to close a channel.", (Throwable)var2);
         }

      }

      public final void deregister(ChannelPromise promise) {
         if (promise.setUncancellable()) {
            if (!AbstractChannel.this.registered) {
               this.safeSetSuccess(promise);
            } else {
               try {
                  AbstractChannel.this.doDeregister();
               } catch (Throwable var6) {
                  this.safeSetFailure(promise, var6);
                  AbstractChannel.logger.warn("Unexpected exception occurred while deregistering a channel.", var6);
               } finally {
                  if (AbstractChannel.this.registered) {
                     AbstractChannel.this.registered = false;
                     this.safeSetSuccess(promise);
                     AbstractChannel.this.pipeline.fireChannelUnregistered();
                  } else {
                     this.safeSetSuccess(promise);
                  }

               }

            }
         }
      }

      public final void beginRead() {
         if (AbstractChannel.this.isActive()) {
            try {
               AbstractChannel.this.doBeginRead();
            } catch (final Exception var2) {
               this.invokeLater(new OneTimeTask() {
                  public void run() {
                     AbstractChannel.this.pipeline.fireExceptionCaught(var2);
                  }
               });
               this.close(this.voidPromise());
            }

         }
      }

      public final void write(Object msg, ChannelPromise promise) {
         ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
         if (outboundBuffer == null) {
            this.safeSetFailure(promise, AbstractChannel.CLOSED_CHANNEL_EXCEPTION);
            ReferenceCountUtil.release(msg);
         } else {
            int size;
            try {
               msg = AbstractChannel.this.filterOutboundMessage(msg);
               size = AbstractChannel.this.estimatorHandle().size(msg);
               if (size < 0) {
                  size = 0;
               }
            } catch (Throwable var6) {
               this.safeSetFailure(promise, var6);
               ReferenceCountUtil.release(msg);
               return;
            }

            outboundBuffer.addMessage(msg, size, promise);
         }
      }

      public final void flush() {
         ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
         if (outboundBuffer != null) {
            outboundBuffer.addFlush();
            this.flush0();
         }
      }

      protected void flush0() {
         if (!this.inFlush0) {
            ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            if (outboundBuffer != null && !outboundBuffer.isEmpty()) {
               this.inFlush0 = true;
               if (!AbstractChannel.this.isActive()) {
                  try {
                     if (AbstractChannel.this.isOpen()) {
                        outboundBuffer.failFlushed(AbstractChannel.NOT_YET_CONNECTED_EXCEPTION);
                     } else {
                        outboundBuffer.failFlushed(AbstractChannel.CLOSED_CHANNEL_EXCEPTION);
                     }
                  } finally {
                     this.inFlush0 = false;
                  }

               } else {
                  try {
                     AbstractChannel.this.doWrite(outboundBuffer);
                  } catch (Throwable var11) {
                     outboundBuffer.failFlushed(var11);
                  } finally {
                     this.inFlush0 = false;
                  }

               }
            }
         }
      }

      public final ChannelPromise voidPromise() {
         return AbstractChannel.this.unsafeVoidPromise;
      }

      protected final boolean ensureOpen(ChannelPromise promise) {
         if (AbstractChannel.this.isOpen()) {
            return true;
         } else {
            this.safeSetFailure(promise, AbstractChannel.CLOSED_CHANNEL_EXCEPTION);
            return false;
         }
      }

      protected final void safeSetSuccess(ChannelPromise promise) {
         if (!(promise instanceof VoidChannelPromise) && !promise.trySuccess()) {
            AbstractChannel.logger.warn("Failed to mark a promise as success because it is done already: {}", (Object)promise);
         }

      }

      protected final void safeSetFailure(ChannelPromise promise, Throwable cause) {
         if (!(promise instanceof VoidChannelPromise) && !promise.tryFailure(cause)) {
            AbstractChannel.logger.warn("Failed to mark a promise as failure because it's done already: {}", promise, cause);
         }

      }

      protected final void closeIfClosed() {
         if (!AbstractChannel.this.isOpen()) {
            this.close(this.voidPromise());
         }
      }

      private void invokeLater(Runnable task) {
         try {
            AbstractChannel.this.eventLoop().unwrap().execute(task);
         } catch (RejectedExecutionException var3) {
            AbstractChannel.logger.warn("Can't invoke task later as EventLoop rejected it", (Throwable)var3);
         }

      }

      protected final Throwable annotateConnectException(Throwable cause, SocketAddress remoteAddress) {
         if (cause instanceof ConnectException) {
            Throwable newT = new ConnectException(((Throwable)cause).getMessage() + ": " + remoteAddress);
            newT.setStackTrace(((Throwable)cause).getStackTrace());
            cause = newT;
         } else if (cause instanceof NoRouteToHostException) {
            Throwable newTx = new NoRouteToHostException(((Throwable)cause).getMessage() + ": " + remoteAddress);
            newTx.setStackTrace(((Throwable)cause).getStackTrace());
            cause = newTx;
         } else if (cause instanceof SocketException) {
            Throwable newTxx = new SocketException(((Throwable)cause).getMessage() + ": " + remoteAddress);
            newTxx.setStackTrace(((Throwable)cause).getStackTrace());
            cause = newTxx;
         }

         return (Throwable)cause;
      }

      protected Executor closeExecutor() {
         return null;
      }
   }
}
