package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionBindingEvent;
import javax.net.ssl.SSLSessionBindingListener;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;
import org.apache.tomcat.jni.Buffer;
import org.apache.tomcat.jni.SSL;

public final class OpenSslEngine extends SSLEngine {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(OpenSslEngine.class);
   private static final Certificate[] EMPTY_CERTIFICATES = new Certificate[0];
   private static final SSLException ENGINE_CLOSED = new SSLException("engine closed");
   private static final SSLException RENEGOTIATION_UNSUPPORTED = new SSLException("renegotiation unsupported");
   private static final SSLException ENCRYPTED_PACKET_OVERSIZED = new SSLException("encrypted packet oversized");
   private static final int MAX_PLAINTEXT_LENGTH = 16384;
   private static final int MAX_COMPRESSED_LENGTH = 17408;
   private static final int MAX_CIPHERTEXT_LENGTH = 18432;
   private static final String PROTOCOL_SSL_V2_HELLO = "SSLv2Hello";
   private static final String PROTOCOL_SSL_V2 = "SSLv2";
   private static final String PROTOCOL_SSL_V3 = "SSLv3";
   private static final String PROTOCOL_TLS_V1 = "TLSv1";
   private static final String PROTOCOL_TLS_V1_1 = "TLSv1.1";
   private static final String PROTOCOL_TLS_V1_2 = "TLSv1.2";
   private static final String[] SUPPORTED_PROTOCOLS;
   private static final Set<String> SUPPORTED_PROTOCOLS_SET;
   static final int MAX_ENCRYPTED_PACKET_LENGTH = 18713;
   static final int MAX_ENCRYPTION_OVERHEAD_LENGTH = 2329;
   private static final AtomicIntegerFieldUpdater<OpenSslEngine> DESTROYED_UPDATER;
   private static final AtomicReferenceFieldUpdater<OpenSslEngine, SSLSession> SESSION_UPDATER;
   private static final String INVALID_CIPHER = "SSL_NULL_WITH_NULL_NULL";
   private static final long EMPTY_ADDR;
   private long ssl;
   private long networkBIO;
   private int accepted;
   private boolean handshakeFinished;
   private boolean receivedShutdown;
   private volatile int destroyed;
   private volatile String cipher;
   private volatile String applicationProtocol;
   private volatile Certificate[] peerCerts;
   private volatile OpenSslEngine.ClientAuthMode clientAuth;
   private boolean isInboundDone;
   private boolean isOutboundDone;
   private boolean engineClosed;
   private final boolean clientMode;
   private final ByteBufAllocator alloc;
   private final String fallbackApplicationProtocol;
   private final OpenSslSessionContext sessionContext;
   private volatile SSLSession session;

   /** @deprecated */
   @Deprecated
   public OpenSslEngine(long sslCtx, ByteBufAllocator alloc, String fallbackApplicationProtocol) {
      this(sslCtx, alloc, fallbackApplicationProtocol, false, (OpenSslSessionContext)null);
   }

   OpenSslEngine(long sslCtx, ByteBufAllocator alloc, String fallbackApplicationProtocol, boolean clientMode, OpenSslSessionContext sessionContext) {
      this.clientAuth = OpenSslEngine.ClientAuthMode.NONE;
      OpenSsl.ensureAvailability();
      if (sslCtx == 0L) {
         throw new NullPointerException("sslContext");
      } else if (alloc == null) {
         throw new NullPointerException("alloc");
      } else {
         this.alloc = alloc;
         this.ssl = SSL.newSSL(sslCtx, !clientMode);
         this.networkBIO = SSL.makeNetworkBIO(this.ssl);
         this.fallbackApplicationProtocol = fallbackApplicationProtocol;
         this.clientMode = clientMode;
         this.sessionContext = sessionContext;
      }
   }

   public synchronized void shutdown() {
      if (DESTROYED_UPDATER.compareAndSet(this, 0, 1)) {
         SSL.freeSSL(this.ssl);
         SSL.freeBIO(this.networkBIO);
         this.ssl = this.networkBIO = 0L;
         this.isInboundDone = this.isOutboundDone = this.engineClosed = true;
      }

   }

