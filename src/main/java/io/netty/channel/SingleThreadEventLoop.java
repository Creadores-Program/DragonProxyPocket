package io.netty.channel;

import io.netty.util.concurrent.SingleThreadEventExecutor;
import java.util.concurrent.Executor;

public abstract class SingleThreadEventLoop extends SingleThreadEventExecutor implements EventLoop {
   private final ChannelHandlerInvoker invoker = new DefaultChannelHandlerInvoker(this);

   protected SingleThreadEventLoop(EventLoopGroup parent, Executor executor, boolean addTaskWakesUp) {
      super(parent, executor, addTaskWakesUp);
   }

   public EventLoopGroup parent() {
      return (EventLoopGroup)super.parent();
   }

   public EventLoop next() {
      return (EventLoop)super.next();
   }

   public ChannelHandlerInvoker asInvoker() {
      return this.invoker;
   }

   public ChannelFuture register(Channel channel) {
      return this.register(channel, new DefaultChannelPromise(channel, this));
   }

   public ChannelFuture register(Channel channel, ChannelPromise promise) {
      if (channel == null) {
         throw new NullPointerException("channel");
      } else if (promise == null) {
         throw new NullPointerException("promise");
      } else {
         channel.unsafe().register(this, promise);
         return promise;
      }
   }

   protected boolean wakesUpForTask(Runnable task) {
      return !(task instanceof SingleThreadEventLoop.NonWakeupRunnable);
   }

   public EventLoop unwrap() {
      return this;
   }

   interface NonWakeupRunnable extends Runnable {
   }
}
