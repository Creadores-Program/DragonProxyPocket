package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.ReferenceCountUtil;
import java.util.List;

public abstract class MessageAggregator<I, S, C extends ByteBufHolder, O extends ByteBufHolder> extends MessageToMessageDecoder<I> {
   private static final int DEFAULT_MAX_COMPOSITEBUFFER_COMPONENTS = 1024;
   private final int maxContentLength;
   private O currentMessage;
   private boolean handlingOversizedMessage;
   private int maxCumulationBufferComponents = 1024;
   private ChannelHandlerContext ctx;
   private ChannelFutureListener continueResponseWriteListener;

   protected MessageAggregator(int maxContentLength) {
      validateMaxContentLength(maxContentLength);
      this.maxContentLength = maxContentLength;
   }

   protected MessageAggregator(int maxContentLength, Class<? extends I> inboundMessageType) {
      super(inboundMessageType);
      validateMaxContentLength(maxContentLength);
      this.maxContentLength = maxContentLength;
   }

   private static void validateMaxContentLength(int maxContentLength) {
      if (maxContentLength <= 0) {
         throw new IllegalArgumentException("maxContentLength must be a positive integer: " + maxContentLength);
      }
   }

   public boolean acceptInboundMessage(Object msg) throws Exception {
      if (!super.acceptInboundMessage(msg)) {
         return false;
      } else {
         return (this.isContentMessage(msg) || this.isStartMessage(msg)) && !this.isAggregated(msg);
      }
   }

   protected abstract boolean isStartMessage(I var1) throws Exception;

   protected abstract boolean isContentMessage(I var1) throws Exception;

   protected abstract boolean isLastContentMessage(C var1) throws Exception;

   protected abstract boolean isAggregated(I var1) throws Exception;

   public final int maxContentLength() {
      return this.maxContentLength;
   }

   public final int maxCumulationBufferComponents() {
      return this.maxCumulationBufferComponents;
   }

   public final void setMaxCumulationBufferComponents(int maxCumulationBufferComponents) {
      if (maxCumulationBufferComponents < 2) {
         throw new IllegalArgumentException("maxCumulationBufferComponents: " + maxCumulationBufferComponents + " (expected: >= 2)");
      } else if (this.ctx == null) {
         this.maxCumulationBufferComponents = maxCumulationBufferComponents;
      } else {
         throw new IllegalStateException("decoder properties cannot be changed once the decoder is added to a pipeline.");
      }
   }

   public final boolean isHandlingOversizedMessage() {
      return this.handlingOversizedMessage;
   }

   protected final ChannelHandlerContext ctx() {
      if (this.ctx == null) {
         throw new IllegalStateException("not added to a pipeline yet");
      } else {
         return this.ctx;
      }
   }

