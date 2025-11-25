package io.netty.handler.codec.http2;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelPromiseAggregator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.AsciiString;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.util.concurrent.Promise;

public class CompressorHttp2ConnectionEncoder extends DefaultHttp2ConnectionEncoder {
   private static final Http2ConnectionAdapter CLEAN_UP_LISTENER = new Http2ConnectionAdapter() {
      public void streamRemoved(Http2Stream stream) {
         EmbeddedChannel compressor = (EmbeddedChannel)stream.getProperty(CompressorHttp2ConnectionEncoder.class);
         if (compressor != null) {
            CompressorHttp2ConnectionEncoder.cleanup(stream, compressor);
         }

      }
   };
   private final int compressionLevel;
   private final int windowBits;
   private final int memLevel;

   protected CompressorHttp2ConnectionEncoder(CompressorHttp2ConnectionEncoder.Builder builder) {
      super(builder);
      if (builder.compressionLevel >= 0 && builder.compressionLevel <= 9) {
         if (builder.windowBits >= 9 && builder.windowBits <= 15) {
            if (builder.memLevel >= 1 && builder.memLevel <= 9) {
               this.compressionLevel = builder.compressionLevel;
               this.windowBits = builder.windowBits;
               this.memLevel = builder.memLevel;
               this.connection().addListener(CLEAN_UP_LISTENER);
            } else {
               throw new IllegalArgumentException("memLevel: " + builder.memLevel + " (expected: 1-9)");
            }
         } else {
            throw new IllegalArgumentException("windowBits: " + builder.windowBits + " (expected: 9-15)");
         }
      } else {
         throw new IllegalArgumentException("compressionLevel: " + builder.compressionLevel + " (expected: 0-9)");
      }
   }

   public ChannelFuture writeData(ChannelHandlerContext ctx, int streamId, ByteBuf data, int padding, boolean endOfStream, ChannelPromise promise) {
      Http2Stream stream = this.connection().stream(streamId);
      EmbeddedChannel channel = stream == null ? null : (EmbeddedChannel)stream.getProperty(CompressorHttp2ConnectionEncoder.class);
      if (channel == null) {
         return super.writeData(ctx, streamId, data, padding, endOfStream, promise);
      } else {
         try {
            channel.writeOutbound(data);
            ByteBuf buf = nextReadableBuf(channel);
            if (buf == null) {
               if (!endOfStream) {
                  promise.setSuccess();
                  ChannelPromise var19 = promise;
                  return var19;
               } else {
                  if (channel.finish()) {
                     buf = nextReadableBuf(channel);
                  }

                  ChannelFuture var18 = super.writeData(ctx, streamId, buf == null ? Unpooled.EMPTY_BUFFER : buf, padding, true, promise);
                  return var18;
               }
            } else {
               ChannelPromiseAggregator aggregator = new ChannelPromiseAggregator(promise);
               ChannelPromise bufPromise = ctx.newPromise();
               aggregator.add(new Promise[]{bufPromise});

               while(true) {
                  ByteBuf nextBuf = nextReadableBuf(channel);
                  boolean compressedEndOfStream = nextBuf == null && endOfStream;
                  if (compressedEndOfStream && channel.finish()) {
                     nextBuf = nextReadableBuf(channel);
                     compressedEndOfStream = nextBuf == null;
                  }

                  ChannelPromise nextPromise;
                  if (nextBuf != null) {
                     nextPromise = ctx.newPromise();
                     aggregator.add(new Promise[]{nextPromise});
                  } else {
                     nextPromise = null;
                  }

                  super.writeData(ctx, streamId, buf, padding, compressedEndOfStream, bufPromise);
                  if (nextBuf == null) {
                     ChannelPromise var20 = promise;
                     return var20;
                  }

                  padding = 0;
                  buf = nextBuf;
                  bufPromise = nextPromise;
               }
            }
         } finally {
            if (endOfStream) {
               cleanup(stream, channel);
            }

         }
      }
   }

   public ChannelFuture writeHeaders(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int padding, boolean endStream, ChannelPromise promise) {
      this.initCompressor(streamId, headers, endStream);
      return super.writeHeaders(ctx, streamId, headers, padding, endStream, promise);
   }

