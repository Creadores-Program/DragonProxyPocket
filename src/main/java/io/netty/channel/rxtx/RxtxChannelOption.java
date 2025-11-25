package io.netty.channel.rxtx;

import io.netty.channel.ChannelOption;

public final class RxtxChannelOption {
   private static final Class<RxtxChannelOption> T = RxtxChannelOption.class;
   public static final ChannelOption<Integer> BAUD_RATE;
   public static final ChannelOption<Boolean> DTR;
   public static final ChannelOption<Boolean> RTS;
   public static final ChannelOption<RxtxChannelConfig.Stopbits> STOP_BITS;
   public static final ChannelOption<RxtxChannelConfig.Databits> DATA_BITS;
   public static final ChannelOption<RxtxChannelConfig.Paritybit> PARITY_BIT;
   public static final ChannelOption<Integer> WAIT_TIME;
   public static final ChannelOption<Integer> READ_TIMEOUT;

   private RxtxChannelOption() {
   }

   static {
      BAUD_RATE = ChannelOption.valueOf(T, "BAUD_RATE");
      DTR = ChannelOption.valueOf(T, "DTR");
      RTS = ChannelOption.valueOf(T, "RTS");
      STOP_BITS = ChannelOption.valueOf(T, "STOP_BITS");
      DATA_BITS = ChannelOption.valueOf(T, "DATA_BITS");
      PARITY_BIT = ChannelOption.valueOf(T, "PARITY_BIT");
      WAIT_TIME = ChannelOption.valueOf(T, "WAIT_TIME");
      READ_TIMEOUT = ChannelOption.valueOf(T, "READ_TIMEOUT");
   }
}
