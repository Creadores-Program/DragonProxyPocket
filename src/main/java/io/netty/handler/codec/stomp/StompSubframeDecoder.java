package io.netty.handler.codec.stomp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.internal.AppendableCharSequence;
import io.netty.util.internal.StringUtil;
import java.util.List;
import java.util.Locale;

public class StompSubframeDecoder extends ReplayingDecoder<StompSubframeDecoder.State> {
   private static final int DEFAULT_CHUNK_SIZE = 8132;
   private static final int DEFAULT_MAX_LINE_LENGTH = 1024;
   private final int maxLineLength;
   private final int maxChunkSize;
   private int alreadyReadChunkSize;
   private LastStompContentSubframe lastContent;
   private long contentLength;

   public StompSubframeDecoder() {
      this(1024, 8132);
   }

   public StompSubframeDecoder(int maxLineLength, int maxChunkSize) {
      super(StompSubframeDecoder.State.SKIP_CONTROL_CHARACTERS);
      if (maxLineLength <= 0) {
         throw new IllegalArgumentException("maxLineLength must be a positive integer: " + maxLineLength);
      } else if (maxChunkSize <= 0) {
         throw new IllegalArgumentException("maxChunkSize must be a positive integer: " + maxChunkSize);
      } else {
         this.maxChunkSize = maxChunkSize;
         this.maxLineLength = maxLineLength;
      }
   }

   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      switch((StompSubframeDecoder.State)this.state()) {
      case SKIP_CONTROL_CHARACTERS:
         skipControlCharacters(in);
         this.checkpoint(StompSubframeDecoder.State.READ_HEADERS);
      case READ_HEADERS:
         StompCommand command = StompCommand.UNKNOWN;
         DefaultStompHeadersSubframe frame = null;

         try {
            command = this.readCommand(in);
            frame = new DefaultStompHeadersSubframe(command);
            this.checkpoint(this.readHeaders(in, frame.headers()));
            out.add(frame);
         } catch (Exception var9) {
            if (frame == null) {
               frame = new DefaultStompHeadersSubframe(command);
            }

            frame.setDecoderResult(DecoderResult.failure(var9));
            out.add(frame);
            this.checkpoint(StompSubframeDecoder.State.BAD_FRAME);
            return;
         }
      default:
         try {
            switch((StompSubframeDecoder.State)this.state()) {
            case READ_CONTENT:
               int toRead = in.readableBytes();
               if (toRead == 0) {
                  return;
               }

               if (toRead > this.maxChunkSize) {
                  toRead = this.maxChunkSize;
               }

               int remainingLength = (int)(this.contentLength - (long)this.alreadyReadChunkSize);
               if (toRead > remainingLength) {
                  toRead = remainingLength;
               }

               ByteBuf chunkBuffer = ByteBufUtil.readBytes(ctx.alloc(), in, toRead);
               if ((long)(this.alreadyReadChunkSize += toRead) >= this.contentLength) {
                  this.lastContent = new DefaultLastStompContentSubframe(chunkBuffer);
                  this.checkpoint(StompSubframeDecoder.State.FINALIZE_FRAME_READ);
               } else {
                  DefaultStompContentSubframe chunk = new DefaultStompContentSubframe(chunkBuffer);
                  out.add(chunk);
               }

               if ((long)this.alreadyReadChunkSize < this.contentLength) {
                  return;
               }
            case FINALIZE_FRAME_READ:
               skipNullCharacter(in);
               if (this.lastContent == null) {
                  this.lastContent = LastStompContentSubframe.EMPTY_LAST_CONTENT;
               }

               out.add(this.lastContent);
               this.resetDecoder();
            }
         } catch (Exception var8) {
            StompContentSubframe errorContent = new DefaultLastStompContentSubframe(Unpooled.EMPTY_BUFFER);
            errorContent.setDecoderResult(DecoderResult.failure(var8));
            out.add(errorContent);
            this.checkpoint(StompSubframeDecoder.State.BAD_FRAME);
         }

         return;
      case BAD_FRAME:
         in.skipBytes(this.actualReadableBytes());
      }
   }

   private StompCommand readCommand(ByteBuf in) {
      String commandStr = readLine(in, this.maxLineLength);
      StompCommand command = null;

      try {
         command = StompCommand.valueOf(commandStr);
      } catch (IllegalArgumentException var6) {
      }

      if (command == null) {
         commandStr = commandStr.toUpperCase(Locale.US);

         try {
            command = StompCommand.valueOf(commandStr);
         } catch (IllegalArgumentException var5) {
         }
      }

      if (command == null) {
         throw new DecoderException("failed to read command from channel");
      } else {
         return command;
      }
   }

   private StompSubframeDecoder.State readHeaders(ByteBuf buffer, StompHeaders headers) {
      while(true) {
         String line = readLine(buffer, this.maxLineLength);
         if (line.isEmpty()) {
            long contentLength = -1L;
            if (headers.contains(StompHeaders.CONTENT_LENGTH)) {
               contentLength = getContentLength(headers, 0L);
            } else {
               int globalIndex = ByteBufUtil.indexOf(buffer, buffer.readerIndex(), buffer.writerIndex(), (byte)0);
               if (globalIndex != -1) {
                  contentLength = (long)(globalIndex - buffer.readerIndex());
               }
            }

            if (contentLength > 0L) {
               this.contentLength = contentLength;
               return StompSubframeDecoder.State.READ_CONTENT;
            }

            return StompSubframeDecoder.State.FINALIZE_FRAME_READ;
         }

         String[] split = StringUtil.split(line, ':');
         if (split.length == 2) {
            headers.add(split[0], (CharSequence)split[1]);
         }
      }
   }

   private static long getContentLength(StompHeaders headers, long defaultValue) {
      return headers.getLong(StompHeaders.CONTENT_LENGTH, defaultValue);
   }

   private static void skipNullCharacter(ByteBuf buffer) {
      byte b = buffer.readByte();
      if (b != 0) {
         throw new IllegalStateException("unexpected byte in buffer " + b + " while expecting NULL byte");
      }
   }

   private static void skipControlCharacters(ByteBuf buffer) {
      byte b;
      do {
         b = buffer.readByte();
      } while(b == 13 || b == 10);

      buffer.readerIndex(buffer.readerIndex() - 1);
   }

   private static String readLine(ByteBuf buffer, int maxLineLength) {
      AppendableCharSequence buf = new AppendableCharSequence(128);
      int lineLength = 0;

      byte nextByte;
      do {
         while(true) {
            nextByte = buffer.readByte();
            if (nextByte == 13) {
               nextByte = buffer.readByte();
               break;
            }

            if (nextByte == 10) {
               return buf.toString();
            }

            if (lineLength >= maxLineLength) {
               throw new TooLongFrameException("An STOMP line is larger than " + maxLineLength + " bytes.");
            }

            ++lineLength;
            buf.append((char)nextByte);
         }
      } while(nextByte != 10);

      return buf.toString();
   }

   private void resetDecoder() {
      this.checkpoint(StompSubframeDecoder.State.SKIP_CONTROL_CHARACTERS);
      this.contentLength = 0L;
      this.alreadyReadChunkSize = 0;
      this.lastContent = null;
   }

   static enum State {
      SKIP_CONTROL_CHARACTERS,
      READ_HEADERS,
      READ_CONTENT,
      FINALIZE_FRAME_READ,
      BAD_FRAME,
      INVALID_CHUNK;
   }
}
