package io.netty.handler.codec.memcache;

public interface FullMemcacheMessage extends MemcacheMessage, LastMemcacheContent {
   FullMemcacheMessage copy();

   FullMemcacheMessage retain(int var1);

   FullMemcacheMessage retain();

   FullMemcacheMessage touch();

   FullMemcacheMessage touch(Object var1);

   FullMemcacheMessage duplicate();
}
