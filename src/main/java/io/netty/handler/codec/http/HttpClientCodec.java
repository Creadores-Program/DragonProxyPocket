package io.netty.handler.codec.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAppender;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.PrematureChannelClosureException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public final class HttpClientCodec extends ChannelHandlerAppender implements HttpClientUpgradeHandler.SourceCodec {
   private final Queue<HttpMethod> queue;
   private boolean done;
   private final AtomicLong requestResponseCounter;
   private final boolean failOnMissingResponse;

   public HttpClientCodec() {
      this(4096, 8192, 8192, false);
   }

   public HttpClientCodec(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize) {
      this(maxInitialLineLength, maxHeaderSize, maxChunkSize, false);
   }

   public HttpClientCodec(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean failOnMissingResponse) {
      this(maxInitialLineLength, maxHeaderSize, maxChunkSize, failOnMissingResponse, true);
   }

   public HttpClientCodec(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean failOnMissingResponse, boolean validateHeaders) {
      this.queue = new ArrayDeque();
      this.requestResponseCounter = new AtomicLong();
      this.add(new HttpClientCodec.Decoder(maxInitialLineLength, maxHeaderSize, maxChunkSize, validateHeaders));
      this.add(new HttpClientCodec.Encoder());
      this.failOnMissingResponse = failOnMissingResponse;
   }

   public void upgradeFrom(ChannelHandlerContext ctx) {
      ctx.pipeline().remove(HttpClientCodec.Decoder.class);
      ctx.pipeline().remove(HttpClientCodec.Encoder.class);
   }

   public HttpRequestEncoder encoder() {
      return (HttpRequestEncoder)this.handlerAt(1);
   }

   public HttpResponseDecoder decoder() {
      return (HttpResponseDecoder)this.handlerAt(0);
   }

   public void setSingleDecode(boolean singleDecode) {
      this.decoder().setSingleDecode(singleDecode);
   }

   public boolean isSingleDecode() {
      return this.decoder().isSingleDecode();
   }

   private final class Decoder extends HttpResponseDecoder {
      Decoder(int maxInitialLineLength, int maxHeaderSize, int maxChunkSize, boolean validateHeaders) {
         super(maxInitialLineLength, maxHeaderSize, maxChunkSize, validateHeaders);
      }

      protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
         int oldSize;
         if (HttpClientCodec.this.done) {
            oldSize = this.actualReadableBytes();
            if (oldSize == 0) {
               return;
            }

            out.add(buffer.readBytes(oldSize));
         } else {
            oldSize = out.size();
            super.decode(ctx, buffer, out);
            if (HttpClientCodec.this.failOnMissingResponse) {
               int size = out.size();

               for(int i = oldSize; i < size; ++i) {
                  this.decrement(out.get(i));
               }
            }
         }

      }

      private void decrement(Object msg) {
         if (msg != null) {
            if (msg instanceof LastHttpContent) {
               HttpClientCodec.this.requestResponseCounter.decrementAndGet();
            }

         }
      }

      protected boolean isContentAlwaysEmpty(HttpMessage msg) {
         int statusCode = ((HttpResponse)msg).status().code();
         if (statusCode == 100) {
            return true;
         } else {
            HttpMethod method = (HttpMethod)HttpClientCodec.this.queue.poll();
            char firstChar = method.name().charAt(0);
            switch(firstChar) {
            case 'C':
               if (statusCode == 200 && HttpMethod.CONNECT.equals(method)) {
                  HttpClientCodec.this.done = true;
                  HttpClientCodec.this.queue.clear();
                  return true;
               }
               break;
            case 'H':
               if (HttpMethod.HEAD.equals(method)) {
                  return true;
               }
            }

            return super.isContentAlwaysEmpty(msg);
         }
      }

      public void channelInactive(ChannelHandlerContext ctx) throws Exception {
         super.channelInactive(ctx);
         if (HttpClientCodec.this.failOnMissingResponse) {
            long missingResponses = HttpClientCodec.this.requestResponseCounter.get();
            if (missingResponses > 0L) {
               ctx.fireExceptionCaught(new PrematureChannelClosureException("channel gone inactive with " + missingResponses + " missing response(s)"));
            }
         }

      }
   }

   private final class Encoder extends HttpRequestEncoder {
      private Encoder() {
      }

      protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {
         if (msg instanceof HttpRequest && !HttpClientCodec.this.done) {
            HttpClientCodec.this.queue.offer(((HttpRequest)msg).method());
         }

         super.encode(ctx, msg, out);
         if (HttpClientCodec.this.failOnMissingResponse && msg instanceof LastHttpContent) {
            HttpClientCodec.this.requestResponseCounter.incrementAndGet();
         }

      }

      // $FF: synthetic method
      Encoder(Object x1) {
         this();
      }
   }
}
