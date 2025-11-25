package io.netty.handler.codec.socksx.v4;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.util.NetUtil;
import java.util.List;

public class Socks4ClientDecoder extends ReplayingDecoder<Socks4ClientDecoder.State> {
   public Socks4ClientDecoder() {
      super(Socks4ClientDecoder.State.START);
      this.setSingleDecode(true);
   }

   protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
      try {
         switch((Socks4ClientDecoder.State)this.state()) {
         case START:
            int version = in.readUnsignedByte();
            if (version != 0) {
               throw new DecoderException("unsupported reply version: " + version + " (expected: 0)");
            }

            Socks4CommandStatus status = Socks4CommandStatus.valueOf(in.readByte());
            int dstPort = in.readUnsignedShort();
            String dstAddr = NetUtil.intToIpAddress(in.readInt());
            out.add(new DefaultSocks4CommandResponse(status, dstAddr, dstPort));
            this.checkpoint(Socks4ClientDecoder.State.SUCCESS);
         case SUCCESS:
            int readableBytes = this.actualReadableBytes();
            if (readableBytes > 0) {
               out.add(in.readSlice(readableBytes).retain());
            }
            break;
         case FAILURE:
            in.skipBytes(this.actualReadableBytes());
         }
      } catch (Exception var8) {
         this.fail(out, var8);
      }

   }

   private void fail(List<Object> out, Throwable cause) {
      if (!(cause instanceof DecoderException)) {
         cause = new DecoderException((Throwable)cause);
      }

      Socks4CommandResponse m = new DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED);
      m.setDecoderResult(DecoderResult.failure((Throwable)cause));
      out.add(m);
      this.checkpoint(Socks4ClientDecoder.State.FAILURE);
   }

   static enum State {
      START,
      SUCCESS,
      FAILURE;
   }
}
