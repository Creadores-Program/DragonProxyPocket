package io.netty.handler.codec.spdy;

import io.netty.util.internal.StringUtil;

public class DefaultSpdySynReplyFrame extends DefaultSpdyHeadersFrame implements SpdySynReplyFrame {
   public DefaultSpdySynReplyFrame(int streamId) {
      super(streamId);
   }

   public SpdySynReplyFrame setStreamId(int streamId) {
      super.setStreamId(streamId);
      return this;
   }

   public SpdySynReplyFrame setLast(boolean last) {
      super.setLast(last);
      return this;
   }

   public SpdySynReplyFrame setInvalid() {
      super.setInvalid();
      return this;
   }

   public String toString() {
      StringBuilder buf = (new StringBuilder()).append(StringUtil.simpleClassName((Object)this)).append("(last: ").append(this.isLast()).append(')').append(StringUtil.NEWLINE).append("--> Stream-ID = ").append(this.streamId()).append(StringUtil.NEWLINE).append("--> Headers:").append(StringUtil.NEWLINE);
      this.appendHeaders(buf);
      buf.setLength(buf.length() - StringUtil.NEWLINE.length());
      return buf.toString();
   }
}
