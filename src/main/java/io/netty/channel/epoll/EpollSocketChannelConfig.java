package io.netty.channel.epoll;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.socket.SocketChannelConfig;
import io.netty.util.internal.PlatformDependent;
import java.util.Map;

public final class EpollSocketChannelConfig extends EpollChannelConfig implements SocketChannelConfig {
   private final EpollSocketChannel channel;
   private volatile boolean allowHalfClosure;

   EpollSocketChannelConfig(EpollSocketChannel channel) {
      super(channel);
      this.channel = channel;
      if (PlatformDependent.canEnableTcpNoDelayByDefault()) {
         this.setTcpNoDelay(true);
      }

   }

   public Map<ChannelOption<?>, Object> getOptions() {
      return this.getOptions(super.getOptions(), new ChannelOption[]{ChannelOption.SO_RCVBUF, ChannelOption.SO_SNDBUF, ChannelOption.TCP_NODELAY, ChannelOption.SO_KEEPALIVE, ChannelOption.SO_REUSEADDR, ChannelOption.SO_LINGER, ChannelOption.IP_TOS, ChannelOption.ALLOW_HALF_CLOSURE, EpollChannelOption.TCP_CORK, EpollChannelOption.TCP_KEEPCNT, EpollChannelOption.TCP_KEEPIDLE, EpollChannelOption.TCP_KEEPINTVL});
   }

   public <T> T getOption(ChannelOption<T> option) {
      if (option == ChannelOption.SO_RCVBUF) {
         return this.getReceiveBufferSize();
      } else if (option == ChannelOption.SO_SNDBUF) {
         return this.getSendBufferSize();
      } else if (option == ChannelOption.TCP_NODELAY) {
         return this.isTcpNoDelay();
      } else if (option == ChannelOption.SO_KEEPALIVE) {
         return this.isKeepAlive();
      } else if (option == ChannelOption.SO_REUSEADDR) {
         return this.isReuseAddress();
      } else if (option == ChannelOption.SO_LINGER) {
         return this.getSoLinger();
      } else if (option == ChannelOption.IP_TOS) {
         return this.getTrafficClass();
      } else if (option == ChannelOption.ALLOW_HALF_CLOSURE) {
         return this.isAllowHalfClosure();
      } else if (option == EpollChannelOption.TCP_CORK) {
         return this.isTcpCork();
      } else if (option == EpollChannelOption.TCP_KEEPIDLE) {
         return this.getTcpKeepIdle();
      } else if (option == EpollChannelOption.TCP_KEEPINTVL) {
         return this.getTcpKeepIntvl();
      } else {
         return option == EpollChannelOption.TCP_KEEPCNT ? this.getTcpKeepCnt() : super.getOption(option);
      }
   }

   public <T> boolean setOption(ChannelOption<T> option, T value) {
      this.validate(option, value);
      if (option == ChannelOption.SO_RCVBUF) {
         this.setReceiveBufferSize((Integer)value);
      } else if (option == ChannelOption.SO_SNDBUF) {
         this.setSendBufferSize((Integer)value);
      } else if (option == ChannelOption.TCP_NODELAY) {
         this.setTcpNoDelay((Boolean)value);
      } else if (option == ChannelOption.SO_KEEPALIVE) {
         this.setKeepAlive((Boolean)value);
      } else if (option == ChannelOption.SO_REUSEADDR) {
         this.setReuseAddress((Boolean)value);
      } else if (option == ChannelOption.SO_LINGER) {
         this.setSoLinger((Integer)value);
      } else if (option == ChannelOption.IP_TOS) {
         this.setTrafficClass((Integer)value);
      } else if (option == ChannelOption.ALLOW_HALF_CLOSURE) {
         this.setAllowHalfClosure((Boolean)value);
      } else if (option == EpollChannelOption.TCP_CORK) {
         this.setTcpCork((Boolean)value);
      } else if (option == EpollChannelOption.TCP_KEEPIDLE) {
         this.setTcpKeepIdle((Integer)value);
      } else if (option == EpollChannelOption.TCP_KEEPCNT) {
         this.setTcpKeepCntl((Integer)value);
      } else {
         if (option != EpollChannelOption.TCP_KEEPINTVL) {
            return super.setOption(option, value);
         }

         this.setTcpKeepIntvl((Integer)value);
      }

      return true;
   }

   public int getReceiveBufferSize() {
      return Native.getReceiveBufferSize(this.channel.fd().intValue());
   }

   public int getSendBufferSize() {
      return Native.getSendBufferSize(this.channel.fd().intValue());
   }

   public int getSoLinger() {
      return Native.getSoLinger(this.channel.fd().intValue());
   }

