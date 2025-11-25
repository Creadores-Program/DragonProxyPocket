package io.netty.handler.codec.memcache.binary;

import io.netty.channel.ChannelHandlerAppender;

public class BinaryMemcacheServerCodec extends ChannelHandlerAppender {
   public BinaryMemcacheServerCodec() {
      this(8192);
   }

   public BinaryMemcacheServerCodec(int decodeChunkSize) {
      this.add(new BinaryMemcacheRequestDecoder(decodeChunkSize));
      this.add(new BinaryMemcacheResponseEncoder());
   }
}
