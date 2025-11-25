package io.netty.handler.codec.stomp;

public interface StompFrame extends StompHeadersSubframe, LastStompContentSubframe {
   StompFrame copy();

   StompFrame duplicate();

   StompFrame retain();

   StompFrame retain(int var1);

   StompFrame touch();

   StompFrame touch(Object var1);
}
