package io.netty.handler.codec.memcache;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderResult;

public interface LastMemcacheContent extends MemcacheContent {
   LastMemcacheContent EMPTY_LAST_CONTENT = new LastMemcacheContent() {
      public LastMemcacheContent copy() {
         return EMPTY_LAST_CONTENT;
      }

      public LastMemcacheContent retain(int increment) {
         return this;
      }

      public LastMemcacheContent retain() {
         return this;
      }

      public LastMemcacheContent touch() {
         return this;
      }

      public LastMemcacheContent touch(Object hint) {
         return this;
      }

      public LastMemcacheContent duplicate() {
         return this;
      }

      public ByteBuf content() {
         return Unpooled.EMPTY_BUFFER;
      }

      public DecoderResult decoderResult() {
         return DecoderResult.SUCCESS;
      }

      public void setDecoderResult(DecoderResult result) {
         throw new UnsupportedOperationException("read only");
      }

      public int refCnt() {
         return 1;
      }

      public boolean release() {
         return false;
      }

      public boolean release(int decrement) {
         return false;
      }
   };

   LastMemcacheContent copy();

   LastMemcacheContent retain(int var1);

   LastMemcacheContent retain();

   LastMemcacheContent touch();

   LastMemcacheContent touch(Object var1);

   LastMemcacheContent duplicate();
}
