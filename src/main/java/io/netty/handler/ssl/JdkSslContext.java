package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

public abstract class JdkSslContext extends SslContext {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(JdkSslContext.class);
   static final String PROTOCOL = "TLS";
   static final String[] PROTOCOLS;
   static final List<String> DEFAULT_CIPHERS;
   static final Set<String> SUPPORTED_CIPHERS;
   private final String[] cipherSuites;
   private final List<String> unmodifiableCipherSuites;
   private final JdkApplicationProtocolNegotiator apn;

   private static void addIfSupported(Set<String> supported, List<String> enabled, String... names) {
      String[] arr$ = names;
      int len$ = names.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String n = arr$[i$];
         if (supported.contains(n)) {
            enabled.add(n);
         }
      }

   }

   JdkSslContext(Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig config, boolean isServer) {
      this(ciphers, cipherFilter, toNegotiator(config, isServer));
   }

   JdkSslContext(Iterable<String> ciphers, CipherSuiteFilter cipherFilter, JdkApplicationProtocolNegotiator apn) {
      this.apn = (JdkApplicationProtocolNegotiator)ObjectUtil.checkNotNull(apn, "apn");
      this.cipherSuites = ((CipherSuiteFilter)ObjectUtil.checkNotNull(cipherFilter, "cipherFilter")).filterCipherSuites(ciphers, DEFAULT_CIPHERS, SUPPORTED_CIPHERS);
      this.unmodifiableCipherSuites = Collections.unmodifiableList(Arrays.asList(this.cipherSuites));
   }

   public abstract SSLContext context();

   public final SSLSessionContext sessionContext() {
      return this.isServer() ? this.context().getServerSessionContext() : this.context().getClientSessionContext();
   }

   public final List<String> cipherSuites() {
      return this.unmodifiableCipherSuites;
   }

   public final long sessionCacheSize() {
      return (long)this.sessionContext().getSessionCacheSize();
   }

   public final long sessionTimeout() {
      return (long)this.sessionContext().getSessionTimeout();
   }

   public final SSLEngine newEngine(ByteBufAllocator alloc) {
      SSLEngine engine = this.context().createSSLEngine();
      engine.setEnabledCipherSuites(this.cipherSuites);
      engine.setEnabledProtocols(PROTOCOLS);
      engine.setUseClientMode(this.isClient());
      return this.wrapEngine(engine);
   }

   public final SSLEngine newEngine(ByteBufAllocator alloc, String peerHost, int peerPort) {
      SSLEngine engine = this.context().createSSLEngine(peerHost, peerPort);
      engine.setEnabledCipherSuites(this.cipherSuites);
      engine.setEnabledProtocols(PROTOCOLS);
      engine.setUseClientMode(this.isClient());
      return this.wrapEngine(engine);
   }

   private SSLEngine wrapEngine(SSLEngine engine) {
      return this.apn.wrapperFactory().wrapSslEngine(engine, this.apn, this.isServer());
   }

   public JdkApplicationProtocolNegotiator applicationProtocolNegotiator() {
      return this.apn;
   }

   static JdkApplicationProtocolNegotiator toNegotiator(ApplicationProtocolConfig config, boolean isServer) {
      if (config == null) {
         return JdkDefaultApplicationProtocolNegotiator.INSTANCE;
      } else {
         switch(config.protocol()) {
         case NONE:
            return JdkDefaultApplicationProtocolNegotiator.INSTANCE;
         case ALPN:
            if (isServer) {
               switch(config.selectorFailureBehavior()) {
               case FATAL_ALERT:
                  return new JdkAlpnApplicationProtocolNegotiator(true, config.supportedProtocols());
               case NO_ADVERTISE:
                  return new JdkAlpnApplicationProtocolNegotiator(false, config.supportedProtocols());
               default:
                  throw new UnsupportedOperationException("JDK provider does not support " + config.selectorFailureBehavior() + " failure behavior");
               }
            } else {
               switch(config.selectedListenerFailureBehavior()) {
               case ACCEPT:
                  return new JdkAlpnApplicationProtocolNegotiator(false, config.supportedProtocols());
               case FATAL_ALERT:
                  return new JdkAlpnApplicationProtocolNegotiator(true, config.supportedProtocols());
               default:
                  throw new UnsupportedOperationException("JDK provider does not support " + config.selectedListenerFailureBehavior() + " failure behavior");
               }
            }
         case NPN:
            if (isServer) {
               switch(config.selectedListenerFailureBehavior()) {
               case ACCEPT:
                  return new JdkNpnApplicationProtocolNegotiator(false, config.supportedProtocols());
               case FATAL_ALERT:
                  return new JdkNpnApplicationProtocolNegotiator(true, config.supportedProtocols());
               default:
                  throw new UnsupportedOperationException("JDK provider does not support " + config.selectedListenerFailureBehavior() + " failure behavior");
               }
            } else {
               switch(config.selectorFailureBehavior()) {
               case FATAL_ALERT:
                  return new JdkNpnApplicationProtocolNegotiator(true, config.supportedProtocols());
               case NO_ADVERTISE:
                  return new JdkNpnApplicationProtocolNegotiator(false, config.supportedProtocols());
               default:
                  throw new UnsupportedOperationException("JDK provider does not support " + config.selectorFailureBehavior() + " failure behavior");
               }
            }
         default:
            throw new UnsupportedOperationException("JDK provider does not support " + config.protocol() + " protocol");
         }
      }
   }

   protected static KeyManagerFactory buildKeyManagerFactory(File certChainFile, File keyFile, String keyPassword, KeyManagerFactory kmf) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException, CertificateException, KeyException, IOException {
      String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
      if (algorithm == null) {
         algorithm = "SunX509";
      }

      return buildKeyManagerFactory(certChainFile, algorithm, keyFile, keyPassword, kmf);
   }

   protected static KeyManagerFactory buildKeyManagerFactory(File certChainFile, String keyAlgorithm, File keyFile, String keyPassword, KeyManagerFactory kmf) throws KeyStoreException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException, IOException, CertificateException, KeyException, UnrecoverableKeyException {
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load((InputStream)null, (char[])null);
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      KeyFactory rsaKF = KeyFactory.getInstance("RSA");
      KeyFactory dsaKF = KeyFactory.getInstance("DSA");
      ByteBuf encodedKeyBuf = PemReader.readPrivateKey(keyFile);
      byte[] encodedKey = new byte[encodedKeyBuf.readableBytes()];
      encodedKeyBuf.readBytes(encodedKey).release();
      char[] keyPasswordChars = keyPassword == null ? EmptyArrays.EMPTY_CHARS : keyPassword.toCharArray();
      PKCS8EncodedKeySpec encodedKeySpec = generateKeySpec(keyPasswordChars, encodedKey);

      PrivateKey key;
      try {
         key = rsaKF.generatePrivate(encodedKeySpec);
      } catch (InvalidKeySpecException var28) {
         key = dsaKF.generatePrivate(encodedKeySpec);
      }

      List<Certificate> certChain = new ArrayList();
      ByteBuf[] certs = PemReader.readCertificates(certChainFile);
      boolean var27 = false;

      ByteBuf[] arr$;
      int len$;
      int i$;
      ByteBuf buf;
      try {
         var27 = true;
         arr$ = certs;
         len$ = certs.length;

         for(i$ = 0; i$ < len$; ++i$) {
            buf = arr$[i$];
            certChain.add(cf.generateCertificate(new ByteBufInputStream(buf)));
         }

         var27 = false;
      } finally {
         if (var27) {
            ByteBuf[] arr$ = certs;
            int len$ = certs.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               ByteBuf buf = arr$[i$];
               buf.release();
            }

         }
      }

      arr$ = certs;
      len$ = certs.length;

      for(i$ = 0; i$ < len$; ++i$) {
         buf = arr$[i$];
         buf.release();
      }

      ks.setKeyEntry("key", key, keyPasswordChars, (Certificate[])certChain.toArray(new Certificate[certChain.size()]));
      if (kmf == null) {
         kmf = KeyManagerFactory.getInstance(keyAlgorithm);
      }

      kmf.init(ks, keyPasswordChars);
      return kmf;
   }

   protected static TrustManagerFactory buildTrustManagerFactory(File certChainFile, TrustManagerFactory trustManagerFactory) throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException {
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load((InputStream)null, (char[])null);
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      ByteBuf[] certs = PemReader.readCertificates(certChainFile);
      boolean var17 = false;

      ByteBuf[] arr$;
      int len$;
      int i$;
      ByteBuf buf;
      try {
         var17 = true;
         arr$ = certs;
         len$ = certs.length;

         for(i$ = 0; i$ < len$; ++i$) {
            buf = arr$[i$];
            X509Certificate cert = (X509Certificate)cf.generateCertificate(new ByteBufInputStream(buf));
            X500Principal principal = cert.getSubjectX500Principal();
            ks.setCertificateEntry(principal.getName("RFC2253"), cert);
         }

         var17 = false;
      } finally {
         if (var17) {
            ByteBuf[] arr$ = certs;
            int len$ = certs.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               ByteBuf buf = arr$[i$];
               buf.release();
            }

         }
      }

      arr$ = certs;
      len$ = certs.length;

      for(i$ = 0; i$ < len$; ++i$) {
         buf = arr$[i$];
         buf.release();
      }

      if (trustManagerFactory == null) {
         trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      }

      trustManagerFactory.init(ks);
      return trustManagerFactory;
   }

   static {
      SSLContext context;
      try {
         context = SSLContext.getInstance("TLS");
         context.init((KeyManager[])null, (TrustManager[])null, (SecureRandom)null);
      } catch (Exception var8) {
         throw new Error("failed to initialize the default SSL context", var8);
      }

      SSLEngine engine = context.createSSLEngine();
      String[] supportedProtocols = engine.getSupportedProtocols();
      Set<String> supportedProtocolsSet = new HashSet(supportedProtocols.length);

      int i;
      for(i = 0; i < supportedProtocols.length; ++i) {
         supportedProtocolsSet.add(supportedProtocols[i]);
      }

      List<String> protocols = new ArrayList();
      addIfSupported(supportedProtocolsSet, protocols, "TLSv1.2", "TLSv1.1", "TLSv1");
      if (!protocols.isEmpty()) {
         PROTOCOLS = (String[])protocols.toArray(new String[protocols.size()]);
      } else {
         PROTOCOLS = engine.getEnabledProtocols();
      }

      String[] supportedCiphers = engine.getSupportedCipherSuites();
      SUPPORTED_CIPHERS = new HashSet(supportedCiphers.length);

      for(i = 0; i < supportedCiphers.length; ++i) {
         SUPPORTED_CIPHERS.add(supportedCiphers[i]);
      }

      List<String> ciphers = new ArrayList();
      addIfSupported(SUPPORTED_CIPHERS, ciphers, "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA", "TLS_RSA_WITH_AES_128_GCM_SHA256", "TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_RSA_WITH_RC4_128_SHA");
      if (!ciphers.isEmpty()) {
         DEFAULT_CIPHERS = Collections.unmodifiableList(ciphers);
      } else {
         DEFAULT_CIPHERS = Collections.unmodifiableList(Arrays.asList(engine.getEnabledCipherSuites()));
      }

      if (logger.isDebugEnabled()) {
         logger.debug("Default protocols (JDK): {} ", (Object)Arrays.asList(PROTOCOLS));
         logger.debug("Default cipher suites (JDK): {}", (Object)DEFAULT_CIPHERS);
      }

   }
}
