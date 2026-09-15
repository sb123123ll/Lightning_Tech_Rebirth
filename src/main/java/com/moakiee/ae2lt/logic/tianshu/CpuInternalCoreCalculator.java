package com.moakiee.ae2lt.logic.tianshu;

import com.moakiee.ae2lt.logic.compute.ComputingUnitTotals;
import com.moakiee.ae2lt.logic.compute.CraftingComputeEnvelope;
import com.moakiee.ae2lt.logic.compute.UnifiedCraftingComputeCalculator;

public final class CpuInternalCoreCalculator {
   public static final long STORAGE_PER_UNIT = 67108864L;
   public static final int PARALLEL_PER_UNIT = 128;
   public static final int PARALLEL_CAP = 16384;

   public static CpuInternalCoreProfile calculate(CpuMainCoreTier mainCore, int storageUnitCount, int parallelUnitCount) {
      return calculate(mainCore, storageUnitCount, parallelUnitCount, 0);
   }

   public static CpuInternalCoreProfile calculate(CpuMainCoreTier mainCore, int storageUnitCount, int parallelUnitCount, int amplifierUnitCount) {
      if (mainCore == null) {
         throw new IllegalArgumentException("Main core is required");
      } else if (storageUnitCount >= 0 && parallelUnitCount >= 0 && amplifierUnitCount >= 0 && storageUnitCount + parallelUnitCount + amplifierUnitCount <= 26) {
         ComputingUnitTotals units = new ComputingUnitTotals(parallelUnitCount, amplifierUnitCount, storageUnitCount, 0);
         CraftingComputeEnvelope envelope = UnifiedCraftingComputeCalculator.cpuEnvelope(mainCore.computeTier(), units);
         return new CpuInternalCoreProfile(
            mainCore,
            storageUnitCount,
            parallelUnitCount,
            amplifierUnitCount,
            envelope.storageBytes(),
            envelope.successfulDispatchesPerTick(),
            envelope.maxCopiesPerTick(),
            envelope.unboundedBatch(),
            envelope.dispatchCapped()
         );
      } else {
         throw new IllegalArgumentException("Peripheral unit count must be between 0 and 26");
      }
   }

   static long saturatingMultiply(long left, long right) {
      return UnifiedCraftingComputeCalculator.saturatedMultiply(left, right);
   }

   private CpuInternalCoreCalculator() {
   }
}
