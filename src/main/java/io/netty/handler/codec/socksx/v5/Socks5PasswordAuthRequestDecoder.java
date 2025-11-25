package io.netty.handler.codec.socksx.v5;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.CharsetUtil;
import java.util.List;

public class Socks5PasswordAuthRequestDecoder extends ReplayingDecoder<Socks5PasswordAuthRequestDecoder.State> {
   public Socks5PasswordAuthRequestDecoder() {
      super(Socks5PasswordAuthRequestDecoder.State.INIT);
   }

   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      try {
         int readableBytes;
         switch((Socks5PasswordAuthRequestDecoder.State)this.state()) {
         case INIT:
            readableBytes = in.readerIndex();
            byte version = in.getByte(readableBytes);
            if (version != 1) {
               throw new DecoderException("unsupported subnegotiation version: " + version + " (expected: 1)");
            }

            int usernameLength = in.getUnsignedByte(readableBytes + 1);
            int passwordLength = in.getUnsignedByte(readableBytes + 2 + usernameLength);
            int totalLength = usernameLength + passwordLength + 3;
            in.skipBytes(totalLength);
            out.add(new DefaultSocks5PasswordAuthRequest(in.toString(readableBytes + 2, usernameLength, CharsetUtil.US_ASCII), in.toString(readableBytes + 3 + usernameLength, passwordLength, CharsetUtil.US_ASCII)));
            this.checkpoint(Socks5PasswordAuthRequestDecoder.State.SUCCESS);
         case SUCCESS:
            readableBytes = this.actualReadableBytes();
            if (readableBytes > 0) {
               out.add(in.readSlice(readableBytes).retain());
            }
            break;
         case FAILURE:
            in.skipBytes(this.actualReadableBytes());
         }
      } catch (Exception var9) {
         this.fail(out, var9);
      }

   }

   private void fail(List<Object> out, Throwable cause) {
      if (!(cause instanceof DecoderException)) {
         cause = new DecoderException((Throwable)cause);
      }

      this.checkpoint(Socks5PasswordAuthRequestDecoder.State.FAILURE);
      Socks5Message m = new DefaultSocks5PasswordAuthRequest("", "");
      m.setDecoderResult(DecoderResult.failure((Throwable)cause));
      out.add(m);
   }

   static enum State {
      INIT,
      SUCCESS,
      FAILURE;
   }
}
