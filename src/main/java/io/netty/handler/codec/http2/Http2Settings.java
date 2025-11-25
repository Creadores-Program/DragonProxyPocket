package io.netty.handler.codec.http2;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.internal.ObjectUtil;

public final class Http2Settings extends IntObjectHashMap<Long> {
   public Http2Settings() {
      this(6);
   }

   public Http2Settings(int initialCapacity, float loadFactor) {
      super(initialCapacity, loadFactor);
   }

   public Http2Settings(int initialCapacity) {
      super(initialCapacity);
   }

   public Long put(int key, Long value) {
      verifyStandardSetting(key, value);
      return (Long)super.put(key, value);
   }

   public Long headerTableSize() {
      return (Long)this.get(1);
   }

   public Http2Settings headerTableSize(int value) {
      this.put(1, (Long)((long)value));
      return this;
   }

   public Boolean pushEnabled() {
      Long value = (Long)this.get(2);
      return value == null ? null : value != 0L;
   }

   public Http2Settings pushEnabled(boolean enabled) {
      this.put(2, (Long)(enabled ? 1L : 0L));
      return this;
   }

   public Long maxConcurrentStreams() {
      return (Long)this.get(3);
   }

   public Http2Settings maxConcurrentStreams(long value) {
      this.put(3, (Long)value);
      return this;
   }

   public Integer initialWindowSize() {
      return this.getIntValue(4);
   }

   public Http2Settings initialWindowSize(int value) {
      this.put(4, (Long)((long)value));
      return this;
   }

   public Integer maxFrameSize() {
      return this.getIntValue(5);
   }

   public Http2Settings maxFrameSize(int value) {
      this.put(5, (Long)((long)value));
      return this;
   }

   public Integer maxHeaderListSize() {
      return this.getIntValue(6);
   }

   public Http2Settings maxHeaderListSize(int value) {
      this.put(6, (Long)((long)value));
      return this;
   }

   public Http2Settings copyFrom(Http2Settings settings) {
      this.clear();
      this.putAll(settings);
      return this;
   }

   Integer getIntValue(int key) {
      Long value = (Long)this.get(key);
      return value == null ? null : value.intValue();
   }

   private static void verifyStandardSetting(int key, Long value) {
      ObjectUtil.checkNotNull(value, "value");
      switch(key) {
      case 1:
         if (value < 0L || value > 2147483647L) {
            throw new IllegalArgumentException("Setting HEADER_TABLE_SIZE is invalid: " + value);
         }
         break;
      case 2:
         if (value != 0L && value != 1L) {
            throw new IllegalArgumentException("Setting ENABLE_PUSH is invalid: " + value);
         }
         break;
      case 3:
         if (value >= 0L && value <= 4294967295L) {
            break;
         }

         throw new IllegalArgumentException("Setting MAX_CONCURRENT_STREAMS is invalid: " + value);
      case 4:
         if (value >= 0L && value <= 2147483647L) {
            break;
         }

         throw new IllegalArgumentException("Setting INITIAL_WINDOW_SIZE is invalid: " + value);
      case 5:
         if (!Http2CodecUtil.isMaxFrameSizeValid(value.intValue())) {
            throw new IllegalArgumentException("Setting MAX_FRAME_SIZE is invalid: " + value);
         }
         break;
      case 6:
         if (value < 0L || value > Long.MAX_VALUE) {
            throw new IllegalArgumentException("Setting MAX_HEADER_LIST_SIZE is invalid: " + value);
         }
         break;
      default:
         throw new IllegalArgumentException("key");
      }

   }

   protected String keyToString(int key) {
      switch(key) {
      case 1:
         return "HEADER_TABLE_SIZE";
      case 2:
         return "ENABLE_PUSH";
      case 3:
         return "MAX_CONCURRENT_STREAMS";
      case 4:
         return "INITIAL_WINDOW_SIZE";
      case 5:
         return "MAX_FRAME_SIZE";
      case 6:
         return "MAX_HEADER_LIST_SIZE";
      default:
         return super.keyToString(key);
      }
   }
}
