package com.google.common.reflect;

import com.google.common.annotations.Beta;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

@Beta
public final class ImmutableTypeToInstanceMap<B> extends ForwardingMap<TypeToken<? extends B>, B> implements TypeToInstanceMap<B> {
   private final ImmutableMap<TypeToken<? extends B>, B> delegate;

   public static <B> ImmutableTypeToInstanceMap<B> of() {
      return new ImmutableTypeToInstanceMap(ImmutableMap.of());
   }

   public static <B> ImmutableTypeToInstanceMap.Builder<B> builder() {
      return new ImmutableTypeToInstanceMap.Builder();
   }

   private ImmutableTypeToInstanceMap(ImmutableMap<TypeToken<? extends B>, B> delegate) {
      this.delegate = delegate;
   }

   public <T extends B> T getInstance(TypeToken<T> type) {
      return this.trustedGet(type.rejectTypeVariables());
   }

   public <T extends B> T putInstance(TypeToken<T> type, T value) {
      throw new UnsupportedOperationException();
   }

   public <T extends B> T getInstance(Class<T> type) {
      return this.trustedGet(TypeToken.of(type));
   }

   public <T extends B> T putInstance(Class<T> type, T value) {
      throw new UnsupportedOperationException();
   }

   protected Map<TypeToken<? extends B>, B> delegate() {
      return this.delegate;
   }

   private <T extends B> T trustedGet(TypeToken<T> type) {
      return this.delegate.get(type);
   }

   // $FF: synthetic method
   ImmutableTypeToInstanceMap(ImmutableMap x0, Object x1) {
      this(x0);
   }

   @Beta
   public static final class Builder<B> {
      private final ImmutableMap.Builder<TypeToken<? extends B>, B> mapBuilder;

      private Builder() {
         this.mapBuilder = ImmutableMap.builder();
      }

      public <T extends B> ImmutableTypeToInstanceMap.Builder<B> put(Class<T> key, T value) {
         this.mapBuilder.put(TypeToken.of(key), value);
         return this;
      }

      public <T extends B> ImmutableTypeToInstanceMap.Builder<B> put(TypeToken<T> key, T value) {
         this.mapBuilder.put(key.rejectTypeVariables(), value);
         return this;
      }

      public ImmutableTypeToInstanceMap<B> build() {
         return new ImmutableTypeToInstanceMap(this.mapBuilder.build());
      }

      // $FF: synthetic method
      Builder(Object x0) {
         this();
      }
   }
}
