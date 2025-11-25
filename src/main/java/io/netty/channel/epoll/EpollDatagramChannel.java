package io.netty.channel.epoll;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.AddressedEnvelope;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultAddressedEnvelope;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramChannelConfig;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.unix.FileDescriptor;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.NotYetConnectedException;
import java.util.ArrayList;
import java.util.List;

public final class EpollDatagramChannel extends AbstractEpollChannel implements DatagramChannel {
   private static final ChannelMetadata METADATA = new ChannelMetadata(true);
   private static final String EXPECTED_TYPES = " (expected: " + StringUtil.simpleClassName(DatagramPacket.class) + ", " + StringUtil.simpleClassName(AddressedEnvelope.class) + '<' + StringUtil.simpleClassName(ByteBuf.class) + ", " + StringUtil.simpleClassName(InetSocketAddress.class) + ">, " + StringUtil.simpleClassName(ByteBuf.class) + ')';
   private volatile InetSocketAddress local;
   private volatile InetSocketAddress remote;
   private volatile boolean connected;
   private final EpollDatagramChannelConfig config = new EpollDatagramChannelConfig(this);

   public EpollDatagramChannel() {
      super(Native.socketDgramFd(), Native.EPOLLIN);
   }

   public EpollDatagramChannel(FileDescriptor fd) {
      super((Channel)null, fd, Native.EPOLLIN, true);
      this.local = Native.localAddress(fd.intValue());
   }

   public InetSocketAddress remoteAddress() {
      return (InetSocketAddress)super.remoteAddress();
   }

   public InetSocketAddress localAddress() {
      return (InetSocketAddress)super.localAddress();
   }

   public ChannelMetadata metadata() {
      return METADATA;
   }

   public boolean isActive() {
      return this.fd().isOpen() && ((Boolean)this.config.getOption(ChannelOption.DATAGRAM_CHANNEL_ACTIVE_ON_REGISTRATION) && this.isRegistered() || this.active);
   }

   public boolean isConnected() {
      return this.connected;
   }

   public ChannelFuture joinGroup(InetAddress multicastAddress) {
      return this.joinGroup(multicastAddress, this.newPromise());
   }

   public ChannelFuture joinGroup(InetAddress multicastAddress, ChannelPromise promise) {
      try {
         return this.joinGroup(multicastAddress, NetworkInterface.getByInetAddress(this.localAddress().getAddress()), (InetAddress)null, promise);
      } catch (SocketException var4) {
         promise.setFailure(var4);
         return promise;
      }
   }

