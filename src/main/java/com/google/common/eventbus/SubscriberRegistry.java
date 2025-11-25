package com.google.common.eventbus;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.j2objc.annotations.Weak;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.annotation.Nullable;

final class SubscriberRegistry {
   private final ConcurrentMap<Class<?>, CopyOnWriteArraySet<Subscriber>> subscribers = Maps.newConcurrentMap();
   @Weak
   private final EventBus bus;
   private static final LoadingCache<Class<?>, ImmutableList<Method>> subscriberMethodsCache = CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<Class<?>, ImmutableList<Method>>() {
      public ImmutableList<Method> load(Class<?> concreteClass) throws Exception {
         return SubscriberRegistry.getAnnotatedMethodsNotCached(concreteClass);
      }
   });
   private static final LoadingCache<Class<?>, ImmutableSet<Class<?>>> flattenHierarchyCache = CacheBuilder.newBuilder().weakKeys().build(new CacheLoader<Class<?>, ImmutableSet<Class<?>>>() {
      public ImmutableSet<Class<?>> load(Class<?> concreteClass) {
         return ImmutableSet.copyOf((Collection)TypeToken.of(concreteClass).getTypes().rawTypes());
      }
   });

   SubscriberRegistry(EventBus bus) {
      this.bus = (EventBus)Preconditions.checkNotNull(bus);
   }

   void register(Object listener) {
      Multimap<Class<?>, Subscriber> listenerMethods = this.findAllSubscribers(listener);

      Collection eventMethodsInListener;
      CopyOnWriteArraySet eventSubscribers;
      for(Iterator i$ = listenerMethods.asMap().entrySet().iterator(); i$.hasNext(); eventSubscribers.addAll(eventMethodsInListener)) {
         Entry<Class<?>, Collection<Subscriber>> entry = (Entry)i$.next();
         Class<?> eventType = (Class)entry.getKey();
         eventMethodsInListener = (Collection)entry.getValue();
         eventSubscribers = (CopyOnWriteArraySet)this.subscribers.get(eventType);
         if (eventSubscribers == null) {
            CopyOnWriteArraySet<Subscriber> newSet = new CopyOnWriteArraySet();
            eventSubscribers = (CopyOnWriteArraySet)MoreObjects.firstNonNull(this.subscribers.putIfAbsent(eventType, newSet), newSet);
         }
      }

   }

   void unregister(Object listener) {
      Multimap<Class<?>, Subscriber> listenerMethods = this.findAllSubscribers(listener);
      Iterator i$ = listenerMethods.asMap().entrySet().iterator();

      Collection listenerMethodsForType;
      CopyOnWriteArraySet currentSubscribers;
      do {
         if (!i$.hasNext()) {
            return;
         }

         Entry<Class<?>, Collection<Subscriber>> entry = (Entry)i$.next();
         Class<?> eventType = (Class)entry.getKey();
         listenerMethodsForType = (Collection)entry.getValue();
         currentSubscribers = (CopyOnWriteArraySet)this.subscribers.get(eventType);
      } while(currentSubscribers != null && currentSubscribers.removeAll(listenerMethodsForType));

      throw new IllegalArgumentException("missing event subscriber for an annotated method. Is " + listener + " registered?");
   }

   @VisibleForTesting
   Set<Subscriber> getSubscribersForTesting(Class<?> eventType) {
      return (Set)MoreObjects.firstNonNull(this.subscribers.get(eventType), ImmutableSet.of());
   }

   Iterator<Subscriber> getSubscribers(Object event) {
      ImmutableSet<Class<?>> eventTypes = flattenHierarchy(event.getClass());
      List<Iterator<Subscriber>> subscriberIterators = Lists.newArrayListWithCapacity(eventTypes.size());
      Iterator i$ = eventTypes.iterator();

      while(i$.hasNext()) {
         Class<?> eventType = (Class)i$.next();
         CopyOnWriteArraySet<Subscriber> eventSubscribers = (CopyOnWriteArraySet)this.subscribers.get(eventType);
         if (eventSubscribers != null) {
            subscriberIterators.add(eventSubscribers.iterator());
         }
      }

      return Iterators.concat(subscriberIterators.iterator());
   }

   private Multimap<Class<?>, Subscriber> findAllSubscribers(Object listener) {
      Multimap<Class<?>, Subscriber> methodsInListener = HashMultimap.create();
      Class<?> clazz = listener.getClass();
      Iterator i$ = getAnnotatedMethods(clazz).iterator();

      while(i$.hasNext()) {
         Method method = (Method)i$.next();
         Class<?>[] parameterTypes = method.getParameterTypes();
         Class<?> eventType = parameterTypes[0];
         methodsInListener.put(eventType, Subscriber.create(this.bus, listener, method));
      }

      return methodsInListener;
   }

   private static ImmutableList<Method> getAnnotatedMethods(Class<?> clazz) {
      return (ImmutableList)subscriberMethodsCache.getUnchecked(clazz);
   }

   private static ImmutableList<Method> getAnnotatedMethodsNotCached(Class<?> clazz) {
      Set<? extends Class<?>> supertypes = TypeToken.of(clazz).getTypes().rawTypes();
      Map<SubscriberRegistry.MethodIdentifier, Method> identifiers = Maps.newHashMap();
      Iterator i$ = supertypes.iterator();

      while(i$.hasNext()) {
         Class<?> supertype = (Class)i$.next();
         Method[] arr$ = supertype.getDeclaredMethods();
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            Method method = arr$[i$];
            if (method.isAnnotationPresent(Subscribe.class) && !method.isSynthetic()) {
               Class<?>[] parameterTypes = method.getParameterTypes();
               Preconditions.checkArgument(parameterTypes.length == 1, "Method %s has @Subscribe annotation but has %s parameters.Subscriber methods must have exactly 1 parameter.", method, parameterTypes.length);
               SubscriberRegistry.MethodIdentifier ident = new SubscriberRegistry.MethodIdentifier(method);
               if (!identifiers.containsKey(ident)) {
                  identifiers.put(ident, method);
               }
            }
         }
      }

      return ImmutableList.copyOf(identifiers.values());
   }

   @VisibleForTesting
   static ImmutableSet<Class<?>> flattenHierarchy(Class<?> concreteClass) {
      try {
         return (ImmutableSet)flattenHierarchyCache.getUnchecked(concreteClass);
      } catch (UncheckedExecutionException var2) {
         throw Throwables.propagate(var2.getCause());
      }
   }

   private static final class MethodIdentifier {
      private final String name;
      private final List<Class<?>> parameterTypes;

      MethodIdentifier(Method method) {
         this.name = method.getName();
         this.parameterTypes = Arrays.asList(method.getParameterTypes());
      }

      public int hashCode() {
         return Objects.hashCode(this.name, this.parameterTypes);
      }

      public boolean equals(@Nullable Object o) {
         if (!(o instanceof SubscriberRegistry.MethodIdentifier)) {
            return false;
         } else {
            SubscriberRegistry.MethodIdentifier ident = (SubscriberRegistry.MethodIdentifier)o;
            return this.name.equals(ident.name) && this.parameterTypes.equals(ident.parameterTypes);
         }
      }
   }
}
