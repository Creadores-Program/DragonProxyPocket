package io.netty.handler.codec.haproxy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;
import io.netty.util.internal.StringUtil;

public final class HAProxyMessage {
   private static final HAProxyMessage V1_UNKNOWN_MSG;
   private static final HAProxyMessage V2_UNKNOWN_MSG;
   private static final HAProxyMessage V2_LOCAL_MSG;
   private final HAProxyProtocolVersion protocolVersion;
   private final HAProxyCommand command;
   private final HAProxyProxiedProtocol proxiedProtocol;
   private final String sourceAddress;
   private final String destinationAddress;
   private final int sourcePort;
   private final int destinationPort;

   private HAProxyMessage(HAProxyProtocolVersion protocolVersion, HAProxyCommand command, HAProxyProxiedProtocol proxiedProtocol, String sourceAddress, String destinationAddress, String sourcePort, String destinationPort) {
      this(protocolVersion, command, proxiedProtocol, sourceAddress, destinationAddress, portStringToInt(sourcePort), portStringToInt(destinationPort));
   }

   private HAProxyMessage(HAProxyProtocolVersion protocolVersion, HAProxyCommand command, HAProxyProxiedProtocol proxiedProtocol, String sourceAddress, String destinationAddress, int sourcePort, int destinationPort) {
      if (proxiedProtocol == null) {
         throw new NullPointerException("proxiedProtocol");
      } else {
         HAProxyProxiedProtocol.AddressFamily addrFamily = proxiedProtocol.addressFamily();
         checkAddress(sourceAddress, addrFamily);
         checkAddress(destinationAddress, addrFamily);
         checkPort(sourcePort);
         checkPort(destinationPort);
         this.protocolVersion = protocolVersion;
         this.command = command;
         this.proxiedProtocol = proxiedProtocol;
         this.sourceAddress = sourceAddress;
         this.destinationAddress = destinationAddress;
         this.sourcePort = sourcePort;
         this.destinationPort = destinationPort;
      }
   }

