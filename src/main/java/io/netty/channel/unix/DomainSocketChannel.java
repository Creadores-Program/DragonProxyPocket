package io.netty.channel.unix;

public interface DomainSocketChannel extends UnixChannel {
   DomainSocketAddress remoteAddress();

   DomainSocketAddress localAddress();

   DomainSocketChannelConfig config();
}
