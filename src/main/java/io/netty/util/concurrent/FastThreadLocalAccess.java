package io.netty.util.concurrent;

import io.netty.util.internal.InternalThreadLocalMap;

public interface FastThreadLocalAccess {
   InternalThreadLocalMap threadLocalMap();

   void setThreadLocalMap(InternalThreadLocalMap var1);
}
