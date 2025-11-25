package io.netty.handler.codec.spdy;

import io.netty.util.internal.StringUtil;
import java.util.Iterator;
import java.util.Map.Entry;

public class DefaultSpdyHeadersFrame extends DefaultSpdyStreamFrame implements SpdyHeadersFrame {
   private boolean invalid;
   private boolean truncated;
   private final SpdyHeaders headers = new DefaultSpdyHeaders();

   public DefaultSpdyHeadersFrame(int streamId) {
      super(streamId);
   }

   public SpdyHeadersFrame setStreamId(int streamId) {
      super.setStreamId(streamId);
      return this;
   }

   public SpdyHeadersFrame setLast(boolean last) {
      super.setLast(last);
      return this;
   }

   public boolean isInvalid() {
      return this.invalid;
   }

   public SpdyHeadersFrame setInvalid() {
      this.invalid = true;
      return this;
   }

   public boolean isTruncated() {
      return this.truncated;
   }

   public SpdyHeadersFrame setTruncated() {
      this.truncated = true;
      return this;
   }

   public SpdyHeaders headers() {
      return this.headers;
   }

   public String toString() {
      StringBuilder buf = (new StringBuilder()).append(StringUtil.simpleClassName((Object)this)).append("(last: ").append(this.isLast()).append(')').append(StringUtil.NEWLINE).append("--> Stream-ID = ").append(this.streamId()).append(StringUtil.NEWLINE).append("--> Headers:").append(StringUtil.NEWLINE);
      this.appendHeaders(buf);
      buf.setLength(buf.length() - StringUtil.NEWLINE.length());
      return buf.toString();
   }

   protected void appendHeaders(StringBuilder buf) {
      Iterator i$ = this.headers().iterator();

      while(i$.hasNext()) {
         Entry<CharSequence, CharSequence> e = (Entry)i$.next();
         buf.append("    ");
         buf.append((CharSequence)e.getKey());
         buf.append(": ");
         buf.append((CharSequence)e.getValue());
         buf.append(StringUtil.NEWLINE);
      }

   }
}
