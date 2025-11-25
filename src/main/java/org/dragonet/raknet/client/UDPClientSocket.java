package org.dragonet.raknet.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.logging.Logger;

public class UDPClientSocket implements Closeable {
   private Logger logger;
   private DatagramSocket socket;

   public UDPClientSocket(Logger logger) {
      this.logger = logger;

      try {
         this.socket = new DatagramSocket();
         this.socket.setBroadcast(true);
         this.socket.setSendBufferSize(8388608);
         this.socket.setReceiveBufferSize(8388608);
         this.socket.setSoTimeout(1);
      } catch (SocketException var3) {
         logger.severe("**** FAILED TO CREATE SOCKET!");
         logger.severe("java.net.SocketException: " + var3.getMessage());
         System.exit(1);
      }

   }

   public DatagramSocket getSocket() {
      return this.socket;
   }

   public void close() {
      this.socket.close();
   }

   public DatagramPacket readPacket() throws IOException {
      DatagramPacket dp = new DatagramPacket(new byte['\uffff'], 65535);

      try {
         this.socket.receive(dp);
         dp.setData(Arrays.copyOf(dp.getData(), dp.getLength()));
         return dp;
      } catch (SocketTimeoutException var3) {
         return null;
      }
   }

   public DatagramPacket readPacketBlocking(int blockFor) throws IOException {
      DatagramPacket dp = new DatagramPacket(new byte['\uffff'], 65535);

      try {
         this.socket.setSoTimeout(blockFor);
         this.socket.receive(dp);
         this.socket.setSoTimeout(1);
         dp.setData(Arrays.copyOf(dp.getData(), dp.getLength()));
         return dp;
      } catch (SocketTimeoutException var4) {
         return null;
      }
   }

   public void writePacket(byte[] buffer, SocketAddress dest) throws IOException {
      DatagramPacket dp = new DatagramPacket(buffer, buffer.length, dest);
      this.socket.send(dp);
   }

   public void setSendBuffer(int size) throws SocketException {
      this.socket.setSendBufferSize(size);
   }

   public void setRecvBuffer(int size) throws SocketException {
      this.socket.setReceiveBufferSize(size);
   }
}
