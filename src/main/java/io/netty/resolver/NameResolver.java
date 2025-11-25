package io.netty.resolver;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import java.io.Closeable;
import java.net.SocketAddress;

public interface NameResolver<T extends SocketAddress> extends Closeable {
   boolean isSupported(SocketAddress var1);

   boolean isResolved(SocketAddress var1);

   Future<T> resolve(String var1, int var2);

   Future<T> resolve(String var1, int var2, Promise<T> var3);

   Future<T> resolve(SocketAddress var1);

   Future<T> resolve(SocketAddress var1, Promise<T> var2);

   void close();
}
