package io.netty.handler.codec.stomp;

import io.netty.buffer.ByteBufHolder;

public interface StompContentSubframe extends ByteBufHolder, StompSubframe {
   StompContentSubframe copy();

   StompContentSubframe duplicate();

   StompContentSubframe retain();

   StompContentSubframe retain(int var1);

   StompContentSubframe touch();

   StompContentSubframe touch(Object var1);
}
