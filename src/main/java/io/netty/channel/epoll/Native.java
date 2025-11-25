package io.netty.channel.epoll;

import io.netty.channel.ChannelException;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.NativeLibraryLoader;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SystemPropertyUtil;
import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Locale;

final class Native {
   public static final int EPOLLIN;
   public static final int EPOLLOUT;
   public static final int EPOLLRDHUP;
   public static final int EPOLLET;
   public static final int IOV_MAX;
   public static final int UIO_MAX_IOV;
   public static final boolean IS_SUPPORTING_SENDMMSG;
   private static final byte[] IPV4_MAPPED_IPV6_PREFIX;
   private static final int ERRNO_EBADF_NEGATIVE;
   private static final int ERRNO_EPIPE_NEGATIVE;
   private static final int ERRNO_ECONNRESET_NEGATIVE;
   private static final int ERRNO_EAGAIN_NEGATIVE;
   private static final int ERRNO_EWOULDBLOCK_NEGATIVE;
   private static final int ERRNO_EINPROGRESS_NEGATIVE;
   private static final String[] ERRORS;
   private static final ClosedChannelException CLOSED_CHANNEL_EXCEPTION;
   private static final IOException CONNECTION_RESET_EXCEPTION_WRITE;
   private static final IOException CONNECTION_RESET_EXCEPTION_WRITEV;
   private static final IOException CONNECTION_RESET_EXCEPTION_READ;
   private static final IOException CONNECTION_RESET_EXCEPTION_SENDFILE;
   private static final IOException CONNECTION_RESET_EXCEPTION_SENDTO;
   private static final IOException CONNECTION_RESET_EXCEPTION_SENDMSG;
   private static final IOException CONNECTION_RESET_EXCEPTION_SENDMMSG;

   private static IOException newConnectionResetException(String method, int errnoNegative) {
      IOException exception = newIOException(method, errnoNegative);
      exception.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
      return exception;
   }

   private static IOException newIOException(String method, int err) {
      return new IOException(method + "() failed: " + ERRORS[-err]);
   }

   private static int ioResult(String method, int err, IOException resetCause) throws IOException {
      if (err != ERRNO_EAGAIN_NEGATIVE && err != ERRNO_EWOULDBLOCK_NEGATIVE) {
         if (err != ERRNO_EPIPE_NEGATIVE && err != ERRNO_ECONNRESET_NEGATIVE) {
            if (err == ERRNO_EBADF_NEGATIVE) {
               throw CLOSED_CHANNEL_EXCEPTION;
            } else {
               throw newIOException(method, err);
            }
         } else {
            throw resetCause;
         }
      } else {
         return 0;
      }
   }

   public static native int eventFd();

   public static native void eventFdWrite(int var0, long var1);

   public static native void eventFdRead(int var0);

   public static native int epollCreate();

   public static int epollWait(int efd, EpollEventArray events, int timeout) throws IOException {
      int ready = epollWait0(efd, events.memoryAddress(), events.length(), timeout);
      if (ready < 0) {
         throw newIOException("epoll_wait", ready);
      } else {
         return ready;
      }
   }

   private static native int epollWait0(int var0, long var1, int var3, int var4);

   public static native void epollCtlAdd(int var0, int var1, int var2);

   public static native void epollCtlMod(int var0, int var1, int var2);

   public static native void epollCtlDel(int var0, int var1);

   private static native int errnoEBADF();

   private static native int errnoEPIPE();

   private static native int errnoECONNRESET();

   private static native int errnoEAGAIN();

   private static native int errnoEWOULDBLOCK();

   private static native int errnoEINPROGRESS();

   private static native String strError(int var0);

   public static void close(int fd) throws IOException {
      int res = close0(fd);
      if (res < 0) {
         throw newIOException("close", res);
      }
   }

   private static native int close0(int var0);

   public static int write(int fd, ByteBuffer buf, int pos, int limit) throws IOException {
      int res = write0(fd, buf, pos, limit);
      return res >= 0 ? res : ioResult("write", res, CONNECTION_RESET_EXCEPTION_WRITE);
   }

   private static native int write0(int var0, ByteBuffer var1, int var2, int var3);

   public static int writeAddress(int fd, long address, int pos, int limit) throws IOException {
      int res = writeAddress0(fd, address, pos, limit);
      return res >= 0 ? res : ioResult("writeAddress", res, CONNECTION_RESET_EXCEPTION_WRITE);
   }

