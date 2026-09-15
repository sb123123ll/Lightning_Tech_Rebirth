package com.moakiee.ae2lt.machine.lightningassembly.recipe;

import com.moakiee.ae2lt.machine.lightningassembly.LightningAssemblyChamberInventory;
import java.util.Arrays;
import net.minecraft.world.item.ItemStack;

public final class LightningAssemblyRecipeMatch {
   private final int[] inputConsumptions;

   public LightningAssemblyRecipeMatch(int[] inputConsumptions) {
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
         throw new IllegalArgumentException("slot must be one of the nine input slots");
      }
   }

   public int[] inputConsumptions() {
      return Arrays.copyOf(this.inputConsumptions, this.inputConsumptions.length);
   }

   public boolean canFitResult(LightningAssemblyChamberInventory inventory, ItemStack result) {
      return inventory.canAcceptRecipeOutput(result);
   }
}
