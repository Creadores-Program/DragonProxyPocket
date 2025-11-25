package org.dragonet.proxy;

public class TickerThread extends Thread {
   private final DragonProxy proxy;

   public TickerThread(DragonProxy proxy) {
      this.proxy = proxy;
      this.setDaemon(true);
   }

   public void run() {
      while(!this.proxy.isShuttingDown()) {
         long time = System.currentTimeMillis();
         this.proxy.onTick();
         time = System.currentTimeMillis() - time;
         if (time < 50L) {
            try {
               Thread.sleep(50L - time);
            } catch (InterruptedException var4) {
               return;
            }
         }
      }

   }
}