   private static native int writeAddress0(int var0, long var1, int var3, int var4);

   public static long writev(int fd, ByteBuffer[] buffers, int offset, int length) throws IOException {
      long res = writev0(fd, buffers, offset, length);
      return res >= 0L ? res : (long)ioResult("writev", (int)res, CONNECTION_RESET_EXCEPTION_WRITEV);
   }

   private static native long writev0(int var0, ByteBuffer[] var1, int var2, int var3);

   public static long writevAddresses(int fd, long memoryAddress, int length) throws IOException {
      long res = writevAddresses0(fd, memoryAddress, length);
      return res >= 0L ? res : (long)ioResult("writevAddresses", (int)res, CONNECTION_RESET_EXCEPTION_WRITEV);
   }

   private static native long writevAddresses0(int var0, long var1, int var3);

   public static int read(int fd, ByteBuffer buf, int pos, int limit) throws IOException {
      int res = read0(fd, buf, pos, limit);
      if (res > 0) {
         return res;
      } else {
         return res == 0 ? -1 : ioResult("read", res, CONNECTION_RESET_EXCEPTION_READ);
      }
   }

   private static native int read0(int var0, ByteBuffer var1, int var2, int var3);

   public static int readAddress(int fd, long address, int pos, int limit) throws IOException {
      int res = readAddress0(fd, address, pos, limit);
      if (res > 0) {
         return res;
      } else {
         return res == 0 ? -1 : ioResult("readAddress", res, CONNECTION_RESET_EXCEPTION_READ);
      }
   }

   private static native int readAddress0(int var0, long var1, int var3, int var4);

   public static long sendfile(int dest, DefaultFileRegion src, long baseOffset, long offset, long length) throws IOException {
      src.open();
      long res = sendfile0(dest, src, baseOffset, offset, length);
      return res >= 0L ? res : (long)ioResult("sendfile", (int)res, CONNECTION_RESET_EXCEPTION_SENDFILE);
   }

   private static native long sendfile0(int var0, DefaultFileRegion var1, long var2, long var4, long var6) throws IOException;

   public static int sendTo(int fd, ByteBuffer buf, int pos, int limit, InetAddress addr, int port) throws IOException {
      byte[] address;
      int scopeId;
      if (addr instanceof Inet6Address) {
         address = addr.getAddress();
         scopeId = ((Inet6Address)addr).getScopeId();
      } else {
         scopeId = 0;
         address = ipv4MappedIpv6Address(addr.getAddress());
      }

      int res = sendTo0(fd, buf, pos, limit, address, scopeId, port);
      return res >= 0 ? res : ioResult("sendTo", res, CONNECTION_RESET_EXCEPTION_SENDTO);
   }

   private static native int sendTo0(int var0, ByteBuffer var1, int var2, int var3, byte[] var4, int var5, int var6);

   public static int sendToAddress(int fd, long memoryAddress, int pos, int limit, InetAddress addr, int port) throws IOException {
      byte[] address;
      int scopeId;
      if (addr instanceof Inet6Address) {
         address = addr.getAddress();
         scopeId = ((Inet6Address)addr).getScopeId();
      } else {
         scopeId = 0;
         address = ipv4MappedIpv6Address(addr.getAddress());
      }

      int res = sendToAddress0(fd, memoryAddress, pos, limit, address, scopeId, port);
      return res >= 0 ? res : ioResult("sendToAddress", res, CONNECTION_RESET_EXCEPTION_SENDTO);
   }

   private static native int sendToAddress0(int var0, long var1, int var3, int var4, byte[] var5, int var6, int var7);

   public static int sendToAddresses(int fd, long memoryAddress, int length, InetAddress addr, int port) throws IOException {
      byte[] address;
      int scopeId;
      if (addr instanceof Inet6Address) {
         address = addr.getAddress();
         scopeId = ((Inet6Address)addr).getScopeId();
      } else {
         scopeId = 0;
         address = ipv4MappedIpv6Address(addr.getAddress());
      }

      int res = sendToAddresses(fd, memoryAddress, length, address, scopeId, port);
      return res >= 0 ? res : ioResult("sendToAddresses", res, CONNECTION_RESET_EXCEPTION_SENDMSG);
   }

