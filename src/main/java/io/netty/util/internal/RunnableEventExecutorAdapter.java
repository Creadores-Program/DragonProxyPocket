package io.netty.util.internal;

import io.netty.util.concurrent.EventExecutor;

public interface RunnableEventExecutorAdapter extends Runnable {
   EventExecutor executor();

   Runnable unwrap();
}
