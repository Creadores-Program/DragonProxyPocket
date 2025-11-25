package io.netty.handler.codec.http2;

import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.BinaryHeaders;
import java.util.HashSet;
import java.util.Set;

public interface Http2Headers extends BinaryHeaders {
   Http2Headers add(AsciiString var1, AsciiString var2);

   Http2Headers add(AsciiString var1, Iterable<? extends AsciiString> var2);

   Http2Headers add(AsciiString var1, AsciiString... var2);

   Http2Headers addObject(AsciiString var1, Object var2);

   Http2Headers addObject(AsciiString var1, Iterable<?> var2);

   Http2Headers addObject(AsciiString var1, Object... var2);

   Http2Headers addBoolean(AsciiString var1, boolean var2);

   Http2Headers addByte(AsciiString var1, byte var2);

   Http2Headers addChar(AsciiString var1, char var2);

   Http2Headers addShort(AsciiString var1, short var2);

   Http2Headers addInt(AsciiString var1, int var2);

   Http2Headers addLong(AsciiString var1, long var2);

   Http2Headers addFloat(AsciiString var1, float var2);

   Http2Headers addDouble(AsciiString var1, double var2);

   Http2Headers addTimeMillis(AsciiString var1, long var2);

   Http2Headers add(BinaryHeaders var1);

   Http2Headers set(AsciiString var1, AsciiString var2);

   Http2Headers set(AsciiString var1, Iterable<? extends AsciiString> var2);

   Http2Headers set(AsciiString var1, AsciiString... var2);

   Http2Headers setObject(AsciiString var1, Object var2);

   Http2Headers setObject(AsciiString var1, Iterable<?> var2);

   Http2Headers setObject(AsciiString var1, Object... var2);

   Http2Headers setBoolean(AsciiString var1, boolean var2);

   Http2Headers setByte(AsciiString var1, byte var2);

   Http2Headers setChar(AsciiString var1, char var2);

   Http2Headers setShort(AsciiString var1, short var2);

   Http2Headers setInt(AsciiString var1, int var2);

   Http2Headers setLong(AsciiString var1, long var2);

   Http2Headers setFloat(AsciiString var1, float var2);

   Http2Headers setDouble(AsciiString var1, double var2);

   Http2Headers setTimeMillis(AsciiString var1, long var2);

   Http2Headers set(BinaryHeaders var1);

   Http2Headers setAll(BinaryHeaders var1);

   Http2Headers clear();

   Http2Headers method(AsciiString var1);

   Http2Headers scheme(AsciiString var1);

   Http2Headers authority(AsciiString var1);

   Http2Headers path(AsciiString var1);

   Http2Headers status(AsciiString var1);

   AsciiString method();

   AsciiString scheme();

   AsciiString authority();

   AsciiString path();

   AsciiString status();

   public static enum PseudoHeaderName {
      METHOD(":method"),
      SCHEME(":scheme"),
      AUTHORITY(":authority"),
      PATH(":path"),
      STATUS(":status");

      private final AsciiString value;
      private static final Set<AsciiString> PSEUDO_HEADERS = new HashSet();

      private PseudoHeaderName(String value) {
         this.value = new AsciiString(value);
      }

      public AsciiString value() {
         return this.value;
      }

      public static boolean isPseudoHeader(AsciiString header) {
         return PSEUDO_HEADERS.contains(header);
      }

      static {
         Http2Headers.PseudoHeaderName[] arr$ = values();
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            Http2Headers.PseudoHeaderName pseudoHeader = arr$[i$];
            PSEUDO_HEADERS.add(pseudoHeader.value());
         }

      }
   }
}