   private static native int sendToAddresses(int var0, long var1, int var3, byte[] var4, int var5, int var6);

   public static native EpollDatagramChannel.DatagramSocketAddress recvFrom(int var0, ByteBuffer var1, int var2, int var3) throws IOException;

   public static native EpollDatagramChannel.DatagramSocketAddress recvFromAddress(int var0, long var1, int var3, int var4) throws IOException;

   public static int sendmmsg(int fd, NativeDatagramPacketArray.NativeDatagramPacket[] msgs, int offset, int len) throws IOException {
      int res = sendmmsg0(fd, msgs, offset, len);
      return res >= 0 ? res : ioResult("sendmmsg", res, CONNECTION_RESET_EXCEPTION_SENDMMSG);
   }

   private static native int sendmmsg0(int var0, NativeDatagramPacketArray.NativeDatagramPacket[] var1, int var2, int var3);

   private static native boolean isSupportingSendmmsg();

   public static int socketStreamFd() {
      int res = socketStream();
      if (res < 0) {
         throw new ChannelException(newIOException("socketStreamFd", res));
      } else {
         return res;
      }
   }

   public static int socketDgramFd() {
      int res = socketDgram();
      if (res < 0) {
         throw new ChannelException(newIOException("socketDgramFd", res));
      } else {
         return res;
      }
   }

   public static int socketDomainFd() {
      int res = socketDomain();
      if (res < 0) {
         throw new ChannelException(newIOException("socketDomain", res));
      } else {
         return res;
      }
   }

   private static native int socketStream();

   private static native int socketDgram();

   private static native int socketDomain();

   public static void bind(int fd, SocketAddress socketAddress) throws IOException {
      if (socketAddress instanceof InetSocketAddress) {
         InetSocketAddress addr = (InetSocketAddress)socketAddress;
         Native.NativeInetAddress address = toNativeInetAddress(addr.getAddress());
         int res = bind(fd, address.address, address.scopeId, addr.getPort());
         if (res < 0) {
            throw newIOException("bind", res);
         }
      } else {
         if (!(socketAddress instanceof DomainSocketAddress)) {
            throw new Error("Unexpected SocketAddress implementation " + socketAddress);
         }

         DomainSocketAddress addr = (DomainSocketAddress)socketAddress;
         int res = bindDomainSocket(fd, addr.path());
         if (res < 0) {
            throw newIOException("bind", res);
         }
      }

   }

   private static native int bind(int var0, byte[] var1, int var2, int var3);

   private static native int bindDomainSocket(int var0, String var1);

   public static void listen(int fd, int backlog) throws IOException {
      int res = listen0(fd, backlog);
      if (res < 0) {
         throw newIOException("listen", res);
      }
   }

   private static native int listen0(int var0, int var1);

   public static boolean connect(int fd, SocketAddress socketAddress) throws IOException {
      int res;
      if (socketAddress instanceof InetSocketAddress) {
         InetSocketAddress inetSocketAddress = (InetSocketAddress)socketAddress;
         Native.NativeInetAddress address = toNativeInetAddress(inetSocketAddress.getAddress());
         res = connect(fd, address.address, address.scopeId, inetSocketAddress.getPort());
      } else {
         if (!(socketAddress instanceof DomainSocketAddress)) {
            throw new Error("Unexpected SocketAddress implementation " + socketAddress);
         }

         DomainSocketAddress unixDomainSocketAddress = (DomainSocketAddress)socketAddress;
         res = connectDomainSocket(fd, unixDomainSocketAddress.path());
      }

      if (res < 0) {
         if (res == ERRNO_EINPROGRESS_NEGATIVE) {
            return false;
         } else {
            throw newIOException("connect", res);
         }
      } else {
         return true;
      }
   }

   private static native int connect(int var0, byte[] var1, int var2, int var3);

   private static native int connectDomainSocket(int var0, String var1);

   public static boolean finishConnect(int fd) throws IOException {
      int res = finishConnect0(fd);
      if (res < 0) {
         if (res == ERRNO_EINPROGRESS_NEGATIVE) {
            return false;
         } else {
            throw newIOException("finishConnect", res);
         }
      } else {
         return true;
      }
   }

   private static native int finishConnect0(int var0);

   public static InetSocketAddress remoteAddress(int fd) {
      byte[] addr = remoteAddress0(fd);
      return addr == null ? null : address(addr, 0, addr.length);
   }

