package io.netty.channel.epoll;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.EventLoop;
import io.netty.channel.unix.FileDescriptor;
import io.netty.channel.unix.UnixChannel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.OneTimeTask;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.UnresolvedAddressException;

abstract class AbstractEpollChannel extends AbstractChannel implements UnixChannel {
   private static final ChannelMetadata DATA = new ChannelMetadata(false);
   private final int readFlag;
   private final FileDescriptor fileDescriptor;
   protected int flags;
   protected volatile boolean active;

   AbstractEpollChannel(int fd, int flag) {
      this((Channel)null, fd, flag, false);
   }

   AbstractEpollChannel(Channel parent, int fd, int flag, boolean active) {
      this(parent, new FileDescriptor(fd), flag, active);
   }

   AbstractEpollChannel(Channel parent, FileDescriptor fd, int flag, boolean active) {
      super(parent);
      this.flags = Native.EPOLLET;
      if (fd == null) {
         throw new NullPointerException("fd");
      } else {
         this.readFlag = flag;
         this.flags |= flag;
         this.active = active;
         this.fileDescriptor = fd;
      }
   }

   void setFlag(int flag) {
      if (!this.isFlagSet(flag)) {
         this.flags |= flag;
         this.modifyEvents();
      }

   }

   void clearFlag(int flag) {
      if (this.isFlagSet(flag)) {
         this.flags &= ~flag;
         this.modifyEvents();
      }

   }

   boolean isFlagSet(int flag) {
      return (this.flags & flag) != 0;
   }

   public final FileDescriptor fd() {
      return this.fileDescriptor;
   }

   public abstract EpollChannelConfig config();

   public boolean isActive() {
      return this.active;
   }

   public ChannelMetadata metadata() {
      return DATA;
   }

   protected void doClose() throws Exception {
      this.active = false;
      this.doDeregister();
      FileDescriptor fd = this.fileDescriptor;
      fd.close();
   }

   protected void doDisconnect() throws Exception {
      this.doClose();
   }

   protected boolean isCompatible(EventLoop loop) {
      return loop instanceof EpollEventLoop;
   }

   public boolean isOpen() {
      return this.fileDescriptor.isOpen();
   }

   protected void doDeregister() throws Exception {
      ((EpollEventLoop)this.eventLoop().unwrap()).remove(this);
   }

   protected void doBeginRead() throws Exception {
      ((AbstractEpollChannel.AbstractEpollUnsafe)this.unsafe()).readPending = true;
      this.setFlag(this.readFlag);
   }

   final void clearEpollIn() {
      if (this.isRegistered()) {
         EventLoop loop = this.eventLoop();
         final AbstractEpollChannel.AbstractEpollUnsafe unsafe = (AbstractEpollChannel.AbstractEpollUnsafe)this.unsafe();
         if (loop.inEventLoop()) {
            unsafe.clearEpollIn0();
         } else {
            loop.execute(new OneTimeTask() {
               public void run() {
                  if (!AbstractEpollChannel.this.config().isAutoRead() && !unsafe.readPending) {
                     unsafe.clearEpollIn0();
                  }

               }
            });
         }
      } else {
         this.flags &= ~this.readFlag;
      }

   }

   private void modifyEvents() {
      if (this.isOpen() && this.isRegistered()) {
         ((EpollEventLoop)this.eventLoop().unwrap()).modify(this);
      }

   }

   protected void doRegister() throws Exception {
      ((EpollEventLoop)this.eventLoop().unwrap()).add(this);
   }

   protected abstract AbstractEpollChannel.AbstractEpollUnsafe newUnsafe();

   protected final ByteBuf newDirectBuffer(ByteBuf buf) {
      return this.newDirectBuffer(buf, buf);
   }

   protected final ByteBuf newDirectBuffer(Object holder, ByteBuf buf) {
      int readableBytes = buf.readableBytes();
      if (readableBytes == 0) {
         ReferenceCountUtil.safeRelease(holder);
         return Unpooled.EMPTY_BUFFER;
      } else {
         ByteBufAllocator alloc = this.alloc();
         if (alloc.isDirectBufferPooled()) {
            return newDirectBuffer0(holder, buf, alloc, readableBytes);
         } else {
            ByteBuf directBuf = ByteBufUtil.threadLocalDirectBuffer();
            if (directBuf == null) {
               return newDirectBuffer0(holder, buf, alloc, readableBytes);
            } else {
               directBuf.writeBytes(buf, buf.readerIndex(), readableBytes);
               ReferenceCountUtil.safeRelease(holder);
               return directBuf;
            }
         }
      }
   }

