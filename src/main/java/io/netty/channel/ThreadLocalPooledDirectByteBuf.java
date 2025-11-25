package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.buffer.UnpooledDirectByteBuf;
import io.netty.buffer.UnpooledUnsafeDirectByteBuf;
import io.netty.util.Recycler;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

final class ThreadLocalPooledDirectByteBuf {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(ThreadLocalPooledDirectByteBuf.class);
   public static final int threadLocalDirectBufferSize = SystemPropertyUtil.getInt("io.netty.threadLocalDirectBufferSize", 65536);

   public static ByteBuf newInstance() {
      return (ByteBuf)(PlatformDependent.hasUnsafe() ? ThreadLocalPooledDirectByteBuf.ThreadLocalUnsafeDirectByteBuf.newInstance() : ThreadLocalPooledDirectByteBuf.ThreadLocalDirectByteBuf.newInstance());
   }

   private ThreadLocalPooledDirectByteBuf() {
   }

   static {
      logger.debug("-Dio.netty.threadLocalDirectBufferSize: {}", (Object)threadLocalDirectBufferSize);
   }

   static final class ThreadLocalDirectByteBuf extends UnpooledDirectByteBuf {
      private static final Recycler<ThreadLocalPooledDirectByteBuf.ThreadLocalDirectByteBuf> RECYCLER = new Recycler<ThreadLocalPooledDirectByteBuf.ThreadLocalDirectByteBuf>() {
         protected ThreadLocalPooledDirectByteBuf.ThreadLocalDirectByteBuf newObject(Recycler.Handle<ThreadLocalPooledDirectByteBuf.ThreadLocalDirectByteBuf> handle) {
            return new ThreadLocalPooledDirectByteBuf.ThreadLocalDirectByteBuf(handle);
         }
      };
      private final Recycler.Handle<ThreadLocalPooledDirectByteBuf.ThreadLocalDirectByteBuf> handle;

      static ThreadLocalPooledDirectByteBuf.ThreadLocalDirectByteBuf newInstance() {
         ThreadLocalPooledDirectByteBuf.ThreadLocalDirectByteBuf buf = (ThreadLocalPooledDirectByteBuf.ThreadLocalDirectByteBuf)RECYCLER.get();
         buf.setRefCnt(1);
         return buf;
      }

      private ThreadLocalDirectByteBuf(Recycler.Handle<ThreadLocalPooledDirectByteBuf.ThreadLocalDirectByteBuf> handle) {
         super(UnpooledByteBufAllocator.DEFAULT, 256, Integer.MAX_VALUE);
         this.handle = handle;
      }

      protected void deallocate() {
         if (this.capacity() > ThreadLocalPooledDirectByteBuf.threadLocalDirectBufferSize) {
            super.deallocate();
         } else {
            this.clear();
            RECYCLER.recycle(this, this.handle);
         }

      }

      // $FF: synthetic method
      ThreadLocalDirectByteBuf(Recycler.Handle x0, Object x1) {
         this(x0);
      }
   }

   static final class ThreadLocalUnsafeDirectByteBuf extends UnpooledUnsafeDirectByteBuf {
      private static final Recycler<ThreadLocalPooledDirectByteBuf.ThreadLocalUnsafeDirectByteBuf> RECYCLER = new Recycler<ThreadLocalPooledDirectByteBuf.ThreadLocalUnsafeDirectByteBuf>() {
         protected ThreadLocalPooledDirectByteBuf.ThreadLocalUnsafeDirectByteBuf newObject(Recycler.Handle<ThreadLocalPooledDirectByteBuf.ThreadLocalUnsafeDirectByteBuf> handle) {
            return new ThreadLocalPooledDirectByteBuf.ThreadLocalUnsafeDirectByteBuf(handle);
         }
      };
      private final Recycler.Handle<ThreadLocalPooledDirectByteBuf.ThreadLocalUnsafeDirectByteBuf> handle;

      static ThreadLocalPooledDirectByteBuf.ThreadLocalUnsafeDirectByteBuf newInstance() {
         ThreadLocalPooledDirectByteBuf.ThreadLocalUnsafeDirectByteBuf buf = (ThreadLocalPooledDirectByteBuf.ThreadLocalUnsafeDirectByteBuf)RECYCLER.get();
         buf.setRefCnt(1);
         return buf;
      }

      private ThreadLocalUnsafeDirectByteBuf(Recycler.Handle<ThreadLocalPooledDirectByteBuf.ThreadLocalUnsafeDirectByteBuf> handle) {
         super(UnpooledByteBufAllocator.DEFAULT, 256, Integer.MAX_VALUE);
         this.handle = handle;
      }

      protected void deallocate() {
         if (this.capacity() > ThreadLocalPooledDirectByteBuf.threadLocalDirectBufferSize) {
            super.deallocate();
         } else {
            this.clear();
            RECYCLER.recycle(this, this.handle);
         }

      }

      // $FF: synthetic method
      ThreadLocalUnsafeDirectByteBuf(Recycler.Handle x0, Object x1) {
         this(x0);
      }
   }
}
