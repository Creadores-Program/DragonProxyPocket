package io.netty.handler.traffic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.internal.PlatformDependent;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@ChannelHandler.Sharable
public class GlobalTrafficShapingHandler extends AbstractTrafficShapingHandler {
   private final ConcurrentMap<Integer, GlobalTrafficShapingHandler.PerChannel> channelQueues = PlatformDependent.newConcurrentHashMap();
   private final AtomicLong queuesSize = new AtomicLong();
   long maxGlobalWriteSize = 419430400L;

   void createGlobalTrafficCounter(ScheduledExecutorService executor) {
      if (executor == null) {
         throw new NullPointerException("executor");
      } else {
         TrafficCounter tc = new TrafficCounter(this, executor, "GlobalTC", this.checkInterval);
         this.setTrafficCounter(tc);
         tc.start();
      }
   }

   public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long writeLimit, long readLimit, long checkInterval, long maxTime) {
      super(writeLimit, readLimit, checkInterval, maxTime);
      this.createGlobalTrafficCounter(executor);
   }

   public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long writeLimit, long readLimit, long checkInterval) {
      super(writeLimit, readLimit, checkInterval);
      this.createGlobalTrafficCounter(executor);
   }

   public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long writeLimit, long readLimit) {
      super(writeLimit, readLimit);
      this.createGlobalTrafficCounter(executor);
   }

   public GlobalTrafficShapingHandler(ScheduledExecutorService executor, long checkInterval) {
      super(checkInterval);
      this.createGlobalTrafficCounter(executor);
   }

   public GlobalTrafficShapingHandler(EventExecutor executor) {
      this.createGlobalTrafficCounter(executor);
   }

   public long getMaxGlobalWriteSize() {
      return this.maxGlobalWriteSize;
   }

   public void setMaxGlobalWriteSize(long maxGlobalWriteSize) {
      this.maxGlobalWriteSize = maxGlobalWriteSize;
   }

   public long queuesSize() {
      return this.queuesSize.get();
   }

   public final void release() {
      this.trafficCounter.stop();
   }

   private GlobalTrafficShapingHandler.PerChannel getOrSetPerChannel(ChannelHandlerContext ctx) {
      Channel channel = ctx.channel();
      Integer key = channel.hashCode();
      GlobalTrafficShapingHandler.PerChannel perChannel = (GlobalTrafficShapingHandler.PerChannel)this.channelQueues.get(key);
      if (perChannel == null) {
         perChannel = new GlobalTrafficShapingHandler.PerChannel();
         perChannel.messagesQueue = new ArrayDeque();
         perChannel.queueSize = 0L;
         perChannel.lastReadTimestamp = TrafficCounter.milliSecondFromNano();
         perChannel.lastWriteTimestamp = perChannel.lastReadTimestamp;
         this.channelQueues.put(key, perChannel);
      }

      return perChannel;
   }

   public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      this.getOrSetPerChannel(ctx);
      super.handlerAdded(ctx);
   }

   public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
      Channel channel = ctx.channel();
      Integer key = channel.hashCode();
      GlobalTrafficShapingHandler.PerChannel perChannel = (GlobalTrafficShapingHandler.PerChannel)this.channelQueues.remove(key);
      if (perChannel != null) {
         synchronized(perChannel) {
            Iterator i$;
            GlobalTrafficShapingHandler.ToSend toSend;
            if (channel.isActive()) {
               i$ = perChannel.messagesQueue.iterator();

               while(i$.hasNext()) {
                  toSend = (GlobalTrafficShapingHandler.ToSend)i$.next();
                  long size = this.calculateSize(toSend.toSend);
                  this.trafficCounter.bytesRealWriteFlowControl(size);
                  perChannel.queueSize -= size;
                  this.queuesSize.addAndGet(-size);
                  ctx.write(toSend.toSend, toSend.promise);
               }
            } else {
               this.queuesSize.addAndGet(-perChannel.queueSize);
               i$ = perChannel.messagesQueue.iterator();

               while(i$.hasNext()) {
                  toSend = (GlobalTrafficShapingHandler.ToSend)i$.next();
                  if (toSend.toSend instanceof ByteBuf) {
                     ((ByteBuf)toSend.toSend).release();
                  }
               }
            }

            perChannel.messagesQueue.clear();
         }
      }

      this.releaseWriteSuspended(ctx);
      this.releaseReadSuspended(ctx);
      super.handlerRemoved(ctx);
   }

   long checkWaitReadTime(ChannelHandlerContext ctx, long wait, long now) {
      Integer key = ctx.channel().hashCode();
      GlobalTrafficShapingHandler.PerChannel perChannel = (GlobalTrafficShapingHandler.PerChannel)this.channelQueues.get(key);
      if (perChannel != null && wait > this.maxTime && now + wait - perChannel.lastReadTimestamp > this.maxTime) {
         wait = this.maxTime;
      }

      return wait;
   }

   void informReadOperation(ChannelHandlerContext ctx, long now) {
      Integer key = ctx.channel().hashCode();
      GlobalTrafficShapingHandler.PerChannel perChannel = (GlobalTrafficShapingHandler.PerChannel)this.channelQueues.get(key);
      if (perChannel != null) {
         perChannel.lastReadTimestamp = now;
      }

   }

   void submitWrite(final ChannelHandlerContext ctx, Object msg, long size, long writedelay, long now, ChannelPromise promise) {
      Channel channel = ctx.channel();
      Integer key = channel.hashCode();
      final GlobalTrafficShapingHandler.PerChannel perChannel = (GlobalTrafficShapingHandler.PerChannel)this.channelQueues.get(key);
      if (perChannel == null) {
         perChannel = this.getOrSetPerChannel(ctx);
      }

      long delay = writedelay;
      boolean globalSizeExceeded = false;
      GlobalTrafficShapingHandler.ToSend newToSend;
      synchronized(perChannel) {
         if (writedelay == 0L && perChannel.messagesQueue.isEmpty()) {
            this.trafficCounter.bytesRealWriteFlowControl(size);
            ctx.write(msg, promise);
            perChannel.lastWriteTimestamp = now;
            return;
         }

         if (delay > this.maxTime && now + delay - perChannel.lastWriteTimestamp > this.maxTime) {
            delay = this.maxTime;
         }

         newToSend = new GlobalTrafficShapingHandler.ToSend(delay + now, msg, size, promise);
         perChannel.messagesQueue.addLast(newToSend);
         perChannel.queueSize += size;
         this.queuesSize.addAndGet(size);
         this.checkWriteSuspend(ctx, delay, perChannel.queueSize);
         if (this.queuesSize.get() > this.maxGlobalWriteSize) {
            globalSizeExceeded = true;
         }
      }

      if (globalSizeExceeded) {
         this.setUserDefinedWritability(ctx, false);
      }

      final long futureNow = newToSend.relativeTimeAction;
      ctx.executor().schedule(new Runnable() {
         public void run() {
            GlobalTrafficShapingHandler.this.sendAllValid(ctx, perChannel, futureNow);
         }
      }, delay, TimeUnit.MILLISECONDS);
   }

   private void sendAllValid(ChannelHandlerContext ctx, GlobalTrafficShapingHandler.PerChannel perChannel, long now) {
      synchronized(perChannel) {
         GlobalTrafficShapingHandler.ToSend newToSend = (GlobalTrafficShapingHandler.ToSend)perChannel.messagesQueue.pollFirst();

         while(true) {
            if (newToSend != null) {
               if (newToSend.relativeTimeAction <= now) {
                  long size = newToSend.size;
                  this.trafficCounter.bytesRealWriteFlowControl(size);
                  perChannel.queueSize -= size;
                  this.queuesSize.addAndGet(-size);
                  ctx.write(newToSend.toSend, newToSend.promise);
                  perChannel.lastWriteTimestamp = now;
                  newToSend = (GlobalTrafficShapingHandler.ToSend)perChannel.messagesQueue.pollFirst();
                  continue;
               }

               perChannel.messagesQueue.addFirst(newToSend);
            }

            if (perChannel.messagesQueue.isEmpty()) {
               this.releaseWriteSuspended(ctx);
            }
            break;
         }
      }

      ctx.flush();
   }

   private static final class ToSend {
      final long relativeTimeAction;
      final Object toSend;
      final long size;
      final ChannelPromise promise;

      private ToSend(long delay, Object toSend, long size, ChannelPromise promise) {
         this.relativeTimeAction = delay;
         this.toSend = toSend;
         this.size = size;
         this.promise = promise;
      }

      // $FF: synthetic method
      ToSend(long x0, Object x1, long x2, ChannelPromise x3, Object x4) {
         this(x0, x1, x2, x3);
      }
   }

   private static final class PerChannel {
      ArrayDeque<GlobalTrafficShapingHandler.ToSend> messagesQueue;
      long queueSize;
      long lastWriteTimestamp;
      long lastReadTimestamp;

      private PerChannel() {
      }

      // $FF: synthetic method
      PerChannel(Object x0) {
         this();
      }
   }
}
