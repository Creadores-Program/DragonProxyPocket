package io.netty.handler.codec.http.websocketx;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.ByteOrder;
import java.util.List;

public class WebSocket08FrameDecoder extends ByteToMessageDecoder implements WebSocketFrameDecoder {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(WebSocket08FrameDecoder.class);
   private static final byte OPCODE_CONT = 0;
   private static final byte OPCODE_TEXT = 1;
   private static final byte OPCODE_BINARY = 2;
   private static final byte OPCODE_CLOSE = 8;
   private static final byte OPCODE_PING = 9;
   private static final byte OPCODE_PONG = 10;
   private final long maxFramePayloadLength;
   private final boolean allowExtensions;
   private final boolean expectMaskedFrames;
   private final boolean allowMaskMismatch;
   private int fragmentedFramesCount;
   private boolean frameFinalFlag;
   private boolean frameMasked;
   private int frameRsv;
   private int frameOpcode;
   private long framePayloadLength;
   private byte[] maskingKey;
   private int framePayloadLen1;
   private boolean receivedClosingHandshake;
   private WebSocket08FrameDecoder.State state;

   public WebSocket08FrameDecoder(boolean expectMaskedFrames, boolean allowExtensions, int maxFramePayloadLength) {
      this(expectMaskedFrames, allowExtensions, maxFramePayloadLength, false);
   }