   private int writePlaintextData(ByteBuffer src) {
      int pos = src.position();
      int limit = src.limit();
      int len = Math.min(limit - pos, 16384);
      int sslWrote;
      if (src.isDirect()) {
         long addr = Buffer.address(src) + (long)pos;
         sslWrote = SSL.writeToSSL(this.ssl, addr, len);
         if (sslWrote > 0) {
            src.position(pos + sslWrote);
            return sslWrote;
         }
      } else {
         ByteBuf buf = this.alloc.directBuffer(len);

         try {
            long addr = memoryAddress(buf);
            src.limit(pos + len);
            buf.setBytes(0, (ByteBuffer)src);
            src.limit(limit);
            sslWrote = SSL.writeToSSL(this.ssl, addr, len);
            if (sslWrote > 0) {
               src.position(pos + sslWrote);
               int var9 = sslWrote;
               return var9;
            }

            src.position(pos);
         } finally {
            buf.release();
         }
      }

      throw new IllegalStateException("SSL.writeToSSL() returned a non-positive value: " + sslWrote);
   }

   private int writeEncryptedData(ByteBuffer src) {
      int pos = src.position();
      int len = src.remaining();
      if (src.isDirect()) {
         long addr = Buffer.address(src) + (long)pos;
         int netWrote = SSL.writeToBIO(this.networkBIO, addr, len);
         if (netWrote >= 0) {
            src.position(pos + netWrote);
            return netWrote;
         }
      } else {
         ByteBuf buf = this.alloc.directBuffer(len);

         try {
            long addr = memoryAddress(buf);
            buf.setBytes(0, (ByteBuffer)src);
            int netWrote = SSL.writeToBIO(this.networkBIO, addr, len);
            if (netWrote >= 0) {
               src.position(pos + netWrote);
               int var8 = netWrote;
               return var8;
            }

            src.position(pos);
         } finally {
            buf.release();
         }
      }

      return -1;
   }

   private int readPlaintextData(ByteBuffer dst) {
      int pos;
      if (dst.isDirect()) {
         pos = dst.position();
         long addr = Buffer.address(dst) + (long)pos;
         int len = dst.limit() - pos;
         int sslRead = SSL.readFromSSL(this.ssl, addr, len);
         if (sslRead > 0) {
            dst.position(pos + sslRead);
            return sslRead;
         } else {
            return 0;
         }
      } else {
         pos = dst.position();
         int limit = dst.limit();
         int len = Math.min(18713, limit - pos);
         ByteBuf buf = this.alloc.directBuffer(len);

         int var9;
         try {
            long addr = memoryAddress(buf);
            int sslRead = SSL.readFromSSL(this.ssl, addr, len);
            if (sslRead <= 0) {
               return 0;
            }

            dst.limit(pos + sslRead);
            buf.getBytes(0, (ByteBuffer)dst);
            dst.limit(limit);
            var9 = sslRead;
         } finally {
            buf.release();
         }

         return var9;
      }
   }

   private int readEncryptedData(ByteBuffer dst, int pending) {
      long addr;
      int bioRead;
      if (dst.isDirect() && dst.remaining() >= pending) {
         int pos = dst.position();
         addr = Buffer.address(dst) + (long)pos;
         bioRead = SSL.readFromBIO(this.networkBIO, addr, pending);
         if (bioRead > 0) {
            dst.position(pos + bioRead);
            return bioRead;
         }
      } else {
         ByteBuf buf = this.alloc.directBuffer(pending);

         try {
            addr = memoryAddress(buf);
            bioRead = SSL.readFromBIO(this.networkBIO, addr, pending);
            if (bioRead > 0) {
               int oldLimit = dst.limit();
               dst.limit(dst.position() + bioRead);
               buf.getBytes(0, (ByteBuffer)dst);
               dst.limit(oldLimit);
               int var8 = bioRead;
               return var8;
            }
         } finally {
            buf.release();
         }
      }

      return 0;
   }

