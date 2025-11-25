package io.netty.resolver;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.io.Closeable;
import java.net.SocketAddress;
import java.util.IdentityHashMap;
import java.util.Map;

public abstract class NameResolverGroup<T extends SocketAddress> implements Closeable {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(NameResolverGroup.class);
   private final Map<EventExecutor, NameResolver<T>> resolvers = new IdentityHashMap();

   protected NameResolverGroup() {
   }

   public NameResolver<T> getResolver(EventExecutor executor) {
      if (executor == null) {
         throw new NullPointerException("executor");
      } else if (executor.isShuttingDown()) {
         throw new IllegalStateException("executor not accepting a task");
      } else {
         return this.getResolver0(executor.unwrap());
      }
   }

   private NameResolver<T> getResolver0(final EventExecutor executor) {
      synchronized(this.resolvers) {
         NameResolver<T> r = (NameResolver)this.resolvers.get(executor);
         if (r == null) {
            final NameResolver newResolver;
            try {
               newResolver = this.newResolver(executor);
            } catch (Exception var7) {
               throw new IllegalStateException("failed to create a new resolver", var7);
            }

            this.resolvers.put(executor, newResolver);
            executor.terminationFuture().addListener(new FutureListener<Object>() {
               public void operationComplete(Future<Object> future) throws Exception {
                  NameResolverGroup.this.resolvers.remove(executor);
                  newResolver.close();
               }
            });
            r = newResolver;
         }

         return r;
      }
   }

   protected abstract NameResolver<T> newResolver(EventExecutor var1) throws Exception;

   public void close() {
      NameResolver[] rArray;
      synchronized(this.resolvers) {
         rArray = (NameResolver[])((NameResolver[])this.resolvers.values().toArray(new NameResolver[this.resolvers.size()]));
         this.resolvers.clear();
      }

      NameResolver[] arr$ = rArray;
      int len$ = rArray.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         NameResolver r = arr$[i$];

         try {
            r.close();
         } catch (Throwable var7) {
            logger.warn("Failed to close a resolver:", var7);
         }
      }

   }
}
