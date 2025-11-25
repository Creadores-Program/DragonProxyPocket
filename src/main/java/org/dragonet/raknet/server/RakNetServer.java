package org.dragonet.raknet.server;

import java.util.concurrent.ConcurrentLinkedQueue;

public class RakNetServer extends Thread {
   protected int port;
   protected String interfaz;
   protected ConcurrentLinkedQueue<byte[]> externalQueue;
   protected ConcurrentLinkedQueue<byte[]> internalQueue;
   protected boolean shutdown;

   public RakNetServer(int port) {
      this(port, "0.0.0.0");
   }

   public RakNetServer(int port, String interfaz) {
      this.port = port;
      if (port >= 1 && port <= 65536) {
         this.interfaz = interfaz;
         this.externalQueue = new ConcurrentLinkedQueue();
         this.internalQueue = new ConcurrentLinkedQueue();
         this.start();
      } else {
         throw new IllegalArgumentException("Invalid port range");
      }
   }

   public boolean isShutdown() {
      return this.shutdown;
   }

   public void shutdown() {
      this.shutdown = true;
   }

   public int getPort() {
      return this.port;
   }

   public String getInterface() {
      return this.interfaz;
   }

   public ConcurrentLinkedQueue<byte[]> getExternalQueue() {
      return this.externalQueue;
   }

   public ConcurrentLinkedQueue<byte[]> getInternalQueue() {
      return this.internalQueue;
   }

   public void pushMainToThreadPacket(byte[] data) {
      this.internalQueue.add(data);
   }

   public byte[] readMainToThreadPacket() {
      return (byte[])this.internalQueue.poll();
   }

   public void pushThreadToMainPacket(byte[] data) {
      this.externalQueue.add(data);
   }

   public byte[] readThreadToMainPacket() {
      return (byte[])this.externalQueue.poll();
   }

   public void run() {
      this.setName("RakNet Thread #" + Thread.currentThread().getId());
      Runtime.getRuntime().addShutdownHook(new RakNetServer.ShutdownHandler());
      UDPServerSocket socket = new UDPServerSocket(this.port, this.interfaz);

      try {
         new SessionManager(this, socket);
      } catch (Exception var3) {
         var3.printStackTrace();
      }

   }

   private class ShutdownHandler extends Thread {
      private ShutdownHandler() {
      }

      public void run() {
         if (!RakNetServer.this.shutdown) {
         }

      }

      // $FF: synthetic method
      ShutdownHandler(Object x1) {
         this();
      }
   }
}
