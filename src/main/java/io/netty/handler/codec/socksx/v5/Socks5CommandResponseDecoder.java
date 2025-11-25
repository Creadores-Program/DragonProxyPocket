package io.netty.handler.codec.socksx.v5;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.socksx.SocksVersion;
import java.util.List;

public class Socks5CommandResponseDecoder extends ReplayingDecoder<Socks5CommandResponseDecoder.State> {
   private final Socks5AddressDecoder addressDecoder;

   public Socks5CommandResponseDecoder() {
      this(Socks5AddressDecoder.DEFAULT);
   }

   public Socks5CommandResponseDecoder(Socks5AddressDecoder addressDecoder) {
      super(Socks5CommandResponseDecoder.State.INIT);
      if (addressDecoder == null) {
         throw new NullPointerException("addressDecoder");
      } else {
         this.addressDecoder = addressDecoder;
      }
   }

   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      try {
         switch((Socks5CommandResponseDecoder.State)this.state()) {
         case INIT:
            byte version = in.readByte();
            if (version != SocksVersion.SOCKS5.byteValue()) {
               throw new DecoderException("unsupported version: " + version + " (expected: " + SocksVersion.SOCKS5.byteValue() + ')');
            }

            Socks5CommandStatus status = Socks5CommandStatus.valueOf(in.readByte());
            in.skipBytes(1);
            Socks5AddressType addrType = Socks5AddressType.valueOf(in.readByte());
            String addr = this.addressDecoder.decodeAddress(addrType, in);
            int port = in.readUnsignedShort();
            out.add(new DefaultSocks5CommandResponse(status, addrType, addr, port));
            this.checkpoint(Socks5CommandResponseDecoder.State.SUCCESS);
         case SUCCESS:
            int readableBytes = this.actualReadableBytes();
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

      this.checkpoint(Socks5CommandResponseDecoder.State.FAILURE);
      Socks5Message m = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4, (String)null, 0);
      m.setDecoderResult(DecoderResult.failure((Throwable)cause));
      out.add(m);
   }

   static enum State {
      INIT,
      SUCCESS,
      FAILURE;
   }
}
