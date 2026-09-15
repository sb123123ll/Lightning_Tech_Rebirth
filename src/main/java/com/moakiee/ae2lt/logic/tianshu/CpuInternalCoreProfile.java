package com.moakiee.ae2lt.logic.tianshu;

public record CpuInternalCoreProfile(
   CpuMainCoreTier mainCore,
   int storageUnitCount,
   int parallelUnitCount,
   int amplifierUnitCount,
   long storageBytes,
   int successfulDispatchesPerTick,
   long maxCopiesPerTick,
   boolean unboundedBatch,
   boolean parallelCapped
) {
   public static CpuInternalCoreProfile empty() {
      return new CpuInternalCoreProfile(null, 0, 0, 0, 0L, 0, 0L, false, false);
   }

   public int parallelism() {
      return this.successfulDispatchesPerTick;
   }

   public int coProcessors() {
      return Math.max(0, this.successfulDispatchesPerTick - 1);
   }
}
