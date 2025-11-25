package io.netty.handler.codec.stomp;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderResult;

public class DefaultStompContentSubframe implements StompContentSubframe {
   private DecoderResult decoderResult;
   private final ByteBuf content;

   public DefaultStompContentSubframe(ByteBuf content) {
      this.decoderResult = DecoderResult.SUCCESS;
      if (content == null) {
         throw new NullPointerException("content");
      } else {
         this.content = content;
      }
   }

   public ByteBuf content() {
      return this.content;
   }

   public StompContentSubframe copy() {
      return new DefaultStompContentSubframe(this.content().copy());
   }

   public StompContentSubframe duplicate() {
      return new DefaultStompContentSubframe(this.content().duplicate());
   }

   public int refCnt() {
      return this.content().refCnt();
   }

   public StompContentSubframe retain() {
      this.content().retain();
      return this;
   }

   public StompContentSubframe retain(int increment) {
      this.content().retain(increment);
      return this;
   }

   public StompContentSubframe touch() {
      this.content.touch();
      return this;
   }

   public StompContentSubframe touch(Object hint) {
      this.content.touch(hint);
      return this;
   }

   public boolean release() {
      return this.content().release();
   }

   public boolean release(int decrement) {
      return this.content().release(decrement);
   }

   public DecoderResult decoderResult() {
      return this.decoderResult;
   }

   public void setDecoderResult(DecoderResult decoderResult) {
      this.decoderResult = decoderResult;
   }

   public String toString() {
      return "DefaultStompContent{decoderResult=" + this.decoderResult + '}';
   }
}
