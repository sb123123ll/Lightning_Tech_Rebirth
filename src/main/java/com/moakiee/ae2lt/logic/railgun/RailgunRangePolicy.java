package com.moakiee.ae2lt.logic.railgun;

import com.moakiee.ae2lt.device.capability.DeviceCapability;
import java.util.List;

public final class RailgunRangePolicy {
   private RailgunRangePolicy() {
   }

   public static double effectiveRange(double baseRange, List<DeviceCapability> capabilities) {
      double multiplier = 1.0;
      if (capabilities != null) {
         for (DeviceCapability capability : capabilities) {
            if (capability instanceof DeviceCapability.RangeMultiplier range) {
               multiplier = Math.min(4.0, multiplier * range.factor());
            }
         }
      }

      return Math.max(0.0, baseRange) * multiplier;
   }
}
