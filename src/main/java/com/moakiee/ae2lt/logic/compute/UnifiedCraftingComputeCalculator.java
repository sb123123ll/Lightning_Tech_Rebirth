package com.moakiee.ae2lt.logic.compute;

public final class UnifiedCraftingComputeCalculator {
   public static final long STORAGE_PER_UNIT = 67108864L;
   public static final int DISPATCH_PER_UNIT = 128;

   private UnifiedCraftingComputeCalculator() {
   }

   public static long rawDispatch(ComputingUnitTotals units) {
      if (units == null) {
         throw new IllegalArgumentException("Units are required");
      } else {
         return saturatedMultiply(128L, (long)units.dispatchUnits());
      }
   }

   public static long rawExternalStorage(ComputingUnitTotals units) {
      if (units == null) {
         throw new IllegalArgumentException("Units are required");
      } else {
         return saturatedMultiply(67108864L, (long)units.storageUnits());
      }
   }

   public static CraftingComputeEnvelope cpuEnvelope(ComputeTier tier, ComputingUnitTotals units) {
      validate(tier, units, true);
      if (tier.multidimensional()) {
         return new CraftingComputeEnvelope(Long.MAX_VALUE, tier.dispatchCap(), Long.MAX_VALUE, true, false);
      } else {
         long dispatchGain = dispatchGain(tier, units.amplifierUnits());
         long rawDispatch = saturatedMultiply(rawDispatch(units), dispatchGain);
         int successfulDispatches = (int)Math.min(rawDispatch, (long)tier.dispatchCap());
         boolean dispatchCapped = rawDispatch >= (long)tier.dispatchCap();
         long externalStorage = rawExternalStorage(units);
         long storage = saturatedAdd(tier.internalStorage(), saturatedMultiply(externalStorage, storageGain(tier, units.amplifierUnits())));
         int copyGain = copyGain(tier, units.amplifierUnits());
         long copies = Math.min(saturatedMultiply((long)successfulDispatches, (long)copyGain), tier.copyCap());
         return new CraftingComputeEnvelope(storage, successfulDispatches, copies, false, dispatchCapped);
      }
   }

   public static MatrixComputeEnvelope matrixEnvelope(ComputeTier tier, ComputingUnitTotals units, double thermalEfficiency) {
      return matrixEnvelope(tier, units, thermalEfficiency, tier == null ? 0L : tier.copyCap());
   }

   public static MatrixComputeEnvelope matrixEnvelope(ComputeTier tier, ComputingUnitTotals units, double thermalEfficiency, long operationCap) {
      validate(tier, units, false);
      if (tier.multidimensional()) {
         return new MatrixComputeEnvelope(Long.MAX_VALUE, 16384, 1.0, true);
      } else {
         long baseOperations = saturatedMultiply(
            saturatedMultiply(rawDispatch(units), dispatchGain(tier, units.amplifierUnits())), (long)copyGain(tier, units.amplifierUnits())
         );
         baseOperations = Math.min(baseOperations, Math.max(0L, operationCap));
         long operations = floorSaturated((double)baseOperations * sanitizeEfficiency(thermalEfficiency));
         return new MatrixComputeEnvelope(operations, 16384, sanitizeEfficiency(thermalEfficiency), false);
      }
   }

   public static long dispatchGain(ComputeTier tier, int amplifierUnits) {
      validateAmplifiers(tier, amplifierUnits);
      if (tier == ComputeTier.BASELINE) {
         return 1L;
      } else {
         return tier.multidimensional() ? Long.MAX_VALUE : 2L * (1L + (long)amplifierUnits);
      }
   }

   public static long storageGain(ComputeTier tier, int amplifierUnits) {
      long dispatchGain = dispatchGain(tier, amplifierUnits);
      return tier == ComputeTier.OVERLOAD ? saturatedMultiply(dispatchGain, dispatchGain) : dispatchGain;
   }

   public static int copyGain(ComputeTier tier, int amplifierUnits) {
      validateAmplifiers(tier, amplifierUnits);
      int width = 1 + amplifierUnits;

      return switch (tier) {
         case BASELINE -> 2;
         case QUANTUM -> width;
         case OVERLOAD -> width * width;
         case MULTIDIMENSIONAL -> Integer.MAX_VALUE;
      };
   }

   public static long saturatedAdd(long left, long right) {
      return left >= 0L && right >= 0L && left <= Long.MAX_VALUE - right ? left + right : Long.MAX_VALUE;
   }

   public static long saturatedMultiply(long left, long right) {
      if (left <= 0L || right <= 0L) {
         return 0L;
      } else {
         return left > Long.MAX_VALUE / right ? Long.MAX_VALUE : left * right;
      }
   }

   private static void validate(ComputeTier tier, ComputingUnitTotals units, boolean cpu) {
      if (tier != null && units != null) {
         validateAmplifiers(tier, units.amplifierUnits());
         if (!tier.multidimensional() && units.dispatchUnits() <= 0) {
            throw new IllegalArgumentException("A finite compute structure requires at least one dispatch unit");
         } else if (cpu && units.coolingUnits() > 0) {
            throw new IllegalArgumentException("A crafting CPU cannot use cooling units");
         } else if (!cpu && units.storageUnits() > 0) {
            throw new IllegalArgumentException("A crafting matrix cannot use storage units");
         } else if (tier.multidimensional()
            && (units.dispatchUnits() > 0 || units.amplifierUnits() > 0 || units.storageUnits() > 0 || units.coolingUnits() > 0)) {
            throw new IllegalArgumentException("Multidimensional structures use only their main core budget");
         }
      } else {
         throw new IllegalArgumentException("Tier and units are required");
      }
   }

   private static void validateAmplifiers(ComputeTier tier, int amplifierUnits) {
      if (tier == null) {
         throw new IllegalArgumentException("Tier is required");
      } else if (amplifierUnits < 0 || amplifierUnits > tier.maxAmplifierUnits()) {
         throw new IllegalArgumentException("Amplifier unit count is invalid for " + tier);
      }
   }

   private static double sanitizeEfficiency(double efficiency) {
      return Double.isFinite(efficiency) && !(efficiency <= 0.0) ? Math.min(1.0, efficiency) : 0.0;
   }

   private static long floorSaturated(double value) {
      if (!Double.isFinite(value)) {
         return value > 0.0 ? Long.MAX_VALUE : 0L;
      } else if (value <= 0.0) {
         return 0L;
      } else {
         return value >= 9.223372E18F ? Long.MAX_VALUE : (long)Math.floor(value);
      }
   }
}
