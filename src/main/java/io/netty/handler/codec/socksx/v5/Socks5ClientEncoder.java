package io.netty.handler.codec.socksx.v5;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.internal.StringUtil;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

@ChannelHandler.Sharable
public class Socks5ClientEncoder extends MessageToByteEncoder<Socks5Message> {
   public static final Socks5ClientEncoder DEFAULT = new Socks5ClientEncoder();
   private final Socks5AddressEncoder addressEncoder;

   protected Socks5ClientEncoder() {
      this(Socks5AddressEncoder.DEFAULT);
   }

   public Socks5ClientEncoder(Socks5AddressEncoder addressEncoder) {
      if (addressEncoder == null) {
         throw new NullPointerException("addressEncoder");
      } else {
         this.addressEncoder = addressEncoder;
      }
   }

   protected final Socks5AddressEncoder addressEncoder() {
      return this.addressEncoder;
   }

   protected void encode(ChannelHandlerContext ctx, Socks5Message msg, ByteBuf out) throws Exception {
      if (msg instanceof Socks5InitialRequest) {
         encodeAuthMethodRequest((Socks5InitialRequest)msg, out);
      } else if (msg instanceof Socks5PasswordAuthRequest) {
         encodePasswordAuthRequest((Socks5PasswordAuthRequest)msg, out);
      } else {
         if (!(msg instanceof Socks5CommandRequest)) {
            throw new EncoderException("unsupported message type: " + StringUtil.simpleClassName((Object)msg));
         }

         this.encodeCommandRequest((Socks5CommandRequest)msg, out);
      }

   }

   private static void encodeAuthMethodRequest(Socks5InitialRequest msg, ByteBuf out) {
      out.writeByte(msg.version().byteValue());
      List<Socks5AuthMethod> authMethods = msg.authMethods();
      int numAuthMethods = authMethods.size();
      out.writeByte(numAuthMethods);
      if (authMethods instanceof RandomAccess) {
         for(int i = 0; i < numAuthMethods; ++i) {
            out.writeByte(((Socks5AuthMethod)authMethods.get(i)).byteValue());
         }
      } else {
         Iterator i$ = authMethods.iterator();

         while(i$.hasNext()) {
            Socks5AuthMethod a = (Socks5AuthMethod)i$.next();
            out.writeByte(a.byteValue());
         }
      }

   }

   private static void encodePasswordAuthRequest(Socks5PasswordAuthRequest msg, ByteBuf out) {
      out.writeByte(1);
      String username = msg.username();
      out.writeByte(username.length());
      ByteBufUtil.writeAscii(out, username);
      String password = msg.password();
      out.writeByte(password.length());
      ByteBufUtil.writeAscii(out, password);
   }

   private void encodeCommandRequest(Socks5CommandRequest msg, ByteBuf out) throws Exception {
      out.writeByte(msg.version().byteValue());
      out.writeByte(msg.type().byteValue());
      out.writeByte(0);
      Socks5AddressType dstAddrType = msg.dstAddrType();
      out.writeByte(dstAddrType.byteValue());
      this.addressEncoder.encodeAddress(dstAddrType, msg.dstAddr(), out);
      out.writeShort(msg.dstPort());
   }
}
