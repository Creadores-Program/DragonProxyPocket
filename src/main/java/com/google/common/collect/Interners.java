package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.util.concurrent.ConcurrentMap;

@Beta
public final class Interners {
   private Interners() {
   }

   public static <E> Interner<E> newStrongInterner() {
      final ConcurrentMap<E, E> map = (new MapMaker()).makeMap();
      return new Interner<E>() {
         public E intern(E sample) {
            E canonical = map.putIfAbsent(Preconditions.checkNotNull(sample), sample);
            return canonical == null ? sample : canonical;
         }
      };
   }

   @GwtIncompatible("java.lang.ref.WeakReference")
   public static <E> Interner<E> newWeakInterner() {
      return new Interners.WeakInterner();
   }

   public static <E> Function<E, E> asFunction(Interner<E> interner) {
      return new Interners.InternerFunction((Interner)Preconditions.checkNotNull(interner));
   }

   private static class InternerFunction<E> implements Function<E, E> {
      private final Interner<E> interner;

      public InternerFunction(Interner<E> interner) {
         this.interner = interner;
      }

      public E apply(E input) {
         return this.interner.intern(input);
      }

      public int hashCode() {
         return this.interner.hashCode();
      }

      public boolean equals(Object other) {
         if (other instanceof Interners.InternerFunction) {
            Interners.InternerFunction<?> that = (Interners.InternerFunction)other;
            return this.interner.equals(that.interner);
         } else {
            return false;
         }
      }
   }

   private static class WeakInterner<E> implements Interner<E> {
      private final MapMakerInternalMap<E, Interners.WeakInterner.Dummy> map;

      private WeakInterner() {
         this.map = (new MapMaker()).weakKeys().keyEquivalence(Equivalence.equals()).makeCustomMap();
      }

      public E intern(E sample) {
         Interners.WeakInterner.Dummy sneaky;
         do {
            MapMakerInternalMap.ReferenceEntry<E, Interners.WeakInterner.Dummy> entry = this.map.getEntry(sample);
            if (entry != null) {
               E canonical = entry.getKey();
               if (canonical != null) {
                  return canonical;
               }
            }

            sneaky = (Interners.WeakInterner.Dummy)this.map.putIfAbsent(sample, Interners.WeakInterner.Dummy.VALUE);
         } while(sneaky != null);

         return sample;
      }

      // $FF: synthetic method
      WeakInterner(Object x0) {
         this();
      }

      private static enum Dummy {
         VALUE;
      }
   }
}
