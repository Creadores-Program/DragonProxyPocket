package io.netty.handler.codec.memcache.binary;

import io.netty.handler.codec.memcache.FullMemcacheMessage;

public interface FullBinaryMemcacheResponse extends BinaryMemcacheResponse, FullMemcacheMessage {
   FullBinaryMemcacheResponse copy();

   FullBinaryMemcacheResponse retain(int var1);

   FullBinaryMemcacheResponse retain();

   FullBinaryMemcacheResponse touch();

   FullBinaryMemcacheResponse touch(Object var1);

   FullBinaryMemcacheResponse duplicate();
}
