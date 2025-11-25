package io.netty.channel.epoll;

import io.netty.channel.ChannelOption;
import io.netty.channel.unix.DomainSocketReadMode;

public final class EpollChannelOption {
   private static final Class<EpollChannelOption> T = EpollChannelOption.class;
   public static final ChannelOption<Boolean> TCP_CORK;
   public static final ChannelOption<Boolean> SO_REUSEPORT;
   public static final ChannelOption<Integer> TCP_KEEPIDLE;
   public static final ChannelOption<Integer> TCP_KEEPINTVL;
   public static final ChannelOption<Integer> TCP_KEEPCNT;
   public static final ChannelOption<DomainSocketReadMode> DOMAIN_SOCKET_READ_MODE;
   public static final ChannelOption<EpollMode> EPOLL_MODE;

   private EpollChannelOption() {
   }

   static {
      TCP_CORK = ChannelOption.valueOf(T, "TCP_CORK");
      SO_REUSEPORT = ChannelOption.valueOf(T, "SO_REUSEPORT");
      TCP_KEEPIDLE = ChannelOption.valueOf(T, "TCP_KEEPIDLE");
      TCP_KEEPINTVL = ChannelOption.valueOf(T, "TCP_KEEPINTVL");
      TCP_KEEPCNT = ChannelOption.valueOf(T, "TCP_KEEPCNT");
      DOMAIN_SOCKET_READ_MODE = ChannelOption.valueOf(T, "DOMAIN_SOCKET_READ_MODE");
      EPOLL_MODE = ChannelOption.valueOf(T, "EPOLL_MODE");
   }
}
