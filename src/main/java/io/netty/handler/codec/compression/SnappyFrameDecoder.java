package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.Arrays;
import java.util.List;

public class SnappyFrameDecoder extends ByteToMessageDecoder {
   private static final byte[] SNAPPY = new byte[]{115, 78, 97, 80, 112, 89};
   private static final int MAX_UNCOMPRESSED_DATA_SIZE = 65540;
   private final Snappy snappy;
   private final boolean validateChecksums;
   private boolean started;
   private boolean corrupted;

   public SnappyFrameDecoder() {
      this(false);
   }

   public SnappyFrameDecoder(boolean validateChecksums) {
      this.snappy = new Snappy();
      this.validateChecksums = validateChecksums;
   }

   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      if (this.corrupted) {
         in.skipBytes(in.readableBytes());
      } else {
         try {
            int idx = in.readerIndex();
            int inSize = in.readableBytes();
            if (inSize >= 4) {
               int chunkTypeVal = in.getUnsignedByte(idx);
               SnappyFrameDecoder.ChunkType chunkType = mapChunkType((byte)chunkTypeVal);
               int chunkLength = ByteBufUtil.swapMedium(in.getUnsignedMedium(idx + 1));
               int checksum;
               switch(chunkType) {
               case STREAM_IDENTIFIER:
                  if (chunkLength != SNAPPY.length) {
                     throw new DecompressionException("Unexpected length of stream identifier: " + chunkLength);
                  }

                  if (inSize >= 4 + SNAPPY.length) {
                     byte[] identifier = new byte[chunkLength];
                     in.skipBytes(4).readBytes(identifier);
                     if (!Arrays.equals(identifier, SNAPPY)) {
                        throw new DecompressionException("Unexpected stream identifier contents. Mismatched snappy protocol version?");
                     }

                     this.started = true;
                  }
                  break;
               case RESERVED_SKIPPABLE:
                  if (!this.started) {
                     throw new DecompressionException("Received RESERVED_SKIPPABLE tag before STREAM_IDENTIFIER");
                  }

                  if (inSize < 4 + chunkLength) {
                     return;
                  }

                  in.skipBytes(4 + chunkLength);
                  break;
               case RESERVED_UNSKIPPABLE:
                  throw new DecompressionException("Found reserved unskippable chunk type: 0x" + Integer.toHexString(chunkTypeVal));
               case UNCOMPRESSED_DATA:
                  if (!this.started) {
                     throw new DecompressionException("Received UNCOMPRESSED_DATA tag before STREAM_IDENTIFIER");
                  }

                  if (chunkLength > 65540) {
                     throw new DecompressionException("Received UNCOMPRESSED_DATA larger than 65540 bytes");
                  }

                  if (inSize < 4 + chunkLength) {
                     return;
                  }

                  in.skipBytes(4);
                  if (this.validateChecksums) {
                     checksum = ByteBufUtil.swapInt(in.readInt());
                     Snappy.validateChecksum(checksum, in, in.readerIndex(), chunkLength - 4);
                  } else {
                     in.skipBytes(4);
                  }

                  out.add(in.readSlice(chunkLength - 4).retain());
                  break;
               case COMPRESSED_DATA:
                  if (!this.started) {
                     throw new DecompressionException("Received COMPRESSED_DATA tag before STREAM_IDENTIFIER");
                  }

                  if (inSize < 4 + chunkLength) {
                     return;
                  }

                  in.skipBytes(4);
                  checksum = ByteBufUtil.swapInt(in.readInt());
                  ByteBuf uncompressed = ctx.alloc().buffer(0);
                  if (this.validateChecksums) {
                     int oldWriterIndex = in.writerIndex();

                     try {
                        in.writerIndex(in.readerIndex() + chunkLength - 4);
                        this.snappy.decode(in, uncompressed);
                     } finally {
                        in.writerIndex(oldWriterIndex);
                     }

                     Snappy.validateChecksum(checksum, uncompressed, 0, uncompressed.writerIndex());
                  } else {
                     this.snappy.decode(in.readSlice(chunkLength - 4), uncompressed);
                  }

                  out.add(uncompressed);
                  this.snappy.reset();
               }

            }
         } catch (Exception var17) {
            this.corrupted = true;
            throw var17;
         }
      }
   }

   private static SnappyFrameDecoder.ChunkType mapChunkType(byte type) {
      if (type == 0) {
         return SnappyFrameDecoder.ChunkType.COMPRESSED_DATA;
      } else if (type == 1) {
         return SnappyFrameDecoder.ChunkType.UNCOMPRESSED_DATA;
      } else if (type == -1) {
         return SnappyFrameDecoder.ChunkType.STREAM_IDENTIFIER;
      } else {
         return (type & 128) == 128 ? SnappyFrameDecoder.ChunkType.RESERVED_SKIPPABLE : SnappyFrameDecoder.ChunkType.RESERVED_UNSKIPPABLE;
      }
   }

   private static enum ChunkType {
      STREAM_IDENTIFIER,
      COMPRESSED_DATA,
      UNCOMPRESSED_DATA,
      RESERVED_UNSKIPPABLE,
      RESERVED_SKIPPABLE;
   }
}
