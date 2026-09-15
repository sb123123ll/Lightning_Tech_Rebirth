package com.moakiee.ae2lt.machine.firmament.recipe;

import com.moakiee.ae2lt.machine.firmament.FirmamentConversionInventory;
import java.util.Arrays;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public final class FirmamentConversionRecipeMatch {
   private final int[] inputConsumptions;

   public FirmamentConversionRecipeMatch(int[] inputConsumptions) {
      if (inputConsumptions.length != 3) {
         throw new IllegalArgumentException("inputConsumptions must have length 3");
      } else {
         this.inputConsumptions = Arrays.copyOf(inputConsumptions, inputConsumptions.length);
      }
   }

   public int getConsumptionForSlot(int slot) {
      if (slot >= 0 && slot <= 2) {
         return this.inputConsumptions[slot];
      } else {
         throw new IllegalArgumentException("slot must be one of the three input slots");
      }
   }

   public int[] inputConsumptions() {
      return Arrays.copyOf(this.inputConsumptions, this.inputConsumptions.length);
   }

   public boolean canFitResult(FirmamentConversionInventory inventory, ItemStack result) {
      return inventory.canAcceptRecipeOutput(result);
   }

   public boolean canFitResults(FirmamentConversionInventory inventory, List<ItemStack> results) {
      return inventory.canAcceptRecipeOutputs(results);
   }
}
