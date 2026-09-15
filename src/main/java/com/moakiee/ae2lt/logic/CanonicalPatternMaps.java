package com.moakiee.ae2lt.logic;

import appeng.api.crafting.IPatternDetails;
import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.util.Map;

final class CanonicalPatternMaps {
   private static final Strategy<IPatternDetails> STRATEGY = new Strategy<IPatternDetails>() {
      public int hashCode(IPatternDetails pattern) {
         return pattern == null ? 0 : pattern.hashCode();
      }

      public boolean equals(IPatternDetails left, IPatternDetails right) {
         return left == right;
      }
   };

   static <V> Map<IPatternDetails, V> create() {
      return new Object2ObjectOpenCustomHashMap(STRATEGY);
   }

   private CanonicalPatternMaps() {
   }
}
