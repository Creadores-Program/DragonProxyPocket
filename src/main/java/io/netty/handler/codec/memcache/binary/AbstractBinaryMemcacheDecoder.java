package io.netty.handler.codec.memcache.binary;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.memcache.AbstractMemcacheObjectDecoder;
import io.netty.handler.codec.memcache.DefaultLastMemcacheContent;
import io.netty.handler.codec.memcache.DefaultMemcacheContent;
import io.netty.handler.codec.memcache.LastMemcacheContent;
import io.netty.handler.codec.memcache.MemcacheContent;
import io.netty.util.CharsetUtil;
import java.util.List;

public abstract class AbstractBinaryMemcacheDecoder<M extends BinaryMemcacheMessage> extends AbstractMemcacheObjectDecoder {
   public static final int DEFAULT_MAX_CHUNK_SIZE = 8192;
   private final int chunkSize;
   private M currentMessage;
   private int alreadyReadChunkSize;
   private AbstractBinaryMemcacheDecoder.State state;

   protected AbstractBinaryMemcacheDecoder() {
      this(8192);
   }

   protected AbstractBinaryMemcacheDecoder(int chunkSize) {
      this.state = AbstractBinaryMemcacheDecoder.State.READ_HEADER;
      if (chunkSize < 0) {
         throw new IllegalArgumentException("chunkSize must be a positive integer: " + chunkSize);
      } else {
         this.chunkSize = chunkSize;
      }
   }

   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      switch(this.state) {
      case READ_HEADER:
         try {
            if (in.readableBytes() < 24) {
               return;
            } else {
               this.resetDecoder();
               this.currentMessage = this.decodeHeader(in);
               this.state = AbstractBinaryMemcacheDecoder.State.READ_EXTRAS;
            }
         } catch (Exception var12) {
            out.add(this.invalidMessage(var12));
            return;
         }
      case READ_EXTRAS:
         try {
            byte extrasLength = this.currentMessage.extrasLength();
            if (extrasLength > 0) {
               if (in.readableBytes() < extrasLength) {
                  return;
               }

               this.currentMessage.setExtras(ByteBufUtil.readBytes(ctx.alloc(), in, extrasLength));
            }

            this.state = AbstractBinaryMemcacheDecoder.State.READ_KEY;
         } catch (Exception var11) {
            out.add(this.invalidMessage(var11));
            return;
         }
      case READ_KEY:
         try {
            short keyLength = this.currentMessage.keyLength();
            if (keyLength > 0) {
               if (in.readableBytes() < keyLength) {
                  return;
               }

               this.currentMessage.setKey(in.toString(in.readerIndex(), keyLength, CharsetUtil.UTF_8));
               in.skipBytes(keyLength);
            }

            out.add(this.currentMessage);
            this.state = AbstractBinaryMemcacheDecoder.State.READ_CONTENT;
         } catch (Exception var10) {
            out.add(this.invalidMessage(var10));
            return;
         }
      case READ_CONTENT:
         try {
            int valueLength = this.currentMessage.totalBodyLength() - this.currentMessage.keyLength() - this.currentMessage.extrasLength();
            int toRead = in.readableBytes();
            if (valueLength > 0) {
               if (toRead == 0) {
                  return;
               }

               if (toRead > this.chunkSize) {
                  toRead = this.chunkSize;
               }

               int remainingLength = valueLength - this.alreadyReadChunkSize;
               if (toRead > remainingLength) {
                  toRead = remainingLength;
               }

               ByteBuf chunkBuffer = ByteBufUtil.readBytes(ctx.alloc(), in, toRead);
               Object chunk;
               if ((this.alreadyReadChunkSize += toRead) >= valueLength) {
                  chunk = new DefaultLastMemcacheContent(chunkBuffer);
               } else {
                  chunk = new DefaultMemcacheContent(chunkBuffer);
               }

               out.add(chunk);
               if (this.alreadyReadChunkSize < valueLength) {
                  return;
               }
            } else {
               out.add(LastMemcacheContent.EMPTY_LAST_CONTENT);
            }

            this.state = AbstractBinaryMemcacheDecoder.State.READ_HEADER;
            return;
         } catch (Exception var9) {
            out.add(this.invalidChunk(var9));
            return;
         }
      case BAD_MESSAGE:
         in.skipBytes(this.actualReadableBytes());
         return;
      default:
         throw new Error("Unknown state reached: " + this.state);
      }
   }

   private M invalidMessage(Exception cause) {
      this.state = AbstractBinaryMemcacheDecoder.State.BAD_MESSAGE;
      M message = this.buildInvalidMessage();
      message.setDecoderResult(DecoderResult.failure(cause));
      return message;
   }

   private MemcacheContent invalidChunk(Exception cause) {
      this.state = AbstractBinaryMemcacheDecoder.State.BAD_MESSAGE;
      MemcacheContent chunk = new DefaultLastMemcacheContent(Unpooled.EMPTY_BUFFER);
      chunk.setDecoderResult(DecoderResult.failure(cause));
      return chunk;
   }

   public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      super.channelInactive(ctx);
      if (this.currentMessage != null) {
         this.currentMessage.release();
      }

      this.resetDecoder();
   }

   protected void resetDecoder() {
      this.currentMessage = null;
      this.alreadyReadChunkSize = 0;
   }

   protected abstract M decodeHeader(ByteBuf var1);

   protected abstract M buildInvalidMessage();

   static enum State {
      READ_HEADER,
      READ_EXTRAS,
      READ_KEY,
      READ_CONTENT,
      BAD_MESSAGE;
   }
}
