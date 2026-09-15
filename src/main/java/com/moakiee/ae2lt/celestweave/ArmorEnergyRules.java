package com.moakiee.ae2lt.celestweave;

public final class ArmorEnergyRules {
   public static final long BASE_CAPACITY_FE = 10000000L;
   public static final long MODULE_T1_CAPACITY_FE = 1000000000L;
   public static final long MODULE_T2_CAPACITY_FE = 5000000000L;
   public static final long MODULE_T3_CAPACITY_FE = 20000000000L;
   public static final long MODULE_T1_LEGACY_CAPACITY_FE = 100000000L;
   public static final long MODULE_T2_LEGACY_CAPACITY_FE = 500000000L;
   public static final long MODULE_T3_LEGACY_CAPACITY_FE = 2000000000L;

   private ArmorEnergyRules() {
   }

   public static long capacityForExtraModuleFe(long extraModuleFe) {
      long module = Math.max(0L, extraModuleFe);
      return module > 0L ? module : 10000000L;
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
