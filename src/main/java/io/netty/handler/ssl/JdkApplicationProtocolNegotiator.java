package io.netty.handler.ssl;

import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLEngine;

public interface JdkApplicationProtocolNegotiator extends ApplicationProtocolNegotiator {
   JdkApplicationProtocolNegotiator.SslEngineWrapperFactory wrapperFactory();

   JdkApplicationProtocolNegotiator.ProtocolSelectorFactory protocolSelectorFactory();

   JdkApplicationProtocolNegotiator.ProtocolSelectionListenerFactory protocolListenerFactory();

   public interface ProtocolSelectionListenerFactory {
      JdkApplicationProtocolNegotiator.ProtocolSelectionListener newListener(SSLEngine var1, List<String> var2);
   }

   public interface ProtocolSelectorFactory {
      JdkApplicationProtocolNegotiator.ProtocolSelector newSelector(SSLEngine var1, Set<String> var2);
   }

   public interface ProtocolSelectionListener {
      void unsupported();

      void selected(String var1) throws Exception;
   }

   public interface ProtocolSelector {
      void unsupported();

      String select(List<String> var1) throws Exception;
   }

   public interface SslEngineWrapperFactory {
      SSLEngine wrapSslEngine(SSLEngine var1, JdkApplicationProtocolNegotiator var2, boolean var3);
   }
}
