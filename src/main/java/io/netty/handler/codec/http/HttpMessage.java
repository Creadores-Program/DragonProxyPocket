package io.netty.handler.codec.http;

public interface HttpMessage extends HttpObject {
   HttpVersion protocolVersion();

   HttpMessage setProtocolVersion(HttpVersion var1);

   HttpHeaders headers();
}
