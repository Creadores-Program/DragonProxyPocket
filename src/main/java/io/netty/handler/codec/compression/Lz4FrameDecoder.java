package io.netty.handler.codec.compression;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import java.util.zip.Checksum;
import net.jpountz.lz4.LZ4Exception;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import net.jpountz.xxhash.XXHashFactory;

public class Lz4FrameDecoder extends ByteToMessageDecoder {
   private Lz4FrameDecoder.State currentState;
   private LZ4FastDecompressor decompressor;
   private Checksum checksum;
   private int blockType;
   private int compressedLength;
   private int decompressedLength;
   private int currentChecksum;

   public Lz4FrameDecoder() {
      this(false);
   }

   public Lz4FrameDecoder(boolean validateChecksums) {
      this(LZ4Factory.fastestInstance(), validateChecksums);
   }

   public Lz4FrameDecoder(LZ4Factory factory, boolean validateChecksums) {
      this(factory, validateChecksums ? XXHashFactory.fastestInstance().newStreamingHash32(-1756908916).asChecksum() : null);
   }

   public Lz4FrameDecoder(LZ4Factory factory, Checksum checksum) {
      this.currentState = Lz4FrameDecoder.State.INIT_BLOCK;
      if (factory == null) {
         throw new NullPointerException("factory");
      } else {
         this.decompressor = factory.fastDecompressor();
         this.checksum = checksum;
      }
   }

   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      try {
         int blockType;
         int compressedLength;
         int decompressedLength;
         int currentChecksum;
         switch(this.currentState) {
         case INIT_BLOCK:
            if (in.readableBytes() < 21) {
               break;
            }

            long magic = in.readLong();
            if (magic != 5501767354678207339L) {
               throw new DecompressionException("unexpected block identifier");
            }

            int token = in.readByte();
            int compressionLevel = (token & 15) + 10;
            blockType = token & 240;
            compressedLength = Integer.reverseBytes(in.readInt());
            if (compressedLength < 0 || compressedLength > 33554432) {
               throw new DecompressionException(String.format("invalid compressedLength: %d (expected: 0-%d)", compressedLength, 33554432));
            }

            decompressedLength = Integer.reverseBytes(in.readInt());
            int maxDecompressedLength = 1 << compressionLevel;
            if (decompressedLength < 0 || decompressedLength > maxDecompressedLength) {
               throw new DecompressionException(String.format("invalid decompressedLength: %d (expected: 0-%d)", decompressedLength, maxDecompressedLength));
            }

            if (decompressedLength == 0 && compressedLength != 0 || decompressedLength != 0 && compressedLength == 0 || blockType == 16 && decompressedLength != compressedLength) {
               throw new DecompressionException(String.format("stream corrupted: compressedLength(%d) and decompressedLength(%d) mismatch", compressedLength, decompressedLength));
            }

            currentChecksum = Integer.reverseBytes(in.readInt());
            if (decompressedLength == 0 && compressedLength == 0) {
               if (currentChecksum != 0) {
                  throw new DecompressionException("stream corrupted: checksum error");
               }

               this.currentState = Lz4FrameDecoder.State.FINISHED;
               this.decompressor = null;
               this.checksum = null;
               break;
            } else {
               this.blockType = blockType;
               this.compressedLength = compressedLength;
               this.decompressedLength = decompressedLength;
               this.currentChecksum = currentChecksum;
               this.currentState = Lz4FrameDecoder.State.DECOMPRESS_DATA;
            }
         case DECOMPRESS_DATA:
            blockType = this.blockType;
            compressedLength = this.compressedLength;
            decompressedLength = this.decompressedLength;
            currentChecksum = this.currentChecksum;
            if (in.readableBytes() >= compressedLength) {
               int idx = in.readerIndex();
               ByteBuf uncompressed = ctx.alloc().heapBuffer(decompressedLength, decompressedLength);
               byte[] dest = uncompressed.array();
               int destOff = uncompressed.arrayOffset() + uncompressed.writerIndex();
               boolean success = false;

               try {
                  int checksumResult;
                  switch(blockType) {
                  case 16:
                     in.getBytes(idx, dest, destOff, decompressedLength);
                     break;
                  case 32:
                     byte[] src;
                     if (in.hasArray()) {
                        src = in.array();
                        checksumResult = in.arrayOffset() + idx;
                     } else {
                        src = new byte[compressedLength];
                        in.getBytes(idx, src);
                        checksumResult = 0;
                     }

                     try {
                        int readBytes = this.decompressor.decompress(src, checksumResult, dest, destOff, decompressedLength);
                        if (compressedLength != readBytes) {
                           throw new DecompressionException(String.format("stream corrupted: compressedLength(%d) and actual length(%d) mismatch", compressedLength, readBytes));
                        }
                        break;
                     } catch (LZ4Exception var25) {
                        throw new DecompressionException(var25);
                     }
                  default:
                     throw new DecompressionException(String.format("unexpected blockType: %d (expected: %d or %d)", blockType, 16, 32));
                  }

                  Checksum checksum = this.checksum;
                  if (checksum != null) {
                     checksum.reset();
                     checksum.update(dest, destOff, decompressedLength);
                     checksumResult = (int)checksum.getValue();
                     if (checksumResult != currentChecksum) {
                        throw new DecompressionException(String.format("stream corrupted: mismatching checksum: %d (expected: %d)", checksumResult, currentChecksum));
                     }
                  }

                  uncompressed.writerIndex(uncompressed.writerIndex() + decompressedLength);
                  out.add(uncompressed);
                  in.skipBytes(compressedLength);
                  this.currentState = Lz4FrameDecoder.State.INIT_BLOCK;
                  success = true;
               } finally {
                  if (!success) {
                     uncompressed.release();
                  }

               }
            }
            break;
         case FINISHED:
         case CORRUPTED:
            in.skipBytes(in.readableBytes());
            break;
         default:
            throw new IllegalStateException();
         }

      } catch (Exception var27) {
         this.currentState = Lz4FrameDecoder.State.CORRUPTED;
         throw var27;
      }
   }

   public boolean isClosed() {
      return this.currentState == Lz4FrameDecoder.State.FINISHED;
   }

   private static enum State {
      INIT_BLOCK,
      DECOMPRESS_DATA,
      FINISHED,
      CORRUPTED;
   }
}
