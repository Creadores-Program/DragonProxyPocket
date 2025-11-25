package io.netty.util.internal;

import io.netty.util.concurrent.EventExecutor;
import java.util.concurrent.Callable;

public interface CallableEventExecutorAdapter<V> extends Callable<V> {
   EventExecutor executor();

   Callable<V> unwrap();
}