   public synchronized SSLEngineResult wrap(ByteBuffer[] srcs, int offset, int length, ByteBuffer dst) throws SSLException {
      if (this.destroyed != 0) {
         return new SSLEngineResult(Status.CLOSED, HandshakeStatus.NOT_HANDSHAKING, 0, 0);
      } else if (srcs == null) {
         throw new IllegalArgumentException("srcs is null");
      } else if (dst == null) {
         throw new IllegalArgumentException("dst is null");
      } else if (offset < srcs.length && offset + length <= srcs.length) {
         if (dst.isReadOnly()) {
            throw new ReadOnlyBufferException();
         } else {
            if (this.accepted == 0) {
               this.beginHandshakeImplicitly();
            }

            HandshakeStatus handshakeStatus = this.getHandshakeStatus();
            if ((!this.handshakeFinished || this.engineClosed) && handshakeStatus == HandshakeStatus.NEED_UNWRAP) {
               return new SSLEngineResult(this.getEngineStatus(), HandshakeStatus.NEED_UNWRAP, 0, 0);
            } else {
               int bytesProduced = 0;
               int pendingNet = SSL.pendingWrittenBytesInBIO(this.networkBIO);
               int bytesConsumed;
               int bytesProduced;
               if (pendingNet > 0) {
                  bytesConsumed = dst.remaining();
                  if (bytesConsumed < pendingNet) {
                     return new SSLEngineResult(Status.BUFFER_OVERFLOW, handshakeStatus, 0, bytesProduced);
                  } else {
                     try {
                        bytesProduced = bytesProduced + this.readEncryptedData(dst, pendingNet);
                     } catch (Exception var14) {
                        throw new SSLException(var14);
                     }

                     if (this.isOutboundDone) {
                        this.shutdown();
                     }

                     return new SSLEngineResult(this.getEngineStatus(), this.getHandshakeStatus(), 0, bytesProduced);
                  }
               } else {
                  bytesConsumed = 0;
                  int endOffset = offset + length;

                  for(int i = offset; i < endOffset; ++i) {
                     ByteBuffer src = srcs[i];
                     if (src == null) {
                        throw new IllegalArgumentException("srcs[" + i + "] is null");
                     }

                     while(src.hasRemaining()) {
                        try {
                           bytesConsumed += this.writePlaintextData(src);
                        } catch (Exception var16) {
                           throw new SSLException(var16);
                        }

                        pendingNet = SSL.pendingWrittenBytesInBIO(this.networkBIO);
                        if (pendingNet > 0) {
                           int capacity = dst.remaining();
                           if (capacity < pendingNet) {
                              return new SSLEngineResult(Status.BUFFER_OVERFLOW, this.getHandshakeStatus(), bytesConsumed, bytesProduced);
                           }

                           try {
                              bytesProduced = bytesProduced + this.readEncryptedData(dst, pendingNet);
                           } catch (Exception var15) {
                              throw new SSLException(var15);
                           }

                           return new SSLEngineResult(this.getEngineStatus(), this.getHandshakeStatus(), bytesConsumed, bytesProduced);
                        }
                     }
                  }

                  return new SSLEngineResult(this.getEngineStatus(), this.getHandshakeStatus(), bytesConsumed, bytesProduced);
               }
            }
         }
      } else {
         throw new IndexOutOfBoundsException("offset: " + offset + ", length: " + length + " (expected: offset <= offset + length <= srcs.length (" + srcs.length + "))");
      }
   }

