package com.moakiee.ae2lt.api.patternprovider;

import java.util.function.IntSupplier;

public final class WirelessPatternProviderPolicy {
   private static volatile IntSupplier maxDistanceSupplier = () -> 0;

   private WirelessPatternProviderPolicy() {
   }

   public static void setMaxDistanceSupplier(IntSupplier supplier) {
      if (supplier == null) {
         throw new IllegalArgumentException("supplier must not be null");
      } else {
         maxDistanceSupplier = supplier;
      }
   }

   public static int maxDistance() {
      return Math.max(0, maxDistanceSupplier.getAsInt());
   }
}
