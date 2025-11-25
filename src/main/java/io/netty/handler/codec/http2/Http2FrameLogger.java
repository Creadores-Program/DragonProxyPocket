package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogLevel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public class Http2FrameLogger extends ChannelHandlerAdapter {
   private final InternalLogger logger;
   private final InternalLogLevel level;

   public Http2FrameLogger(InternalLogLevel level) {
      this(level, InternalLoggerFactory.getInstance(Http2FrameLogger.class));
   }

   public Http2FrameLogger(InternalLogLevel level, InternalLogger logger) {
      this.level = (InternalLogLevel)ObjectUtil.checkNotNull(level, "level");
      this.logger = (InternalLogger)ObjectUtil.checkNotNull(logger, "logger");
   }

   public void logData(Http2FrameLogger.Direction direction, int streamId, ByteBuf data, int padding, boolean endStream) {
      this.log(direction, "DATA: streamId=%d, padding=%d, endStream=%b, length=%d, bytes=%s", streamId, padding, endStream, data.readableBytes(), ByteBufUtil.hexDump(data));
   }

   public void logHeaders(Http2FrameLogger.Direction direction, int streamId, Http2Headers headers, int padding, boolean endStream) {
      this.log(direction, "HEADERS: streamId:%d, headers=%s, padding=%d, endStream=%b", streamId, headers, padding, endStream);
   }

   public void logHeaders(Http2FrameLogger.Direction direction, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endStream) {
      this.log(direction, "HEADERS: streamId:%d, headers=%s, streamDependency=%d, weight=%d, exclusive=%b, padding=%d, endStream=%b", streamId, headers, streamDependency, weight, exclusive, padding, endStream);
   }

   public void logPriority(Http2FrameLogger.Direction direction, int streamId, int streamDependency, short weight, boolean exclusive) {
      this.log(direction, "PRIORITY: streamId=%d, streamDependency=%d, weight=%d, exclusive=%b", streamId, streamDependency, weight, exclusive);
   }

   public void logRstStream(Http2FrameLogger.Direction direction, int streamId, long errorCode) {
      this.log(direction, "RST_STREAM: streamId=%d, errorCode=%d", streamId, errorCode);
   }

   public void logSettingsAck(Http2FrameLogger.Direction direction) {
      this.log(direction, "SETTINGS ack=true");
   }

   public void logSettings(Http2FrameLogger.Direction direction, Http2Settings settings) {
      this.log(direction, "SETTINGS: ack=false, settings=%s", settings);
   }

   public void logPing(Http2FrameLogger.Direction direction, ByteBuf data) {
      this.log(direction, "PING: ack=false, length=%d, bytes=%s", data.readableBytes(), ByteBufUtil.hexDump(data));
   }

   public void logPingAck(Http2FrameLogger.Direction direction, ByteBuf data) {
      this.log(direction, "PING: ack=true, length=%d, bytes=%s", data.readableBytes(), ByteBufUtil.hexDump(data));
   }

   public void logPushPromise(Http2FrameLogger.Direction direction, int streamId, int promisedStreamId, Http2Headers headers, int padding) {
      this.log(direction, "PUSH_PROMISE: streamId=%d, promisedStreamId=%d, headers=%s, padding=%d", streamId, promisedStreamId, headers, padding);
   }

   public void logGoAway(Http2FrameLogger.Direction direction, int lastStreamId, long errorCode, ByteBuf debugData) {
      this.log(direction, "GO_AWAY: lastStreamId=%d, errorCode=%d, length=%d, bytes=%s", lastStreamId, errorCode, debugData.readableBytes(), ByteBufUtil.hexDump(debugData));
   }

   public void logWindowsUpdate(Http2FrameLogger.Direction direction, int streamId, int windowSizeIncrement) {
      this.log(direction, "WINDOW_UPDATE: streamId=%d, windowSizeIncrement=%d", streamId, windowSizeIncrement);
   }

   public void logUnknownFrame(Http2FrameLogger.Direction direction, byte frameType, int streamId, Http2Flags flags, ByteBuf data) {
      this.log(direction, "UNKNOWN: frameType=%d, streamId=%d, flags=%d, length=%d, bytes=%s", frameType & 255, streamId, flags.value(), data.readableBytes(), ByteBufUtil.hexDump(data));
   }

   private void log(Http2FrameLogger.Direction direction, String format, Object... args) {
      if (this.logger.isEnabled(this.level)) {
         StringBuilder b = new StringBuilder(200);
         b.append("\n----------------").append(direction.name()).append("--------------------\n").append(String.format(format, args)).append("\n------------------------------------");
         this.logger.log(this.level, b.toString());
      }

   }

   public static enum Direction {
      INBOUND,
      OUTBOUND;
   }
}
