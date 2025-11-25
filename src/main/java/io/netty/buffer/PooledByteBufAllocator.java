package io.netty.buffer;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class PooledByteBufAllocator extends AbstractByteBufAllocator {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(PooledByteBufAllocator.class);
   private static final int DEFAULT_NUM_HEAP_ARENA;
   private static final int DEFAULT_NUM_DIRECT_ARENA;
   private static final int DEFAULT_PAGE_SIZE;
   private static final int DEFAULT_MAX_ORDER;
   private static final int DEFAULT_TINY_CACHE_SIZE;
   private static final int DEFAULT_SMALL_CACHE_SIZE;
   private static final int DEFAULT_NORMAL_CACHE_SIZE;
   private static final int DEFAULT_MAX_CACHED_BUFFER_CAPACITY;
   private static final int DEFAULT_CACHE_TRIM_INTERVAL;
   private static final int MIN_PAGE_SIZE = 4096;
   private static final int MAX_CHUNK_SIZE = 1073741824;
   public static final PooledByteBufAllocator DEFAULT;
   private final PoolArena<byte[]>[] heapArenas;
   private final PoolArena<ByteBuffer>[] directArenas;
   private final int tinyCacheSize;
   private final int smallCacheSize;
   private final int normalCacheSize;
   final PooledByteBufAllocator.PoolThreadLocalCache threadCache;

   public PooledByteBufAllocator() {
      this(false);
   }

   public PooledByteBufAllocator(boolean preferDirect) {
      this(preferDirect, DEFAULT_NUM_HEAP_ARENA, DEFAULT_NUM_DIRECT_ARENA, DEFAULT_PAGE_SIZE, DEFAULT_MAX_ORDER);
   }

   public PooledByteBufAllocator(int nHeapArena, int nDirectArena, int pageSize, int maxOrder) {
      this(false, nHeapArena, nDirectArena, pageSize, maxOrder);
   }

   public PooledByteBufAllocator(boolean preferDirect, int nHeapArena, int nDirectArena, int pageSize, int maxOrder) {
      this(preferDirect, nHeapArena, nDirectArena, pageSize, maxOrder, DEFAULT_TINY_CACHE_SIZE, DEFAULT_SMALL_CACHE_SIZE, DEFAULT_NORMAL_CACHE_SIZE);
   }

   public PooledByteBufAllocator(boolean preferDirect, int nHeapArena, int nDirectArena, int pageSize, int maxOrder, int tinyCacheSize, int smallCacheSize, int normalCacheSize) {
      super(preferDirect);
      this.threadCache = new PooledByteBufAllocator.PoolThreadLocalCache();
      this.tinyCacheSize = tinyCacheSize;
      this.smallCacheSize = smallCacheSize;
      this.normalCacheSize = normalCacheSize;
      int chunkSize = validateAndCalculateChunkSize(pageSize, maxOrder);
      if (nHeapArena < 0) {
         throw new IllegalArgumentException("nHeapArena: " + nHeapArena + " (expected: >= 0)");
      } else if (nDirectArena < 0) {
         throw new IllegalArgumentException("nDirectArea: " + nDirectArena + " (expected: >= 0)");
      } else {
         int pageShifts = validateAndCalculatePageShifts(pageSize);
         int i;
         if (nHeapArena > 0) {
            this.heapArenas = newArenaArray(nHeapArena);

            for(i = 0; i < this.heapArenas.length; ++i) {
               this.heapArenas[i] = new PoolArena.HeapArena(this, pageSize, maxOrder, pageShifts, chunkSize);
            }
         } else {
            this.heapArenas = null;
         }

         if (nDirectArena > 0) {
            this.directArenas = newArenaArray(nDirectArena);

            for(i = 0; i < this.directArenas.length; ++i) {
               this.directArenas[i] = new PoolArena.DirectArena(this, pageSize, maxOrder, pageShifts, chunkSize);
            }
         } else {
            this.directArenas = null;
         }

      }
   }

   private static <T> PoolArena<T>[] newArenaArray(int size) {
      return new PoolArena[size];
   }

   private static int validateAndCalculatePageShifts(int pageSize) {
      if (pageSize < 4096) {
         throw new IllegalArgumentException("pageSize: " + pageSize + " (expected: " + 4096 + "+)");
      } else if ((pageSize & pageSize - 1) != 0) {
         throw new IllegalArgumentException("pageSize: " + pageSize + " (expected: power of 2)");
      } else {
         return 31 - Integer.numberOfLeadingZeros(pageSize);
      }
   }

   private static int validateAndCalculateChunkSize(int pageSize, int maxOrder) {
      if (maxOrder > 14) {
         throw new IllegalArgumentException("maxOrder: " + maxOrder + " (expected: 0-14)");
      } else {
         int chunkSize = pageSize;

         for(int i = maxOrder; i > 0; --i) {
            if (chunkSize > 536870912) {
               throw new IllegalArgumentException(String.format("pageSize (%d) << maxOrder (%d) must not exceed %d", pageSize, maxOrder, 1073741824));
            }

            chunkSize <<= 1;
         }

         return chunkSize;
      }
   }

   protected ByteBuf newHeapBuffer(int initialCapacity, int maxCapacity) {
      PoolThreadCache cache = (PoolThreadCache)this.threadCache.get();
      PoolArena<byte[]> heapArena = cache.heapArena;
      Object buf;
      if (heapArena != null) {
         buf = heapArena.allocate(cache, initialCapacity, maxCapacity);
      } else {
         buf = new UnpooledHeapByteBuf(this, initialCapacity, maxCapacity);
      }

      return toLeakAwareBuffer((ByteBuf)buf);
   }

   protected ByteBuf newDirectBuffer(int initialCapacity, int maxCapacity) {
      PoolThreadCache cache = (PoolThreadCache)this.threadCache.get();
      PoolArena<ByteBuffer> directArena = cache.directArena;
      Object buf;
      if (directArena != null) {
         buf = directArena.allocate(cache, initialCapacity, maxCapacity);
      } else if (PlatformDependent.hasUnsafe()) {
         buf = new UnpooledUnsafeDirectByteBuf(this, initialCapacity, maxCapacity);
      } else {
         buf = new UnpooledDirectByteBuf(this, initialCapacity, maxCapacity);
      }

      return toLeakAwareBuffer((ByteBuf)buf);
   }

   public boolean isDirectBufferPooled() {
      return this.directArenas != null;
   }

   static {
      int defaultPageSize = SystemPropertyUtil.getInt("io.netty.allocator.pageSize", 8192);
      Throwable pageSizeFallbackCause = null;

      try {
         validateAndCalculatePageShifts(defaultPageSize);
      } catch (Throwable var7) {
         pageSizeFallbackCause = var7;
         defaultPageSize = 8192;
      }

      DEFAULT_PAGE_SIZE = defaultPageSize;
      int defaultMaxOrder = SystemPropertyUtil.getInt("io.netty.allocator.maxOrder", 11);
      Throwable maxOrderFallbackCause = null;

      try {
         validateAndCalculateChunkSize(DEFAULT_PAGE_SIZE, defaultMaxOrder);
      } catch (Throwable var6) {
         maxOrderFallbackCause = var6;
         defaultMaxOrder = 11;
      }

      DEFAULT_MAX_ORDER = defaultMaxOrder;
      Runtime runtime = Runtime.getRuntime();
      int defaultChunkSize = DEFAULT_PAGE_SIZE << DEFAULT_MAX_ORDER;
      DEFAULT_NUM_HEAP_ARENA = Math.max(0, SystemPropertyUtil.getInt("io.netty.allocator.numHeapArenas", (int)Math.min((long)runtime.availableProcessors(), Runtime.getRuntime().maxMemory() / (long)defaultChunkSize / 2L / 3L)));
      DEFAULT_NUM_DIRECT_ARENA = Math.max(0, SystemPropertyUtil.getInt("io.netty.allocator.numDirectArenas", (int)Math.min((long)runtime.availableProcessors(), PlatformDependent.maxDirectMemory() / (long)defaultChunkSize / 2L / 3L)));
      DEFAULT_TINY_CACHE_SIZE = SystemPropertyUtil.getInt("io.netty.allocator.tinyCacheSize", 512);
      DEFAULT_SMALL_CACHE_SIZE = SystemPropertyUtil.getInt("io.netty.allocator.smallCacheSize", 256);
      DEFAULT_NORMAL_CACHE_SIZE = SystemPropertyUtil.getInt("io.netty.allocator.normalCacheSize", 64);
      DEFAULT_MAX_CACHED_BUFFER_CAPACITY = SystemPropertyUtil.getInt("io.netty.allocator.maxCachedBufferCapacity", 32768);
      DEFAULT_CACHE_TRIM_INTERVAL = SystemPropertyUtil.getInt("io.netty.allocator.cacheTrimInterval", 8192);
      if (logger.isDebugEnabled()) {
         logger.debug("-Dio.netty.allocator.numHeapArenas: {}", (Object)DEFAULT_NUM_HEAP_ARENA);
         logger.debug("-Dio.netty.allocator.numDirectArenas: {}", (Object)DEFAULT_NUM_DIRECT_ARENA);
         if (pageSizeFallbackCause == null) {
            logger.debug("-Dio.netty.allocator.pageSize: {}", (Object)DEFAULT_PAGE_SIZE);
         } else {
            logger.debug("-Dio.netty.allocator.pageSize: {}", DEFAULT_PAGE_SIZE, pageSizeFallbackCause);
         }

         if (maxOrderFallbackCause == null) {
            logger.debug("-Dio.netty.allocator.maxOrder: {}", (Object)DEFAULT_MAX_ORDER);
         } else {
            logger.debug("-Dio.netty.allocator.maxOrder: {}", DEFAULT_MAX_ORDER, maxOrderFallbackCause);
         }

         logger.debug("-Dio.netty.allocator.chunkSize: {}", (Object)(DEFAULT_PAGE_SIZE << DEFAULT_MAX_ORDER));
         logger.debug("-Dio.netty.allocator.tinyCacheSize: {}", (Object)DEFAULT_TINY_CACHE_SIZE);
         logger.debug("-Dio.netty.allocator.smallCacheSize: {}", (Object)DEFAULT_SMALL_CACHE_SIZE);
         logger.debug("-Dio.netty.allocator.normalCacheSize: {}", (Object)DEFAULT_NORMAL_CACHE_SIZE);
         logger.debug("-Dio.netty.allocator.maxCachedBufferCapacity: {}", (Object)DEFAULT_MAX_CACHED_BUFFER_CAPACITY);
         logger.debug("-Dio.netty.allocator.cacheTrimInterval: {}", (Object)DEFAULT_CACHE_TRIM_INTERVAL);
      }

      DEFAULT = new PooledByteBufAllocator(PlatformDependent.directBufferPreferred());
   }

   final class PoolThreadLocalCache extends FastThreadLocal<PoolThreadCache> {
      private final AtomicInteger index = new AtomicInteger();

      protected PoolThreadCache initialValue() {
         int idx = this.index.getAndIncrement();
         PoolArena heapArena;
         if (PooledByteBufAllocator.this.heapArenas != null) {
            heapArena = PooledByteBufAllocator.this.heapArenas[Math.abs(idx % PooledByteBufAllocator.this.heapArenas.length)];
         } else {
            heapArena = null;
         }

         PoolArena directArena;
         if (PooledByteBufAllocator.this.directArenas != null) {
            directArena = PooledByteBufAllocator.this.directArenas[Math.abs(idx % PooledByteBufAllocator.this.directArenas.length)];
         } else {
            directArena = null;
         }

         return new PoolThreadCache(heapArena, directArena, PooledByteBufAllocator.this.tinyCacheSize, PooledByteBufAllocator.this.smallCacheSize, PooledByteBufAllocator.this.normalCacheSize, PooledByteBufAllocator.DEFAULT_MAX_CACHED_BUFFER_CAPACITY, PooledByteBufAllocator.DEFAULT_CACHE_TRIM_INTERVAL);
      }

      protected void onRemoval(PoolThreadCache value) {
         value.free();
      }
   }
}
