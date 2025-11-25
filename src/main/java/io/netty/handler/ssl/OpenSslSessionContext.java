package io.netty.handler.ssl;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import org.apache.tomcat.jni.SSLContext;

public abstract class OpenSslSessionContext implements SSLSessionContext {
   private static final Enumeration<byte[]> EMPTY = new OpenSslSessionContext.EmptyEnumeration();
   private final OpenSslSessionStats stats;
   final long context;

   OpenSslSessionContext(long context) {
      this.context = context;
      this.stats = new OpenSslSessionStats(context);
   }

   public SSLSession getSession(byte[] bytes) {
      if (bytes == null) {
         throw new NullPointerException("bytes");
      } else {
         return null;
      }
   }

   public Enumeration<byte[]> getIds() {
      return EMPTY;
   }

   public void setTicketKeys(byte[] keys) {
      if (keys == null) {
         throw new NullPointerException("keys");
      } else {
         SSLContext.setSessionTicketKeys(this.context, keys);
      }
   }

   public abstract void setSessionCacheEnabled(boolean var1);

   public abstract boolean isSessionCacheEnabled();

   public OpenSslSessionStats stats() {
      return this.stats;
   }

   private static final class EmptyEnumeration implements Enumeration<byte[]> {
      private EmptyEnumeration() {
      }

      public boolean hasMoreElements() {
         return false;
      }

      public byte[] nextElement() {
         throw new NoSuchElementException();
      }

      // $FF: synthetic method
      EmptyEnumeration(Object x0) {
         this();
      }
   }
}
