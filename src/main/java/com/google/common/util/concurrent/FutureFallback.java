package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;

/** @deprecated */
@Deprecated
@Beta
@GwtCompatible
public interface FutureFallback<V> {
   ListenableFuture<V> create(Throwable var1) throws Exception;
}
