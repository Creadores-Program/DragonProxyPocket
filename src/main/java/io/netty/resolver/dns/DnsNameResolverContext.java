package io.netty.resolver.dns;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.handler.codec.dns.DnsClass;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsResource;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsType;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.StringUtil;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class DnsNameResolverContext {
   private static final int INADDRSZ4 = 4;
   private static final int INADDRSZ6 = 16;
   private static final FutureListener<DnsResponse> RELEASE_RESPONSE = new FutureListener<DnsResponse>() {
      public void operationComplete(Future<DnsResponse> future) {
         if (future.isSuccess()) {
            ((DnsResponse)future.getNow()).release();
         }

      }
   };
   private final DnsNameResolver parent;
   private final Promise<InetSocketAddress> promise;
   private final String hostname;
   private final int port;
   private final int maxAllowedQueries;
   private final InternetProtocolFamily[] resolveAddressTypes;
   private final Set<Future<DnsResponse>> queriesInProgress = Collections.newSetFromMap(new IdentityHashMap());
   private List<InetAddress> resolvedAddresses;
   private StringBuilder trace;
   private int allowedQueries;
   private boolean triedCNAME;

   DnsNameResolverContext(DnsNameResolver parent, String hostname, int port, Promise<InetSocketAddress> promise) {
      this.parent = parent;
      this.promise = promise;
      this.hostname = hostname;
      this.port = port;
      this.maxAllowedQueries = parent.maxQueriesPerResolve();
      this.resolveAddressTypes = parent.resolveAddressTypesUnsafe();
      this.allowedQueries = this.maxAllowedQueries;
   }

   void resolve() {
      InternetProtocolFamily[] arr$ = this.resolveAddressTypes;
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         InternetProtocolFamily f = arr$[i$];
         DnsType type;
         switch(f) {
         case IPv4:
            type = DnsType.A;
            break;
         case IPv6:
            type = DnsType.AAAA;
            break;
         default:
            throw new Error();
         }

         this.query(this.parent.nameServerAddresses, new DnsQuestion(this.hostname, type));
      }

   }

   private void query(Iterable<InetSocketAddress> nameServerAddresses, final DnsQuestion question) {
      if (this.allowedQueries != 0 && !this.promise.isCancelled()) {
         --this.allowedQueries;
         Future<DnsResponse> f = this.parent.query(nameServerAddresses, question);
         this.queriesInProgress.add(f);
         f.addListener(new FutureListener<DnsResponse>() {
            public void operationComplete(Future<DnsResponse> future) throws Exception {
               DnsNameResolverContext.this.queriesInProgress.remove(future);
               if (!DnsNameResolverContext.this.promise.isDone()) {
                  try {
                     if (future.isSuccess()) {
                        DnsNameResolverContext.this.onResponse(question, (DnsResponse)future.getNow());
                     } else {
                        DnsNameResolverContext.this.addTrace(future.cause());
                     }
                  } finally {
                     DnsNameResolverContext.this.tryToFinishResolve();
                  }

               }
            }
         });
      }
   }

   void onResponse(DnsQuestion question, DnsResponse response) {
      DnsType type = question.type();

      try {
         if (type != DnsType.A && type != DnsType.AAAA) {
            if (type == DnsType.CNAME) {
               this.onResponseCNAME(question, response);
            }
         } else {
            this.onResponseAorAAAA(type, question, response);
         }
      } finally {
         ReferenceCountUtil.safeRelease(response);
      }

   }

   private void onResponseAorAAAA(DnsType qType, DnsQuestion question, DnsResponse response) {
      Map<String, String> cnames = buildAliasMap(response);
      boolean found = false;
      Iterator i$ = response.answers().iterator();

      while(true) {
         int contentLen;
         ByteBuf content;
         do {
            DnsResource r;
            String resolved;
            do {
               DnsType type;
               do {
                  if (!i$.hasNext()) {
                     if (found) {
                        return;
                     }

                     this.addTrace(response.sender(), "no matching " + qType + " record found");
                     if (!cnames.isEmpty()) {
                        this.onResponseCNAME(question, response, cnames, false);
                     }

                     return;
                  }

                  r = (DnsResource)i$.next();
                  type = r.type();
               } while(type != DnsType.A && type != DnsType.AAAA);

               String qName = question.name().toLowerCase(Locale.US);
               String rName = r.name().toLowerCase(Locale.US);
               if (rName.equals(qName)) {
                  break;
               }

               resolved = qName;

               do {
                  resolved = (String)cnames.get(resolved);
               } while(!rName.equals(resolved) && resolved != null);
            } while(resolved == null);

            content = r.content();
            contentLen = content.readableBytes();
         } while(contentLen != 4 && contentLen != 16);

         byte[] addrBytes = new byte[contentLen];
         content.getBytes(content.readerIndex(), addrBytes);

         try {
            InetAddress resolved = InetAddress.getByAddress(this.hostname, addrBytes);
            if (this.resolvedAddresses == null) {
               this.resolvedAddresses = new ArrayList();
            }

            this.resolvedAddresses.add(resolved);
            found = true;
         } catch (UnknownHostException var15) {
            throw new Error(var15);
         }
      }
   }

   private void onResponseCNAME(DnsQuestion question, DnsResponse response) {
      this.onResponseCNAME(question, response, buildAliasMap(response), true);
   }

   private void onResponseCNAME(DnsQuestion question, DnsResponse response, Map<String, String> cnames, boolean trace) {
      String name = question.name().toLowerCase(Locale.US);
      String resolved = name;
      boolean found = false;

      while(true) {
         String next = (String)cnames.get(resolved);
         if (next == null) {
            if (found) {
               this.followCname(response.sender(), name, resolved);
            } else if (trace) {
               this.addTrace(response.sender(), "no matching CNAME record found");
            }

            return;
         }

         found = true;
         resolved = next;
      }
   }

   private static Map<String, String> buildAliasMap(DnsResponse response) {
      Map<String, String> cnames = null;
      Iterator i$ = response.answers().iterator();

      while(i$.hasNext()) {
         DnsResource r = (DnsResource)i$.next();
         DnsType type = r.type();
         if (type == DnsType.CNAME) {
            String content = decodeDomainName(r.content());
            if (content != null) {
               if (cnames == null) {
                  cnames = new HashMap();
               }

               cnames.put(r.name().toLowerCase(Locale.US), content.toLowerCase(Locale.US));
            }
         }
      }

      return (Map)(cnames != null ? cnames : Collections.emptyMap());
   }

   void tryToFinishResolve() {
      if (!this.queriesInProgress.isEmpty()) {
         if (this.gotPreferredAddress()) {
            this.finishResolve();
         }

      } else if (this.resolvedAddresses == null && !this.triedCNAME) {
         this.triedCNAME = true;
         this.query(this.parent.nameServerAddresses, new DnsQuestion(this.hostname, DnsType.CNAME, DnsClass.IN));
      } else {
         this.finishResolve();
      }
   }

   private boolean gotPreferredAddress() {
      if (this.resolvedAddresses == null) {
         return false;
      } else {
         int size = this.resolvedAddresses.size();
         int i;
         switch(this.resolveAddressTypes[0]) {
         case IPv4:
            for(i = 0; i < size; ++i) {
               if (this.resolvedAddresses.get(i) instanceof Inet4Address) {
                  return true;
               }
            }

            return false;
         case IPv6:
            for(i = 0; i < size; ++i) {
               if (this.resolvedAddresses.get(i) instanceof Inet6Address) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private void finishResolve() {
      if (!this.queriesInProgress.isEmpty()) {
         Iterator i = this.queriesInProgress.iterator();

         while(i.hasNext()) {
            Future<DnsResponse> f = (Future)i.next();
            i.remove();
            if (!f.cancel(false)) {
               f.addListener(RELEASE_RESPONSE);
            }
         }
      }

      if (this.resolvedAddresses != null) {
         InternetProtocolFamily[] arr$ = this.resolveAddressTypes;
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            InternetProtocolFamily f = arr$[i$];
            switch(f) {
            case IPv4:
               if (this.finishResolveWithIPv4()) {
                  return;
               }
               break;
            case IPv6:
               if (this.finishResolveWithIPv6()) {
                  return;
               }
            }
         }
      }

      int tries = this.maxAllowedQueries - this.allowedQueries;
      UnknownHostException cause;
      if (tries > 1) {
         cause = new UnknownHostException("failed to resolve " + this.hostname + " after " + tries + " queries:" + this.trace);
      } else {
         cause = new UnknownHostException("failed to resolve " + this.hostname + ':' + this.trace);
      }

      this.promise.tryFailure(cause);
   }

   private boolean finishResolveWithIPv4() {
      List<InetAddress> resolvedAddresses = this.resolvedAddresses;
      int size = resolvedAddresses.size();

      for(int i = 0; i < size; ++i) {
         InetAddress a = (InetAddress)resolvedAddresses.get(i);
         if (a instanceof Inet4Address) {
            this.promise.trySuccess(new InetSocketAddress(a, this.port));
            return true;
         }
      }

      return false;
   }

   private boolean finishResolveWithIPv6() {
      List<InetAddress> resolvedAddresses = this.resolvedAddresses;
      int size = resolvedAddresses.size();

      for(int i = 0; i < size; ++i) {
         InetAddress a = (InetAddress)resolvedAddresses.get(i);
         if (a instanceof Inet6Address) {
            this.promise.trySuccess(new InetSocketAddress(a, this.port));
            return true;
         }
      }

      return false;
   }

   static String decodeDomainName(ByteBuf buf) {
      buf.markReaderIndex();

      String var11;
      try {
         int position = -1;
         int checked = 0;
         int length = buf.writerIndex();
         StringBuilder name = new StringBuilder(64);

         for(short len = buf.readUnsignedByte(); buf.isReadable() && len != 0; len = buf.readUnsignedByte()) {
            boolean pointer = (len & 192) == 192;
            if (pointer) {
               if (position == -1) {
                  position = buf.readerIndex() + 1;
               }

               buf.readerIndex((len & 63) << 8 | buf.readUnsignedByte());
               checked += 2;
               if (checked >= length) {
                  Object var7 = null;
                  return (String)var7;
               }
            } else {
               name.append(buf.toString(buf.readerIndex(), len, CharsetUtil.UTF_8)).append('.');
               buf.skipBytes(len);
            }
         }

         if (position != -1) {
            buf.readerIndex(position);
         }

         if (name.length() == 0) {
            var11 = null;
            return var11;
         }

         var11 = name.substring(0, name.length() - 1);
      } finally {
         buf.resetReaderIndex();
      }

      return var11;
   }

   private void followCname(InetSocketAddress nameServerAddr, String name, String cname) {
      if (this.trace == null) {
         this.trace = new StringBuilder(128);
      }

      this.trace.append(StringUtil.NEWLINE);
      this.trace.append("\tfrom ");
      this.trace.append(nameServerAddr);
      this.trace.append(": ");
      this.trace.append(name);
      this.trace.append(" CNAME ");
      this.trace.append(cname);
      this.query(this.parent.nameServerAddresses, new DnsQuestion(cname, DnsType.A, DnsClass.IN));
      this.query(this.parent.nameServerAddresses, new DnsQuestion(cname, DnsType.AAAA, DnsClass.IN));
   }

   private void addTrace(InetSocketAddress nameServerAddr, String msg) {
      if (this.trace == null) {
         this.trace = new StringBuilder(128);
      }

      this.trace.append(StringUtil.NEWLINE);
      this.trace.append("\tfrom ");
      this.trace.append(nameServerAddr);
      this.trace.append(": ");
      this.trace.append(msg);
   }

   private void addTrace(Throwable cause) {
      if (this.trace == null) {
         this.trace = new StringBuilder(128);
      }

      this.trace.append(StringUtil.NEWLINE);
      this.trace.append("Caused by: ");
      this.trace.append(cause);
   }
}
