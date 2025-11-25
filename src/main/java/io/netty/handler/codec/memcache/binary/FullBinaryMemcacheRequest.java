package io.netty.handler.codec.memcache.binary;

import io.netty.handler.codec.memcache.FullMemcacheMessage;

public interface FullBinaryMemcacheRequest extends BinaryMemcacheRequest, FullMemcacheMessage {
   FullBinaryMemcacheRequest copy();

   FullBinaryMemcacheRequest retain(int var1);

   FullBinaryMemcacheRequest retain();

   FullBinaryMemcacheRequest touch();

   FullBinaryMemcacheRequest touch(Object var1);

   FullBinaryMemcacheRequest duplicate();
}
