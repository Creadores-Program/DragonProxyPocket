package io.netty.handler.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Signal;
import io.netty.util.internal.RecyclableArrayList;
import io.netty.util.internal.StringUtil;
import java.util.List;

public abstract class ReplayingDecoder<S> extends ByteToMessageDecoder {
   static final Signal REPLAY = Signal.valueOf(ReplayingDecoder.class, "REPLAY");
   private final ReplayingDecoderBuffer replayable;
   private S state;
   private int checkpoint;

   protected ReplayingDecoder() {
      this((Object)null);
   }

   protected ReplayingDecoder(S initialState) {
      this.replayable = new ReplayingDecoderBuffer();
      this.checkpoint = -1;
      this.state = initialState;
   }

   protected void checkpoint() {
      this.checkpoint = this.internalBuffer().readerIndex();
   }

   protected void checkpoint(S state) {
      this.checkpoint();
      this.state(state);
   }

   protected S state() {
      return this.state;
   }

   protected S state(S newState) {
      S oldState = this.state;
      this.state = newState;
      return oldState;
   }

   public void channelInactive(ChannelHandlerContext ctx) throws Exception {
      RecyclableArrayList out = RecyclableArrayList.newInstance();
      boolean var39 = false;

      int size;
      int i;
      label457: {
         try {
            var39 = true;
            this.replayable.terminate();
            this.callDecode(ctx, this.internalBuffer(), out);
            this.decodeLast(ctx, this.replayable, out);
            var39 = false;
            break label457;
         } catch (Signal var43) {
            var43.expect(REPLAY);
            var39 = false;
         } catch (DecoderException var44) {
            throw var44;
         } catch (Exception var45) {
            throw new DecoderException(var45);
         } finally {
            if (var39) {
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

            size = out.size();

            for(i = 0; i < size; ++i) {
               ctx.fireChannelRead(out.get(i));
            }

            if (size > 0) {
               ctx.fireChannelReadComplete();
            }

            ctx.fireChannelInactive();
            return;
         } finally {
            out.recycle();
         }
      }

      try {
         if (this.cumulation != null) {
            this.cumulation.release();
            this.cumulation = null;
         }

         size = out.size();

         for(i = 0; i < size; ++i) {
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
      this.replayable.setCumulation(in);

      try {
         while(in.isReadable()) {
            int oldReaderIndex = this.checkpoint = in.readerIndex();
            int outSize = out.size();
            S oldState = this.state;
            int oldInputLength = in.readableBytes();

            try {
               this.decode(ctx, this.replayable, out);
               if (ctx.isRemoved()) {
                  break;
               }

               if (outSize == out.size()) {
                  if (oldInputLength == in.readableBytes() && oldState == this.state) {
                     throw new DecoderException(StringUtil.simpleClassName(this.getClass()) + ".decode() must consume the inbound " + "data or change its state if it did not decode anything.");
                  }
                  continue;
               }
            } catch (Signal var10) {
               var10.expect(REPLAY);
               if (!ctx.isRemoved()) {
                  int checkpoint = this.checkpoint;
                  if (checkpoint >= 0) {
                     in.readerIndex(checkpoint);
                  }
               }
               break;
            }

            if (oldReaderIndex == in.readerIndex() && oldState == this.state) {
               throw new DecoderException(StringUtil.simpleClassName(this.getClass()) + ".decode() method must consume the inbound data " + "or change its state if it decoded something.");
            }

            if (this.isSingleDecode()) {
               break;
            }
         }

      } catch (DecoderException var11) {
         throw var11;
      } catch (Throwable var12) {
         throw new DecoderException(var12);
      }
   }
}
