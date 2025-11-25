package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;

public class DefaultBinaryMemcacheRequest extends AbstractBinaryMemcacheMessage implements BinaryMemcacheRequest {
   public static final byte REQUEST_MAGIC_BYTE = -128;
   private short reserved;

   public DefaultBinaryMemcacheRequest() {
      this((String)null, (ByteBuf)null);
   }

   public DefaultBinaryMemcacheRequest(String key) {
      this(key, (ByteBuf)null);
   }

   public DefaultBinaryMemcacheRequest(ByteBuf extras) {
      this((String)null, extras);
   }

   public DefaultBinaryMemcacheRequest(String key, ByteBuf extras) {
      super(key, extras);
      this.setMagic((byte)-128);
   }

   public short reserved() {
      return this.reserved;
   }

   public BinaryMemcacheRequest setReserved(short reserved) {
      this.reserved = reserved;
      return this;
   }

   public BinaryMemcacheRequest retain() {
      super.retain();
      return this;
   }

   public BinaryMemcacheRequest retain(int increment) {
      super.retain(increment);
      return this;
   }

   public BinaryMemcacheRequest touch() {
      super.touch();
      return this;
   }

   public BinaryMemcacheRequest touch(Object hint) {
      super.touch(hint);
      return this;
   }
}
