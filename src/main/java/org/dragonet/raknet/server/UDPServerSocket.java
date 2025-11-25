package org.dragonet.raknet.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;

public class UDPServerSocket {
   protected DatagramChannel channel;
   protected DatagramSocket socket;

   public UDPServerSocket() {
      this(19132, "0.0.0.0");
   }

   public UDPServerSocket(int port) {
      this(port, "0.0.0.0");
   }

   public UDPServerSocket(int port, String interfaz) {
      try {
         this.channel = DatagramChannel.open();
         this.channel.configureBlocking(false);
         this.socket = this.channel.socket();
         this.socket.bind(new InetSocketAddress(interfaz, port));
         this.socket.setReuseAddress(true);
         this.setSendBuffer(8388608).setRecvBuffer(8388608);
      } catch (IOException var4) {
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
      ByteBuffer buffer = ByteBuffer.allocate(65536);
      InetSocketAddress socketAddress = (InetSocketAddress)this.channel.receive(buffer);
      if (socketAddress == null) {
         return null;
      } else {
         DatagramPacket packet = new DatagramPacket(new byte[buffer.position()], buffer.position());
         packet.setAddress(socketAddress.getAddress());
         packet.setPort(socketAddress.getPort());
         packet.setLength(buffer.position());
         packet.setData(Arrays.copyOf(buffer.array(), packet.getLength()));
         return packet;
      }
   }

   public int writePacket(byte[] data, String dest, int port) throws IOException {
      return this.writePacket(data, new InetSocketAddress(dest, port));
   }

   public int writePacket(byte[] data, InetSocketAddress dest) throws IOException {
      return this.channel.send(ByteBuffer.wrap(data), dest);
   }

   public UDPServerSocket setSendBuffer(int size) throws SocketException {
      this.socket.setSendBufferSize(size);
      return this;
   }

   public UDPServerSocket setRecvBuffer(int size) throws SocketException {
      this.socket.setReceiveBufferSize(size);
      return this;
   }
}
