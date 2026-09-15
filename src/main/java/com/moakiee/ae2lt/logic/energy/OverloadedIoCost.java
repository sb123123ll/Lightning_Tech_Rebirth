package com.moakiee.ae2lt.logic.energy;

final class OverloadedIoCost {
   static final long ITEMS_PER_OPERATION = 4L;
   static final long FLUID_PER_OPERATION = 500L;

   private OverloadedIoCost() {
   }

   static double cost(long amount, long amountPerOperation) {
      if (amount <= 0L) {
         return 0.0;
      } else {
         long perOperation = Math.max(1L, amountPerOperation);
         return (double)((amount + perOperation - 1L) / perOperation);
      }
   }

   static long amountForOperations(long requested, long amountPerOperation, long operations) {
      if (requested > 0L && operations > 0L) {
         long perOperation = Math.max(1L, amountPerOperation);

         long affordable;
         try {
            affordable = Math.multiplyExact(operations, perOperation);
         } catch (ArithmeticException var11) {
            affordable = Long.MAX_VALUE;
         }

         return Math.min(requested, affordable);
      } else {
         return 0L;
      }
   }
}
