package io.netty.handler.ssl;

import java.io.File;
import java.security.SecureRandom;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.TrustManagerFactory;

public final class JdkSslClientContext extends JdkSslContext {
   private final SSLContext ctx;

   public JdkSslClientContext() throws SSLException {
      this((File)null, (TrustManagerFactory)null);
   }

   public JdkSslClientContext(File certChainFile) throws SSLException {
      this(certChainFile, (TrustManagerFactory)null);
   }

   public JdkSslClientContext(TrustManagerFactory trustManagerFactory) throws SSLException {
      this((File)null, trustManagerFactory);
   }

   public JdkSslClientContext(File certChainFile, TrustManagerFactory trustManagerFactory) throws SSLException {
      this(certChainFile, trustManagerFactory, (Iterable)null, IdentityCipherSuiteFilter.INSTANCE, (JdkApplicationProtocolNegotiator)JdkDefaultApplicationProtocolNegotiator.INSTANCE, 0L, 0L);
   }

   public JdkSslClientContext(File certChainFile, TrustManagerFactory trustManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout) throws SSLException {
      this(certChainFile, trustManagerFactory, ciphers, cipherFilter, toNegotiator(apn, false), sessionCacheSize, sessionTimeout);
   }

   public JdkSslClientContext(File certChainFile, TrustManagerFactory trustManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, JdkApplicationProtocolNegotiator apn, long sessionCacheSize, long sessionTimeout) throws SSLException {
      this(certChainFile, trustManagerFactory, (File)null, (File)null, (String)null, (KeyManagerFactory)null, ciphers, cipherFilter, (JdkApplicationProtocolNegotiator)apn, sessionCacheSize, sessionTimeout);
   }

   public JdkSslClientContext(File trustCertChainFile, TrustManagerFactory trustManagerFactory, File keyCertChainFile, File keyFile, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout) throws SSLException {
      this(trustCertChainFile, trustManagerFactory, keyCertChainFile, keyFile, keyPassword, keyManagerFactory, ciphers, cipherFilter, toNegotiator(apn, false), sessionCacheSize, sessionTimeout);
   }

   public JdkSslClientContext(File trustCertChainFile, TrustManagerFactory trustManagerFactory, File keyCertChainFile, File keyFile, String keyPassword, KeyManagerFactory keyManagerFactory, Iterable<String> ciphers, CipherSuiteFilter cipherFilter, JdkApplicationProtocolNegotiator apn, long sessionCacheSize, long sessionTimeout) throws SSLException {
      super(ciphers, cipherFilter, apn);

      try {
         if (trustCertChainFile != null) {
            trustManagerFactory = buildTrustManagerFactory(trustCertChainFile, trustManagerFactory);
         }

         if (keyFile != null) {
            keyManagerFactory = buildKeyManagerFactory(keyCertChainFile, keyFile, keyPassword, keyManagerFactory);
         }

         this.ctx = SSLContext.getInstance("TLS");
         this.ctx.init(keyManagerFactory == null ? null : keyManagerFactory.getKeyManagers(), trustManagerFactory == null ? null : trustManagerFactory.getTrustManagers(), (SecureRandom)null);
         SSLSessionContext sessCtx = this.ctx.getClientSessionContext();
         if (sessionCacheSize > 0L) {
            sessCtx.setSessionCacheSize((int)Math.min(sessionCacheSize, 2147483647L));
         }

         if (sessionTimeout > 0L) {
            sessCtx.setSessionTimeout((int)Math.min(sessionTimeout, 2147483647L));
         }

      } catch (Exception var15) {
         throw new SSLException("failed to initialize the client-side SSL context", var15);
      }
   }

   public boolean isClient() {
      return true;
   }

   public SSLContext context() {
      return this.ctx;
   }
}
