package com.moakiee.ae2lt.item.railgun;

public final class RailgunEnergyRules {
   public static final long CHARGE_COST_LV_PER_TICK_FE = 1000L;
   public static final long CHARGE_COST_MV_PER_TICK_FE = 4000L;
   public static final long CHARGE_COST_HV_PER_TICK_FE = 10000L;
   public static final long OVERLOAD_EXECUTION_COST_FE = 20000000L;

   private RailgunEnergyRules() {
   }

   public static long chargeCostPerTickFe(RailgunChargeTier tier) {
      return switch (tier) {
         case EHV1 -> 1000L;
         case EHV2 -> 4000L;
         case EHV3 -> 10000L;
         default -> 0L;
      };
   }

   public static long overloadExecutionCostFe() {
      return 20000000L;
   }

   public static int receivableFe(long stored, long capacity, int requested) {
      if (requested > 0 && capacity > 0L && stored < capacity) {
         long room = capacity - Math.max(0L, stored);
         return (int)Math.min(2147483647L, Math.min(room, (long)requested));
      } else {
         return 0;
      }
   }
}
