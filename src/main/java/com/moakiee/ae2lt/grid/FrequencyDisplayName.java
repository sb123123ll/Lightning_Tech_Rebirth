package com.moakiee.ae2lt.grid;

import org.jetbrains.annotations.Nullable;

public final class FrequencyDisplayName {
   private FrequencyDisplayName() {
   }

   public static String of(int frequencyId, @Nullable String configuredName) {
      return configuredName != null && !configuredName.isBlank() ? configuredName : "#" + frequencyId;
   }
}