   public static InetSocketAddress localAddress(int fd) {
      byte[] addr = localAddress0(fd);
      return addr == null ? null : address(addr, 0, addr.length);
   }

   static InetSocketAddress address(byte[] addr, int offset, int len) {
      int port = decodeInt(addr, offset + len - 4);

      try {
         Object address;
         switch(len) {
         case 8:
            byte[] ipv4 = new byte[4];
            System.arraycopy(addr, offset, ipv4, 0, 4);
            address = InetAddress.getByAddress(ipv4);
            break;
         case 24:
            byte[] ipv6 = new byte[16];
            System.arraycopy(addr, offset, ipv6, 0, 16);
            int scopeId = decodeInt(addr, offset + len - 8);
            address = Inet6Address.getByAddress((String)null, ipv6, scopeId);
            break;
         default:
            throw new Error();
         }

         return new InetSocketAddress((InetAddress)address, port);
      } catch (UnknownHostException var8) {
         throw new Error("Should never happen", var8);
      }
   }

   static int decodeInt(byte[] addr, int index) {
      return (addr[index] & 255) << 24 | (addr[index + 1] & 255) << 16 | (addr[index + 2] & 255) << 8 | addr[index + 3] & 255;
   }

   private static native byte[] remoteAddress0(int var0);

   private static native byte[] localAddress0(int var0);

   public static int accept(int fd, byte[] addr) throws IOException {
      int res = accept0(fd, addr);
      if (res >= 0) {
         return res;
      } else if (res != ERRNO_EAGAIN_NEGATIVE && res != ERRNO_EWOULDBLOCK_NEGATIVE) {
         throw newIOException("accept", res);
      } else {
         return -1;
      }
   }

   private static native int accept0(int var0, byte[] var1);

   public static int recvFd(int fd) throws IOException {
      int res = recvFd0(fd);
      if (res > 0) {
         return res;
      } else if (res == 0) {
         return -1;
      } else if (res != ERRNO_EAGAIN_NEGATIVE && res != ERRNO_EWOULDBLOCK_NEGATIVE) {
         throw newIOException("recvFd", res);
      } else {
         return 0;
      }
   }

   private static native int recvFd0(int var0);

   public static int sendFd(int socketFd, int fd) throws IOException {
      int res = sendFd0(socketFd, fd);
      if (res >= 0) {
         return res;
      } else if (res != ERRNO_EAGAIN_NEGATIVE && res != ERRNO_EWOULDBLOCK_NEGATIVE) {
         throw newIOException("sendFd", res);
      } else {
         return -1;
      }
   }

   private static native int sendFd0(int var0, int var1);

   public static void shutdown(int fd, boolean read, boolean write) throws IOException {
      int res = shutdown0(fd, read, write);
      if (res < 0) {
         throw newIOException("shutdown", res);
      }
   }

   private static native int shutdown0(int var0, boolean var1, boolean var2);

   public static native int getReceiveBufferSize(int var0);

   public static native int getSendBufferSize(int var0);

   public static native int isKeepAlive(int var0);

   public static native int isReuseAddress(int var0);

   public static native int isReusePort(int var0);

   public static native int isTcpNoDelay(int var0);

   public static native int isTcpCork(int var0);

   public static native int getSoLinger(int var0);

   public static native int getTrafficClass(int var0);

   public static native int isBroadcast(int var0);

   public static native int getTcpKeepIdle(int var0);

   public static native int getTcpKeepIntvl(int var0);

   public static native int getTcpKeepCnt(int var0);

   public static native int getSoError(int var0);

   public static native void setKeepAlive(int var0, int var1);

   public static native void setReceiveBufferSize(int var0, int var1);

   public static native void setReuseAddress(int var0, int var1);

   public static native void setReusePort(int var0, int var1);

   public static native void setSendBufferSize(int var0, int var1);

   public static native void setTcpNoDelay(int var0, int var1);

   public static native void setTcpCork(int var0, int var1);

   public static native void setSoLinger(int var0, int var1);

   public static native void setTrafficClass(int var0, int var1);

   public static native void setBroadcast(int var0, int var1);

   public static native void setTcpKeepIdle(int var0, int var1);

   public static native void setTcpKeepIntvl(int var0, int var1);