   public int getTrafficClass() {
      return Native.getTrafficClass(this.channel.fd().intValue());
   }

   public boolean isKeepAlive() {
      return Native.isKeepAlive(this.channel.fd().intValue()) == 1;
   }

   public boolean isReuseAddress() {
      return Native.isReuseAddress(this.channel.fd().intValue()) == 1;
   }

   public boolean isTcpNoDelay() {
      return Native.isTcpNoDelay(this.channel.fd().intValue()) == 1;
   }

   public boolean isTcpCork() {
      return Native.isTcpCork(this.channel.fd().intValue()) == 1;
   }

   public int getTcpKeepIdle() {
      return Native.getTcpKeepIdle(this.channel.fd().intValue());
   }

   public int getTcpKeepIntvl() {
      return Native.getTcpKeepIntvl(this.channel.fd().intValue());
   }

   public int getTcpKeepCnt() {
      return Native.getTcpKeepCnt(this.channel.fd().intValue());
   }

   public EpollSocketChannelConfig setKeepAlive(boolean keepAlive) {
      Native.setKeepAlive(this.channel.fd().intValue(), keepAlive ? 1 : 0);
      return this;
   }

   public EpollSocketChannelConfig setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
      return this;
   }

   public EpollSocketChannelConfig setReceiveBufferSize(int receiveBufferSize) {
      Native.setReceiveBufferSize(this.channel.fd().intValue(), receiveBufferSize);
      return this;
   }

   public EpollSocketChannelConfig setReuseAddress(boolean reuseAddress) {
      Native.setReuseAddress(this.channel.fd().intValue(), reuseAddress ? 1 : 0);
      return this;
   }

   public EpollSocketChannelConfig setSendBufferSize(int sendBufferSize) {
      Native.setSendBufferSize(this.channel.fd().intValue(), sendBufferSize);
      return this;
   }

   public EpollSocketChannelConfig setSoLinger(int soLinger) {
      Native.setSoLinger(this.channel.fd().intValue(), soLinger);
      return this;
   }

   public EpollSocketChannelConfig setTcpNoDelay(boolean tcpNoDelay) {
      Native.setTcpNoDelay(this.channel.fd().intValue(), tcpNoDelay ? 1 : 0);
      return this;
   }

   public EpollSocketChannelConfig setTcpCork(boolean tcpCork) {
      Native.setTcpCork(this.channel.fd().intValue(), tcpCork ? 1 : 0);
      return this;
   }

   public EpollSocketChannelConfig setTrafficClass(int trafficClass) {
      Native.setTrafficClass(this.channel.fd().intValue(), trafficClass);
      return this;
   }

   public EpollSocketChannelConfig setTcpKeepIdle(int seconds) {
      Native.setTcpKeepIdle(this.channel.fd().intValue(), seconds);
      return this;
   }

   public EpollSocketChannelConfig setTcpKeepIntvl(int seconds) {
      Native.setTcpKeepIntvl(this.channel.fd().intValue(), seconds);
      return this;
   }

   public EpollSocketChannelConfig setTcpKeepCntl(int probes) {
      Native.setTcpKeepCnt(this.channel.fd().intValue(), probes);
      return this;
   }

   public boolean isAllowHalfClosure() {
      return this.allowHalfClosure;
   }

   public EpollSocketChannelConfig setAllowHalfClosure(boolean allowHalfClosure) {
      this.allowHalfClosure = allowHalfClosure;
      return this;
   }

   public EpollSocketChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
      super.setConnectTimeoutMillis(connectTimeoutMillis);
      return this;
   }

   public EpollSocketChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead) {
      super.setMaxMessagesPerRead(maxMessagesPerRead);
      return this;
   }

   public EpollSocketChannelConfig setWriteSpinCount(int writeSpinCount) {
      super.setWriteSpinCount(writeSpinCount);
      return this;
   }

   public EpollSocketChannelConfig setAllocator(ByteBufAllocator allocator) {
      super.setAllocator(allocator);
      return this;
   }

   public EpollSocketChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator) {
      super.setRecvByteBufAllocator(allocator);
      return this;
   }

   public EpollSocketChannelConfig setAutoRead(boolean autoRead) {
      super.setAutoRead(autoRead);
      return this;
   }

   public EpollSocketChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
      super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
      return this;
   }

   public EpollSocketChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
      super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
      return this;
   }

   public EpollSocketChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator) {
      super.setMessageSizeEstimator(estimator);
      return this;
   }

   public EpollSocketChannelConfig setEpollMode(EpollMode mode) {
      super.setEpollMode(mode);
      return this;
   }
}
