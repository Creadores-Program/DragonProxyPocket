package io.netty.handler.codec.socksx.v5;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.SocksVersion;
import java.util.List;

public class Socks5InitialResponseDecoder extends ReplayingDecoder<Socks5InitialResponseDecoder.State> {
   public Socks5InitialResponseDecoder() {
      super(Socks5InitialResponseDecoder.State.INIT);
   }

   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      try {
         switch((Socks5InitialResponseDecoder.State)this.state()) {
         case INIT:
            byte version = in.readByte();
            if (version != SocksVersion.SOCKS5.byteValue()) {
               throw new DecoderException("unsupported version: " + version + " (expected: " + SocksVersion.SOCKS5.byteValue() + ')');
            }

            Socks5AuthMethod authMethod = Socks5AuthMethod.valueOf(in.readByte());
            out.add(new DefaultSocks5InitialResponse(authMethod));
            this.checkpoint(Socks5InitialResponseDecoder.State.SUCCESS);
         case SUCCESS:
            int readableBytes = this.actualReadableBytes();
            if (readableBytes > 0) {
               out.add(in.readSlice(readableBytes).retain());
            }
            break;
         case FAILURE:
            in.skipBytes(this.actualReadableBytes());
         }
      } catch (Exception var6) {
         this.fail(out, var6);
      }

   }

   private void fail(List<Object> out, Throwable cause) {
      if (!(cause instanceof DecoderException)) {
         cause = new DecoderException((Throwable)cause);
      }

      this.checkpoint(Socks5InitialResponseDecoder.State.FAILURE);
      Socks5Message m = new DefaultSocks5InitialResponse(Socks5AuthMethod.UNACCEPTED);
      m.setDecoderResult(DecoderResult.failure((Throwable)cause));
      out.add(m);
   }

   static enum State {
      INIT,
      SUCCESS,
      FAILURE;
   }
}
