package io.netty.resolver.dns;

import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.ThreadLocalRandom;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public final class DnsServerAddresses {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(DnsServerAddresses.class);
   private static final List<InetSocketAddress> DEFAULT_NAME_SERVER_LIST;
   private static final InetSocketAddress[] DEFAULT_NAME_SERVER_ARRAY;

   public static List<InetSocketAddress> defaultAddresses() {
      return DEFAULT_NAME_SERVER_LIST;
   }

   public static Iterable<InetSocketAddress> sequential(Iterable<? extends InetSocketAddress> addresses) {
      return sequential0(sanitize(addresses));
   }

   public static Iterable<InetSocketAddress> sequential(InetSocketAddress... addresses) {
      return sequential0(sanitize(addresses));
   }

   private static Iterable<InetSocketAddress> sequential0(final InetSocketAddress[] addresses) {
      return new Iterable<InetSocketAddress>() {
         public Iterator<InetSocketAddress> iterator() {
            return new DnsServerAddresses.SequentialAddressIterator(addresses, 0);
         }
      };
   }

   public static Iterable<InetSocketAddress> shuffled(Iterable<? extends InetSocketAddress> addresses) {
      return shuffled0(sanitize(addresses));
   }

   public static Iterable<InetSocketAddress> shuffled(InetSocketAddress... addresses) {
      return shuffled0(sanitize(addresses));
   }

   private static Iterable<InetSocketAddress> shuffled0(final InetSocketAddress[] addresses) {
      return addresses.length == 1 ? singleton(addresses[0]) : new Iterable<InetSocketAddress>() {
         public Iterator<InetSocketAddress> iterator() {
            return new DnsServerAddresses.ShuffledAddressIterator(addresses);
         }
      };
   }

   public static Iterable<InetSocketAddress> rotational(Iterable<? extends InetSocketAddress> addresses) {
      return rotational0(sanitize(addresses));
   }

   public static Iterable<InetSocketAddress> rotational(InetSocketAddress... addresses) {
      return rotational0(sanitize(addresses));
   }

   private static Iterable<InetSocketAddress> rotational0(InetSocketAddress[] addresses) {
      return new DnsServerAddresses.RotationalAddresses(addresses);
   }

   public static Iterable<InetSocketAddress> singleton(final InetSocketAddress address) {
      if (address == null) {
         throw new NullPointerException("address");
      } else if (address.isUnresolved()) {
         throw new IllegalArgumentException("cannot use an unresolved DNS server address: " + address);
      } else {
         return new Iterable<InetSocketAddress>() {
            private final Iterator<InetSocketAddress> iterator = new Iterator<InetSocketAddress>() {
               public boolean hasNext() {
                  return true;
               }

               public InetSocketAddress next() {
                  return address;
               }

               public void remove() {
                  throw new UnsupportedOperationException();
               }
            };

            public Iterator<InetSocketAddress> iterator() {
               return this.iterator;
            }
         };
      }
   }

   private static InetSocketAddress[] sanitize(Iterable<? extends InetSocketAddress> addresses) {
      if (addresses == null) {
         throw new NullPointerException("addresses");
      } else {
         ArrayList list;
         if (addresses instanceof Collection) {
            list = new ArrayList(((Collection)addresses).size());
         } else {
            list = new ArrayList(4);
         }

         Iterator i$ = addresses.iterator();

         while(i$.hasNext()) {
            InetSocketAddress a = (InetSocketAddress)i$.next();
            if (a == null) {
               break;
            }

            if (a.isUnresolved()) {
               throw new IllegalArgumentException("cannot use an unresolved DNS server address: " + a);
            }

            list.add(a);
         }

         return list.isEmpty() ? DEFAULT_NAME_SERVER_ARRAY : (InetSocketAddress[])list.toArray(new InetSocketAddress[list.size()]);
      }
   }

   private static InetSocketAddress[] sanitize(InetSocketAddress[] addresses) {
      if (addresses == null) {
         throw new NullPointerException("addresses");
      } else {
         List<InetSocketAddress> list = new ArrayList(addresses.length);
         InetSocketAddress[] arr$ = addresses;
         int len$ = addresses.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            InetSocketAddress a = arr$[i$];
            if (a == null) {
               break;
            }

            if (a.isUnresolved()) {
               throw new IllegalArgumentException("cannot use an unresolved DNS server address: " + a);
            }

            list.add(a);
         }

         return list.isEmpty() ? DEFAULT_NAME_SERVER_ARRAY : (InetSocketAddress[])list.toArray(new InetSocketAddress[list.size()]);
      }
   }

   private DnsServerAddresses() {
   }

   static {
      int DNS_PORT = true;
      ArrayList defaultNameServers = new ArrayList(2);

      try {
         Class<?> configClass = Class.forName("sun.net.dns.ResolverConfiguration");
         Method open = configClass.getMethod("open");
         Method nameservers = configClass.getMethod("nameservers");
         Object instance = open.invoke((Object)null);
         List<String> list = (List)nameservers.invoke(instance);
         int size = list.size();

         for(int i = 0; i < size; ++i) {
            String dnsAddr = (String)list.get(i);
            if (dnsAddr != null) {
               defaultNameServers.add(new InetSocketAddress(InetAddress.getByName(dnsAddr), 53));
            }
         }
      } catch (Exception var10) {
      }

      if (!defaultNameServers.isEmpty()) {
         if (logger.isDebugEnabled()) {
            logger.debug("Default DNS servers: {} (sun.net.dns.ResolverConfiguration)", (Object)defaultNameServers);
         }
      } else {
         Collections.addAll(defaultNameServers, new InetSocketAddress[]{new InetSocketAddress("8.8.8.8", 53), new InetSocketAddress("8.8.4.4", 53)});
         if (logger.isWarnEnabled()) {
            logger.warn("Default DNS servers: {} (Google Public DNS as a fallback)", (Object)defaultNameServers);
         }
      }

      DEFAULT_NAME_SERVER_LIST = Collections.unmodifiableList(defaultNameServers);
      DEFAULT_NAME_SERVER_ARRAY = (InetSocketAddress[])defaultNameServers.toArray(new InetSocketAddress[defaultNameServers.size()]);
   }

   private static final class RotationalAddresses implements Iterable<InetSocketAddress> {
      private static final AtomicIntegerFieldUpdater<DnsServerAddresses.RotationalAddresses> startIdxUpdater;
      private final InetSocketAddress[] addresses;
      private volatile int startIdx;

      RotationalAddresses(InetSocketAddress[] addresses) {
         this.addresses = addresses;
      }

      public Iterator<InetSocketAddress> iterator() {
         int curStartIdx;
         int nextStartIdx;
         do {
            curStartIdx = this.startIdx;
            nextStartIdx = curStartIdx + 1;
            if (nextStartIdx >= this.addresses.length) {
               nextStartIdx = 0;
            }
         } while(!startIdxUpdater.compareAndSet(this, curStartIdx, nextStartIdx));

         return new DnsServerAddresses.SequentialAddressIterator(this.addresses, curStartIdx);
      }

      static {
         AtomicIntegerFieldUpdater<DnsServerAddresses.RotationalAddresses> updater = PlatformDependent.newAtomicIntegerFieldUpdater(DnsServerAddresses.RotationalAddresses.class, "startIdx");
         if (updater == null) {
            updater = AtomicIntegerFieldUpdater.newUpdater(DnsServerAddresses.RotationalAddresses.class, "startIdx");
         }

         startIdxUpdater = updater;
      }
   }

   private static final class ShuffledAddressIterator implements Iterator<InetSocketAddress> {
      private final InetSocketAddress[] addresses;
      private int i;

      ShuffledAddressIterator(InetSocketAddress[] addresses) {
         this.addresses = (InetSocketAddress[])addresses.clone();
         this.shuffle();
      }

      private void shuffle() {
         InetSocketAddress[] addresses = this.addresses;
         Random r = ThreadLocalRandom.current();

         for(int i = addresses.length - 1; i >= 0; --i) {
            InetSocketAddress tmp = addresses[i];
            int j = r.nextInt(i + 1);
            addresses[i] = addresses[j];
            addresses[j] = tmp;
         }

      }

      public boolean hasNext() {
         return true;
      }

      public InetSocketAddress next() {
         int i = this.i;
         InetSocketAddress next = this.addresses[i];
         ++i;
         if (i < this.addresses.length) {
            this.i = i;
         } else {
            this.i = 0;
            this.shuffle();
         }

         return next;
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }
   }

   private static final class SequentialAddressIterator implements Iterator<InetSocketAddress> {
      private final InetSocketAddress[] addresses;
      private int i;

      SequentialAddressIterator(InetSocketAddress[] addresses, int startIdx) {
         this.addresses = addresses;
         this.i = startIdx;
      }

      public boolean hasNext() {
         return true;
      }

      public InetSocketAddress next() {
         int i = this.i;
         InetSocketAddress next = this.addresses[i];
         ++i;
         if (i < this.addresses.length) {
            this.i = i;
         } else {
            this.i = 0;
         }

         return next;
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }
   }
}
