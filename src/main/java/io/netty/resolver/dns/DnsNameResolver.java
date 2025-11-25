package io.netty.resolver.dns;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoop;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.ReflectiveChannelFactory;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.handler.codec.dns.DnsClass;
import io.netty.handler.codec.dns.DnsQueryEncoder;
import io.netty.handler.codec.dns.DnsQuestion;
import io.netty.handler.codec.dns.DnsResource;
import io.netty.handler.codec.dns.DnsResponse;
import io.netty.handler.codec.dns.DnsResponseCode;
import io.netty.handler.codec.dns.DnsResponseDecoder;
import io.netty.resolver.SimpleNameResolver;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.OneTimeTask;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import java.net.IDN;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class DnsNameResolver extends SimpleNameResolver<InetSocketAddress> {
   private static final InternalLogger logger = InternalLoggerFactory.getInstance(DnsNameResolver.class);
   static final InetSocketAddress ANY_LOCAL_ADDR = new InetSocketAddress(0);
   private static final InternetProtocolFamily[] DEFAULT_RESOLVE_ADDRESS_TYPES = new InternetProtocolFamily[2];
   private static final DnsResponseDecoder DECODER;
   private static final DnsQueryEncoder ENCODER;
   final Iterable<InetSocketAddress> nameServerAddresses;
   final ChannelFuture bindFuture;
   final DatagramChannel ch;
   final AtomicReferenceArray<DnsQueryContext> promises;
   final ConcurrentMap<DnsQuestion, DnsNameResolver.DnsCacheEntry> queryCache;
   private final DnsNameResolver.DnsResponseHandler responseHandler;
   private volatile long queryTimeoutMillis;
   private volatile int minTtl;
   private volatile int maxTtl;
   private volatile int negativeTtl;
   private volatile int maxTriesPerQuery;
   private volatile InternetProtocolFamily[] resolveAddressTypes;
   private volatile boolean recursionDesired;
   private volatile int maxQueriesPerResolve;
   private volatile int maxPayloadSize;
   private volatile DnsClass maxPayloadSizeClass;

   public DnsNameResolver(EventLoop eventLoop, Class<? extends DatagramChannel> channelType, InetSocketAddress nameServerAddress) {
      this(eventLoop, channelType, ANY_LOCAL_ADDR, nameServerAddress);
   }

   public DnsNameResolver(EventLoop eventLoop, Class<? extends DatagramChannel> channelType, InetSocketAddress localAddress, InetSocketAddress nameServerAddress) {
      this(eventLoop, (ChannelFactory)(new ReflectiveChannelFactory(channelType)), localAddress, (InetSocketAddress)nameServerAddress);
   }

   public DnsNameResolver(EventLoop eventLoop, ChannelFactory<? extends DatagramChannel> channelFactory, InetSocketAddress nameServerAddress) {
      this(eventLoop, channelFactory, ANY_LOCAL_ADDR, nameServerAddress);
   }

   public DnsNameResolver(EventLoop eventLoop, ChannelFactory<? extends DatagramChannel> channelFactory, InetSocketAddress localAddress, InetSocketAddress nameServerAddress) {
      this(eventLoop, channelFactory, localAddress, DnsServerAddresses.singleton(nameServerAddress));
   }

   public DnsNameResolver(EventLoop eventLoop, Class<? extends DatagramChannel> channelType, Iterable<InetSocketAddress> nameServerAddresses) {
      this(eventLoop, channelType, ANY_LOCAL_ADDR, nameServerAddresses);
   }

   public DnsNameResolver(EventLoop eventLoop, Class<? extends DatagramChannel> channelType, InetSocketAddress localAddress, Iterable<InetSocketAddress> nameServerAddresses) {
      this(eventLoop, (ChannelFactory)(new ReflectiveChannelFactory(channelType)), localAddress, (Iterable)nameServerAddresses);
   }

   public DnsNameResolver(EventLoop eventLoop, ChannelFactory<? extends DatagramChannel> channelFactory, Iterable<InetSocketAddress> nameServerAddresses) {
      this(eventLoop, channelFactory, ANY_LOCAL_ADDR, nameServerAddresses);
   }

   public DnsNameResolver(EventLoop eventLoop, ChannelFactory<? extends DatagramChannel> channelFactory, InetSocketAddress localAddress, Iterable<InetSocketAddress> nameServerAddresses) {
      super(eventLoop);
      this.promises = new AtomicReferenceArray(65536);
      this.queryCache = PlatformDependent.newConcurrentHashMap();
      this.responseHandler = new DnsNameResolver.DnsResponseHandler();
      this.queryTimeoutMillis = 5000L;
      this.maxTtl = Integer.MAX_VALUE;
      this.maxTriesPerQuery = 2;
      this.resolveAddressTypes = DEFAULT_RESOLVE_ADDRESS_TYPES;
      this.recursionDesired = true;
      this.maxQueriesPerResolve = 8;
      if (channelFactory == null) {
         throw new NullPointerException("channelFactory");
      } else if (nameServerAddresses == null) {
         throw new NullPointerException("nameServerAddresses");
      } else if (!nameServerAddresses.iterator().hasNext()) {
         throw new NullPointerException("nameServerAddresses is empty");
      } else if (localAddress == null) {
         throw new NullPointerException("localAddress");
      } else {
         this.nameServerAddresses = nameServerAddresses;
         this.bindFuture = this.newChannel(channelFactory, localAddress);
         this.ch = (DatagramChannel)this.bindFuture.channel();
         this.setMaxPayloadSize(4096);
      }
   }

   private ChannelFuture newChannel(ChannelFactory<? extends DatagramChannel> channelFactory, InetSocketAddress localAddress) {
      Bootstrap b = new Bootstrap();
      b.group(this.executor());
      b.channelFactory(channelFactory);
      b.handler(new ChannelInitializer<DatagramChannel>() {
         protected void initChannel(DatagramChannel ch) throws Exception {
            ch.pipeline().addLast(DnsNameResolver.DECODER, DnsNameResolver.ENCODER, DnsNameResolver.this.responseHandler);
         }
      });
      ChannelFuture bindFuture = b.bind(localAddress);
      bindFuture.channel().closeFuture().addListener(new ChannelFutureListener() {
         public void operationComplete(ChannelFuture future) throws Exception {
            DnsNameResolver.this.clearCache();
         }
      });
      return bindFuture;
   }

   public int minTtl() {
      return this.minTtl;
   }

   public int maxTtl() {
      return this.maxTtl;
   }

   public DnsNameResolver setTtl(int minTtl, int maxTtl) {
      if (minTtl < 0) {
         throw new IllegalArgumentException("minTtl: " + minTtl + " (expected: >= 0)");
      } else if (maxTtl < 0) {
         throw new IllegalArgumentException("maxTtl: " + maxTtl + " (expected: >= 0)");
      } else if (minTtl > maxTtl) {
         throw new IllegalArgumentException("minTtl: " + minTtl + ", maxTtl: " + maxTtl + " (expected: 0 <= minTtl <= maxTtl)");
      } else {
         this.maxTtl = maxTtl;
         this.minTtl = minTtl;
         return this;
      }
   }

   public int negativeTtl() {
      return this.negativeTtl;
   }

   public DnsNameResolver setNegativeTtl(int negativeTtl) {
      if (negativeTtl < 0) {
         throw new IllegalArgumentException("negativeTtl: " + negativeTtl + " (expected: >= 0)");
      } else {
         this.negativeTtl = negativeTtl;
         return this;
      }
   }

   public long queryTimeoutMillis() {
      return this.queryTimeoutMillis;
   }

   public DnsNameResolver setQueryTimeoutMillis(long queryTimeoutMillis) {
      if (queryTimeoutMillis < 0L) {
         throw new IllegalArgumentException("queryTimeoutMillis: " + queryTimeoutMillis + " (expected: >= 0)");
      } else {
         this.queryTimeoutMillis = queryTimeoutMillis;
         return this;
      }
   }

   public int maxTriesPerQuery() {
      return this.maxTriesPerQuery;
   }

   public DnsNameResolver setMaxTriesPerQuery(int maxTriesPerQuery) {
      if (maxTriesPerQuery < 1) {
         throw new IllegalArgumentException("maxTries: " + maxTriesPerQuery + " (expected: > 0)");
      } else {
         this.maxTriesPerQuery = maxTriesPerQuery;
         return this;
      }
   }

   public List<InternetProtocolFamily> resolveAddressTypes() {
      return Arrays.asList(this.resolveAddressTypes);
   }

   InternetProtocolFamily[] resolveAddressTypesUnsafe() {
      return this.resolveAddressTypes;
   }

   public DnsNameResolver setResolveAddressTypes(InternetProtocolFamily... resolveAddressTypes) {
      if (resolveAddressTypes == null) {
         throw new NullPointerException("resolveAddressTypes");
      } else {
         List<InternetProtocolFamily> list = new ArrayList(InternetProtocolFamily.values().length);
         InternetProtocolFamily[] arr$ = resolveAddressTypes;
         int len$ = resolveAddressTypes.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            InternetProtocolFamily f = arr$[i$];
            if (f == null) {
               break;
            }

            if (!list.contains(f)) {
               list.add(f);
            }
         }

         if (list.isEmpty()) {
            throw new IllegalArgumentException("no protocol family specified");
         } else {
            this.resolveAddressTypes = (InternetProtocolFamily[])list.toArray(new InternetProtocolFamily[list.size()]);
            return this;
         }
      }
   }

   public DnsNameResolver setResolveAddressTypes(Iterable<InternetProtocolFamily> resolveAddressTypes) {
      if (resolveAddressTypes == null) {
         throw new NullPointerException("resolveAddressTypes");
      } else {
         List<InternetProtocolFamily> list = new ArrayList(InternetProtocolFamily.values().length);
         Iterator i$ = resolveAddressTypes.iterator();

         while(i$.hasNext()) {
            InternetProtocolFamily f = (InternetProtocolFamily)i$.next();
            if (f == null) {
               break;
            }

            if (!list.contains(f)) {
               list.add(f);
            }
         }

         if (list.isEmpty()) {
            throw new IllegalArgumentException("no protocol family specified");
         } else {
            this.resolveAddressTypes = (InternetProtocolFamily[])list.toArray(new InternetProtocolFamily[list.size()]);
            return this;
         }
      }
   }

   public boolean isRecursionDesired() {
      return this.recursionDesired;
   }

   public DnsNameResolver setRecursionDesired(boolean recursionDesired) {
      this.recursionDesired = recursionDesired;
      return this;
   }

   public int maxQueriesPerResolve() {
      return this.maxQueriesPerResolve;
   }

   public DnsNameResolver setMaxQueriesPerResolve(int maxQueriesPerResolve) {
      if (maxQueriesPerResolve <= 0) {
         throw new IllegalArgumentException("maxQueriesPerResolve: " + maxQueriesPerResolve + " (expected: > 0)");
      } else {
         this.maxQueriesPerResolve = maxQueriesPerResolve;
         return this;
      }
   }

   public int maxPayloadSize() {
      return this.maxPayloadSize;
   }

   public DnsNameResolver setMaxPayloadSize(int maxPayloadSize) {
      if (maxPayloadSize <= 0) {
         throw new IllegalArgumentException("maxPayloadSize: " + maxPayloadSize + " (expected: > 0)");
      } else if (this.maxPayloadSize == maxPayloadSize) {
         return this;
      } else {
         this.maxPayloadSize = maxPayloadSize;
         this.maxPayloadSizeClass = DnsClass.valueOf(maxPayloadSize);
         this.ch.config().setRecvByteBufAllocator(new FixedRecvByteBufAllocator(maxPayloadSize));
         return this;
      }
   }

   DnsClass maxPayloadSizeClass() {
      return this.maxPayloadSizeClass;
   }

   public DnsNameResolver clearCache() {
      Iterator i = this.queryCache.entrySet().iterator();

      while(i.hasNext()) {
         Entry<DnsQuestion, DnsNameResolver.DnsCacheEntry> e = (Entry)i.next();
         i.remove();
         ((DnsNameResolver.DnsCacheEntry)e.getValue()).release();
      }

      return this;
   }

   public boolean clearCache(DnsQuestion question) {
      DnsNameResolver.DnsCacheEntry e = (DnsNameResolver.DnsCacheEntry)this.queryCache.remove(question);
      if (e != null) {
         e.release();
         return true;
      } else {
         return false;
      }
   }

   public void close() {
      this.ch.close();
   }

   protected EventLoop executor() {
      return (EventLoop)super.executor();
   }

   protected boolean doIsResolved(InetSocketAddress address) {
      return !address.isUnresolved();
   }

   protected void doResolve(InetSocketAddress unresolvedAddress, Promise<InetSocketAddress> promise) throws Exception {
      String hostname = IDN.toASCII(hostname(unresolvedAddress));
      int port = unresolvedAddress.getPort();
      DnsNameResolverContext ctx = new DnsNameResolverContext(this, hostname, port, promise);
      ctx.resolve();
   }

   private static String hostname(InetSocketAddress addr) {
      return PlatformDependent.javaVersion() < 7 ? addr.getHostName() : addr.getHostString();
   }

   public Future<DnsResponse> query(DnsQuestion question) {
      return this.query(this.nameServerAddresses, question);
   }

   public Future<DnsResponse> query(DnsQuestion question, Promise<DnsResponse> promise) {
      return this.query(this.nameServerAddresses, question, promise);
   }

   public Future<DnsResponse> query(Iterable<InetSocketAddress> nameServerAddresses, DnsQuestion question) {
      if (nameServerAddresses == null) {
         throw new NullPointerException("nameServerAddresses");
      } else if (question == null) {
         throw new NullPointerException("question");
      } else {
         EventLoop eventLoop = this.ch.eventLoop();
         DnsNameResolver.DnsCacheEntry cachedResult = (DnsNameResolver.DnsCacheEntry)this.queryCache.get(question);
         if (cachedResult != null) {
            return cachedResult.response != null ? eventLoop.newSucceededFuture(cachedResult.response.retain()) : eventLoop.newFailedFuture(cachedResult.cause);
         } else {
            return this.query0(nameServerAddresses, question, eventLoop.newPromise());
         }
      }
   }

   public Future<DnsResponse> query(Iterable<InetSocketAddress> nameServerAddresses, DnsQuestion question, Promise<DnsResponse> promise) {
      if (nameServerAddresses == null) {
         throw new NullPointerException("nameServerAddresses");
      } else if (question == null) {
         throw new NullPointerException("question");
      } else if (promise == null) {
         throw new NullPointerException("promise");
      } else {
         DnsNameResolver.DnsCacheEntry cachedResult = (DnsNameResolver.DnsCacheEntry)this.queryCache.get(question);
         if (cachedResult != null) {
            return cachedResult.response != null ? promise.setSuccess(cachedResult.response.retain()) : promise.setFailure(cachedResult.cause);
         } else {
            return this.query0(nameServerAddresses, question, promise);
         }
      }
   }

   private Future<DnsResponse> query0(Iterable<InetSocketAddress> nameServerAddresses, DnsQuestion question, Promise<DnsResponse> promise) {
      try {
         (new DnsQueryContext(this, nameServerAddresses, question, promise)).query();
         return promise;
      } catch (Exception var5) {
         return promise.setFailure(var5);
      }
   }

   void cache(final DnsQuestion question, DnsNameResolver.DnsCacheEntry entry, long delaySeconds) {
      DnsNameResolver.DnsCacheEntry oldEntry = (DnsNameResolver.DnsCacheEntry)this.queryCache.put(question, entry);
      if (oldEntry != null) {
         oldEntry.release();
      }

      boolean scheduled = false;

      try {
         entry.expirationFuture = this.ch.eventLoop().schedule(new OneTimeTask() {
            public void run() {
               DnsNameResolver.this.clearCache(question);
            }
         }, delaySeconds, TimeUnit.SECONDS);
         scheduled = true;
      } finally {
         if (!scheduled) {
            this.clearCache(question);
            entry.release();
         }

      }

   }

   static {
      if ("true".equalsIgnoreCase(SystemPropertyUtil.get("java.net.preferIPv6Addresses"))) {
         DEFAULT_RESOLVE_ADDRESS_TYPES[0] = InternetProtocolFamily.IPv6;
         DEFAULT_RESOLVE_ADDRESS_TYPES[1] = InternetProtocolFamily.IPv4;
         logger.debug("-Djava.net.preferIPv6Addresses: true");
      } else {
         DEFAULT_RESOLVE_ADDRESS_TYPES[0] = InternetProtocolFamily.IPv4;
         DEFAULT_RESOLVE_ADDRESS_TYPES[1] = InternetProtocolFamily.IPv6;
         logger.debug("-Djava.net.preferIPv6Addresses: false");
      }

      DECODER = new DnsResponseDecoder();
      ENCODER = new DnsQueryEncoder();
   }

   static final class DnsCacheEntry {
      final DnsResponse response;
      final Throwable cause;
      volatile ScheduledFuture<?> expirationFuture;

      DnsCacheEntry(DnsResponse response) {
         this.response = response.retain();
         this.cause = null;
      }

      DnsCacheEntry(Throwable cause) {
         this.cause = cause;
         this.response = null;
      }

      void release() {
         DnsResponse response = this.response;
         if (response != null) {
            ReferenceCountUtil.safeRelease(response);
         }

         ScheduledFuture<?> expirationFuture = this.expirationFuture;
         if (expirationFuture != null) {
            expirationFuture.cancel(false);
         }

      }
   }

   private final class DnsResponseHandler extends ChannelHandlerAdapter {
      private DnsResponseHandler() {
      }

      public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
         try {
            DnsResponse res = (DnsResponse)msg;
            int queryId = res.header().id();
            if (DnsNameResolver.logger.isDebugEnabled()) {
               DnsNameResolver.logger.debug("{} RECEIVED: [{}: {}], {}", DnsNameResolver.this.ch, queryId, res.sender(), res);
            }

            DnsQueryContext qCtx = (DnsQueryContext)DnsNameResolver.this.promises.get(queryId);
            if (qCtx == null) {
               if (DnsNameResolver.logger.isWarnEnabled()) {
                  DnsNameResolver.logger.warn("Received a DNS response with an unknown ID: {}", (Object)queryId);
               }

               return;
            }

            List<DnsQuestion> questions = res.questions();
            if (questions.size() != 1) {
               DnsNameResolver.logger.warn("Received a DNS response with invalid number of questions: {}", (Object)res);
               return;
            }

            DnsQuestion q = qCtx.question();
            if (!q.equals(questions.get(0))) {
               DnsNameResolver.logger.warn("Received a mismatching DNS response: {}", (Object)res);
               return;
            }

            ScheduledFuture<?> timeoutFuture = qCtx.timeoutFuture();
            if (timeoutFuture != null) {
               timeoutFuture.cancel(false);
            }

            if (res.header().responseCode() == DnsResponseCode.NOERROR) {
               this.cache(q, res);
               DnsNameResolver.this.promises.set(queryId, (Object)null);
               Promise<DnsResponse> qPromise = qCtx.promise();
               if (qPromise.setUncancellable()) {
                  qPromise.setSuccess(res.retain());
               }
            } else {
               qCtx.retry(res.sender(), "response code: " + res.header().responseCode() + " with " + res.answers().size() + " answer(s) and " + res.authorityResources().size() + " authority resource(s)");
            }
         } finally {
            ReferenceCountUtil.safeRelease(msg);
         }

      }

      private void cache(DnsQuestion question, DnsResponse res) {
         int maxTtl = DnsNameResolver.this.maxTtl();
         if (maxTtl != 0) {
            long ttl = Long.MAX_VALUE;
            Iterator i$ = res.answers().iterator();

            while(i$.hasNext()) {
               DnsResource r = (DnsResource)i$.next();
               long rTtl = r.timeToLive();
               if (ttl > rTtl) {
                  ttl = rTtl;
               }
            }

            ttl = Math.max((long)DnsNameResolver.this.minTtl(), Math.min((long)maxTtl, ttl));
            DnsNameResolver.this.cache(question, new DnsNameResolver.DnsCacheEntry(res), ttl);
         }
      }

      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
         DnsNameResolver.logger.warn("Unexpected exception: ", cause);
      }

      // $FF: synthetic method
      DnsResponseHandler(Object x1) {
         this();
      }
   }
}