   public static native void setTcpKeepCnt(int var0, int var1);

   public static void tcpInfo(int fd, EpollTcpInfo info) {
      tcpInfo0(fd, info.info);
   }

   private static native void tcpInfo0(int var0, int[] var1);

   private static Native.NativeInetAddress toNativeInetAddress(InetAddress addr) {
      byte[] bytes = addr.getAddress();
      return addr instanceof Inet6Address ? new Native.NativeInetAddress(bytes, ((Inet6Address)addr).getScopeId()) : new Native.NativeInetAddress(ipv4MappedIpv6Address(bytes));
   }

   static byte[] ipv4MappedIpv6Address(byte[] ipv4) {
      byte[] address = new byte[16];
      System.arraycopy(IPV4_MAPPED_IPV6_PREFIX, 0, address, 0, IPV4_MAPPED_IPV6_PREFIX.length);
      System.arraycopy(ipv4, 0, address, 12, ipv4.length);
      return address;
   }

   public static native String kernelVersion();

   private static native int iovMax();

   private static native int uioMaxIov();

   public static native int sizeofEpollEvent();

   public static native int offsetofEpollData();

   private static native int epollin();

   private static native int epollout();

   private static native int epollrdhup();

   private static native int epollet();

   private Native() {
   }

   static {
      String name = SystemPropertyUtil.get("os.name").toLowerCase(Locale.UK).trim();
      if (!name.startsWith("linux")) {
         throw new IllegalStateException("Only supported on Linux");
      } else {
         NativeLibraryLoader.load("netty-transport-native-epoll", PlatformDependent.getClassLoader(Native.class));
         EPOLLIN = epollin();
         EPOLLOUT = epollout();
         EPOLLRDHUP = epollrdhup();
         EPOLLET = epollet();
         IOV_MAX = iovMax();
         UIO_MAX_IOV = uioMaxIov();
         IS_SUPPORTING_SENDMMSG = isSupportingSendmmsg();
         IPV4_MAPPED_IPV6_PREFIX = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1};
         ERRNO_EBADF_NEGATIVE = -errnoEBADF();
         ERRNO_EPIPE_NEGATIVE = -errnoEPIPE();
         ERRNO_ECONNRESET_NEGATIVE = -errnoECONNRESET();
         ERRNO_EAGAIN_NEGATIVE = -errnoEAGAIN();
         ERRNO_EWOULDBLOCK_NEGATIVE = -errnoEWOULDBLOCK();
         ERRNO_EINPROGRESS_NEGATIVE = -errnoEINPROGRESS();
         ERRORS = new String[1024];

         for(int i = 0; i < ERRORS.length; ++i) {
            ERRORS[i] = strError(i);
         }

         CONNECTION_RESET_EXCEPTION_READ = newConnectionResetException("syscall:read(...)", ERRNO_ECONNRESET_NEGATIVE);
         CONNECTION_RESET_EXCEPTION_WRITE = newConnectionResetException("syscall:write(...)", ERRNO_EPIPE_NEGATIVE);
         CONNECTION_RESET_EXCEPTION_WRITEV = newConnectionResetException("syscall:writev(...)", ERRNO_EPIPE_NEGATIVE);
         CONNECTION_RESET_EXCEPTION_SENDFILE = newConnectionResetException("syscall:sendfile(...)", ERRNO_EPIPE_NEGATIVE);
         CONNECTION_RESET_EXCEPTION_SENDTO = newConnectionResetException("syscall:sendto(...)", ERRNO_EPIPE_NEGATIVE);
         CONNECTION_RESET_EXCEPTION_SENDMSG = newConnectionResetException("syscall:sendmsg(...)", ERRNO_EPIPE_NEGATIVE);
         CONNECTION_RESET_EXCEPTION_SENDMMSG = newConnectionResetException("syscall:sendmmsg(...)", ERRNO_EPIPE_NEGATIVE);
         CLOSED_CHANNEL_EXCEPTION = new ClosedChannelException();
         CLOSED_CHANNEL_EXCEPTION.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
      }
   }

   private static class NativeInetAddress {
      final byte[] address;
      final int scopeId;

      NativeInetAddress(byte[] address, int scopeId) {
         this.address = address;
         this.scopeId = scopeId;
      }

      NativeInetAddress(byte[] address) {
         this(address, 0);
      }
   }
}
