package com.moakiee.ae2lt.machine.overloadfactory.recipe;

import java.util.Arrays;

public final class OverloadProcessingRecipeMatch {
   private final int[] inputConsumptions;

   public OverloadProcessingRecipeMatch(int[] inputConsumptions) {
      if (inputConsumptions.length != 9) {
         throw new IllegalArgumentException("inputConsumptions must have length 9");
      } else {
         this.inputConsumptions = Arrays.copyOf(inputConsumptions, inputConsumptions.length);
      }
   }

   public int getConsumptionForSlot(int slot) {
      if (slot >= 0 && slot <= 8) {
         return this.inputConsumptions[slot];
      } else {
         throw new IllegalArgumentException("slot must be an input slot");
      }
   }

   public int[] inputConsumptions() {
      return Arrays.copyOf(this.inputConsumptions, this.inputConsumptions.length);
   }
}
