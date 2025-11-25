package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;

public class DefaultHttp2FrameReader implements Http2FrameReader, Http2FrameSizePolicy, Http2FrameReader.Configuration {
   private final Http2HeadersDecoder headersDecoder;
   private DefaultHttp2FrameReader.State state;
   private byte frameType;
   private int streamId;
   private Http2Flags flags;
   private int payloadLength;
   private DefaultHttp2FrameReader.HeadersContinuation headersContinuation;
   private int maxFrameSize;

   public DefaultHttp2FrameReader() {
      this(new DefaultHttp2HeadersDecoder());
   }

   public DefaultHttp2FrameReader(Http2HeadersDecoder headersDecoder) {
      this.state = DefaultHttp2FrameReader.State.FRAME_HEADER;
      this.headersDecoder = headersDecoder;
      this.maxFrameSize = 16384;
   }

   public Http2HeaderTable headerTable() {
      return this.headersDecoder.configuration().headerTable();
   }

   public Http2FrameReader.Configuration configuration() {
      return this;
   }

   public Http2FrameSizePolicy frameSizePolicy() {
      return this;
   }

   public void maxFrameSize(int max) throws Http2Exception {
      if (!Http2CodecUtil.isMaxFrameSizeValid(max)) {
         throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Invalid MAX_FRAME_SIZE specified in sent settings: %d", max);
      } else {
         this.maxFrameSize = max;
      }
   }

   public int maxFrameSize() {
      return this.maxFrameSize;
   }

   public void close() {
      if (this.headersContinuation != null) {
         this.headersContinuation.close();
      }

   }

   public void readFrame(ChannelHandlerContext ctx, ByteBuf input, Http2FrameListener listener) throws Http2Exception {
      try {
         while(true) {
            if (input.isReadable()) {
               switch(this.state) {
               case FRAME_HEADER:
                  this.processHeaderState(input);
                  if (this.state == DefaultHttp2FrameReader.State.FRAME_HEADER) {
                     return;
                  }
               case FRAME_PAYLOAD:
                  this.processPayloadState(ctx, input, listener);
                  if (this.state != DefaultHttp2FrameReader.State.FRAME_PAYLOAD) {
                     continue;
                  }

                  return;
               case ERROR:
                  input.skipBytes(input.readableBytes());
                  return;
               default:
                  throw new IllegalStateException("Should never get here");
               }
            }

            return;
         }
      } catch (Http2Exception var5) {
         this.state = DefaultHttp2FrameReader.State.ERROR;
         throw var5;
      } catch (RuntimeException var6) {
         this.state = DefaultHttp2FrameReader.State.ERROR;
         throw var6;
      } catch (Error var7) {
         this.state = DefaultHttp2FrameReader.State.ERROR;
         throw var7;
      }
   }

   private void processHeaderState(ByteBuf in) throws Http2Exception {
      if (in.readableBytes() >= 9) {
         this.payloadLength = in.readUnsignedMedium();
         if (this.payloadLength > this.maxFrameSize) {
            throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Frame length: %d exceeds maximum: %d", this.payloadLength, this.maxFrameSize);
         } else {
            this.frameType = in.readByte();
            this.flags = new Http2Flags(in.readUnsignedByte());
            this.streamId = Http2CodecUtil.readUnsignedInt(in);
            switch(this.frameType) {
            case 0:
               this.verifyDataFrame();
               break;
            case 1:
               this.verifyHeadersFrame();
               break;
            case 2:
               this.verifyPriorityFrame();
               break;
            case 3:
               this.verifyRstStreamFrame();
               break;
            case 4:
               this.verifySettingsFrame();
               break;
            case 5:
               this.verifyPushPromiseFrame();
               break;
            case 6:
               this.verifyPingFrame();
               break;
            case 7:
               this.verifyGoAwayFrame();
               break;
            case 8:
               this.verifyWindowUpdateFrame();
               break;
            case 9:
               this.verifyContinuationFrame();
            }

            this.state = DefaultHttp2FrameReader.State.FRAME_PAYLOAD;
         }
      }
   }

   private void processPayloadState(ChannelHandlerContext ctx, ByteBuf in, Http2FrameListener listener) throws Http2Exception {
      if (in.readableBytes() >= this.payloadLength) {
         ByteBuf payload = in.readSlice(this.payloadLength);
         switch(this.frameType) {
         case 0:
            this.readDataFrame(ctx, payload, listener);
            break;
         case 1:
            this.readHeadersFrame(ctx, payload, listener);
            break;
         case 2:
            this.readPriorityFrame(ctx, payload, listener);
            break;
         case 3:
            this.readRstStreamFrame(ctx, payload, listener);
            break;
         case 4:
            this.readSettingsFrame(ctx, payload, listener);
            break;
         case 5:
            this.readPushPromiseFrame(ctx, payload, listener);
            break;
         case 6:
            this.readPingFrame(ctx, payload, listener);
            break;
         case 7:
            readGoAwayFrame(ctx, payload, listener);
            break;
         case 8:
            this.readWindowUpdateFrame(ctx, payload, listener);
            break;
         case 9:
            this.readContinuationFrame(payload, listener);
            break;
         default:
            this.readUnknownFrame(ctx, payload, listener);
         }

         this.state = DefaultHttp2FrameReader.State.FRAME_HEADER;
      }
   }

