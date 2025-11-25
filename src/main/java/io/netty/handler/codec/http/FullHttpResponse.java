package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;

public interface FullHttpResponse extends HttpResponse, FullHttpMessage {
   FullHttpResponse copy(ByteBuf var1);

   FullHttpResponse copy();

   FullHttpResponse retain(int var1);

   FullHttpResponse retain();

   FullHttpResponse touch();

   FullHttpResponse touch(Object var1);

   FullHttpResponse duplicate();

   FullHttpResponse setProtocolVersion(HttpVersion var1);

   FullHttpResponse setStatus(HttpResponseStatus var1);
}