   protected void decode(final ChannelHandlerContext ctx, I msg, List<Object> out) throws Exception {
      O currentMessage = this.currentMessage;
      if (this.isStartMessage(msg)) {
         this.handlingOversizedMessage = false;
         if (currentMessage != null) {
            throw new MessageAggregationException();
         }

         if (this.hasContentLength(msg) && this.contentLength(msg) > (long)this.maxContentLength) {
            this.invokeHandleOversizedMessage(ctx, msg);
            return;
         }

         Object continueResponse = this.newContinueResponse(msg);
         if (continueResponse != null) {
            ChannelFutureListener listener = this.continueResponseWriteListener;
            if (listener == null) {
               this.continueResponseWriteListener = listener = new ChannelFutureListener() {
                  public void operationComplete(ChannelFuture future) throws Exception {
                     if (!future.isSuccess()) {
                        ctx.fireExceptionCaught(future.cause());
                     }

                  }
               };
            }

            ctx.writeAndFlush(continueResponse).addListener(listener);
         }

         if (msg instanceof DecoderResultProvider && !((DecoderResultProvider)msg).decoderResult().isSuccess()) {
            ByteBufHolder aggregated;
            if (msg instanceof ByteBufHolder && ((ByteBufHolder)msg).content().isReadable()) {
               aggregated = this.beginAggregation(msg, ((ByteBufHolder)msg).content().retain());
            } else {
               aggregated = this.beginAggregation(msg, Unpooled.EMPTY_BUFFER);
            }

            this.finishAggregation(aggregated);
            out.add(aggregated);
            this.currentMessage = null;
            return;
         }

         CompositeByteBuf content = ctx.alloc().compositeBuffer(this.maxCumulationBufferComponents);
         if (msg instanceof ByteBufHolder) {
            appendPartialContent(content, ((ByteBufHolder)msg).content());
         }

         this.currentMessage = this.beginAggregation(msg, content);
      } else {
         if (!this.isContentMessage(msg)) {
            throw new MessageAggregationException();
         }

         C m = (ByteBufHolder)msg;
         ByteBuf partialContent = ((ByteBufHolder)msg).content();
         boolean isLastContentMessage = this.isLastContentMessage(m);
         if (this.handlingOversizedMessage) {
            if (isLastContentMessage) {
               this.currentMessage = null;
            }

            return;
         }

         if (currentMessage == null) {
            throw new MessageAggregationException();
         }

         CompositeByteBuf content = (CompositeByteBuf)currentMessage.content();
         if (content.readableBytes() > this.maxContentLength - partialContent.readableBytes()) {
            this.invokeHandleOversizedMessage(ctx, currentMessage);
            return;
         }

         appendPartialContent(content, partialContent);
         this.aggregate(currentMessage, m);
         boolean last;
         if (m instanceof DecoderResultProvider) {
            DecoderResult decoderResult = ((DecoderResultProvider)m).decoderResult();
            if (!decoderResult.isSuccess()) {
               if (currentMessage instanceof DecoderResultProvider) {
                  ((DecoderResultProvider)currentMessage).setDecoderResult(DecoderResult.failure(decoderResult.cause()));
               }

               last = true;
            } else {
               last = isLastContentMessage;
            }
         } else {
            last = isLastContentMessage;
         }

         if (last) {
            this.finishAggregation(currentMessage);
            out.add(currentMessage);
            this.currentMessage = null;
         }
      }

   }

   private static void appendPartialContent(CompositeByteBuf content, ByteBuf partialContent) {
      if (partialContent.isReadable()) {
         partialContent.retain();
         content.addComponent(partialContent);
         content.writerIndex(content.writerIndex() + partialContent.readableBytes());
      }

   }

   protected abstract boolean hasContentLength(S var1) throws Exception;

   protected abstract long contentLength(S var1) throws Exception;

   protected abstract Object newContinueResponse(S var1) throws Exception;

   protected abstract O beginAggregation(S var1, ByteBuf var2) throws Exception;

   protected void aggregate(O aggregated, C content) throws Exception {
   }

   protected void finishAggregation(O aggregated) throws Exception {
   }

   private void invokeHandleOversizedMessage(ChannelHandlerContext ctx, S oversized) throws Exception {
      this.handlingOversizedMessage = true;
      this.currentMessage = null;

      try {
         this.handleOversizedMessage(ctx, oversized);
      } finally {
         ReferenceCountUtil.release(oversized);
      }

   }

   protected void handleOversizedMessage(ChannelHandlerContext ctx, S oversized) throws Exception {
      ctx.fireExceptionCaught(new TooLongFrameException("content length exceeded " + this.maxContentLength() + " bytes."));
   }

   public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      if (this.currentMessage != null) {
         this.currentMessage.release();
         this.currentMessage = null;
      }

      super.channelInactive(ctx);
   }

   public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
      this.ctx = ctx;
   }

   public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
      super.handlerRemoved(ctx);
      if (this.currentMessage != null) {
         this.currentMessage.release();
         this.currentMessage = null;
      }

   }
}
