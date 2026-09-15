package com.moakiee.ae2lt.logic;

import appeng.api.crafting.IPatternDetails;
import org.jetbrains.annotations.Nullable;

final class BatchBlockingPolicy {
   static boolean isBlocked(
      boolean craftingLocked,
      boolean blockingEnabled,
      boolean targetContainsPatternInput,
      boolean samePatternBlockingEnabled,
      @Nullable IPatternDetails lastSuccessfulPattern,
      IPatternDetails currentPattern
   ) {
      if (craftingLocked) {
         return true;
      } else {
         return blockingEnabled && targetContainsPatternInput ? !samePatternBlockingEnabled || !samePattern(lastSuccessfulPattern, currentPattern) : false;
      }
   }

   static boolean samePattern(@Nullable IPatternDetails previous, IPatternDetails current) {
      return previous == current;
   }

   private BatchBlockingPolicy() {
   }
}
