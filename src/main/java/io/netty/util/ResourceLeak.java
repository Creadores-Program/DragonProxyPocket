package io.netty.util;

public interface ResourceLeak {
   void record();

   void record(Object var1);

   boolean close();
}