   public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface) {
      return this.joinGroup(multicastAddress, networkInterface, this.newPromise());
   }

   public ChannelFuture joinGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelPromise promise) {
      return this.joinGroup(multicastAddress.getAddress(), networkInterface, (InetAddress)null, promise);
   }

   public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
      return this.joinGroup(multicastAddress, networkInterface, source, this.newPromise());
   }

   public ChannelFuture joinGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise promise) {
      if (multicastAddress == null) {
         throw new NullPointerException("multicastAddress");
      } else if (networkInterface == null) {
         throw new NullPointerException("networkInterface");
      } else {
         promise.setFailure(new UnsupportedOperationException("Multicast not supported"));
         return promise;
      }
   }

   public ChannelFuture leaveGroup(InetAddress multicastAddress) {
      return this.leaveGroup(multicastAddress, this.newPromise());
   }

   public ChannelFuture leaveGroup(InetAddress multicastAddress, ChannelPromise promise) {
      try {
         return this.leaveGroup(multicastAddress, NetworkInterface.getByInetAddress(this.localAddress().getAddress()), (InetAddress)null, promise);
      } catch (SocketException var4) {
         promise.setFailure(var4);
         return promise;
      }
   }

   public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface) {
      return this.leaveGroup(multicastAddress, networkInterface, this.newPromise());
   }

   public ChannelFuture leaveGroup(InetSocketAddress multicastAddress, NetworkInterface networkInterface, ChannelPromise promise) {
      return this.leaveGroup(multicastAddress.getAddress(), networkInterface, (InetAddress)null, promise);
   }

   public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source) {
      return this.leaveGroup(multicastAddress, networkInterface, source, this.newPromise());
   }

   public ChannelFuture leaveGroup(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress source, ChannelPromise promise) {
      if (multicastAddress == null) {
         throw new NullPointerException("multicastAddress");
      } else if (networkInterface == null) {
         throw new NullPointerException("networkInterface");
      } else {
         promise.setFailure(new UnsupportedOperationException("Multicast not supported"));
         return promise;
      }
   }

   public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress sourceToBlock) {
      return this.block(multicastAddress, networkInterface, sourceToBlock, this.newPromise());
   }

   public ChannelFuture block(InetAddress multicastAddress, NetworkInterface networkInterface, InetAddress sourceToBlock, ChannelPromise promise) {
      if (multicastAddress == null) {
         throw new NullPointerException("multicastAddress");
      } else if (sourceToBlock == null) {
         throw new NullPointerException("sourceToBlock");
      } else if (networkInterface == null) {
         throw new NullPointerException("networkInterface");
      } else {
         promise.setFailure(new UnsupportedOperationException("Multicast not supported"));
         return promise;
      }
   }

   public ChannelFuture block(InetAddress multicastAddress, InetAddress sourceToBlock) {
      return this.block(multicastAddress, sourceToBlock, this.newPromise());
   }

   public ChannelFuture block(InetAddress multicastAddress, InetAddress sourceToBlock, ChannelPromise promise) {
      try {
         return this.block(multicastAddress, NetworkInterface.getByInetAddress(this.localAddress().getAddress()), sourceToBlock, promise);
      } catch (Throwable var5) {
         promise.setFailure(var5);
         return promise;
      }
   }

   protected AbstractEpollChannel.AbstractEpollUnsafe newUnsafe() {
      return new EpollDatagramChannel.EpollDatagramChannelUnsafe();
   }

   protected InetSocketAddress localAddress0() {
      return this.local;
   }

   protected InetSocketAddress remoteAddress0() {
      return this.remote;
   }

   protected void doBind(SocketAddress localAddress) throws Exception {
      InetSocketAddress addr = (InetSocketAddress)localAddress;
      checkResolvable(addr);
      int fd = this.fd().intValue();
      Native.bind(fd, addr);
      this.local = Native.localAddress(fd);
      this.active = true;
   }

   protected void doWrite(ChannelOutboundBuffer in) throws Exception {
      label65:
      while(true) {
         Object msg = in.current();
         if (msg == null) {
            this.clearFlag(Native.EPOLLOUT);
         } else {
            try {
               int cnt;
               if (Native.IS_SUPPORTING_SENDMMSG && in.size() > 1) {
                  NativeDatagramPacketArray array = NativeDatagramPacketArray.getInstance(in);
                  cnt = array.count();
                  if (cnt >= 1) {
                     int offset = 0;
                     NativeDatagramPacketArray.NativeDatagramPacket[] packets = array.packets();

                     while(true) {
                        if (cnt <= 0) {
                           continue label65;
                        }

                        int send = Native.sendmmsg(this.fd().intValue(), packets, offset, cnt);
                        if (send == 0) {
                           this.setFlag(Native.EPOLLOUT);
                           return;
                        }

                        for(int i = 0; i < send; ++i) {
                           in.remove();
                        }

                        cnt -= send;
                        offset += send;
                     }
                  }
               }

               boolean done = false;

               for(cnt = this.config().getWriteSpinCount() - 1; cnt >= 0; --cnt) {
                  if (this.doWriteMessage(msg)) {
                     done = true;
                     break;
                  }
               }

               if (done) {
                  in.remove();
                  continue;
               }

               this.setFlag(Native.EPOLLOUT);
            } catch (IOException var9) {
               in.remove(var9);
               continue;
            }
         }

         return;
      }
   }

   private boolean doWriteMessage(Object msg) throws Exception {
      ByteBuf data;
      InetSocketAddress remoteAddress;
      if (msg instanceof AddressedEnvelope) {
         AddressedEnvelope<ByteBuf, InetSocketAddress> envelope = (AddressedEnvelope)msg;
         data = (ByteBuf)envelope.content();
         remoteAddress = (InetSocketAddress)envelope.recipient();
      } else {
         data = (ByteBuf)msg;
         remoteAddress = null;
      }

      int dataLen = data.readableBytes();
      if (dataLen == 0) {
         return true;
      } else {
         if (remoteAddress == null) {
            remoteAddress = this.remote;
            if (remoteAddress == null) {
               throw new NotYetConnectedException();
            }
         }

         int writtenBytes;
         if (data.hasMemoryAddress()) {
            long memoryAddress = data.memoryAddress();
            writtenBytes = Native.sendToAddress(this.fd().intValue(), memoryAddress, data.readerIndex(), data.writerIndex(), remoteAddress.getAddress(), remoteAddress.getPort());
         } else if (data instanceof CompositeByteBuf) {
            IovArray array = IovArrayThreadLocal.get((CompositeByteBuf)data);
            int cnt = array.count();

            assert cnt != 0;

            writtenBytes = Native.sendToAddresses(this.fd().intValue(), array.memoryAddress(0), cnt, remoteAddress.getAddress(), remoteAddress.getPort());
         } else {
            ByteBuffer nioData = data.internalNioBuffer(data.readerIndex(), data.readableBytes());
            writtenBytes = Native.sendTo(this.fd().intValue(), nioData, nioData.position(), nioData.limit(), remoteAddress.getAddress(), remoteAddress.getPort());
         }

         return writtenBytes > 0;
      }
   }

   protected Object filterOutboundMessage(Object msg) {
      ByteBuf content;
      CompositeByteBuf comp;
      if (msg instanceof DatagramPacket) {
         DatagramPacket packet = (DatagramPacket)msg;
         content = (ByteBuf)packet.content();
         if (content.hasMemoryAddress()) {
            return msg;
         } else {
            if (content.isDirect() && content instanceof CompositeByteBuf) {
               comp = (CompositeByteBuf)content;
               if (comp.isDirect() && comp.nioBufferCount() <= Native.IOV_MAX) {
                  return msg;
               }
            }

            return new DatagramPacket(this.newDirectBuffer(packet, content), (InetSocketAddress)packet.recipient());
         }
      } else if (msg instanceof ByteBuf) {
         ByteBuf buf = (ByteBuf)msg;
         if (!buf.hasMemoryAddress() && (PlatformDependent.hasUnsafe() || !buf.isDirect())) {
            if (buf instanceof CompositeByteBuf) {
               CompositeByteBuf comp = (CompositeByteBuf)buf;
               if (!comp.isDirect() || comp.nioBufferCount() > Native.IOV_MAX) {
                  buf = this.newDirectBuffer(buf);

                  assert buf.hasMemoryAddress();
               }
            } else {
               buf = this.newDirectBuffer(buf);

               assert buf.hasMemoryAddress();
            }
         }

         return buf;
      } else {
         if (msg instanceof AddressedEnvelope) {
            AddressedEnvelope<Object, SocketAddress> e = (AddressedEnvelope)msg;
            if (e.content() instanceof ByteBuf && (e.recipient() == null || e.recipient() instanceof InetSocketAddress)) {
               content = (ByteBuf)e.content();
               if (content.hasMemoryAddress()) {
                  return e;
               }

               if (content instanceof CompositeByteBuf) {
                  comp = (CompositeByteBuf)content;
                  if (comp.isDirect() && comp.nioBufferCount() <= Native.IOV_MAX) {
                     return e;
                  }
               }

               return new DefaultAddressedEnvelope(this.newDirectBuffer(e, content), (InetSocketAddress)e.recipient());
            }
         }

         throw new UnsupportedOperationException("unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
      }
   }

   public EpollDatagramChannelConfig config() {
      return this.config;
   }

   protected void doDisconnect() throws Exception {
      this.connected = false;
   }

   static final class DatagramSocketAddress extends InetSocketAddress {
      private static final long serialVersionUID = 1348596211215015739L;
      final int receivedAmount;

      DatagramSocketAddress(String addr, int port, int receivedAmount) {
         super(addr, port);
         this.receivedAmount = receivedAmount;
      }
   }

   final class EpollDatagramChannelUnsafe extends AbstractEpollChannel.AbstractEpollUnsafe {
      private final List<Object> readBuf = new ArrayList();

      EpollDatagramChannelUnsafe() {
         super();
      }

      public void connect(SocketAddress remote, SocketAddress local, ChannelPromise channelPromise) {
         boolean success = false;

         try {
            try {
               boolean wasActive = EpollDatagramChannel.this.isActive();
               InetSocketAddress remoteAddress = (InetSocketAddress)remote;
               if (local != null) {
                  InetSocketAddress localAddress = (InetSocketAddress)local;
                  EpollDatagramChannel.this.doBind(localAddress);
               }

               AbstractEpollChannel.checkResolvable(remoteAddress);
               EpollDatagramChannel.this.remote = remoteAddress;
               EpollDatagramChannel.this.local = Native.localAddress(EpollDatagramChannel.this.fd().intValue());
               success = true;
               if (!wasActive && EpollDatagramChannel.this.isActive()) {
                  EpollDatagramChannel.this.pipeline().fireChannelActive();
               }
            } finally {
               if (!success) {
                  EpollDatagramChannel.this.doClose();
               } else {
                  channelPromise.setSuccess();
                  EpollDatagramChannel.this.connected = true;
               }

            }
         } catch (Throwable var12) {
            channelPromise.setFailure(var12);
         }

      }

      void epollInReady() {
         assert EpollDatagramChannel.this.eventLoop().inEventLoop();

         DatagramChannelConfig config = EpollDatagramChannel.this.config();
         boolean edgeTriggered = EpollDatagramChannel.this.isFlagSet(Native.EPOLLET);
         if (!this.readPending && !edgeTriggered && !config.isAutoRead()) {
            this.clearEpollIn0();
         } else {
            RecvByteBufAllocator.Handle allocHandle = EpollDatagramChannel.this.unsafe().recvBufAllocHandle();
            ChannelPipeline pipeline = EpollDatagramChannel.this.pipeline();
            Throwable exception = null;

            try {
               int maxMessagesPerRead = edgeTriggered ? Integer.MAX_VALUE : config.getMaxMessagesPerRead();
               int messages = 0;

               int i;
               do {
                  ByteBuf data = null;

                  try {
                     data = allocHandle.allocate(config.getAllocator());
                     i = data.writerIndex();
                     EpollDatagramChannel.DatagramSocketAddress remoteAddress;
                     if (data.hasMemoryAddress()) {
                        remoteAddress = Native.recvFromAddress(EpollDatagramChannel.this.fd().intValue(), data.memoryAddress(), i, data.capacity());
                     } else {
                        ByteBuffer nioData = data.internalNioBuffer(i, data.writableBytes());
                        remoteAddress = Native.recvFrom(EpollDatagramChannel.this.fd().intValue(), nioData, nioData.position(), nioData.limit());
                     }

                     if (remoteAddress == null) {
                        break;
                     }

                     int readBytes = remoteAddress.receivedAmount;
                     data.writerIndex(data.writerIndex() + readBytes);
                     allocHandle.record(readBytes);
                     this.readPending = false;
                     this.readBuf.add(new DatagramPacket(data, (InetSocketAddress)this.localAddress(), remoteAddress));
                     data = null;
                  } catch (Throwable var20) {
                     exception = var20;
                  } finally {
                     if (data != null) {
                        data.release();
                     }

                     if (!edgeTriggered && !config.isAutoRead()) {
                        break;
                     }

                  }

                  ++messages;
               } while(messages < maxMessagesPerRead);

               int size = this.readBuf.size();

               for(i = 0; i < size; ++i) {
                  pipeline.fireChannelRead(this.readBuf.get(i));
               }

               this.readBuf.clear();
               pipeline.fireChannelReadComplete();
               if (exception != null) {
                  pipeline.fireExceptionCaught(exception);
               }
            } finally {
               if (!this.readPending && !config.isAutoRead()) {
                  EpollDatagramChannel.this.clearEpollIn();
               }

            }

         }
      }
   }
}