   public synchronized SSLEngineResult unwrap(ByteBuffer[] srcs, int srcsOffset, int srcsLength, ByteBuffer[] dsts, int dstsOffset, int dstsLength) throws SSLException {
      if (this.destroyed != 0) {
         return new SSLEngineResult(Status.CLOSED, HandshakeStatus.NOT_HANDSHAKING, 0, 0);
      } else if (srcs == null) {
         throw new NullPointerException("srcs");
      } else if (srcsOffset < srcs.length && srcsOffset + srcsLength <= srcs.length) {
         if (dsts == null) {
            throw new IllegalArgumentException("dsts is null");
         } else if (dstsOffset < dsts.length && dstsOffset + dstsLength <= dsts.length) {
            int capacity = 0;
            int endOffset = dstsOffset + dstsLength;

            for(int i = dstsOffset; i < endOffset; ++i) {
               ByteBuffer dst = dsts[i];
               if (dst == null) {
                  throw new IllegalArgumentException("dsts[" + i + "] is null");
               }

               if (dst.isReadOnly()) {
                  throw new ReadOnlyBufferException();
               }

               capacity += dst.remaining();
            }

            if (this.accepted == 0) {
               this.beginHandshakeImplicitly();
            }

            HandshakeStatus handshakeStatus = this.getHandshakeStatus();
            if ((!this.handshakeFinished || this.engineClosed) && handshakeStatus == HandshakeStatus.NEED_WRAP) {
               return new SSLEngineResult(this.getEngineStatus(), HandshakeStatus.NEED_WRAP, 0, 0);
            } else {
               int srcsEndOffset = srcsOffset + srcsLength;
               int len = 0;

               int bytesConsumed;
               ByteBuffer src;
               for(bytesConsumed = srcsOffset; bytesConsumed < srcsEndOffset; ++bytesConsumed) {
                  src = srcs[bytesConsumed];
                  if (src == null) {
                     throw new IllegalArgumentException("srcs[" + bytesConsumed + "] is null");
                  }

                  len += src.remaining();
               }

               if (len > 18713) {
                  this.isInboundDone = true;
                  this.isOutboundDone = true;
                  this.engineClosed = true;
                  this.shutdown();
                  throw ENCRYPTED_PACKET_OVERSIZED;
               } else {
                  bytesConsumed = -1;

                  int bytesProduced;
                  int idx;
                  try {
                     while(srcsOffset < srcsEndOffset) {
                        src = srcs[srcsOffset];
                        bytesProduced = src.remaining();
                        idx = this.writeEncryptedData(src);
                        if (idx < 0) {
                           break;
                        }

                        if (bytesConsumed == -1) {
                           bytesConsumed = idx;
                        } else {
                           bytesConsumed += idx;
                        }

                        if (idx == bytesProduced) {
                           ++srcsOffset;
                        } else if (idx == 0) {
                           break;
                        }
                     }
                  } catch (Exception var20) {
                     throw new SSLException(var20);
                  }

                  int pendingApp;
                  if (bytesConsumed >= 0) {
                     pendingApp = SSL.readFromSSL(this.ssl, EMPTY_ADDR, 0);
                     if (pendingApp <= 0) {
                        long error = (long)SSL.getLastErrorNumber();
                        if (OpenSsl.isError(error)) {
                           String err = SSL.getErrorString(error);
                           if (logger.isDebugEnabled()) {
                              logger.debug("SSL_read failed: primingReadResult: " + pendingApp + "; OpenSSL error: '" + err + '\'');
                           }

                           this.shutdown();
                           throw new SSLException(err);
                        }
                     }
                  } else {
                     bytesConsumed = 0;
                  }

                  pendingApp = !this.handshakeFinished && SSL.isInInit(this.ssl) != 0 ? 0 : SSL.pendingReadableBytesInSSL(this.ssl);
                  bytesProduced = 0;
                  if (pendingApp > 0) {
                     if (capacity < pendingApp) {
                        return new SSLEngineResult(Status.BUFFER_OVERFLOW, this.getHandshakeStatus(), bytesConsumed, 0);
                     }

                     idx = dstsOffset;

                     while(idx < endOffset) {
                        ByteBuffer dst = dsts[idx];
                        if (!dst.hasRemaining()) {
                           ++idx;
                        } else {
                           if (pendingApp <= 0) {
                              break;
                           }

                           int bytesRead;
                           try {
                              bytesRead = this.readPlaintextData(dst);
                           } catch (Exception var19) {
                              throw new SSLException(var19);
                           }

                           if (bytesRead == 0) {
                              break;
                           }

                           bytesProduced += bytesRead;
                           pendingApp -= bytesRead;
                           if (!dst.hasRemaining()) {
                              ++idx;
                           }
                        }
                     }
                  }

                  if (!this.receivedShutdown && (SSL.getShutdown(this.ssl) & 2) == 2) {
                     this.receivedShutdown = true;
                     this.closeOutbound();
                     this.closeInbound();
                  }

                  return new SSLEngineResult(this.getEngineStatus(), this.getHandshakeStatus(), bytesConsumed, bytesProduced);
               }
            }
         } else {
            throw new IndexOutOfBoundsException("offset: " + dstsOffset + ", length: " + dstsLength + " (expected: offset <= offset + length <= dsts.length (" + dsts.length + "))");
         }
      } else {
         throw new IndexOutOfBoundsException("offset: " + srcsOffset + ", length: " + srcsLength + " (expected: offset <= offset + length <= srcs.length (" + srcs.length + "))");
      }
   }

   public SSLEngineResult unwrap(ByteBuffer[] srcs, ByteBuffer[] dsts) throws SSLException {
      return this.unwrap(srcs, 0, srcs.length, dsts, 0, dsts.length);
   }

   public SSLEngineResult unwrap(ByteBuffer src, ByteBuffer[] dsts, int offset, int length) throws SSLException {
      return this.unwrap(new ByteBuffer[]{src}, 0, 1, dsts, offset, length);
   }

   public Runnable getDelegatedTask() {
      return null;
   }

   public synchronized void closeInbound() throws SSLException {
      if (!this.isInboundDone) {
         this.isInboundDone = true;
         this.engineClosed = true;
         this.shutdown();
         if (this.accepted != 0 && !this.receivedShutdown) {
            throw new SSLException("Inbound closed before receiving peer's close_notify: possible truncation attack?");
         }
      }
   }

   public synchronized boolean isInboundDone() {
      return this.isInboundDone || this.engineClosed;
   }

