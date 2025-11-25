package io.netty.channel;

import io.netty.buffer.ByteBufUtil;
import io.netty.util.internal.EmptyArrays;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.ThreadLocalRandom;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

final class DefaultChannelId implements ChannelId {
   private static final long serialVersionUID = 3884076183504074063L;
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultChannelId.class);
   private static final Pattern MACHINE_ID_PATTERN = Pattern.compile("^(?:[0-9a-fA-F][:-]?){6,8}$");
   private static final int MACHINE_ID_LEN = 8;
   private static final byte[] MACHINE_ID;
   private static final int PROCESS_ID_LEN = 4;
   private static final int MAX_PROCESS_ID = 4194304;
   private static final int PROCESS_ID;
   private static final int SEQUENCE_LEN = 4;
   private static final int TIMESTAMP_LEN = 8;
   private static final int RANDOM_LEN = 4;
   private static final AtomicInteger nextSequence = new AtomicInteger();
   private final byte[] data = new byte[28];
   private int hashCode;
   private transient String shortValue;
   private transient String longValue;

   static ChannelId newInstance() {
      DefaultChannelId id = new DefaultChannelId();
      id.init();
      return id;
   }

   private static byte[] parseMachineId(String value) {
      value = value.replaceAll("[:-]", "");
      byte[] machineId = new byte[8];

      for(int i = 0; i < value.length(); i += 2) {
         machineId[i] = (byte)Integer.parseInt(value.substring(i, i + 2), 16);
      }

      return machineId;
   }

   private static byte[] defaultMachineId() {
      byte[] NOT_FOUND = new byte[]{-1};
      byte[] bestMacAddr = NOT_FOUND;
      InetAddress bestInetAddr = null;

      try {
         bestInetAddr = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
      } catch (UnknownHostException var11) {
         PlatformDependent.throwException(var11);
      }

      LinkedHashMap ifaces = new LinkedHashMap();

      InetAddress inetAddr;
      try {
         Enumeration i = NetworkInterface.getNetworkInterfaces();

         while(i.hasMoreElements()) {
            NetworkInterface iface = (NetworkInterface)i.nextElement();
            Enumeration<InetAddress> addrs = iface.getInetAddresses();
            if (addrs.hasMoreElements()) {
               inetAddr = (InetAddress)addrs.nextElement();
               if (!inetAddr.isLoopbackAddress()) {
                  ifaces.put(iface, inetAddr);
               }
            }
         }
      } catch (SocketException var13) {
         logger.warn("Failed to retrieve the list of available network interfaces", (Throwable)var13);
      }

      Iterator i$ = ifaces.entrySet().iterator();

      while(true) {
         NetworkInterface iface;
         do {
            if (!i$.hasNext()) {
               if (bestMacAddr == NOT_FOUND) {
                  bestMacAddr = new byte[8];
                  ThreadLocalRandom.current().nextBytes(bestMacAddr);
                  logger.warn("Failed to find a usable hardware address from the network interfaces; using random bytes: {}", (Object)formatAddress(bestMacAddr));
               }

               switch(bestMacAddr.length) {
               case 6:
                  byte[] newAddr = new byte[8];
                  System.arraycopy(bestMacAddr, 0, newAddr, 0, 3);
                  newAddr[3] = -1;
                  newAddr[4] = -2;
                  System.arraycopy(bestMacAddr, 3, newAddr, 5, 3);
                  bestMacAddr = newAddr;
                  break;
               default:
                  bestMacAddr = Arrays.copyOf(bestMacAddr, 8);
               }

               return bestMacAddr;
            }

            Entry<NetworkInterface, InetAddress> entry = (Entry)i$.next();
            iface = (NetworkInterface)entry.getKey();
            inetAddr = (InetAddress)entry.getValue();
         } while(iface.isVirtual());

         byte[] macAddr;
         try {
            macAddr = iface.getHardwareAddress();
         } catch (SocketException var12) {
            logger.debug("Failed to get the hardware address of a network interface: {}", iface, var12);
            continue;
         }

         boolean replace = false;
         int res = compareAddresses(bestMacAddr, macAddr);
         if (res < 0) {
            replace = true;
         } else if (res == 0) {
            res = compareAddresses(bestInetAddr, inetAddr);
            if (res < 0) {
               replace = true;
            } else if (res == 0 && bestMacAddr.length < macAddr.length) {
               replace = true;
            }
         }

         if (replace) {
            bestMacAddr = macAddr;
            bestInetAddr = inetAddr;
         }
      }
   }

   private static int compareAddresses(byte[] current, byte[] candidate) {
      if (candidate == null) {
         return 1;
      } else if (candidate.length < 6) {
         return 1;
      } else {
         boolean onlyZeroAndOne = true;
         byte[] arr$ = candidate;
         int len$ = candidate.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            byte b = arr$[i$];
            if (b != 0 && b != 1) {
               onlyZeroAndOne = false;
               break;
            }
         }

         if (onlyZeroAndOne) {
            return 1;
         } else if ((candidate[0] & 1) != 0) {
            return 1;
         } else if ((current[0] & 2) == 0) {
            return (candidate[0] & 2) == 0 ? 0 : 1;
         } else {
            return (candidate[0] & 2) == 0 ? -1 : 0;
         }
      }
   }

   private static int compareAddresses(InetAddress current, InetAddress candidate) {
      return scoreAddress(current) - scoreAddress(candidate);
   }

   private static int scoreAddress(InetAddress addr) {
      if (addr.isAnyLocalAddress()) {
         return 0;
      } else if (addr.isMulticastAddress()) {
         return 1;
      } else if (addr.isLinkLocalAddress()) {
         return 2;
      } else {
         return addr.isSiteLocalAddress() ? 3 : 4;
      }
   }

   private static String formatAddress(byte[] addr) {
      StringBuilder buf = new StringBuilder(24);
      byte[] arr$ = addr;
      int len$ = addr.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         byte b = arr$[i$];
         buf.append(String.format("%02x:", b & 255));
      }

      return buf.substring(0, buf.length() - 1);
   }

   private static int defaultProcessId() {
      ClassLoader loader = PlatformDependent.getSystemClassLoader();

      String value;
      Class processType;
      Method myPid;
      try {
         Class<?> mgmtFactoryType = Class.forName("java.lang.management.ManagementFactory", true, loader);
         processType = Class.forName("java.lang.management.RuntimeMXBean", true, loader);
         myPid = mgmtFactoryType.getMethod("getRuntimeMXBean", EmptyArrays.EMPTY_CLASSES);
         Object bean = myPid.invoke((Object)null, EmptyArrays.EMPTY_OBJECTS);
         Method getName = processType.getDeclaredMethod("getName", EmptyArrays.EMPTY_CLASSES);
         value = (String)getName.invoke(bean, EmptyArrays.EMPTY_OBJECTS);
      } catch (Exception var9) {
         logger.debug("Could not invoke ManagementFactory.getRuntimeMXBean().getName(); Android?", (Throwable)var9);

         try {
            processType = Class.forName("android.os.Process", true, loader);
            myPid = processType.getMethod("myPid", EmptyArrays.EMPTY_CLASSES);
            value = myPid.invoke((Object)null, EmptyArrays.EMPTY_OBJECTS).toString();
         } catch (Exception var8) {
            logger.debug("Could not invoke Process.myPid(); not Android?", (Throwable)var8);
            value = "";
         }
      }

      int atIndex = value.indexOf(64);
      if (atIndex >= 0) {
         value = value.substring(0, atIndex);
      }

      int pid;
      try {
         pid = Integer.parseInt(value);
      } catch (NumberFormatException var7) {
         pid = -1;
      }

      if (pid < 0 || pid > 4194304) {
         pid = ThreadLocalRandom.current().nextInt(4194305);
         logger.warn("Failed to find the current process ID from '{}'; using a random value: {}", value, pid);
      }

      return pid;
   }

   private void init() {
      int i = 0;
      System.arraycopy(MACHINE_ID, 0, this.data, i, 8);
      int i = i + 8;
      i = this.writeInt(i, PROCESS_ID);
      i = this.writeInt(i, nextSequence.getAndIncrement());
      i = this.writeLong(i, Long.reverse(System.nanoTime()) ^ System.currentTimeMillis());
      int random = ThreadLocalRandom.current().nextInt();
      this.hashCode = random;
      i = this.writeInt(i, random);

      assert i == this.data.length;

   }

   private int writeInt(int i, int value) {
      this.data[i++] = (byte)(value >>> 24);
      this.data[i++] = (byte)(value >>> 16);
      this.data[i++] = (byte)(value >>> 8);
      this.data[i++] = (byte)value;
      return i;
   }

   private int writeLong(int i, long value) {
      this.data[i++] = (byte)((int)(value >>> 56));
      this.data[i++] = (byte)((int)(value >>> 48));
      this.data[i++] = (byte)((int)(value >>> 40));
      this.data[i++] = (byte)((int)(value >>> 32));
      this.data[i++] = (byte)((int)(value >>> 24));
      this.data[i++] = (byte)((int)(value >>> 16));
      this.data[i++] = (byte)((int)(value >>> 8));
      this.data[i++] = (byte)((int)value);
      return i;
   }

   public String asShortText() {
      String shortValue = this.shortValue;
      if (shortValue == null) {
         this.shortValue = shortValue = ByteBufUtil.hexDump((byte[])this.data, 24, 4);
      }

      return shortValue;
   }

   public String asLongText() {
      String longValue = this.longValue;
      if (longValue == null) {
         this.longValue = longValue = this.newLongValue();
      }

      return longValue;
   }

   private String newLongValue() {
      StringBuilder buf = new StringBuilder(2 * this.data.length + 5);
      int i = 0;
      int i = this.appendHexDumpField(buf, i, 8);
      i = this.appendHexDumpField(buf, i, 4);
      i = this.appendHexDumpField(buf, i, 4);
      i = this.appendHexDumpField(buf, i, 8);
      i = this.appendHexDumpField(buf, i, 4);

      assert i == this.data.length;

      return buf.substring(0, buf.length() - 1);
   }

   private int appendHexDumpField(StringBuilder buf, int i, int length) {
      buf.append(ByteBufUtil.hexDump(this.data, i, length));
      buf.append('-');
      i += length;
      return i;
   }

   public int hashCode() {
      return this.hashCode;
   }

   public int compareTo(ChannelId o) {
      return 0;
   }

   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else {
         return !(obj instanceof DefaultChannelId) ? false : Arrays.equals(this.data, ((DefaultChannelId)obj).data);
      }
   }

   public String toString() {
      return this.asShortText();
   }

   static {
      int processId = -1;
      String customProcessId = SystemPropertyUtil.get("io.netty.processId");
      if (customProcessId != null) {
         try {
            processId = Integer.parseInt(customProcessId);
         } catch (NumberFormatException var4) {
         }

         if (processId >= 0 && processId <= 4194304) {
            if (logger.isDebugEnabled()) {
               logger.debug("-Dio.netty.processId: {} (user-set)", (Object)processId);
            }
         } else {
            processId = -1;
            logger.warn("-Dio.netty.processId: {} (malformed)", (Object)customProcessId);
         }
      }

      if (processId < 0) {
         processId = defaultProcessId();
         if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.processId: {} (auto-detected)", (Object)processId);
         }
      }

      PROCESS_ID = processId;
      byte[] machineId = null;
      String customMachineId = SystemPropertyUtil.get("io.netty.machineId");
      if (customMachineId != null) {
         if (MACHINE_ID_PATTERN.matcher(customMachineId).matches()) {
            machineId = parseMachineId(customMachineId);
            logger.debug("-Dio.netty.machineId: {} (user-set)", (Object)customMachineId);
         } else {
            logger.warn("-Dio.netty.machineId: {} (malformed)", (Object)customMachineId);
         }
      }

      if (machineId == null) {
         machineId = defaultMachineId();
         if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.machineId: {} (auto-detected)", (Object)formatAddress(machineId));
         }
      }

      MACHINE_ID = machineId;
   }
}