   private static ByteBuf newDirectBuffer0(Object holder, ByteBuf buf, ByteBufAllocator alloc, int capacity) {
      ByteBuf directBuf = alloc.directBuffer(capacity);
      directBuf.writeBytes(buf, buf.readerIndex(), capacity);
      ReferenceCountUtil.safeRelease(holder);
      return directBuf;
   }

   protected static void checkResolvable(InetSocketAddress addr) {
      if (addr.isUnresolved()) {
         throw new UnresolvedAddressException();
      }
   }

   protected final int doReadBytes(ByteBuf byteBuf) throws Exception {
      int writerIndex = byteBuf.writerIndex();
      int localReadAmount;
      if (byteBuf.hasMemoryAddress()) {
         localReadAmount = Native.readAddress(this.fileDescriptor.intValue(), byteBuf.memoryAddress(), writerIndex, byteBuf.capacity());
      } else {
         ByteBuffer buf = byteBuf.internalNioBuffer(writerIndex, byteBuf.writableBytes());
         localReadAmount = Native.read(this.fileDescriptor.intValue(), buf, buf.position(), buf.limit());
      }

      if (localReadAmount > 0) {
         byteBuf.writerIndex(writerIndex + localReadAmount);
      }

      return localReadAmount;
   }

   protected final int doWriteBytes(ByteBuf buf, int writeSpinCount) throws Exception {
      int readableBytes = buf.readableBytes();
      int writtenBytes = 0;
      int readerIndex;
      int limit;
      int localFlushedAmount;
      if (buf.hasMemoryAddress()) {
         long memoryAddress = buf.memoryAddress();
         readerIndex = buf.readerIndex();
         limit = buf.writerIndex();

         for(localFlushedAmount = writeSpinCount - 1; localFlushedAmount >= 0; --localFlushedAmount) {
            int localFlushedAmount = Native.writeAddress(this.fileDescriptor.intValue(), memoryAddress, readerIndex, limit);
            if (localFlushedAmount <= 0) {
               break;
            }

            writtenBytes += localFlushedAmount;
            if (writtenBytes == readableBytes) {
               return writtenBytes;
            }

            readerIndex += localFlushedAmount;
         }
      } else {
         ByteBuffer nioBuf;
         if (buf.nioBufferCount() == 1) {
            nioBuf = buf.internalNioBuffer(buf.readerIndex(), buf.readableBytes());
         } else {
            nioBuf = buf.nioBuffer();
         }

         for(int i = writeSpinCount - 1; i >= 0; --i) {
            readerIndex = nioBuf.position();
            limit = nioBuf.limit();
            localFlushedAmount = Native.write(this.fileDescriptor.intValue(), nioBuf, readerIndex, limit);
            if (localFlushedAmount <= 0) {
               break;
            }

            nioBuf.position(readerIndex + localFlushedAmount);
            writtenBytes += localFlushedAmount;
            if (writtenBytes == readableBytes) {
               return writtenBytes;
            }
         }
      }

      if (writtenBytes < readableBytes) {
         this.setFlag(Native.EPOLLOUT);
      }

      return writtenBytes;
   }

   protected abstract class AbstractEpollUnsafe extends AbstractChannel.AbstractUnsafe {
      protected boolean readPending;

      protected AbstractEpollUnsafe() {
         super();
      }

      abstract void epollInReady();

      void epollRdHupReady() {
      }

      protected void flush0() {
         if (!AbstractEpollChannel.this.isFlagSet(Native.EPOLLOUT)) {
            super.flush0();
         }
      }

      void epollOutReady() {
         super.flush0();
      }

      protected final void clearEpollIn0() {
         AbstractEpollChannel.this.clearFlag(AbstractEpollChannel.this.readFlag);
      }
   }
}
