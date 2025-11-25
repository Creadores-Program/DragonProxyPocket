package io.netty.channel.epoll;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.util.NetUtil;
import java.util.Map;

public class EpollServerChannelConfig extends EpollChannelConfig {
   protected final AbstractEpollChannel channel;
   private volatile int backlog;

   EpollServerChannelConfig(AbstractEpollChannel channel) {
      super(channel);
      this.backlog = NetUtil.SOMAXCONN;
      this.channel = channel;
   }

   public Map<ChannelOption<?>, Object> getOptions() {
      return this.getOptions(super.getOptions(), new ChannelOption[]{ChannelOption.SO_RCVBUF, ChannelOption.SO_REUSEADDR, ChannelOption.SO_BACKLOG});
   }

   public <T> T getOption(ChannelOption<T> option) {
      if (option == ChannelOption.SO_RCVBUF) {
         return this.getReceiveBufferSize();
      } else if (option == ChannelOption.SO_REUSEADDR) {
         return this.isReuseAddress();
      } else {
         return option == ChannelOption.SO_BACKLOG ? this.getBacklog() : super.getOption(option);
      }
   }

   public <T> boolean setOption(ChannelOption<T> option, T value) {
      this.validate(option, value);
      if (option == ChannelOption.SO_RCVBUF) {
         this.setReceiveBufferSize((Integer)value);
      } else if (option == ChannelOption.SO_REUSEADDR) {
         this.setReuseAddress((Boolean)value);
      } else {
         if (option != ChannelOption.SO_BACKLOG) {
            return super.setOption(option, value);
         }

         this.setBacklog((Integer)value);
      }

      return true;
   }

   public boolean isReuseAddress() {
      return Native.isReuseAddress(this.channel.fd().intValue()) == 1;
   }

   public EpollServerChannelConfig setReuseAddress(boolean reuseAddress) {
      Native.setReuseAddress(this.channel.fd().intValue(), reuseAddress ? 1 : 0);
      return this;
   }

   public int getReceiveBufferSize() {
      return Native.getReceiveBufferSize(this.channel.fd().intValue());
   }

   public EpollServerChannelConfig setReceiveBufferSize(int receiveBufferSize) {
      Native.setReceiveBufferSize(this.channel.fd().intValue(), receiveBufferSize);
      return this;
   }

   public int getBacklog() {
      return this.backlog;
   }

   public EpollServerChannelConfig setBacklog(int backlog) {
      if (backlog < 0) {
         throw new IllegalArgumentException("backlog: " + backlog);
      } else {
         this.backlog = backlog;
         return this;
      }
   }

   public EpollServerChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
      super.setConnectTimeoutMillis(connectTimeoutMillis);
      return this;
   }

   public EpollServerChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead) {
      super.setMaxMessagesPerRead(maxMessagesPerRead);
      return this;
   }

   public EpollServerChannelConfig setWriteSpinCount(int writeSpinCount) {
      super.setWriteSpinCount(writeSpinCount);
      return this;
   }

   public EpollServerChannelConfig setAllocator(ByteBufAllocator allocator) {
      super.setAllocator(allocator);
      return this;
   }

   public EpollServerChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator) {
      super.setRecvByteBufAllocator(allocator);
      return this;
   }

   public EpollServerChannelConfig setAutoRead(boolean autoRead) {
      super.setAutoRead(autoRead);
      return this;
   }

   public EpollServerChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
      super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
      return this;
   }

   public EpollServerChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
      super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
      return this;
   }

   public EpollServerChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator) {
      super.setMessageSizeEstimator(estimator);
      return this;
   }

   public EpollServerChannelConfig setEpollMode(EpollMode mode) {
      super.setEpollMode(mode);
      return this;
   }
}