   static HAProxyMessage decodeHeader(ByteBuf header) {
      if (header == null) {
         throw new NullPointerException("header");
      } else if (header.readableBytes() < 16) {
         throw new HAProxyProtocolException("incomplete header: " + header.readableBytes() + " bytes (expected: 16+ bytes)");
      } else {
         header.skipBytes(12);
         byte verCmdByte = header.readByte();

         HAProxyProtocolVersion ver;
         try {
            ver = HAProxyProtocolVersion.valueOf(verCmdByte);
         } catch (IllegalArgumentException var16) {
            throw new HAProxyProtocolException(var16);
         }

         if (ver != HAProxyProtocolVersion.V2) {
            throw new HAProxyProtocolException("version 1 unsupported: 0x" + Integer.toHexString(verCmdByte));
         } else {
            HAProxyCommand cmd;
            try {
               cmd = HAProxyCommand.valueOf(verCmdByte);
            } catch (IllegalArgumentException var15) {
               throw new HAProxyProtocolException(var15);
            }

            if (cmd == HAProxyCommand.LOCAL) {
               return V2_LOCAL_MSG;
            } else {
               HAProxyProxiedProtocol protAndFam;
               try {
                  protAndFam = HAProxyProxiedProtocol.valueOf(header.readByte());
               } catch (IllegalArgumentException var14) {
                  throw new HAProxyProtocolException(var14);
               }

               if (protAndFam == HAProxyProxiedProtocol.UNKNOWN) {
                  return V2_UNKNOWN_MSG;
               } else {
                  int addressInfoLen = header.readUnsignedShort();
                  int srcPort = 0;
                  int dstPort = 0;
                  HAProxyProxiedProtocol.AddressFamily addressFamily = protAndFam.addressFamily();
                  String srcAddress;
                  String dstAddress;
                  if (addressFamily == HAProxyProxiedProtocol.AddressFamily.AF_UNIX) {
                     if (addressInfoLen < 216 || header.readableBytes() < 216) {
                        throw new HAProxyProtocolException("incomplete UNIX socket address information: " + Math.min(addressInfoLen, header.readableBytes()) + " bytes (expected: 216+ bytes)");
                     }

                     int startIdx = header.readerIndex();
                     int addressEnd = header.forEachByte(startIdx, 108, ByteBufProcessor.FIND_NUL);
                     int addressLen;
                     if (addressEnd == -1) {
                        addressLen = 108;
                     } else {
                        addressLen = addressEnd - startIdx;
                     }

                     srcAddress = header.toString(startIdx, addressLen, CharsetUtil.US_ASCII);
                     startIdx += 108;
                     addressEnd = header.forEachByte(startIdx, 108, ByteBufProcessor.FIND_NUL);
                     if (addressEnd == -1) {
                        addressLen = 108;
                     } else {
                        addressLen = addressEnd - startIdx;
                     }

                     dstAddress = header.toString(startIdx, addressLen, CharsetUtil.US_ASCII);
                  } else {
                     byte addressLen;
                     if (addressFamily == HAProxyProxiedProtocol.AddressFamily.AF_IPv4) {
                        if (addressInfoLen < 12 || header.readableBytes() < 12) {
                           throw new HAProxyProtocolException("incomplete IPv4 address information: " + Math.min(addressInfoLen, header.readableBytes()) + " bytes (expected: 12+ bytes)");
                        }

                        addressLen = 4;
                     } else {
                        if (addressFamily != HAProxyProxiedProtocol.AddressFamily.AF_IPv6) {
                           throw new HAProxyProtocolException("unable to parse address information (unkown address family: " + addressFamily + ')');
                        }

                        if (addressInfoLen < 36 || header.readableBytes() < 36) {
                           throw new HAProxyProtocolException("incomplete IPv6 address information: " + Math.min(addressInfoLen, header.readableBytes()) + " bytes (expected: 36+ bytes)");
                        }

                        addressLen = 16;
                     }

                     srcAddress = ipBytestoString(header, addressLen);
                     dstAddress = ipBytestoString(header, addressLen);
                     srcPort = header.readUnsignedShort();
                     dstPort = header.readUnsignedShort();
                  }

                  return new HAProxyMessage(ver, cmd, protAndFam, srcAddress, dstAddress, srcPort, dstPort);
               }
            }
         }
      }
   }

   static HAProxyMessage decodeHeader(String header) {
      if (header == null) {
         throw new HAProxyProtocolException("header");
      } else {
         String[] parts = StringUtil.split(header, ' ');
         int numParts = parts.length;
         if (numParts < 2) {
            throw new HAProxyProtocolException("invalid header: " + header + " (expected: 'PROXY' and proxied protocol values)");
         } else if (!"PROXY".equals(parts[0])) {
            throw new HAProxyProtocolException("unknown identifier: " + parts[0]);
         } else {
            HAProxyProxiedProtocol protAndFam;
            try {
               protAndFam = HAProxyProxiedProtocol.valueOf(parts[1]);
            } catch (IllegalArgumentException var5) {
               throw new HAProxyProtocolException(var5);
            }

            if (protAndFam != HAProxyProxiedProtocol.TCP4 && protAndFam != HAProxyProxiedProtocol.TCP6 && protAndFam != HAProxyProxiedProtocol.UNKNOWN) {
               throw new HAProxyProtocolException("unsupported v1 proxied protocol: " + parts[1]);
            } else if (protAndFam == HAProxyProxiedProtocol.UNKNOWN) {
               return V1_UNKNOWN_MSG;
            } else if (numParts != 6) {
               throw new HAProxyProtocolException("invalid TCP4/6 header: " + header + " (expected: 6 parts)");
            } else {
               return new HAProxyMessage(HAProxyProtocolVersion.V1, HAProxyCommand.PROXY, protAndFam, parts[2], parts[3], parts[4], parts[5]);
            }
         }
      }
   }

