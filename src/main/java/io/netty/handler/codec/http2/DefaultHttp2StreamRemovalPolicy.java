package io.netty.handler.codec.http2;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DefaultHttp2StreamRemovalPolicy extends ChannelHandlerAdapter implements Http2StreamRemovalPolicy, Runnable {
   private static final long GARBAGE_COLLECTION_INTERVAL;
   private final Queue<DefaultHttp2StreamRemovalPolicy.Garbage> garbage = new ArrayDeque();
   private ScheduledFuture<?> timerFuture;
   private Http2StreamRemovalPolicy.Action action;

   public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      this.timerFuture = ctx.channel().eventLoop().scheduleWithFixedDelay(this, GARBAGE_COLLECTION_INTERVAL, GARBAGE_COLLECTION_INTERVAL, TimeUnit.NANOSECONDS);
   }

   public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
      if (this.timerFuture != null) {
         this.timerFuture.cancel(false);
         this.timerFuture = null;
      }

   }

   public void setAction(Http2StreamRemovalPolicy.Action action) {
      this.action = action;
   }

   public void markForRemoval(Http2Stream stream) {
      this.garbage.add(new DefaultHttp2StreamRemovalPolicy.Garbage(stream));
   }

   public void run() {
      if (!this.garbage.isEmpty() && this.action != null) {
         long time = System.nanoTime();

         while(true) {
            DefaultHttp2StreamRemovalPolicy.Garbage next = (DefaultHttp2StreamRemovalPolicy.Garbage)this.garbage.peek();
            if (next == null || time - next.removalTime <= GARBAGE_COLLECTION_INTERVAL) {
               return;
            }

            this.garbage.remove();
            this.action.removeStream(next.stream);
         }
      }
   }

   static {
      GARBAGE_COLLECTION_INTERVAL = TimeUnit.SECONDS.toNanos(5L);
   }

   private static final class Garbage {
      private final long removalTime = System.nanoTime();
      private final Http2Stream stream;

      Garbage(Http2Stream stream) {
         this.stream = stream;
      }
   }
}
