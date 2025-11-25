package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Beta
@GwtCompatible
public interface CheckedFuture<V, X extends Exception> extends ListenableFuture<V> {
   V checkedGet() throws X;

   V checkedGet(long var1, TimeUnit var3) throws TimeoutException, X;
}
