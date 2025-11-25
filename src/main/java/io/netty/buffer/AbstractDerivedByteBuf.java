package io.netty.buffer;

import java.nio.ByteBuffer;

public abstract class AbstractDerivedByteBuf extends AbstractByteBuf {
   protected AbstractDerivedByteBuf(int maxCapacity) {
      super(maxCapacity);
   }

   public final int refCnt() {
      return this.unwrap().refCnt();
   }

   public final ByteBuf retain() {
      this.unwrap().retain();
      return this;
   }

   public final ByteBuf retain(int increment) {
      this.unwrap().retain(increment);
      return this;
   }

   public final ByteBuf touch() {
      this.unwrap().touch();
      return this;
   }

   public final ByteBuf touch(Object hint) {
      this.unwrap().touch(hint);
      return this;
   }

   public final boolean release() {
      return this.unwrap().release();
   }

   public final boolean release(int decrement) {
      return this.unwrap().release(decrement);
   }

   public ByteBuffer internalNioBuffer(int index, int length) {
      return this.nioBuffer(index, length);
   }

   public ByteBuffer nioBuffer(int index, int length) {
      return this.unwrap().nioBuffer(index, length);
   }
}
