package com.moakiee.ae2lt.logic;

import org.jetbrains.annotations.Nullable;

final class ReturnedCraftingUnlock {
   private ReturnedCraftingUnlock() {
   }

   static ReturnedCraftingUnlock.Result resolveMatchedAmount(boolean matches, long unlockAmount, long returnedAmount) {
      if (matches && unlockAmount > 0L && returnedAmount > 0L) {
         long remaining = unlockAmount - returnedAmount;
         return remaining <= 0L ? ReturnedCraftingUnlock.Result.resetLock() : ReturnedCraftingUnlock.Result.remaining(remaining);
      } else {
         return ReturnedCraftingUnlock.Result.noMatch();
      }
   }

   static record Result(boolean matched, @Nullable Long remainingAmount) {
      private static ReturnedCraftingUnlock.Result noMatch() {
         return new ReturnedCraftingUnlock.Result(false, null);
      }

      private static ReturnedCraftingUnlock.Result resetLock() {
         return new ReturnedCraftingUnlock.Result(true, null);
      }

      private static ReturnedCraftingUnlock.Result remaining(long remainingAmount) {
         return new ReturnedCraftingUnlock.Result(true, remainingAmount);
      }

      boolean shouldResetLock() {
         return this.matched && this.remainingAmount == null;
      }
   }
}
