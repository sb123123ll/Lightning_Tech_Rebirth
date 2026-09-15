package com.moakiee.ae2lt.logic.craft;

public interface MatrixCraftingEnergy {
   MatrixCraftingEnergy UNLIMITED = new MatrixCraftingEnergy() {
      @Override
      public long affordableOperations(long requestedOperations) {
         return Math.max(0L, requestedOperations);
      }

      @Override
      public void consumeOperations(long acceptedOperations) {
      }
   };

   long affordableOperations(long var1);

   void consumeOperations(long var1);
}
