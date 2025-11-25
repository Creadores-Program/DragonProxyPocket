package io.netty.handler.codec.memcache;

import io.netty.handler.codec.DecoderResult;

public abstract class AbstractMemcacheObject implements MemcacheObject {
   private DecoderResult decoderResult;

   protected AbstractMemcacheObject() {
      this.decoderResult = DecoderResult.SUCCESS;
   }

   public DecoderResult decoderResult() {
      return this.decoderResult;
   }

   public void setDecoderResult(DecoderResult result) {
      if (result == null) {
         throw new NullPointerException("DecoderResult should not be null.");
      } else {
         this.decoderResult = result;
      }
   }
}
