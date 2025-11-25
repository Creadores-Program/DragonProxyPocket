package com.google.common.util.concurrent;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;

@GwtCompatible
abstract class CollectionFuture<V, C> extends AggregateFuture<V, C> {
   abstract class CollectionFutureRunningState extends AggregateFuture<V, C>.RunningState {
      private List<Optional<V>> values;

      CollectionFutureRunningState(ImmutableCollection<? extends ListenableFuture<? extends V>> futures, boolean allMustSucceed) {
         super(futures, allMustSucceed, true);
         this.values = (List)(futures.isEmpty() ? ImmutableList.of() : Lists.newArrayListWithCapacity(futures.size()));

         for(int i = 0; i < futures.size(); ++i) {
            this.values.add((Object)null);
         }

      }

      final void collectOneValue(boolean allMustSucceed, int index, @Nullable V returnValue) {
         List<Optional<V>> localValues = this.values;
         if (localValues != null) {
            localValues.set(index, Optional.fromNullable(returnValue));
         } else {
            Preconditions.checkState(allMustSucceed || CollectionFuture.this.isCancelled(), "Future was done before all dependencies completed");
         }

      }

      final void handleAllCompleted() {
         List<Optional<V>> localValues = this.values;
         if (localValues != null) {
            CollectionFuture.this.set(this.combine(localValues));
         } else {
            Preconditions.checkState(CollectionFuture.this.isDone());
         }

      }

      void releaseResourcesAfterFailure() {
         super.releaseResourcesAfterFailure();
         this.values = null;
      }

      abstract C combine(List<Optional<V>> var1);
   }
}
