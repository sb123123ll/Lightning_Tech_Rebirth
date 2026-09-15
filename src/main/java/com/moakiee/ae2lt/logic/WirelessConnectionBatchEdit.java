package com.moakiee.ae2lt.logic;

import java.util.List;
import java.util.function.Function;

@Deprecated(
   forRemoval = false
)
public final class WirelessConnectionBatchEdit {
   private WirelessConnectionBatchEdit() {
   }

   public static <T, C, D, F> WirelessConnectionBatchEdit.Plan<T> planSingleFacePerTarget(
      Iterable<T> targets, D dimension, Iterable<C> connections, F face, Function<C, D> dimensionGetter, Function<C, T> posGetter, Function<C, F> faceGetter
   ) {
      return wrap(
         com.moakiee.ae2lt.logic.wireless.support.WirelessConnectionBatchEdit.planSingleFacePerTarget(
            targets, dimension, connections, face, dimensionGetter, posGetter, faceGetter
         )
      );
   }

   public static <T, C, D, F> WirelessConnectionBatchEdit.Plan<T> planMultiFacePerTarget(
      Iterable<T> targets, D dimension, Iterable<C> connections, F face, Function<C, D> dimensionGetter, Function<C, T> posGetter, Function<C, F> faceGetter
   ) {
      return wrap(
         com.moakiee.ae2lt.logic.wireless.support.WirelessConnectionBatchEdit.planMultiFacePerTarget(
            targets, dimension, connections, face, dimensionGetter, posGetter, faceGetter
         )
      );
   }

   private static <T> WirelessConnectionBatchEdit.Plan<T> wrap(com.moakiee.ae2lt.logic.wireless.support.WirelessConnectionBatchEdit.Plan<T> plan) {
      return new WirelessConnectionBatchEdit.Plan<>(plan.deselecting(), plan.disconnect(), plan.update(), plan.connect());
   }

   public static record Plan<T>(boolean deselecting, List<T> disconnect, List<T> update, List<T> connect) {
      public boolean hasChanges() {
         return !this.disconnect.isEmpty() || !this.update.isEmpty() || !this.connect.isEmpty();
      }
   }
}