   public WebSocket08FrameDecoder(boolean expectMaskedFrames, boolean allowExtensions, int maxFramePayloadLength, boolean allowMaskMismatch) {
      this.state = WebSocket08FrameDecoder.State.READING_FIRST;
      this.expectMaskedFrames = expectMaskedFrames;
      this.allowMaskMismatch = allowMaskMismatch;
      this.allowExtensions = allowExtensions;
      this.maxFramePayloadLength = (long)maxFramePayloadLength;
   }

   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      if (this.receivedClosingHandshake) {
         in.skipBytes(this.actualReadableBytes());
      } else {
         byte b;
         switch(this.state) {
         case READING_FIRST:
            if (!in.isReadable()) {
               return;
            }

            this.framePayloadLength = 0L;
            b = in.readByte();
            this.frameFinalFlag = (b & 128) != 0;
            this.frameRsv = (b & 112) >> 4;
            this.frameOpcode = b & 15;
            if (logger.isDebugEnabled()) {
               logger.debug("Decoding WebSocket Frame opCode={}", (Object)this.frameOpcode);
            }

            this.state = WebSocket08FrameDecoder.State.READING_SECOND;
         case READING_SECOND:
            if (!in.isReadable()) {
               return;
            }

            b = in.readByte();
            this.frameMasked = (b & 128) != 0;
            this.framePayloadLen1 = b & 127;
            if (this.frameRsv != 0 && !this.allowExtensions) {
               this.protocolViolation(ctx, "RSV != 0 and no extension negotiated, RSV:" + this.frameRsv);
               return;
            }

            if (!this.allowMaskMismatch && this.expectMaskedFrames != this.frameMasked) {
               this.protocolViolation(ctx, "received a frame that is not masked as expected");
               return;
            }

            if (this.frameOpcode > 7) {
               if (!this.frameFinalFlag) {
                  this.protocolViolation(ctx, "fragmented control frame");
                  return;
               }

               if (this.framePayloadLen1 > 125) {
                  this.protocolViolation(ctx, "control frame with payload length > 125 octets");
                  return;
               }

               if (this.frameOpcode != 8 && this.frameOpcode != 9 && this.frameOpcode != 10) {
                  this.protocolViolation(ctx, "control frame using reserved opcode " + this.frameOpcode);
                  return;
               }

               if (this.frameOpcode == 8 && this.framePayloadLen1 == 1) {
                  this.protocolViolation(ctx, "received close control frame with payload len 1");
                  return;
               }
            } else {
               if (this.frameOpcode != 0 && this.frameOpcode != 1 && this.frameOpcode != 2) {
                  this.protocolViolation(ctx, "data frame using reserved opcode " + this.frameOpcode);
                  return;
               }

               if (this.fragmentedFramesCount == 0 && this.frameOpcode == 0) {
                  this.protocolViolation(ctx, "received continuation data frame outside fragmented message");
                  return;
               }

               if (this.fragmentedFramesCount != 0 && this.frameOpcode != 0 && this.frameOpcode != 9) {
                  this.protocolViolation(ctx, "received non-continuation data frame while inside fragmented message");
                  return;
               }
            }

            this.state = WebSocket08FrameDecoder.State.READING_SIZE;
         case READING_SIZE:
            if (this.framePayloadLen1 == 126) {
               if (in.readableBytes() < 2) {
                  return;
               }

               this.framePayloadLength = (long)in.readUnsignedShort();
               if (this.framePayloadLength < 126L) {
                  this.protocolViolation(ctx, "invalid data frame length (not using minimal length encoding)");
                  return;
               }
            } else if (this.framePayloadLen1 == 127) {
               if (in.readableBytes() < 8) {
                  return;
               }

               this.framePayloadLength = in.readLong();
               if (this.framePayloadLength < 65536L) {
                  this.protocolViolation(ctx, "invalid data frame length (not using minimal length encoding)");
                  return;
               }
            } else {
               this.framePayloadLength = (long)this.framePayloadLen1;
            }

            if (this.framePayloadLength > this.maxFramePayloadLength) {
               this.protocolViolation(ctx, "Max frame length of " + this.maxFramePayloadLength + " has been exceeded.");
               return;
            }

            if (logger.isDebugEnabled()) {
               logger.debug("Decoding WebSocket Frame length={}", (Object)this.framePayloadLength);
            }

            this.state = WebSocket08FrameDecoder.State.MASKING_KEY;
         case MASKING_KEY:
            if (this.frameMasked) {
               if (in.readableBytes() < 4) {
                  return;
               }

               if (this.maskingKey == null) {
                  this.maskingKey = new byte[4];
               }

               in.readBytes(this.maskingKey);
            }

            this.state = WebSocket08FrameDecoder.State.PAYLOAD;
         case PAYLOAD:
            break;
         case CORRUPT:
            if (in.isReadable()) {
               in.readByte();
            }

            return;
         default:
            throw new Error("Shouldn't reach here.");
         }

         if ((long)in.readableBytes() >= this.framePayloadLength) {
            ByteBuf payloadBuffer = null;

            try {
               payloadBuffer = ByteBufUtil.readBytes(ctx.alloc(), in, toFrameLength(this.framePayloadLength));
               this.state = WebSocket08FrameDecoder.State.READING_FIRST;
               if (this.frameMasked) {
                  this.unmask(payloadBuffer);
               }

               if (this.frameOpcode == 9) {
                  out.add(new PingWebSocketFrame(this.frameFinalFlag, this.frameRsv, payloadBuffer));
                  payloadBuffer = null;
                  return;
               }

               if (this.frameOpcode == 10) {
                  out.add(new PongWebSocketFrame(this.frameFinalFlag, this.frameRsv, payloadBuffer));
                  payloadBuffer = null;
                  return;
               }

               if (this.frameOpcode != 8) {
                  if (this.frameFinalFlag) {
                     if (this.frameOpcode != 9) {
                        this.fragmentedFramesCount = 0;
                     }
                  } else {
                     ++this.fragmentedFramesCount;
                  }

                  if (this.frameOpcode == 1) {
                     out.add(new TextWebSocketFrame(this.frameFinalFlag, this.frameRsv, payloadBuffer));
                     payloadBuffer = null;
                     return;
                  }

                  if (this.frameOpcode == 2) {
                     out.add(new BinaryWebSocketFrame(this.frameFinalFlag, this.frameRsv, payloadBuffer));
                     payloadBuffer = null;
                     return;
                  }

                  if (this.frameOpcode == 0) {
                     out.add(new ContinuationWebSocketFrame(this.frameFinalFlag, this.frameRsv, payloadBuffer));
                     payloadBuffer = null;
                     return;
                  }

                  throw new UnsupportedOperationException("Cannot decode web socket frame with opcode: " + this.frameOpcode);
               }

               this.receivedClosingHandshake = true;
               this.checkCloseFrameBody(ctx, payloadBuffer);
               out.add(new CloseWebSocketFrame(this.frameFinalFlag, this.frameRsv, payloadBuffer));
               payloadBuffer = null;
            } finally {
               if (payloadBuffer != null) {
                  payloadBuffer.release();
               }

            }

         }
      }
   }

   private void unmask(ByteBuf frame) {
      int i = frame.readerIndex();
      int end = frame.writerIndex();
      ByteOrder order = frame.order();
      int intMask = (this.maskingKey[0] & 255) << 24 | (this.maskingKey[1] & 255) << 16 | (this.maskingKey[2] & 255) << 8 | this.maskingKey[3] & 255;
      if (order == ByteOrder.LITTLE_ENDIAN) {
         intMask = Integer.reverseBytes(intMask);
      }

      while(i + 3 < end) {
         int unmasked = frame.getInt(i) ^ intMask;
         frame.setInt(i, unmasked);
         i += 4;
      }

      while(i < end) {
         frame.setByte(i, frame.getByte(i) ^ this.maskingKey[i % 4]);
         ++i;
      }

   }

   private void protocolViolation(ChannelHandlerContext ctx, String reason) {
      this.protocolViolation(ctx, new CorruptedFrameException(reason));
   }

   private void protocolViolation(ChannelHandlerContext ctx, CorruptedFrameException ex) {
      this.state = WebSocket08FrameDecoder.State.CORRUPT;
      if (ctx.channel().isActive()) {
         Object closeMessage;
         if (this.receivedClosingHandshake) {
            closeMessage = Unpooled.EMPTY_BUFFER;
         } else {
            closeMessage = new CloseWebSocketFrame(1002, (String)null);
         }

         ctx.writeAndFlush(closeMessage).addListener(ChannelFutureListener.CLOSE);
      }

      throw ex;
   }

   private static int toFrameLength(long l) {
      if (l > 2147483647L) {
         throw new TooLongFrameException("Length:" + l);
      } else {
         return (int)l;
      }
   }

   protected void checkCloseFrameBody(ChannelHandlerContext ctx, ByteBuf buffer) {
      if (buffer != null && buffer.isReadable()) {
         if (buffer.readableBytes() == 1) {
            this.protocolViolation(ctx, "Invalid close frame body");
         }

         int idx = buffer.readerIndex();
         buffer.readerIndex(0);
         int statusCode = buffer.readShort();
         if (statusCode >= 0 && statusCode <= 999 || statusCode >= 1004 && statusCode <= 1006 || statusCode >= 1012 && statusCode <= 2999) {
            this.protocolViolation(ctx, "Invalid close frame getStatus code: " + statusCode);
         }

         if (buffer.isReadable()) {
            try {
               (new Utf8Validator()).check(buffer);
            } catch (CorruptedFrameException var6) {
               this.protocolViolation(ctx, var6);
            }
         }

         buffer.readerIndex(idx);
      }
   }

   static enum State {
      READING_FIRST,
      READING_SECOND,
      READING_SIZE,
      MASKING_KEY,
      PAYLOAD,
      CORRUPT;
   }
}
