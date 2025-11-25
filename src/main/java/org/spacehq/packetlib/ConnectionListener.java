package org.spacehq.packetlib;

public interface ConnectionListener {
   String getHost();

   int getPort();

   boolean isListening();

   void bind();

   void bind(boolean var1);

   void bind(boolean var1, Runnable var2);

   void close();

   void close(boolean var1);

   void close(boolean var1, Runnable var2);
}
