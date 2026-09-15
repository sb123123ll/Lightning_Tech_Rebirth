package com.moakiee.ae2lt.device.energy;

import com.moakiee.ae2lt.device.capability.DeviceCapability;

public final class LightningCompensationPolicy {
   public static final int DEFAULT_HIGH_VOLTAGE_PER_EXTREME_HIGH_VOLTAGE = 16;

   private LightningCompensationPolicy() {
   }

   public static int bestRatio(Iterable<DeviceCapability> capabilities) {
      int best = 0;
      if (capabilities == null) {
         return best;
      } else {
         for (DeviceCapability capability : capabilities) {
            if (capability instanceof DeviceCapability.LightningCompensation) {
               DeviceCapability.LightningCompensation compensation = (DeviceCapability.LightningCompensation)capability;
               int ratio = compensation.highVoltagePerExtremeHighVoltage();
               if (ratio > 0 && (best == 0 || ratio < best)) {
                  best = ratio;
               }
            }
         }

         return best;
      }
   }

   public static LightningCompensationPolicy.Plan plan(
      long requiredHighVoltage, long requiredExtremeHighVoltage, long availableHighVoltage, long availableExtremeHighVoltage, int compensationRatio
   ) {
      long requiredHv = Math.max(0L, requiredHighVoltage);
      long requiredEhv = Math.max(0L, requiredExtremeHighVoltage);
      long availableHv = Math.max(0L, availableHighVoltage);
      long availableEhv = Math.max(0L, availableExtremeHighVoltage);
      long ehvToConsume = Math.min(requiredEhv, availableEhv);
      long missingEhv = requiredEhv - ehvToConsume;
      if (missingEhv > 0L && compensationRatio <= 0) {
         return new LightningCompensationPolicy.Plan(false, requiredHv, requiredEhv, 0L);
      } else {
         boolean multiplyOverflow = missingEhv > 0L && missingEhv > Long.MAX_VALUE / (long)compensationRatio;
         long compensationHv = multiplyOverflow ? Long.MAX_VALUE : missingEhv * (long)compensationRatio;
         boolean addOverflow = requiredHv > Long.MAX_VALUE - compensationHv;
         long hvToConsume = addOverflow ? Long.MAX_VALUE : requiredHv + compensationHv;
         if (!multiplyOverflow && !addOverflow) {
            boolean canPay = availableHv >= hvToConsume;
            return new LightningCompensationPolicy.Plan(canPay, hvToConsume, ehvToConsume, compensationHv);
         } else {
            return new LightningCompensationPolicy.Plan(false, hvToConsume, ehvToConsume, compensationHv);
         }
      }
   }

   public static long highVoltageRequired(long missingExtremeHighVoltage, int compensationRatio) {
      if (missingExtremeHighVoltage <= 0L) {
         return 0L;
      } else {
         return compensationRatio <= 0 ? Long.MAX_VALUE : saturatingMultiply(missingExtremeHighVoltage, (long)compensationRatio);
      }
   }

   private static long saturatingMultiply(long value, long multiplier) {
      if (value > 0L && multiplier > 0L) {
         return value > Long.MAX_VALUE / multiplier ? Long.MAX_VALUE : value * multiplier;
      } else {
         return 0L;
      }
   }

   public static record Plan(boolean canPay, long highVoltageToConsume, long extremeHighVoltageToConsume, long compensatedHighVoltage) {
   }
}
