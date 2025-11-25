package org.dragonet.raknet.client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import org.dragonet.raknet.protocol.packet.UNCONNECTED_PING;
import org.dragonet.raknet.protocol.packet.UNCONNECTED_PONG;

public class JRakLibClient extends Thread {
   private static long clientID = (new Random(System.currentTimeMillis())).nextLong();
   private static long startTime = -1L;
   protected InetSocketAddress serverEndpoint;
   protected Logger logger;
   protected boolean shutdown = false;
   protected List<byte[]> externalQueue;
   protected List<byte[]> internalQueue;

   public JRakLibClient(Logger logger, String serverIP, int serverPort) {
      if (serverPort >= 1 && serverPort <= 65536) {
         this.logger = logger;
         this.serverEndpoint = new InetSocketAddress(serverIP, serverPort);
         this.externalQueue = new LinkedList();
         this.internalQueue = new LinkedList();
         this.start();
      } else {
         throw new IllegalArgumentException("Invalid port range.");
      }
   }

   public static JRakLibClient.PingResponse pingServer(Logger logger, String serverIP, int serverPort, int tries, int delay) throws IOException {
      if (startTime == -1L) {
         startTime = System.currentTimeMillis();
      }

      UDPClientSocket socket = new UDPClientSocket(logger);

      for(int i = 0; i < tries; ++i) {
         UNCONNECTED_PING ping = new UNCONNECTED_PING();
         ping.pingID = System.currentTimeMillis() - startTime;
         ping.encode();
         socket.writePacket(ping.buffer, new InetSocketAddress(serverIP, serverPort));
         DatagramPacket pkt = socket.readPacketBlocking(delay);
         if (pkt != null && pkt.getData()[0] == UNCONNECTED_PONG.ID) {
            UNCONNECTED_PONG pong = new UNCONNECTED_PONG();
            pong.buffer = pkt.getData();
            pong.decode();
            return new JRakLibClient.PingResponse(pong.serverID, pong.pingID, pong.serverName);
         }
      }

      return null;
   }

   public boolean isShutdown() {
      return this.shutdown;
   }

   public void shutdown() {
      this.shutdown = true;
   }

   public int getServerPort() {
      return this.serverEndpoint.getPort();
   }

   public String getServerIP() {
      return this.serverEndpoint.getHostString();
   }

   public InetSocketAddress getServerEndpoint() {
      return this.serverEndpoint;
   }

   public Logger getLogger() {
      return this.logger;
   }

   public List<byte[]> getExternalQueue() {
      return this.externalQueue;
   }

   public List<byte[]> getInternalQueue() {
      return this.internalQueue;
   }

   public void pushMainToThreadPacket(byte[] bytes) {
      this.internalQueue.add(0, bytes);
   }

   public byte[] readMainToThreadPacket() {
      if (!this.internalQueue.isEmpty()) {
         byte[] data = (byte[])this.internalQueue.get(this.internalQueue.size() - 1);
         this.internalQueue.remove(data);
         return data;
      } else {
         return null;
      }
   }

   public void pushThreadToMainPacket(byte[] bytes) {
      this.externalQueue.add(0, bytes);
   }

   public byte[] readThreadToMainPacket() {
      if (!this.externalQueue.isEmpty()) {
         byte[] data = (byte[])this.externalQueue.get(this.externalQueue.size() - 1);
         this.externalQueue.remove(data);
         return data;
      } else {
         return null;
      }
   }

   public static void regenerateClientID() {
      clientID = (new Random()).nextLong();
   }

   public static void regenerateClientID(long seed) {
      clientID = (new Random(seed)).nextLong();
   }

   public static long getClientID() {
      return clientID;
   }

   public long getTimeSinceStart() {
      return startTime;
   }

   public void run() {
      this.setName("JRakLib Client Thread #" + this.getId());
      Runtime.getRuntime().addShutdownHook(new JRakLibClient.ShutdownHandler());
      UDPClientSocket socket = new UDPClientSocket(this.logger);
      new ConnectionManager(this, socket);
   }

   public static class PingResponse {
      public final long pingId;
      public final long serverId;
      public final String name;

      public PingResponse(long serverId, long pingId, String name) {
         this.pingId = pingId;
         this.serverId = serverId;
         this.name = name;
      }
   }

   private class ShutdownHandler extends Thread {
      private ShutdownHandler() {
      }

      public void run() {
         if (!JRakLibClient.this.shutdown) {
            JRakLibClient.this.logger.severe("[JRakLibClient Thread #" + this.getId() + "] JRakLib crashed!");
         }

      }

      // $FF: synthetic method
      ShutdownHandler(Object x1) {
         this();
      }
   }
}
