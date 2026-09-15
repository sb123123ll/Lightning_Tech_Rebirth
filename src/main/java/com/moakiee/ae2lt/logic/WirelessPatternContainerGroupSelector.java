package com.moakiee.ae2lt.logic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;

public final class WirelessPatternContainerGroupSelector {
   private WirelessPatternContainerGroupSelector() {
   }

   public static <T> Optional<T> selectMostFrequent(List<T> groups) {
      if (groups.isEmpty()) {
         return Optional.empty();
      } else {
         LinkedHashMap<T, Integer> counts = new LinkedHashMap<>();

         for (T group : groups) {
            counts.merge(group, Integer.valueOf(1), Integer::sum);
         }

         T best = null;
         int bestCount = 0;

         for (Entry<T, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
               best = entry.getKey();
               bestCount = entry.getValue();
            }
         }

         return Optional.ofNullable(best);
      }
   }
}