   private void verifyDataFrame() throws Http2Exception {
      this.verifyNotProcessingHeaders();
      this.verifyPayloadLength(this.payloadLength);
      if (this.payloadLength < this.flags.getPaddingPresenceFieldLength()) {
         throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Frame length %d too small.", this.payloadLength);
      }
   }

   private void verifyHeadersFrame() throws Http2Exception {
      this.verifyNotProcessingHeaders();
      this.verifyPayloadLength(this.payloadLength);
      int requiredLength = this.flags.getPaddingPresenceFieldLength() + this.flags.getNumPriorityBytes();
      if (this.payloadLength < requiredLength) {
         throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Frame length too small." + this.payloadLength);
      }
   }

   private void verifyPriorityFrame() throws Http2Exception {
      this.verifyNotProcessingHeaders();
      if (this.payloadLength != 5) {
         throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Invalid frame length %d.", this.payloadLength);
      }
   }

   private void verifyRstStreamFrame() throws Http2Exception {
      this.verifyNotProcessingHeaders();
      if (this.payloadLength != 4) {
         throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Invalid frame length %d.", this.payloadLength);
      }
   }

   private void verifySettingsFrame() throws Http2Exception {
      this.verifyNotProcessingHeaders();
      this.verifyPayloadLength(this.payloadLength);
      if (this.streamId != 0) {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "A stream ID must be zero.");
      } else if (this.flags.ack() && this.payloadLength > 0) {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Ack settings frame must have an empty payload.");
      } else if (this.payloadLength % 6 > 0) {
         throw Http2Exception.connectionError(Http2Error.FRAME_SIZE_ERROR, "Frame length %d invalid.", this.payloadLength);
      }
   }

   private void verifyPushPromiseFrame() throws Http2Exception {
      this.verifyNotProcessingHeaders();
      this.verifyPayloadLength(this.payloadLength);
      int minLength = this.flags.getPaddingPresenceFieldLength() + 4;
      if (this.payloadLength < minLength) {
         throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Frame length %d too small.", this.payloadLength);
      }
   }

   private void verifyPingFrame() throws Http2Exception {
      this.verifyNotProcessingHeaders();
      if (this.streamId != 0) {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "A stream ID must be zero.");
      } else if (this.payloadLength != 8) {
         throw Http2Exception.connectionError(Http2Error.FRAME_SIZE_ERROR, "Frame length %d incorrect size for ping.", this.payloadLength);
      }
   }

   private void verifyGoAwayFrame() throws Http2Exception {
      this.verifyNotProcessingHeaders();
      this.verifyPayloadLength(this.payloadLength);
      if (this.streamId != 0) {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "A stream ID must be zero.");
      } else if (this.payloadLength < 8) {
         throw Http2Exception.connectionError(Http2Error.FRAME_SIZE_ERROR, "Frame length %d too small.", this.payloadLength);
      }
   }

   private void verifyWindowUpdateFrame() throws Http2Exception {
      this.verifyNotProcessingHeaders();
      verifyStreamOrConnectionId(this.streamId, "Stream ID");
      if (this.payloadLength != 4) {
         throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Invalid frame length %d.", this.payloadLength);
      }
   }

   private void verifyContinuationFrame() throws Http2Exception {
      this.verifyPayloadLength(this.payloadLength);
      if (this.headersContinuation == null) {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Received %s frame but not currently processing headers.", this.frameType);
      } else if (this.streamId != this.headersContinuation.getStreamId()) {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Continuation stream ID does not match pending headers. Expected %d, but received %d.", this.headersContinuation.getStreamId(), this.streamId);
      } else if (this.payloadLength < this.flags.getPaddingPresenceFieldLength()) {
         throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Frame length %d too small for padding.", this.payloadLength);
      }
   }

   private void readDataFrame(ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener) throws Http2Exception {
      short padding = this.readPadding(payload);
      int dataLength = payload.readableBytes() - padding;
      if (dataLength < 0) {
         throw Http2Exception.streamError(this.streamId, Http2Error.FRAME_SIZE_ERROR, "Frame payload too small for padding.");
      } else {
         ByteBuf data = payload.readSlice(dataLength);
         listener.onDataRead(ctx, this.streamId, data, padding, this.flags.endOfStream());
         payload.skipBytes(payload.readableBytes());
      }
   }

   private void readHeadersFrame(final ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener) throws Http2Exception {
      final int headersStreamId = this.streamId;
      final Http2Flags headersFlags = this.flags;
      final int padding = this.readPadding(payload);
      if (this.flags.priorityPresent()) {
         long word1 = payload.readUnsignedInt();
         final boolean exclusive = (word1 & 2147483648L) != 0L;
         final int streamDependency = (int)(word1 & 2147483647L);
         final short weight = (short)(payload.readUnsignedByte() + 1);
         ByteBuf fragment = payload.readSlice(payload.readableBytes() - padding);
         this.headersContinuation = new DefaultHttp2FrameReader.HeadersContinuation() {
            public int getStreamId() {
               return headersStreamId;
            }

            public void processFragment(boolean endOfHeaders, ByteBuf fragment, Http2FrameListener listener) throws Http2Exception {
               DefaultHttp2FrameReader.HeadersBlockBuilder hdrBlockBuilder = this.headersBlockBuilder();
               hdrBlockBuilder.addFragment(fragment, ctx.alloc(), endOfHeaders);
               if (endOfHeaders) {
                  listener.onHeadersRead(ctx, headersStreamId, hdrBlockBuilder.headers(), streamDependency, weight, exclusive, padding, headersFlags.endOfStream());
                  this.close();
               }

            }
         };
         this.headersContinuation.processFragment(this.flags.endOfHeaders(), fragment, listener);
      } else {
         this.headersContinuation = new DefaultHttp2FrameReader.HeadersContinuation() {
            public int getStreamId() {
               return headersStreamId;
            }

            public void processFragment(boolean endOfHeaders, ByteBuf fragment, Http2FrameListener listener) throws Http2Exception {
               DefaultHttp2FrameReader.HeadersBlockBuilder hdrBlockBuilder = this.headersBlockBuilder();
               hdrBlockBuilder.addFragment(fragment, ctx.alloc(), endOfHeaders);
               if (endOfHeaders) {
                  listener.onHeadersRead(ctx, headersStreamId, hdrBlockBuilder.headers(), padding, headersFlags.endOfStream());
                  this.close();
               }

            }
         };
         ByteBuf fragment = payload.readSlice(payload.readableBytes() - padding);
         this.headersContinuation.processFragment(this.flags.endOfHeaders(), fragment, listener);
      }
   }

   private void readPriorityFrame(ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener) throws Http2Exception {
      long word1 = payload.readUnsignedInt();
      boolean exclusive = (word1 & 2147483648L) != 0L;
      int streamDependency = (int)(word1 & 2147483647L);
      short weight = (short)(payload.readUnsignedByte() + 1);
      listener.onPriorityRead(ctx, this.streamId, streamDependency, weight, exclusive);
   }

   private void readRstStreamFrame(ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener) throws Http2Exception {
      long errorCode = payload.readUnsignedInt();
      listener.onRstStreamRead(ctx, this.streamId, errorCode);
   }

   private void readSettingsFrame(ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener) throws Http2Exception {
      if (this.flags.ack()) {
         listener.onSettingsAckRead(ctx);
      } else {
         int numSettings = this.payloadLength / 6;
         Http2Settings settings = new Http2Settings();

         for(int index = 0; index < numSettings; ++index) {
            int id = payload.readUnsignedShort();
            long value = payload.readUnsignedInt();

            try {
               settings.put(id, value);
            } catch (IllegalArgumentException var11) {
               switch(id) {
               case 4:
                  throw Http2Exception.connectionError(Http2Error.FLOW_CONTROL_ERROR, var11, var11.getMessage());
               case 5:
                  throw Http2Exception.connectionError(Http2Error.FRAME_SIZE_ERROR, var11, var11.getMessage());
               default:
                  throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, var11, var11.getMessage());
               }
            }
         }

         listener.onSettingsRead(ctx, settings);
      }

   }

   private void readPushPromiseFrame(final ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener) throws Http2Exception {
      final int pushPromiseStreamId = this.streamId;
      final int padding = this.readPadding(payload);
      final int promisedStreamId = Http2CodecUtil.readUnsignedInt(payload);
      this.headersContinuation = new DefaultHttp2FrameReader.HeadersContinuation() {
         public int getStreamId() {
            return pushPromiseStreamId;
         }

         public void processFragment(boolean endOfHeaders, ByteBuf fragment, Http2FrameListener listener) throws Http2Exception {
            this.headersBlockBuilder().addFragment(fragment, ctx.alloc(), endOfHeaders);
            if (endOfHeaders) {
               Http2Headers headers = this.headersBlockBuilder().headers();
               listener.onPushPromiseRead(ctx, pushPromiseStreamId, promisedStreamId, headers, padding);
               this.close();
            }

         }
      };
      ByteBuf fragment = payload.readSlice(payload.readableBytes() - padding);
      this.headersContinuation.processFragment(this.flags.endOfHeaders(), fragment, listener);
   }

   private void readPingFrame(ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener) throws Http2Exception {
      ByteBuf data = payload.readSlice(payload.readableBytes());
      if (this.flags.ack()) {
         listener.onPingAckRead(ctx, data);
      } else {
         listener.onPingRead(ctx, data);
      }

   }

   private static void readGoAwayFrame(ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener) throws Http2Exception {
      int lastStreamId = Http2CodecUtil.readUnsignedInt(payload);
      long errorCode = payload.readUnsignedInt();
      ByteBuf debugData = payload.readSlice(payload.readableBytes());
      listener.onGoAwayRead(ctx, lastStreamId, errorCode, debugData);
   }

   private void readWindowUpdateFrame(ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener) throws Http2Exception {
      int windowSizeIncrement = Http2CodecUtil.readUnsignedInt(payload);
      if (windowSizeIncrement == 0) {
         throw Http2Exception.streamError(this.streamId, Http2Error.PROTOCOL_ERROR, "Received WINDOW_UPDATE with delta 0 for stream: %d", this.streamId);
      } else {
         listener.onWindowUpdateRead(ctx, this.streamId, windowSizeIncrement);
      }
   }

   private void readContinuationFrame(ByteBuf payload, Http2FrameListener listener) throws Http2Exception {
      ByteBuf continuationFragment = payload.readSlice(payload.readableBytes());
      this.headersContinuation.processFragment(this.flags.endOfHeaders(), continuationFragment, listener);
   }

   private void readUnknownFrame(ChannelHandlerContext ctx, ByteBuf payload, Http2FrameListener listener) {
      payload = payload.readSlice(payload.readableBytes());
      listener.onUnknownFrame(ctx, this.frameType, this.streamId, this.flags, payload);
   }

   private short readPadding(ByteBuf payload) {
      return !this.flags.paddingPresent() ? 0 : payload.readUnsignedByte();
   }

   private void verifyNotProcessingHeaders() throws Http2Exception {
      if (this.headersContinuation != null) {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Received frame of type %s while processing headers.", this.frameType);
      }
   }

   private void verifyPayloadLength(int payloadLength) throws Http2Exception {
      if (payloadLength > this.maxFrameSize) {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "Total payload length %d exceeds max frame length.", payloadLength);
      }
   }

   private static void verifyStreamOrConnectionId(int streamId, String argumentName) throws Http2Exception {
      if (streamId < 0) {
         throw Http2Exception.connectionError(Http2Error.PROTOCOL_ERROR, "%s must be >= 0", argumentName);
      }
   }

   protected class HeadersBlockBuilder {
      private ByteBuf headerBlock;

      final void addFragment(ByteBuf fragment, ByteBufAllocator alloc, boolean endOfHeaders) {
         if (this.headerBlock == null) {
            if (endOfHeaders) {
               this.headerBlock = fragment.retain();
            } else {
               this.headerBlock = alloc.buffer(fragment.readableBytes());
               this.headerBlock.writeBytes(fragment);
            }

         } else {
            if (this.headerBlock.isWritable(fragment.readableBytes())) {
               this.headerBlock.writeBytes(fragment);
            } else {
               ByteBuf buf = alloc.buffer(this.headerBlock.readableBytes() + fragment.readableBytes());
               buf.writeBytes(this.headerBlock);
               buf.writeBytes(fragment);
               this.headerBlock.release();
               this.headerBlock = buf;
            }

         }
      }

      Http2Headers headers() throws Http2Exception {
         Http2Headers var1;
         try {
            var1 = DefaultHttp2FrameReader.this.headersDecoder.decodeHeaders(this.headerBlock);
         } finally {
            this.close();
         }

         return var1;
      }

      void close() {
         if (this.headerBlock != null) {
            this.headerBlock.release();
            this.headerBlock = null;
         }

         DefaultHttp2FrameReader.this.headersContinuation = null;
      }
   }

   private abstract class HeadersContinuation {
      private final DefaultHttp2FrameReader.HeadersBlockBuilder builder;

      private HeadersContinuation() {
         this.builder = DefaultHttp2FrameReader.this.new HeadersBlockBuilder();
      }

      abstract int getStreamId();

      abstract void processFragment(boolean var1, ByteBuf var2, Http2FrameListener var3) throws Http2Exception;

      final DefaultHttp2FrameReader.HeadersBlockBuilder headersBlockBuilder() {
         return this.builder;
      }

      final void close() {
         this.builder.close();
      }

      // $FF: synthetic method
      HeadersContinuation(Object x1) {
         this();
      }
   }

   private static enum State {
      FRAME_HEADER,
      FRAME_PAYLOAD,
      ERROR;
   }
}