   public synchronized void closeOutbound() {
      if (!this.isOutboundDone) {
         this.isOutboundDone = true;
         this.engineClosed = true;
         if (this.accepted != 0 && this.destroyed == 0) {
            int mode = SSL.getShutdown(this.ssl);
            if ((mode & 1) != 1) {
               SSL.shutdownSSL(this.ssl);
            }
         } else {
            this.shutdown();
         }

      }
   }

   public synchronized boolean isOutboundDone() {
      return this.isOutboundDone;
   }

   public String[] getSupportedCipherSuites() {
      Set<String> availableCipherSuites = OpenSsl.availableCipherSuites();
      return (String[])availableCipherSuites.toArray(new String[availableCipherSuites.size()]);
   }

   public String[] getEnabledCipherSuites() {
      String[] enabled = SSL.getCiphers(this.ssl);
      if (enabled == null) {
         return EmptyArrays.EMPTY_STRINGS;
      } else {
         for(int i = 0; i < enabled.length; ++i) {
            String mapped = this.toJavaCipherSuite(enabled[i]);
            if (mapped != null) {
               enabled[i] = mapped;
            }
         }

         return enabled;
      }
   }

   public void setEnabledCipherSuites(String[] cipherSuites) {
      if (cipherSuites == null) {
         throw new NullPointerException("cipherSuites");
      } else {
         StringBuilder buf = new StringBuilder();
         String[] arr$ = cipherSuites;
         int len$ = cipherSuites.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String c = arr$[i$];
            if (c == null) {
               break;
            }

            String converted = CipherSuiteConverter.toOpenSsl(c);
            if (converted == null) {
               converted = c;
            }

            if (!OpenSsl.isCipherSuiteAvailable(converted)) {
               throw new IllegalArgumentException("unsupported cipher suite: " + c + '(' + converted + ')');
            }

            buf.append(converted);
            buf.append(':');
         }

         if (buf.length() == 0) {
            throw new IllegalArgumentException("empty cipher suites");
         } else {
            buf.setLength(buf.length() - 1);
            String cipherSuiteSpec = buf.toString();

            try {
               SSL.setCipherSuites(this.ssl, cipherSuiteSpec);
            } catch (Exception var8) {
               throw new IllegalStateException("failed to enable cipher suites: " + cipherSuiteSpec, var8);
            }
         }
      }
   }

   public String[] getSupportedProtocols() {
      return (String[])SUPPORTED_PROTOCOLS.clone();
   }

   public String[] getEnabledProtocols() {
      List<String> enabled = new ArrayList();
      enabled.add("SSLv2Hello");
      int opts = SSL.getOptions(this.ssl);
      if ((opts & 67108864) == 0) {
         enabled.add("TLSv1");
      }

      if ((opts & 134217728) == 0) {
         enabled.add("TLSv1.1");
      }

      if ((opts & 268435456) == 0) {
         enabled.add("TLSv1.2");
      }

      if ((opts & 16777216) == 0) {
         enabled.add("SSLv2");
      }

      if ((opts & 33554432) == 0) {
         enabled.add("SSLv3");
      }

      int size = enabled.size();
      return size == 0 ? EmptyArrays.EMPTY_STRINGS : (String[])enabled.toArray(new String[size]);
   }

   public void setEnabledProtocols(String[] protocols) {
      if (protocols == null) {
         throw new IllegalArgumentException();
      } else {
         boolean sslv2 = false;
         boolean sslv3 = false;
         boolean tlsv1 = false;
         boolean tlsv1_1 = false;
         boolean tlsv1_2 = false;
         String[] arr$ = protocols;
         int len$ = protocols.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String p = arr$[i$];
            if (!SUPPORTED_PROTOCOLS_SET.contains(p)) {
               throw new IllegalArgumentException("Protocol " + p + " is not supported.");
            }

            if (p.equals("SSLv2")) {
               sslv2 = true;
            } else if (p.equals("SSLv3")) {
               sslv3 = true;
            } else if (p.equals("TLSv1")) {
               tlsv1 = true;
            } else if (p.equals("TLSv1.1")) {
               tlsv1_1 = true;
            } else if (p.equals("TLSv1.2")) {
               tlsv1_2 = true;
            }
         }

         SSL.setOptions(this.ssl, 4095);
         if (!sslv2) {
            SSL.setOptions(this.ssl, 16777216);
         }

         if (!sslv3) {
            SSL.setOptions(this.ssl, 33554432);
         }

         if (!tlsv1) {
            SSL.setOptions(this.ssl, 67108864);
         }

         if (!tlsv1_1) {
            SSL.setOptions(this.ssl, 134217728);
         }

         if (!tlsv1_2) {
            SSL.setOptions(this.ssl, 268435456);
         }

      }
   }

   private Certificate[] initPeerCertChain() throws SSLPeerUnverifiedException {
      byte[][] chain = SSL.getPeerCertChain(this.ssl);
      byte[] clientCert;
      if (!this.clientMode) {
         clientCert = SSL.getPeerCertificate(this.ssl);
      } else {
         clientCert = null;
      }

      if (chain == null && clientCert == null) {
         throw new SSLPeerUnverifiedException("peer not verified");
      } else {
         int len = 0;
         if (chain != null) {
            len += chain.length;
         }

         int i = 0;
         Certificate[] peerCerts;
         if (clientCert != null) {
            ++len;
            peerCerts = new Certificate[len];
            peerCerts[i++] = new OpenSslX509Certificate(clientCert);
         } else {
            peerCerts = new Certificate[len];
         }

         if (chain != null) {
            for(int var6 = 0; i < peerCerts.length; ++i) {
               peerCerts[i] = new OpenSslX509Certificate(chain[var6++]);
            }
         }

         return peerCerts;
      }
   }

   public SSLSession getSession() {
      SSLSession session = this.session;
      if (session == null) {
         session = new SSLSession() {
            private X509Certificate[] x509PeerCerts;
            private Map<String, Object> values;

            public byte[] getId() {
               byte[] id = SSL.getSessionId(OpenSslEngine.this.ssl);
               if (id == null) {
                  throw new IllegalStateException("SSL session ID not available");
               } else {
                  return id;
               }
            }

            public SSLSessionContext getSessionContext() {
               return OpenSslEngine.this.sessionContext;
            }

            public long getCreationTime() {
               return SSL.getTime(OpenSslEngine.this.ssl) * 1000L;
            }

            public long getLastAccessedTime() {
               return this.getCreationTime();
            }

            public void invalidate() {
            }

            public boolean isValid() {
               return false;
            }

            public void putValue(String name, Object value) {
               if (name == null) {
                  throw new NullPointerException("name");
               } else if (value == null) {
                  throw new NullPointerException("value");
               } else {
                  Map<String, Object> values = this.values;
                  if (values == null) {
                     values = this.values = new HashMap(2);
                  }

                  Object old = values.put(name, value);
                  if (value instanceof SSLSessionBindingListener) {
                     ((SSLSessionBindingListener)value).valueBound(new SSLSessionBindingEvent(this, name));
                  }

                  this.notifyUnbound(old, name);
               }
            }

            public Object getValue(String name) {
               if (name == null) {
                  throw new NullPointerException("name");
               } else {
                  return this.values == null ? null : this.values.get(name);
               }
            }

            public void removeValue(String name) {
               if (name == null) {
                  throw new NullPointerException("name");
               } else {
                  Map<String, Object> values = this.values;
                  if (values != null) {
                     Object old = values.remove(name);
                     this.notifyUnbound(old, name);
                  }
               }
            }

            public String[] getValueNames() {
               Map<String, Object> values = this.values;
               return values != null && !values.isEmpty() ? (String[])values.keySet().toArray(new String[values.size()]) : EmptyArrays.EMPTY_STRINGS;
            }

            private void notifyUnbound(Object value, String name) {
               if (value instanceof SSLSessionBindingListener) {
                  ((SSLSessionBindingListener)value).valueUnbound(new SSLSessionBindingEvent(this, name));
               }

            }

            public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
               Certificate[] c = OpenSslEngine.this.peerCerts;
               if (c == null) {
                  if (SSL.isInInit(OpenSslEngine.this.ssl) != 0) {
                     throw new SSLPeerUnverifiedException("peer not verified");
                  }

                  c = OpenSslEngine.this.peerCerts = OpenSslEngine.this.initPeerCertChain();
               }

               return c;
            }

            public Certificate[] getLocalCertificates() {
               return OpenSslEngine.EMPTY_CERTIFICATES;
            }

            public X509Certificate[] getPeerCertificateChain() throws SSLPeerUnverifiedException {
               X509Certificate[] c = this.x509PeerCerts;
               if (c == null) {
                  if (SSL.isInInit(OpenSslEngine.this.ssl) != 0) {
                     throw new SSLPeerUnverifiedException("peer not verified");
                  }

                  byte[][] chain = SSL.getPeerCertChain(OpenSslEngine.this.ssl);
                  if (chain == null) {
                     throw new SSLPeerUnverifiedException("peer not verified");
                  }

                  X509Certificate[] peerCerts = new X509Certificate[chain.length];

                  for(int i = 0; i < peerCerts.length; ++i) {
                     try {
                        peerCerts[i] = X509Certificate.getInstance(chain[i]);
                     } catch (CertificateException var6) {
                        throw new IllegalStateException(var6);
                     }
                  }

                  c = this.x509PeerCerts = peerCerts;
               }

               return c;
            }

            public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
               Certificate[] peer = this.getPeerCertificates();
               return peer != null && peer.length != 0 ? this.principal(peer) : null;
            }

            public Principal getLocalPrincipal() {
               Certificate[] local = this.getLocalCertificates();
               return local != null && local.length != 0 ? this.principal(local) : null;
            }

            private Principal principal(Certificate[] certs) {
               return ((java.security.cert.X509Certificate)certs[0]).getIssuerX500Principal();
            }

            public String getCipherSuite() {
               if (!OpenSslEngine.this.handshakeFinished) {
                  return "SSL_NULL_WITH_NULL_NULL";
               } else {
                  if (OpenSslEngine.this.cipher == null) {
                     String c = OpenSslEngine.this.toJavaCipherSuite(SSL.getCipherForSSL(OpenSslEngine.this.ssl));
                     if (c != null) {
                        OpenSslEngine.this.cipher = c;
                     }
                  }

                  return OpenSslEngine.this.cipher;
               }
            }

            public String getProtocol() {
               String applicationProtocol = OpenSslEngine.this.applicationProtocol;
               if (applicationProtocol == null) {
                  applicationProtocol = SSL.getNextProtoNegotiated(OpenSslEngine.this.ssl);
                  if (applicationProtocol == null) {
                     applicationProtocol = OpenSslEngine.this.fallbackApplicationProtocol;
                  }

                  if (applicationProtocol != null) {
                     OpenSslEngine.this.applicationProtocol = applicationProtocol.replace(':', '_');
                  } else {
                     applicationProtocol = "";
                     OpenSslEngine.this.applicationProtocol = "";
                  }
               }

               String version = SSL.getVersion(OpenSslEngine.this.ssl);
               return applicationProtocol.isEmpty() ? version : version + ':' + applicationProtocol;
            }

            public String getPeerHost() {
               return null;
            }

            public int getPeerPort() {
               return 0;
            }

            public int getPacketBufferSize() {
               return 18713;
            }

            public int getApplicationBufferSize() {
               return 16384;
            }
         };
         if (!SESSION_UPDATER.compareAndSet(this, (Object)null, session)) {
            session = this.session;
         }
      }

      return session;
   }

   public synchronized void beginHandshake() throws SSLException {
      if (!this.engineClosed && this.destroyed == 0) {
         switch(this.accepted) {
         case 0:
            this.handshake();
            this.accepted = 2;
            break;
         case 1:
            this.accepted = 2;
            break;
         case 2:
            throw RENEGOTIATION_UNSUPPORTED;
         default:
            throw new Error();
         }

      } else {
         throw ENGINE_CLOSED;
      }
   }

   private void beginHandshakeImplicitly() throws SSLException {
      if (!this.engineClosed && this.destroyed == 0) {
         if (this.accepted == 0) {
            this.handshake();
            this.accepted = 1;
         }

      } else {
         throw ENGINE_CLOSED;
      }
   }

   private void handshake() throws SSLException {
      int code = SSL.doHandshake(this.ssl);
      if (code <= 0) {
         long error = (long)SSL.getLastErrorNumber();
         if (OpenSsl.isError(error)) {
            String err = SSL.getErrorString(error);
            if (logger.isDebugEnabled()) {
               logger.debug("SSL_do_handshake failed: OpenSSL error: '" + err + '\'');
            }

            this.shutdown();
            throw new SSLException(err);
         }
      } else {
         this.handshakeFinished = true;
      }

   }

   private static long memoryAddress(ByteBuf buf) {
      return buf.hasMemoryAddress() ? buf.memoryAddress() : Buffer.address(buf.nioBuffer());
   }

   private Status getEngineStatus() {
      return this.engineClosed ? Status.CLOSED : Status.OK;
   }

   public synchronized HandshakeStatus getHandshakeStatus() {
      if (this.accepted != 0 && this.destroyed == 0) {
         if (!this.handshakeFinished) {
            if (SSL.pendingWrittenBytesInBIO(this.networkBIO) != 0) {
               return HandshakeStatus.NEED_WRAP;
            } else if (SSL.isInInit(this.ssl) == 0) {
               this.handshakeFinished = true;
               return HandshakeStatus.FINISHED;
            } else {
               return HandshakeStatus.NEED_UNWRAP;
            }
         } else if (this.engineClosed) {
            return SSL.pendingWrittenBytesInBIO(this.networkBIO) != 0 ? HandshakeStatus.NEED_WRAP : HandshakeStatus.NEED_UNWRAP;
         } else {
            return HandshakeStatus.NOT_HANDSHAKING;
         }
      } else {
         return HandshakeStatus.NOT_HANDSHAKING;
      }
   }

   private String toJavaCipherSuite(String openSslCipherSuite) {
      if (openSslCipherSuite == null) {
         return null;
      } else {
         String prefix = toJavaCipherSuitePrefix(SSL.getVersion(this.ssl));
         return CipherSuiteConverter.toJava(openSslCipherSuite, prefix);
      }
   }

   private static String toJavaCipherSuitePrefix(String protocolVersion) {
      char c;
      if (protocolVersion != null && protocolVersion.length() != 0) {
         c = protocolVersion.charAt(0);
      } else {
         c = 0;
      }

      switch(c) {
      case 'S':
         return "SSL";
      case 'T':
         return "TLS";
      default:
         return "UNKNOWN";
      }
   }

   public void setUseClientMode(boolean clientMode) {
      if (clientMode != this.clientMode) {
         throw new UnsupportedOperationException();
      }
   }

   public boolean getUseClientMode() {
      return this.clientMode;
   }

   public void setNeedClientAuth(boolean b) {
      this.setClientAuth(b ? OpenSslEngine.ClientAuthMode.REQUIRE : OpenSslEngine.ClientAuthMode.NONE);
   }

   public boolean getNeedClientAuth() {
      return this.clientAuth == OpenSslEngine.ClientAuthMode.REQUIRE;
   }

   public void setWantClientAuth(boolean b) {
      this.setClientAuth(b ? OpenSslEngine.ClientAuthMode.OPTIONAL : OpenSslEngine.ClientAuthMode.NONE);
   }

   public boolean getWantClientAuth() {
      return this.clientAuth == OpenSslEngine.ClientAuthMode.OPTIONAL;
   }

   private void setClientAuth(OpenSslEngine.ClientAuthMode mode) {
      if (!this.clientMode) {
         synchronized(this) {
            if (this.clientAuth != mode) {
               switch(mode) {
               case NONE:
                  SSL.setVerify(this.ssl, 0, 10);
                  break;
               case REQUIRE:
                  SSL.setVerify(this.ssl, 2, 10);
                  break;
               case OPTIONAL:
                  SSL.setVerify(this.ssl, 1, 10);
               }

               this.clientAuth = mode;
            }
         }
      }
   }

   public void setEnableSessionCreation(boolean b) {
      if (b) {
         throw new UnsupportedOperationException();
      }
   }

   public boolean getEnableSessionCreation() {
      return false;
   }

   protected void finalize() throws Throwable {
      super.finalize();
      this.shutdown();
   }

   static {
      ENGINE_CLOSED.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
      RENEGOTIATION_UNSUPPORTED.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
      ENCRYPTED_PACKET_OVERSIZED.setStackTrace(EmptyArrays.EMPTY_STACK_TRACE);
      AtomicIntegerFieldUpdater<OpenSslEngine> destroyedUpdater = PlatformDependent.newAtomicIntegerFieldUpdater(OpenSslEngine.class, "destroyed");
      if (destroyedUpdater == null) {
         destroyedUpdater = AtomicIntegerFieldUpdater.newUpdater(OpenSslEngine.class, "destroyed");
      }

      DESTROYED_UPDATER = destroyedUpdater;
      AtomicReferenceFieldUpdater<OpenSslEngine, SSLSession> sessionUpdater = PlatformDependent.newAtomicReferenceFieldUpdater(OpenSslEngine.class, "session");
      if (sessionUpdater == null) {
         sessionUpdater = AtomicReferenceFieldUpdater.newUpdater(OpenSslEngine.class, SSLSession.class, "session");
      }

      SESSION_UPDATER = sessionUpdater;
      SUPPORTED_PROTOCOLS = new String[]{"SSLv2Hello", "SSLv2", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"};
      SUPPORTED_PROTOCOLS_SET = new HashSet(Arrays.asList(SUPPORTED_PROTOCOLS));
      EMPTY_ADDR = Buffer.address(Unpooled.EMPTY_BUFFER.nioBuffer());
   }

   static enum ClientAuthMode {
      NONE,
      OPTIONAL,
      REQUIRE;
   }
}
