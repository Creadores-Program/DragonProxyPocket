package io.netty.resolver;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.TypeParameterMatcher;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.UnsupportedAddressTypeException;

public abstract class SimpleNameResolver<T extends SocketAddress> implements NameResolver<T> {
   private final EventExecutor executor;
   private final TypeParameterMatcher matcher;

   protected SimpleNameResolver(EventExecutor executor) {
      if (executor == null) {
         throw new NullPointerException("executor");
      } else {
         this.executor = executor;
         this.matcher = TypeParameterMatcher.find(this, SimpleNameResolver.class, "T");
      }
   }

   protected SimpleNameResolver(EventExecutor executor, Class<? extends T> addressType) {
      if (executor == null) {
         throw new NullPointerException("executor");
      } else {
         this.executor = executor;
         this.matcher = TypeParameterMatcher.get(addressType);
      }
   }

   protected EventExecutor executor() {
      return this.executor;
   }

   public boolean isSupported(SocketAddress address) {
      return this.matcher.match(address);
   }

   public final boolean isResolved(SocketAddress address) {
      if (!this.isSupported(address)) {
         throw new UnsupportedAddressTypeException();
      } else {
         return this.doIsResolved(address);
      }
   }

   protected abstract boolean doIsResolved(T var1);

   public final Future<T> resolve(String inetHost, int inetPort) {
      if (inetHost == null) {
         throw new NullPointerException("inetHost");
      } else {
         return this.resolve(InetSocketAddress.createUnresolved(inetHost, inetPort));
      }
   }

   public Future<T> resolve(String inetHost, int inetPort, Promise<T> promise) {
      if (inetHost == null) {
         throw new NullPointerException("inetHost");
      } else {
         return this.resolve(InetSocketAddress.createUnresolved(inetHost, inetPort), promise);
      }
   }

   public final Future<T> resolve(SocketAddress address) {
      if (address == null) {
         throw new NullPointerException("unresolvedAddress");
      } else if (!this.isSupported(address)) {
         return this.executor().newFailedFuture(new UnsupportedAddressTypeException());
      } else if (this.isResolved(address)) {
         return this.executor.newSucceededFuture(address);
      } else {
         try {
            Promise<T> promise = this.executor().newPromise();
            this.doResolve(address, promise);
            return promise;
         } catch (Exception var4) {
            return this.executor().newFailedFuture(var4);
         }
      }
   }

   public final Future<T> resolve(SocketAddress address, Promise<T> promise) {
      if (address == null) {
         throw new NullPointerException("unresolvedAddress");
      } else if (promise == null) {
         throw new NullPointerException("promise");
      } else if (!this.isSupported(address)) {
         return promise.setFailure(new UnsupportedAddressTypeException());
      } else if (this.isResolved(address)) {
         return promise.setSuccess(address);
      } else {
         try {
            this.doResolve(address, promise);
            return promise;
         } catch (Exception var4) {
            return promise.setFailure(var4);
         }
      }
   }

   protected abstract void doResolve(T var1, Promise<T> var2) throws Exception;

   public void close() {
   }
}
