package com.moakiee.ae2lt.item.railgun;

public enum RailgunChargeTier {
   HV,
   EHV1,
   EHV2,
   EHV3;

   public boolean isMax() {
      return this == EHV3;
   }

   public static RailgunChargeTier fromTicks(long charged, int t1Ticks, int t2Ticks, int t3Ticks) {
      if (charged >= (long)t3Ticks) {
         return EHV3;
      } else if (charged >= (long)t2Ticks) {
         return EHV2;
      } else {
         return charged >= (long)t1Ticks ? EHV1 : HV;
      }
   }
}