   public ChannelFuture writeHeaders(ChannelHandlerContext ctx, int streamId, Http2Headers headers, int streamDependency, short weight, boolean exclusive, int padding, boolean endOfStream, ChannelPromise promise) {
      this.initCompressor(streamId, headers, endOfStream);
      return super.writeHeaders(ctx, streamId, headers, streamDependency, weight, exclusive, padding, endOfStream, promise);
   }

   protected EmbeddedChannel newContentCompressor(AsciiString contentEncoding) throws Http2Exception {
      if (!HttpHeaderValues.GZIP.equalsIgnoreCase(contentEncoding) && !HttpHeaderValues.X_GZIP.equalsIgnoreCase(contentEncoding)) {
         return !HttpHeaderValues.DEFLATE.equalsIgnoreCase(contentEncoding) && !HttpHeaderValues.X_DEFLATE.equalsIgnoreCase(contentEncoding) ? null : this.newCompressionChannel(ZlibWrapper.ZLIB);
      } else {
         return this.newCompressionChannel(ZlibWrapper.GZIP);
      }
   }

   protected AsciiString getTargetContentEncoding(AsciiString contentEncoding) throws Http2Exception {
      return contentEncoding;
   }

   private EmbeddedChannel newCompressionChannel(ZlibWrapper wrapper) {
      return new EmbeddedChannel(new ChannelHandler[]{ZlibCodecFactory.newZlibEncoder(wrapper, this.compressionLevel, this.windowBits, this.memLevel)});
   }

   private void initCompressor(int streamId, Http2Headers headers, boolean endOfStream) {
      Http2Stream stream = this.connection().stream(streamId);
      if (stream != null) {
         EmbeddedChannel compressor = (EmbeddedChannel)stream.getProperty(CompressorHttp2ConnectionEncoder.class);
         if (compressor == null) {
            if (!endOfStream) {
               AsciiString encoding = (AsciiString)headers.get(HttpHeaderNames.CONTENT_ENCODING);
               if (encoding == null) {
                  encoding = HttpHeaderValues.IDENTITY;
               }

               try {
                  compressor = this.newContentCompressor(encoding);
                  if (compressor != null) {
                     stream.setProperty(CompressorHttp2ConnectionEncoder.class, compressor);
                     AsciiString targetContentEncoding = this.getTargetContentEncoding(encoding);
                     if (HttpHeaderValues.IDENTITY.equalsIgnoreCase(targetContentEncoding)) {
                        headers.remove(HttpHeaderNames.CONTENT_ENCODING);
                     } else {
                        headers.set(HttpHeaderNames.CONTENT_ENCODING, targetContentEncoding);
                     }
                  }
               } catch (Throwable var8) {
               }
            }
         } else if (endOfStream) {
            cleanup(stream, compressor);
         }

         if (compressor != null) {
            headers.remove(HttpHeaderNames.CONTENT_LENGTH);
         }

      }
   }

   private static void cleanup(Http2Stream stream, EmbeddedChannel compressor) {
      if (compressor.finish()) {
         while(true) {
            ByteBuf buf = (ByteBuf)compressor.readOutbound();
            if (buf == null) {
               break;
            }

            buf.release();
         }
      }

      stream.removeProperty(CompressorHttp2ConnectionEncoder.class);
   }

   private static ByteBuf nextReadableBuf(EmbeddedChannel compressor) {
      while(true) {
         ByteBuf buf = (ByteBuf)compressor.readOutbound();
         if (buf == null) {
            return null;
         }

         if (buf.isReadable()) {
            return buf;
         }

         buf.release();
      }
   }

   public static class Builder extends DefaultHttp2ConnectionEncoder.Builder {
      protected int compressionLevel = 6;
      protected int windowBits = 15;
      protected int memLevel = 8;

      public CompressorHttp2ConnectionEncoder.Builder compressionLevel(int compressionLevel) {
         this.compressionLevel = compressionLevel;
         return this;
      }

      public CompressorHttp2ConnectionEncoder.Builder windowBits(int windowBits) {
         this.windowBits = windowBits;
         return this;
      }

      public CompressorHttp2ConnectionEncoder.Builder memLevel(int memLevel) {
         this.memLevel = memLevel;
         return this;
      }

      public CompressorHttp2ConnectionEncoder build() {
         return new CompressorHttp2ConnectionEncoder(this);
      }
   }
}