   private static String ipBytestoString(ByteBuf header, int addressLen) {
      StringBuilder sb = new StringBuilder();
      if (addressLen == 4) {
         sb.append(header.readByte() & 255);
         sb.append('.');
         sb.append(header.readByte() & 255);
         sb.append('.');
         sb.append(header.readByte() & 255);
         sb.append('.');
         sb.append(header.readByte() & 255);
      } else {
         sb.append(Integer.toHexString(header.readUnsignedShort()));
         sb.append(':');
         sb.append(Integer.toHexString(header.readUnsignedShort()));
         sb.append(':');
         sb.append(Integer.toHexString(header.readUnsignedShort()));
         sb.append(':');
         sb.append(Integer.toHexString(header.readUnsignedShort()));
         sb.append(':');
         sb.append(Integer.toHexString(header.readUnsignedShort()));
         sb.append(':');
         sb.append(Integer.toHexString(header.readUnsignedShort()));
         sb.append(':');
         sb.append(Integer.toHexString(header.readUnsignedShort()));
         sb.append(':');
         sb.append(Integer.toHexString(header.readUnsignedShort()));
      }

      return sb.toString();
   }

   private static int portStringToInt(String value) {
      int port;
      try {
         port = Integer.parseInt(value);
      } catch (NumberFormatException var3) {
         throw new HAProxyProtocolException("invalid port: " + value, var3);
      }

      if (port > 0 && port <= 65535) {
         return port;
      } else {
         throw new HAProxyProtocolException("invalid port: " + value + " (expected: 1 ~ 65535)");
      }
   }

   private static void checkAddress(String address, HAProxyProxiedProtocol.AddressFamily addrFamily) {
      if (addrFamily == null) {
         throw new NullPointerException("addrFamily");
      } else {
         switch(addrFamily) {
         case AF_UNSPEC:
            if (address != null) {
               throw new HAProxyProtocolException("unable to validate an AF_UNSPEC address: " + address);
            }

            return;
         case AF_UNIX:
            return;
         default:
            if (address == null) {
               throw new NullPointerException("address");
            } else {
               switch(addrFamily) {
               case AF_IPv4:
                  if (!NetUtil.isValidIpV4Address(address)) {
                     throw new HAProxyProtocolException("invalid IPv4 address: " + address);
                  }
                  break;
               case AF_IPv6:
                  if (!NetUtil.isValidIpV6Address(address)) {
                     throw new HAProxyProtocolException("invalid IPv6 address: " + address);
                  }
                  break;
               default:
                  throw new Error();
               }

            }
         }
      }
   }

   private static void checkPort(int port) {
      if (port < 0 || port > 65535) {
         throw new HAProxyProtocolException("invalid port: " + port + " (expected: 1 ~ 65535)");
      }
   }

   public HAProxyProtocolVersion protocolVersion() {
      return this.protocolVersion;
   }

   public HAProxyCommand command() {
      return this.command;
   }

   public HAProxyProxiedProtocol proxiedProtocol() {
      return this.proxiedProtocol;
   }

   public String sourceAddress() {
      return this.sourceAddress;
   }

   public String destinationAddress() {
      return this.destinationAddress;
   }

   public int sourcePort() {
      return this.sourcePort;
   }

   public int destinationPort() {
      return this.destinationPort;
   }

   static {
      V1_UNKNOWN_MSG = new HAProxyMessage(HAProxyProtocolVersion.V1, HAProxyCommand.PROXY, HAProxyProxiedProtocol.UNKNOWN, (String)null, (String)null, 0, 0);
      V2_UNKNOWN_MSG = new HAProxyMessage(HAProxyProtocolVersion.V2, HAProxyCommand.PROXY, HAProxyProxiedProtocol.UNKNOWN, (String)null, (String)null, 0, 0);
      V2_LOCAL_MSG = new HAProxyMessage(HAProxyProtocolVersion.V2, HAProxyCommand.LOCAL, HAProxyProxiedProtocol.UNKNOWN, (String)null, (String)null, 0, 0);
   }
}
