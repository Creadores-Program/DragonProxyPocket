package io.netty.handler.codec.http2;

public interface Http2StreamRemovalPolicy {
   void setAction(Http2StreamRemovalPolicy.Action var1);

   void markForRemoval(Http2Stream var1);

   public interface Action {
      void removeStream(Http2Stream var1);
   }
}
