package io.netty.handler.codec.http;

public interface HttpRequest extends HttpMessage {
   HttpMethod method();

   HttpRequest setMethod(HttpMethod var1);

   String uri();

   HttpRequest setUri(String var1);

   HttpRequest setProtocolVersion(HttpVersion var1);
}
