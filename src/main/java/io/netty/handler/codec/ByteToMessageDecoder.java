package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.internal.RecyclableArrayList;
import io.netty.util.internal.StringUtil;
import java.util.List;

public abstract class ByteToMessageDecoder extends ChannelHandlerAdapter {
   public static final ByteToMessageDecoder.Cumulator MERGE_CUMULATOR = new ByteToMessageDecoder.Cumulator() {
      public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in) {
         ByteBuf buffer;
         if (cumulation.writerIndex() <= cumulation.maxCapacity() - in.readableBytes() && cumulation.refCnt() <= 1) {
            buffer = cumulation;
         } else {
            buffer = ByteToMessageDecoder.expandCumulation(alloc, cumulation, in.readableBytes());
         }

         buffer.writeBytes(in);
         in.release();
         return buffer;
      }
   };
   public static final ByteToMessageDecoder.Cumulator COMPOSITE_CUMULATOR = new ByteToMessageDecoder.Cumulator() {
      public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in) {
         Object buffer;
         if (cumulation.refCnt() > 1) {
            buffer = ByteToMessageDecoder.expandCumulation(alloc, cumulation, in.readableBytes());
            ((ByteBuf)buffer).writeBytes(in);
            in.release();
         } else {
            CompositeByteBuf composite;
            if (cumulation instanceof CompositeByteBuf) {
               composite = (CompositeByteBuf)cumulation;
            } else {
               int readable = cumulation.readableBytes();
               composite = alloc.compositeBuffer();
               composite.addComponent(cumulation).writerIndex(readable);
            }

            composite.addComponent(in).writerIndex(composite.writerIndex() + in.readableBytes());
            buffer = composite;
         }

         return (ByteBuf)buffer;
      }
   };
   ByteBuf cumulation;
   private ByteToMessageDecoder.Cumulator cumulator;
   private boolean singleDecode;
   private boolean first;

   protected ByteToMessageDecoder() {
      this.cumulator = MERGE_CUMULATOR;
      CodecUtil.ensureNotSharable(this);
   }

   public void setSingleDecode(boolean singleDecode) {
      this.singleDecode = singleDecode;
   }

   public boolean isSingleDecode() {
      return this.singleDecode;
   }

   public void setCumulator(ByteToMessageDecoder.Cumulator cumulator) {
      if (cumulator == null) {
         throw new NullPointerException("cumulator");
      } else {
         this.cumulator = cumulator;
      }
   }

   protected int actualReadableBytes() {
      return this.internalBuffer().readableBytes();
   }

   protected ByteBuf internalBuffer() {
      return this.cumulation != null ? this.cumulation : Unpooled.EMPTY_BUFFER;
   }

   public final void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
      ByteBuf buf = this.internalBuffer();
      int readable = buf.readableBytes();
      if (readable > 0) {
         ByteBuf bytes = buf.readBytes(readable);
         buf.release();
         ctx.fireChannelRead(bytes);
         ctx.fireChannelReadComplete();
      } else {
         buf.release();
      }

      this.cumulation = null;
      this.handlerRemoved0(ctx);
   }

   protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception {
   }

   public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
      if (msg instanceof ByteBuf) {
         RecyclableArrayList out = RecyclableArrayList.newInstance();
         boolean var12 = false;

         try {
            var12 = true;
            ByteBuf data = (ByteBuf)msg;
            this.first = this.cumulation == null;
            if (this.first) {
               this.cumulation = data;
            } else {
               this.cumulation = this.cumulator.cumulate(ctx.alloc(), this.cumulation, data);
            }

            this.callDecode(ctx, this.cumulation, out);
            var12 = false;
         } catch (DecoderException var13) {
            throw var13;
         } catch (Throwable var14) {
            throw new DecoderException(var14);
         } finally {
            if (var12) {
               if (this.cumulation != null && !this.cumulation.isReadable()) {
                  this.cumulation.release();
                  this.cumulation = null;
               }

               int size = out.size();

               for(int i = 0; i < size; ++i) {
                  ctx.fireChannelRead(out.get(i));
               }

               out.recycle();
            }
         }

         if (this.cumulation != null && !this.cumulation.isReadable()) {
            this.cumulation.release();
            this.cumulation = null;
         }

         int size = out.size();

         for(int i = 0; i < size; ++i) {
            ctx.fireChannelRead(out.get(i));
         }

         out.recycle();
      } else {
         ctx.fireChannelRead(msg);
      }

   }

   public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
      if (this.cumulation != null && !this.first && this.cumulation.refCnt() == 1) {
         this.cumulation.discardSomeReadBytes();
      }

      ctx.fireChannelReadComplete();
   }

   public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      RecyclableArrayList out = RecyclableArrayList.newInstance();
      boolean var25 = false;

      try {
         var25 = true;
         if (this.cumulation != null) {
            this.callDecode(ctx, this.cumulation, out);
            this.decodeLast(ctx, this.cumulation, out);
            var25 = false;
         } else {
            this.decodeLast(ctx, Unpooled.EMPTY_BUFFER, out);
            var25 = false;
         }
      } catch (DecoderException var26) {
         throw var26;
      } catch (Exception var27) {
         throw new DecoderException(var27);
      } finally {
         if (var25) {
            try {
               if (this.cumulation != null) {
                  this.cumulation.release();
                  this.cumulation = null;
               }

               int size = out.size();

               for(int i = 0; i < size; ++i) {
                  ctx.fireChannelRead(out.get(i));
               }

               if (size > 0) {
                  ctx.fireChannelReadComplete();
               }

               ctx.fireChannelInactive();
            } finally {
               out.recycle();
            }
         }
      }

      try {
         if (this.cumulation != null) {
            this.cumulation.release();
            this.cumulation = null;
         }

         int size = out.size();

         for(int i = 0; i < size; ++i) {
            ctx.fireChannelRead(out.get(i));
         }

         if (size > 0) {
            ctx.fireChannelReadComplete();
         }

         ctx.fireChannelInactive();
      } finally {
         out.recycle();
      }

   }

   protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
      try {
         while(true) {
            if (in.isReadable()) {
               int outSize = out.size();
               int oldInputLength = in.readableBytes();
               this.decode(ctx, in, out);
               if (!ctx.isRemoved()) {
                  if (outSize == out.size()) {
                     if (oldInputLength != in.readableBytes()) {
                        continue;
                     }
                  } else {
                     if (oldInputLength == in.readableBytes()) {
                        throw new DecoderException(StringUtil.simpleClassName(this.getClass()) + ".decode() did not read anything but decoded a message.");
                     }

                     if (!this.isSingleDecode()) {
                        continue;
                     }
                  }
               }
            }

            return;
         }
      } catch (DecoderException var6) {
         throw var6;
      } catch (Throwable var7) {
         throw new DecoderException(var7);
      }
   }

   protected abstract void decode(ChannelHandlerContext var1, ByteBuf var2, List<Object> var3) throws Exception;

   protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      this.decode(ctx, in, out);
   }

   static ByteBuf expandCumulation(ByteBufAllocator alloc, ByteBuf cumulation, int readable) {
      ByteBuf oldCumulation = cumulation;
      cumulation = alloc.buffer(cumulation.readableBytes() + readable);
      cumulation.writeBytes(oldCumulation);
      oldCumulation.release();
      return cumulation;
   }

   public interface Cumulator {
      ByteBuf cumulate(ByteBufAllocator var1, ByteBuf var2, ByteBuf var3);
   }
}
