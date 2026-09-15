package com.moakiee.ae2lt.machine.lightningchamber.recipe;

import com.moakiee.ae2lt.machine.lightningchamber.LightningSimulationChamberInventory;
import java.util.Arrays;
import net.minecraft.world.item.ItemStack;

public final class LightningSimulationRecipeMatch {
   private final int[] inputConsumptions;

   public LightningSimulationRecipeMatch(int[] inputConsumptions) {
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

   public boolean canFitResult(LightningSimulationChamberInventory inventory, ItemStack result) {
      return inventory.canAcceptRecipeOutput(result);
   }
}
