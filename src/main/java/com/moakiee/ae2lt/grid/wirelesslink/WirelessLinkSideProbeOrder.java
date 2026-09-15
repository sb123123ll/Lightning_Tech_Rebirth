package com.moakiee.ae2lt.grid.wirelesslink;

import java.util.ArrayList;
import java.util.List;

public final class WirelessLinkSideProbeOrder {
   private static final List<String> ALL_SIDES = List.of("down", "up", "north", "south", "west", "east");

   private WirelessLinkSideProbeOrder() {
   }

   public static List<String> forPreferredSide(String preferredSideName) {
      if (preferredSideName != null && !preferredSideName.isBlank()) {
         ArrayList<String> result = new ArrayList<>(ALL_SIDES.size());
         if (ALL_SIDES.contains(preferredSideName)) {
            result.add(preferredSideName);
         }

         for (String side : ALL_SIDES) {
            if (!side.equals(preferredSideName)) {
               result.add(side);
            }
         }

         return result;
      } else {
         return ALL_SIDES;
      }
   }
}
