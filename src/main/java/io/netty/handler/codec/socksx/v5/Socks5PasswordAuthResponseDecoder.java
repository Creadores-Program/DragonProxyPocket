package io.netty.handler.codec.socksx.v5;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.ReplayingDecoder;
import java.util.List;

public class Socks5PasswordAuthResponseDecoder extends ReplayingDecoder<Socks5PasswordAuthResponseDecoder.State> {
   public Socks5PasswordAuthResponseDecoder() {
      super(Socks5PasswordAuthResponseDecoder.State.INIT);
   }

   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      try {
         switch((Socks5PasswordAuthResponseDecoder.State)this.state()) {
         case INIT:
            byte version = in.readByte();
            if (version != 1) {
               throw new DecoderException("unsupported subnegotiation version: " + version + " (expected: 1)");
            }

            out.add(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.valueOf(in.readByte())));
            this.checkpoint(Socks5PasswordAuthResponseDecoder.State.SUCCESS);
         case SUCCESS:
            int readableBytes = this.actualReadableBytes();
            if (readableBytes > 0) {
               out.add(in.readSlice(readableBytes).retain());
            }
            break;
         case FAILURE:
            in.skipBytes(this.actualReadableBytes());
         }
      } catch (Exception var5) {
         this.fail(out, var5);
      }

   }

   private void fail(List<Object> out, Throwable cause) {
      if (!(cause instanceof DecoderException)) {
         cause = new DecoderException((Throwable)cause);
      }

      this.checkpoint(Socks5PasswordAuthResponseDecoder.State.FAILURE);
      Socks5Message m = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE);
      m.setDecoderResult(DecoderResult.failure((Throwable)cause));
      out.add(m);
   }

   static enum State {
      INIT,
      SUCCESS,
      FAILURE;
   }
}
