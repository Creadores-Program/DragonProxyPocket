package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.nio.AbstractNioByteChannel;
import io.netty.util.internal.PlatformDependent;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class DefaultChannelConfig implements ChannelConfig {
   private static final RecvByteBufAllocator DEFAULT_RCVBUF_ALLOCATOR;
   private static final MessageSizeEstimator DEFAULT_MSG_SIZE_ESTIMATOR;
   private static final int DEFAULT_CONNECT_TIMEOUT = 30000;
   private static final AtomicIntegerFieldUpdater<DefaultChannelConfig> AUTOREAD_UPDATER;
   protected final Channel channel;
   private volatile ByteBufAllocator allocator;
   private volatile RecvByteBufAllocator rcvBufAllocator;
   private volatile MessageSizeEstimator msgSizeEstimator;
   private volatile int connectTimeoutMillis;
   private volatile int maxMessagesPerRead;
   private volatile int writeSpinCount;
   private volatile int autoRead;
   private volatile int writeBufferHighWaterMark;
   private volatile int writeBufferLowWaterMark;

   public DefaultChannelConfig(Channel channel) {
      this.allocator = ByteBufAllocator.DEFAULT;
      this.rcvBufAllocator = DEFAULT_RCVBUF_ALLOCATOR;
      this.msgSizeEstimator = DEFAULT_MSG_SIZE_ESTIMATOR;
      this.connectTimeoutMillis = 30000;
      this.writeSpinCount = 16;
      this.autoRead = 1;
      this.writeBufferHighWaterMark = 65536;
      this.writeBufferLowWaterMark = 32768;
      if (channel == null) {
         throw new NullPointerException("channel");
      } else {
         this.channel = channel;
         if (!(channel instanceof ServerChannel) && !(channel instanceof AbstractNioByteChannel)) {
            this.maxMessagesPerRead = 1;
         } else {
            this.maxMessagesPerRead = 16;
         }

      }
   }

   public Map<ChannelOption<?>, Object> getOptions() {
      return this.getOptions((Map)null, ChannelOption.CONNECT_TIMEOUT_MILLIS, ChannelOption.MAX_MESSAGES_PER_READ, ChannelOption.WRITE_SPIN_COUNT, ChannelOption.ALLOCATOR, ChannelOption.AUTO_READ, ChannelOption.RCVBUF_ALLOCATOR, ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, ChannelOption.MESSAGE_SIZE_ESTIMATOR);
   }

   protected Map<ChannelOption<?>, Object> getOptions(Map<ChannelOption<?>, Object> result, ChannelOption<?>... options) {
      if (result == null) {
         result = new IdentityHashMap();
      }

      ChannelOption[] arr$ = options;
      int len$ = options.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         ChannelOption<?> o = arr$[i$];
         ((Map)result).put(o, this.getOption(o));
      }

      return (Map)result;
   }

   public boolean setOptions(Map<ChannelOption<?>, ?> options) {
      if (options == null) {
         throw new NullPointerException("options");
      } else {
         boolean setAllOptions = true;
         Iterator i$ = options.entrySet().iterator();

         while(i$.hasNext()) {
            Entry<ChannelOption<?>, ?> e = (Entry)i$.next();
            if (!this.setOption((ChannelOption)e.getKey(), e.getValue())) {
               setAllOptions = false;
            }
         }

         return setAllOptions;
      }
   }

   public <T> T getOption(ChannelOption<T> option) {
      if (option == null) {
         throw new NullPointerException("option");
      } else if (option == ChannelOption.CONNECT_TIMEOUT_MILLIS) {
         return this.getConnectTimeoutMillis();
      } else if (option == ChannelOption.MAX_MESSAGES_PER_READ) {
         return this.getMaxMessagesPerRead();
      } else if (option == ChannelOption.WRITE_SPIN_COUNT) {
         return this.getWriteSpinCount();
      } else if (option == ChannelOption.ALLOCATOR) {
         return this.getAllocator();
      } else if (option == ChannelOption.RCVBUF_ALLOCATOR) {
         return this.getRecvByteBufAllocator();
      } else if (option == ChannelOption.AUTO_READ) {
         return this.isAutoRead();
      } else if (option == ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK) {
         return this.getWriteBufferHighWaterMark();
      } else if (option == ChannelOption.WRITE_BUFFER_LOW_WATER_MARK) {
         return this.getWriteBufferLowWaterMark();
      } else {
         return option == ChannelOption.MESSAGE_SIZE_ESTIMATOR ? this.getMessageSizeEstimator() : null;
      }
   }

   public <T> boolean setOption(ChannelOption<T> option, T value) {
      this.validate(option, value);
      if (option == ChannelOption.CONNECT_TIMEOUT_MILLIS) {
         this.setConnectTimeoutMillis((Integer)value);
      } else if (option == ChannelOption.MAX_MESSAGES_PER_READ) {
         this.setMaxMessagesPerRead((Integer)value);
      } else if (option == ChannelOption.WRITE_SPIN_COUNT) {
         this.setWriteSpinCount((Integer)value);
      } else if (option == ChannelOption.ALLOCATOR) {
         this.setAllocator((ByteBufAllocator)value);
      } else if (option == ChannelOption.RCVBUF_ALLOCATOR) {
         this.setRecvByteBufAllocator((RecvByteBufAllocator)value);
      } else if (option == ChannelOption.AUTO_READ) {
         this.setAutoRead((Boolean)value);
      } else if (option == ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK) {
         this.setWriteBufferHighWaterMark((Integer)value);
      } else if (option == ChannelOption.WRITE_BUFFER_LOW_WATER_MARK) {
         this.setWriteBufferLowWaterMark((Integer)value);
      } else {
         if (option != ChannelOption.MESSAGE_SIZE_ESTIMATOR) {
            return false;
         }

         this.setMessageSizeEstimator((MessageSizeEstimator)value);
      }

      return true;
   }

   protected <T> void validate(ChannelOption<T> option, T value) {
      if (option == null) {
         throw new NullPointerException("option");
      } else {
         option.validate(value);
      }
   }

   public int getConnectTimeoutMillis() {
      return this.connectTimeoutMillis;
   }

   public ChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
      if (connectTimeoutMillis < 0) {
         throw new IllegalArgumentException(String.format("connectTimeoutMillis: %d (expected: >= 0)", connectTimeoutMillis));
      } else {
         this.connectTimeoutMillis = connectTimeoutMillis;
         return this;
      }
   }

   public int getMaxMessagesPerRead() {
      return this.maxMessagesPerRead;
   }

   public ChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead) {
      if (maxMessagesPerRead <= 0) {
         throw new IllegalArgumentException("maxMessagesPerRead: " + maxMessagesPerRead + " (expected: > 0)");
      } else {
         this.maxMessagesPerRead = maxMessagesPerRead;
         return this;
      }
   }

   public int getWriteSpinCount() {
      return this.writeSpinCount;
   }

   public ChannelConfig setWriteSpinCount(int writeSpinCount) {
      if (writeSpinCount <= 0) {
         throw new IllegalArgumentException("writeSpinCount must be a positive integer.");
      } else {
         this.writeSpinCount = writeSpinCount;
         return this;
      }
   }

   public ByteBufAllocator getAllocator() {
      return this.allocator;
   }

   public ChannelConfig setAllocator(ByteBufAllocator allocator) {
      if (allocator == null) {
         throw new NullPointerException("allocator");
      } else {
         this.allocator = allocator;
         return this;
      }
   }

   public RecvByteBufAllocator getRecvByteBufAllocator() {
      return this.rcvBufAllocator;
   }

   public ChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator) {
      if (allocator == null) {
         throw new NullPointerException("allocator");
      } else {
         this.rcvBufAllocator = allocator;
         return this;
      }
   }

   public boolean isAutoRead() {
      return this.autoRead == 1;
   }

   public ChannelConfig setAutoRead(boolean autoRead) {
      boolean oldAutoRead = AUTOREAD_UPDATER.getAndSet(this, autoRead ? 1 : 0) == 1;
      if (autoRead && !oldAutoRead) {
         this.channel.read();
      } else if (!autoRead && oldAutoRead) {
         this.autoReadCleared();
      }

      return this;
   }

   protected void autoReadCleared() {
   }

   public int getWriteBufferHighWaterMark() {
      return this.writeBufferHighWaterMark;
   }

   public ChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
      if (writeBufferHighWaterMark < this.getWriteBufferLowWaterMark()) {
         throw new IllegalArgumentException("writeBufferHighWaterMark cannot be less than writeBufferLowWaterMark (" + this.getWriteBufferLowWaterMark() + "): " + writeBufferHighWaterMark);
      } else if (writeBufferHighWaterMark < 0) {
         throw new IllegalArgumentException("writeBufferHighWaterMark must be >= 0");
      } else {
         this.writeBufferHighWaterMark = writeBufferHighWaterMark;
         return this;
      }
   }

   public int getWriteBufferLowWaterMark() {
      return this.writeBufferLowWaterMark;
   }

   public ChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
      if (writeBufferLowWaterMark > this.getWriteBufferHighWaterMark()) {
         throw new IllegalArgumentException("writeBufferLowWaterMark cannot be greater than writeBufferHighWaterMark (" + this.getWriteBufferHighWaterMark() + "): " + writeBufferLowWaterMark);
      } else if (writeBufferLowWaterMark < 0) {
         throw new IllegalArgumentException("writeBufferLowWaterMark must be >= 0");
      } else {
         this.writeBufferLowWaterMark = writeBufferLowWaterMark;
         return this;
      }
   }

   public MessageSizeEstimator getMessageSizeEstimator() {
      return this.msgSizeEstimator;
   }

   public ChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator) {
      if (estimator == null) {
         throw new NullPointerException("estimator");
      } else {
         this.msgSizeEstimator = estimator;
         return this;
      }
   }

   static {
      DEFAULT_RCVBUF_ALLOCATOR = AdaptiveRecvByteBufAllocator.DEFAULT;
      DEFAULT_MSG_SIZE_ESTIMATOR = DefaultMessageSizeEstimator.DEFAULT;
      AtomicIntegerFieldUpdater<DefaultChannelConfig> autoReadUpdater = PlatformDependent.newAtomicIntegerFieldUpdater(DefaultChannelConfig.class, "autoRead");
      if (autoReadUpdater == null) {
         autoReadUpdater = AtomicIntegerFieldUpdater.newUpdater(DefaultChannelConfig.class, "autoRead");
      }

      AUTOREAD_UPDATER = autoReadUpdater;
   }
}
