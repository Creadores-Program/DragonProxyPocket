package io.netty.handler.ssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.x500.X500Principal;
import org.apache.tomcat.jni.CertificateVerifier;
import org.apache.tomcat.jni.SSL;
import org.apache.tomcat.jni.SSLContext;

public final class OpenSslClientContext extends OpenSslContext {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(OpenSslClientContext.class);
   private final OpenSslSessionContext sessionContext;

   public OpenSslClientContext() throws SSLException {
      this((File)null, (TrustManagerFactory)null, (Iterable)null, (ApplicationProtocolConfig)null, 0L, 0L);
   }

   public OpenSslClientContext(File certChainFile) throws SSLException {
      this(certChainFile, (TrustManagerFactory)null);
   }

   public OpenSslClientContext(TrustManagerFactory trustManagerFactory) throws SSLException {
      this((File)null, trustManagerFactory);
   }

   public OpenSslClientContext(File certChainFile, TrustManagerFactory trustManagerFactory) throws SSLException {
      this(certChainFile, trustManagerFactory, (Iterable)null, (ApplicationProtocolConfig)null, 0L, 0L);
   }

   public OpenSslClientContext(File certChainFile, TrustManagerFactory trustManagerFactory, Iterable<String> ciphers, ApplicationProtocolConfig apn, long sessionCacheSize, long sessionTimeout) throws SSLException {
      super(ciphers, (ApplicationProtocolConfig)apn, sessionCacheSize, sessionTimeout, 0);
      boolean success = false;

      try {
         if (certChainFile != null && !certChainFile.isFile()) {
            throw new IllegalArgumentException("certChainFile is not a file: " + certChainFile);
         }

         Class var10 = OpenSslContext.class;
         synchronized(OpenSslContext.class) {
            if (certChainFile != null && !SSLContext.setCertificateChainFile(this.ctx, certChainFile.getPath(), true)) {
               long error = (long)SSL.getLastErrorNumber();
               if (OpenSsl.isError(error)) {
                  throw new SSLException("failed to set certificate chain: " + certChainFile + " (" + SSL.getErrorString(error) + ')');
               }
            }

            SSLContext.setVerify(this.ctx, 0, 10);

            try {
               if (trustManagerFactory == null) {
                  trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
               }

               initTrustManagerFactory(certChainFile, trustManagerFactory);
               final X509TrustManager manager = chooseTrustManager(trustManagerFactory.getTrustManagers());
               SSLContext.setCertVerifyCallback(this.ctx, new CertificateVerifier() {
                  public boolean verify(long ssl, byte[][] chain, String auth) {
                     X509Certificate[] peerCerts = OpenSslContext.certificates(chain);

                     try {
                        manager.checkServerTrusted(peerCerts, auth);
                        return true;
                     } catch (Exception var7) {
                        OpenSslClientContext.logger.debug("verification of certificate failed", (Throwable)var7);
                        return false;
                     }
                  }
               });
            } catch (Exception var18) {
               throw new SSLException("unable to setup trustmanager", var18);
            }
         }

         this.sessionContext = new OpenSslClientContext.OpenSslClientSessionContext(this.ctx);
         success = true;
      } finally {
         if (!success) {
            this.destroyPools();
         }

      }

   }

   private static void initTrustManagerFactory(File certChainFile, TrustManagerFactory trustManagerFactory) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load((InputStream)null, (char[])null);
      if (certChainFile != null) {
         ByteBuf[] certs = PemReader.readCertificates(certChainFile);
         boolean var16 = false;

         ByteBuf[] arr$;
         int len$;
         int i$;
         ByteBuf buf;
         try {
            var16 = true;
            arr$ = certs;
            len$ = certs.length;

            for(i$ = 0; i$ < len$; ++i$) {
               buf = arr$[i$];
               X509Certificate cert = (X509Certificate)X509_CERT_FACTORY.generateCertificate(new ByteBufInputStream(buf));
               X500Principal principal = cert.getSubjectX500Principal();
               ks.setCertificateEntry(principal.getName("RFC2253"), cert);
            }

            var16 = false;
         } finally {
            if (var16) {
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
      }

      trustManagerFactory.init(ks);
   }

   public OpenSslSessionContext sessionContext() {
      return this.sessionContext;
   }

   private static final class OpenSslClientSessionContext extends OpenSslSessionContext {
      private OpenSslClientSessionContext(long context) {
         super(context);
      }

      public void setSessionTimeout(int seconds) {
         if (seconds < 0) {
            throw new IllegalArgumentException();
         }
      }

      public int getSessionTimeout() {
         return 0;
      }

      public void setSessionCacheSize(int size) {
         if (size < 0) {
            throw new IllegalArgumentException();
         }
      }

      public int getSessionCacheSize() {
         return 0;
      }

      public void setSessionCacheEnabled(boolean enabled) {
      }

      public boolean isSessionCacheEnabled() {
         return false;
      }

      // $FF: synthetic method
      OpenSslClientSessionContext(long x0, Object x1) {
         this(x0);
      }
   }
}
