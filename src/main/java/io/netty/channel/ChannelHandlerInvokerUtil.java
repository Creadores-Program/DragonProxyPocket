package io.netty.channel;

import io.netty.util.internal.StringUtil;
import java.net.SocketAddress;

public final class ChannelHandlerInvokerUtil {
   public static void invokeChannelRegisteredNow(ChannelHandlerContext ctx) {
      try {
         ctx.handler().channelRegistered(ctx);
      } catch (Throwable var2) {
         notifyHandlerException(ctx, var2);
      }

   }

   public static void invokeChannelUnregisteredNow(ChannelHandlerContext ctx) {
      try {
         ctx.handler().channelUnregistered(ctx);
      } catch (Throwable var2) {
         notifyHandlerException(ctx, var2);
      }

   }

   public static void invokeChannelActiveNow(ChannelHandlerContext ctx) {
      try {
         ctx.handler().channelActive(ctx);
      } catch (Throwable var2) {
         notifyHandlerException(ctx, var2);
      }

   }

   public static void invokeChannelInactiveNow(ChannelHandlerContext ctx) {
      try {
         ctx.handler().channelInactive(ctx);
      } catch (Throwable var2) {
         notifyHandlerException(ctx, var2);
      }

   }

   public static void invokeExceptionCaughtNow(ChannelHandlerContext ctx, Throwable cause) {
      try {
         ctx.handler().exceptionCaught(ctx, cause);
      } catch (Throwable var3) {
         if (DefaultChannelPipeline.logger.isWarnEnabled()) {
            DefaultChannelPipeline.logger.warn("An exception was thrown by a user handler's exceptionCaught() method:", var3);
            DefaultChannelPipeline.logger.warn(".. and the cause of the exceptionCaught() was:", cause);
         }
      }

   }

   public static void invokeUserEventTriggeredNow(ChannelHandlerContext ctx, Object event) {
      try {
         ctx.handler().userEventTriggered(ctx, event);
      } catch (Throwable var3) {
         notifyHandlerException(ctx, var3);
      }

   }

   public static void invokeChannelReadNow(ChannelHandlerContext ctx, Object msg) {
      try {
         ((AbstractChannelHandlerContext)ctx).invokedThisChannelRead = true;
         ctx.handler().channelRead(ctx, msg);
      } catch (Throwable var3) {
         notifyHandlerException(ctx, var3);
      }

   }

   public static void invokeChannelReadCompleteNow(ChannelHandlerContext ctx) {
      try {
         ctx.handler().channelReadComplete(ctx);
      } catch (Throwable var2) {
         notifyHandlerException(ctx, var2);
      }

   }

   public static void invokeChannelWritabilityChangedNow(ChannelHandlerContext ctx) {
      try {
         ctx.handler().channelWritabilityChanged(ctx);
      } catch (Throwable var2) {
         notifyHandlerException(ctx, var2);
      }

   }

   public static void invokeBindNow(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) {
      try {
         ctx.handler().bind(ctx, localAddress, promise);
      } catch (Throwable var4) {
         notifyOutboundHandlerException(var4, promise);
      }

   }

   public static void invokeConnectNow(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
      try {
         ctx.handler().connect(ctx, remoteAddress, localAddress, promise);
      } catch (Throwable var5) {
         notifyOutboundHandlerException(var5, promise);
      }

   }

   public static void invokeDisconnectNow(ChannelHandlerContext ctx, ChannelPromise promise) {
      try {
         ctx.handler().disconnect(ctx, promise);
      } catch (Throwable var3) {
         notifyOutboundHandlerException(var3, promise);
      }

   }

   public static void invokeCloseNow(ChannelHandlerContext ctx, ChannelPromise promise) {
      try {
         ctx.handler().close(ctx, promise);
      } catch (Throwable var3) {
         notifyOutboundHandlerException(var3, promise);
      }

   }

   public static void invokeDeregisterNow(ChannelHandlerContext ctx, ChannelPromise promise) {
      try {
         ctx.handler().deregister(ctx, promise);
      } catch (Throwable var3) {
         notifyOutboundHandlerException(var3, promise);
      }

   }

   public static void invokeReadNow(ChannelHandlerContext ctx) {
      try {
         ctx.handler().read(ctx);
      } catch (Throwable var2) {
         notifyHandlerException(ctx, var2);
      }

   }

   public static void invokeWriteNow(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
      try {
         ctx.handler().write(ctx, msg, promise);
      } catch (Throwable var4) {
         notifyOutboundHandlerException(var4, promise);
      }

   }

   public static void invokeFlushNow(ChannelHandlerContext ctx) {
      try {
         ctx.handler().flush(ctx);
      } catch (Throwable var2) {
         notifyHandlerException(ctx, var2);
      }

   }

   public static boolean validatePromise(ChannelHandlerContext ctx, ChannelPromise promise, boolean allowVoidPromise) {
      if (ctx == null) {
         throw new NullPointerException("ctx");
      } else if (promise == null) {
         throw new NullPointerException("promise");
      } else if (promise.isDone()) {
         if (promise.isCancelled()) {
            return false;
         } else {
            throw new IllegalArgumentException("promise already done: " + promise);
         }
      } else if (promise.channel() != ctx.channel()) {
         throw new IllegalArgumentException(String.format("promise.channel does not match: %s (expected: %s)", promise.channel(), ctx.channel()));
      } else if (promise.getClass() == DefaultChannelPromise.class) {
         return true;
      } else if (!allowVoidPromise && promise instanceof VoidChannelPromise) {
         throw new IllegalArgumentException(StringUtil.simpleClassName(VoidChannelPromise.class) + " not allowed for this operation");
      } else if (promise instanceof AbstractChannel.CloseFuture) {
         throw new IllegalArgumentException(StringUtil.simpleClassName(AbstractChannel.CloseFuture.class) + " not allowed in a pipeline");
      } else {
         return true;
      }
   }

   private static void notifyHandlerException(ChannelHandlerContext ctx, Throwable cause) {
      if (inExceptionCaught(cause)) {
         if (DefaultChannelPipeline.logger.isWarnEnabled()) {
            DefaultChannelPipeline.logger.warn("An exception was thrown by a user handler while handling an exceptionCaught event", cause);
         }

      } else {
         invokeExceptionCaughtNow(ctx, cause);
      }
   }

   private static void notifyOutboundHandlerException(Throwable cause, ChannelPromise promise) {
      if (!promise.tryFailure(cause) && !(promise instanceof VoidChannelPromise) && DefaultChannelPipeline.logger.isWarnEnabled()) {
         DefaultChannelPipeline.logger.warn("Failed to fail the promise because it's done already: {}", promise, cause);
      }

   }

   private static boolean inExceptionCaught(Throwable cause) {
      do {
         StackTraceElement[] trace = cause.getStackTrace();
         if (trace != null) {
            StackTraceElement[] arr$ = trace;
            int len$ = trace.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               StackTraceElement t = arr$[i$];
               if (t == null) {
                  break;
               }

               if ("exceptionCaught".equals(t.getMethodName())) {
                  return true;
               }
            }
         }

         cause = cause.getCause();
      } while(cause != null);

      return false;
   }

   private ChannelHandlerInvokerUtil() {
   }
}
